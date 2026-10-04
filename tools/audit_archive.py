#!/usr/bin/env python3
"""Lossless audit of the supplied ZIP. Never modifies the input archive.

Keeps every entry under an index-based filename and records the raw ZIP name.
Decoded names are metadata, not permission to overwrite colliding entries.
"""
from __future__ import annotations
import argparse
import collections
import hashlib
import json
from pathlib import Path, PurePosixPath
import re
import struct
import unicodedata
import zipfile


def sha256(data):
    return hashlib.sha256(data).hexdigest()


def reverse_legacy(text):
    """Reverse CP1251/1252 mojibake, retaining strict byte ambiguity checks."""
    result = bytearray()
    families = set()
    for char in unicodedata.normalize('NFC', text):
        if ord(char) < 128 or 128 <= ord(char) < 160:
            result.append(ord(char))
            continue
        candidates = {}
        for codec in ('cp1252', 'cp1251'):
            try:
                value = char.encode(codec)
            except UnicodeEncodeError:
                continue
            candidates.setdefault(value, []).append(codec)
        if len(candidates) != 1:
            raise ValueError(f'ambiguous or unknown character {char!r}: {candidates}')
        value, codecs = next(iter(candidates.items()))
        result.extend(value)
        if len(codecs) == 1:
            families.add(codecs[0])
    return bytes(result), sorted(families)


def decode_name(raw, flags):
    if flags & 0x800:
        return raw.decode('utf8'), 'zip_utf8', None
    # This archive contains UTF-8 encoded *mojibake* without ZIP's UTF-8 flag.
    unicode_mojibake = raw.decode('utf8')
    legacy, codecs = reverse_legacy(unicode_mojibake)
    return legacy.decode('cp932'), 'utf8/NFC/reverse_' + '+'.join(codecs or ['ASCII']) + '/cp932', legacy.hex()


def read_pe(data):
    off = struct.unpack_from('<I', data, 0x3c)[0]
    if data[off:off+4] != b'PE\0\0':
        raise ValueError('invalid PE signature')
    machine, count, timestamp = struct.unpack_from('<HHI', data, off+4)
    optional_size = struct.unpack_from('<H', data, off+20)[0]
    optional = off+24
    magic = struct.unpack_from('<H', data, optional)[0]
    sections = []
    for n in range(count):
        p = optional+optional_size+n*40
        name = data[p:p+8].split(b'\0')[0].decode('ascii')
        virtual_size, rva, raw_size, raw_off = struct.unpack_from('<IIII', data, p+8)
        sections.append(dict(name=name, rva=rva, virtual_size=virtual_size, raw_size=raw_size, raw_offset=raw_off))
    def fileoff(rva):
        for s in sections:
            if s['rva'] <= rva < s['rva']+max(s['virtual_size'],s['raw_size']):
                return s['raw_offset']+rva-s['rva']
        raise ValueError(f'unmapped RVA {rva:#x}')
    def cstr(p):
        return data[p:data.index(b'\0',p)].decode('ascii')
    if magic != 0x10b:
        raise ValueError('audit currently expects PE32')
    imports = {}
    imp_rva = struct.unpack_from('<I',data,optional+104)[0]
    if imp_rva:
        p = fileoff(imp_rva)
        while any(data[p:p+20]):
            oft, _, _, name_rva, ft = struct.unpack_from('<IIIII',data,p)
            dll = cstr(fileoff(name_rva)); values=[]
            thunk=fileoff(oft or ft)
            while (value:=struct.unpack_from('<I',data,thunk)[0]):
                values.append(f'ordinal:{value & 0xffff}' if value & 0x80000000 else cstr(fileoff(value)+2))
                thunk += 4
            imports[dll]=values; p+=20
    strings=[m.group().decode() for m in re.finditer(rb'[\x20-\x7e]{5,}',data)]
    relevant=[s for s in strings if any(w.lower() in s.lower() for w in ['KGT','version','DirectDraw','DirectSound','Fighter','2D','copyright'])]
    # VS_FIXEDFILEINFO contains fixed numeric version, independent of translation.
    versions=[]
    for m in re.finditer(re.escape(bytes.fromhex('bd04effe')),data):
        if m.start()+52<=len(data):
            v=struct.unpack_from('<13I',data,m.start())
            versions.append(dict(offset=m.start(),struct_version=hex(v[1]),file_version=[v[2]>>16,v[2]&65535,v[3]>>16,v[3]&65535],product_version=[v[4]>>16,v[4]&65535,v[5]>>16,v[5]&65535]))
    return dict(machine=hex(machine),optional_magic=hex(magic),timestamp=timestamp,sections=sections,imports=imports,relevant_ascii_strings=relevant,fixed_versions=versions)


def main():
    ap=argparse.ArgumentParser(); ap.add_argument('archive'); ap.add_argument('--root',default=str(Path(__file__).resolve().parents[1])); args=ap.parse_args()
    root=Path(args.root); out=root/'source_original'/'entries'; out.mkdir(parents=True,exist_ok=True)
    research=root/'research'; research.mkdir(parents=True,exist_ok=True)
    archive=Path(args.archive); before=sha256(archive.read_bytes())
    rows=[]; payload_by_hash={}; by_decoded=collections.defaultdict(list)
    with archive.open('rb') as rawzip, zipfile.ZipFile(archive) as z:
        bad=z.testzip()
        if bad is not None: raise ValueError('CRC failure '+bad)
        for index,e in enumerate(z.infolist()):
            rawzip.seek(e.header_offset); header=rawzip.read(30)
            if header[:4]!=b'PK\x03\x04': raise ValueError('invalid local header')
            nlen,xlen=struct.unpack_from('<HH',header,26); raw=rawzip.read(nlen)
            reconstructed=e.filename.encode('utf8' if e.flag_bits&0x800 else 'cp437')
            if raw!=reconstructed: raise ValueError('local and central names differ')
            b=z.read(e); suffix=PurePosixPath(e.filename).suffix.lower()
            meta=e.filename.startswith('__MACOSX/') or '/.' in e.filename
            stored=f'source_original/entries/{index:04d}{suffix or ".bin"}' if not e.is_dir() else None
            if stored: (root/stored).write_bytes(b)
            try:
                decoded,chain,cp932=decode_name(raw,e.flag_bits); decode_error=None
            except (ValueError,UnicodeError) as exc:
                decoded=chain=cp932=None; decode_error=str(exc)
            title=b[16:80].split(b'\0')[0].decode('cp932') if b[:7]==b'2DKGT2G' else None
            digest=sha256(b)
            row=dict(entry_index=index,raw_zip_name_hex=raw.hex(),zip_display_name=e.filename,decoded_japanese_path=decoded,decode_chain=chain,decoded_cp932_hex=cp932,decode_error=decode_error,is_directory=e.is_dir(),is_metadata=meta,bytes=len(b),crc32=f'{e.CRC:08x}',sha256=digest,stored_path=stored,android_safe_path=f'assets/by_entry/{index:04d}{suffix}' if stored and not meta else None,internal_title=title,signature=b[:16].hex())
            rows.append(row)
            if stored and not meta:
                by_decoded[decoded].append(row); payload_by_hash.setdefault(digest,row)
                if suffix=='.exe':
                    (research/f'pe_{index:04d}.json').write_text(json.dumps(read_pe(b),ensure_ascii=False,indent=2),encoding='utf8')
                if suffix in ('.txt','.ini','.htm'):
                    (research/f'text_{index:04d}{suffix}.utf8.txt').write_text(b.decode('cp932'),encoding='utf8')
    after=sha256(archive.read_bytes())
    if before!=after: raise ValueError('original archive changed')
    conflicts={k:[dict(entry_index=x['entry_index'],sha256=x['sha256'],bytes=x['bytes'],stored_path=x['stored_path']) for x in v] for k,v in by_decoded.items() if len({x['sha256'] for x in v})>1}
    data=dict(archive_name=archive.name,archive_sha256=before,archive_bytes=archive.stat().st_size,entries=rows)
    (research/'filename_manifest.json').write_text(json.dumps(data,ensure_ascii=False,indent=2),encoding='utf8')
    counts=collections.Counter(Path(x['stored_path']).suffix for x in rows if x['stored_path'] and not x['is_metadata'])
    unique=collections.Counter(Path(x['stored_path']).suffix for x in payload_by_hash.values())
    summary=dict(archive_sha256=before,zip_entries=len(rows),content_entries=sum(counts.values()),unique_payloads=len(payload_by_hash),extension_counts=dict(counts),unique_payloads_by_extension=dict(unique),decode_failures=[x['entry_index'] for x in rows if x['decode_error']],name_content_conflicts=conflicts,crc_test='PASS',original_unchanged=True)
    (research/'audit_summary.json').write_text(json.dumps(summary,ensure_ascii=False,indent=2),encoding='utf8')
    print(json.dumps(summary,ensure_ascii=False,indent=2))


if __name__=='__main__': main()
