#!/usr/bin/env python3
"""Lossless instructions and indexed images -> portable EFP1 packs + PNG/audio.

Pixel indices and all eight palettes are retained in a zlib sidecar.
PNG is a cache of palette zero, not a replacement for the source data.
"""
import hashlib
import io
import json
from pathlib import Path
import struct
import sys
import zlib
from PIL import Image
from inspect_containers import parse,unpack_sprite

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'app/src/main/assets/game'

class Writer:
    def __init__(self):self.b=io.BytesIO()
    def i(self,v):self.b.write(struct.pack('>i',v))
    def raw(self,b):self.b.write(b)
    def blob(self,b):self.i(len(b));self.raw(b)
    def s(self,s):self.blob(s.encode('utf8'))
    def ints(self,a):self.i(len(a));[self.i(v) for v in a]

def export(entry):
    source=ROOT/entry['stored_path'];data=source.read_bytes();j=parse(data,source.suffix)
    ident=source.stem;out=OUT/ident;out.mkdir(parents=True,exist_ok=True)
    w=Writer();w.raw(b'EFP1');w.s(j['title']);w.s(source.suffix);w.s(hashlib.sha256(data).hexdigest())
    w.i(len(j['skills']))
    for s in j['skills']:
        w.s(s['name']);w.i(s['block_start']);w.i(s['type'])
    w.blob(b''.join(bytes.fromhex(b) for b in j['blocks_hex']))
    paldata=data[j['sections']['palettes']:][:8*1056]
    w.i(len(j['images']))
    indexed=Writer();indexed.raw(paldata)
    for im in j['images']:
        width,height=im['width'],im['height'];w.i(width);w.i(height)
        if not width or not height:indexed.blob(b'');continue
        pos=im['offset']+20;raw=data[pos:pos+(im['packed_size'] or im['unpacked_size'])]
        if im['packed_size']:raw=unpack_sprite(raw,im['unpacked_size'])[0]
        indexed.blob(raw)
        palette=raw[:1024] if im['palette_type'] else paldata[:1024]
        pixels=raw[1024:] if im['palette_type'] else raw
        # Original 0x40d5b5/0x40c2af: RGB==0 is the transparent palette entry.
        rgb=bytes(c for n in range(256) for c in (palette[n*4+2],palette[n*4+1],palette[n*4]))
        alpha=bytes(255 if any(palette[n*4:n*4+3]) else 0 for n in range(256))
        image=Image.frombytes('P',(width,height),pixels);image.putpalette(rgb)
        image.save(out/f'{im["index"]:04d}.png',transparency=alpha,optimize=False)
    (out/'indexed.zlib').write_bytes(zlib.compress(indexed.b.getvalue(),9))
    w.i(len(j['sounds']))
    for s in j['sounds']:
        raw=data[s['offset']+42:][:s['size']]
        ext='wav' if raw[:4]==b'RIFF' else 'mid' if raw[:4]==b'MThd' else 'bin'
        filename=f'sound{s["index"]:03d}.{ext}' if raw else ''
        w.s(filename);w.i(s['flags'])
        if raw:(out/filename).write_bytes(raw)
    sp=j['specific'];w.ints(sp.get('builtin_skill_indices',[]))
    commands=sp.get('commands',[]);w.i(len(commands))
    for c in commands:
        w.s(c['name']);w.i(c['time_raw']);w.ints(c['skill_indices']);w.blob(bytes.fromhex(c['steps_raw']));w.ints(c['amounts_raw'])
    w.i(len(sp.get('hit_junctions',[])))
    for h in sp.get('hit_junctions',[]):w.i(h.get('hit_skill',0));w.i(h.get('spark_skill',0))
    w.blob(bytes.fromhex(sp.get('player_settings_raw','')))
    w.i(sp.get('bgm_index',0));w.i(sp.get('time_raw',0));w.i(sp.get('skip_input_raw',0))
    w.blob(data[j['sections']['type_specific']:])
    (out/'data.efp').write_bytes(w.b.getvalue())
    # Human/audit metadata kept outside APK; no 8192-slot JSON overhead on phone.
    return {'id':ident,'name':j['title'],'kind':source.suffix,'sha256':hashlib.sha256(data).hexdigest()}

def main():
    manifest=json.loads((ROOT/'research/filename_manifest.json').read_text());seen=set();catalog=[]
    OUT.mkdir(parents=True,exist_ok=True)
    for e in manifest['entries']:
        if not e['stored_path'] or e['is_metadata'] or e['sha256'] in seen:continue
        if Path(e['stored_path']).suffix not in ('.kgt','.player','.stage','.demo'):continue
        seen.add(e['sha256']);row=export(e);catalog.append(row);print(row['id'],row['name'],flush=True)
    (OUT/'catalog.json').write_text(json.dumps(catalog,ensure_ascii=False,indent=2))
    (ROOT/'research/conversion_manifest.json').write_text(json.dumps(catalog,ensure_ascii=False,indent=2))
    export_names(manifest,catalog)
    print('Converted',len(catalog),'unique containers')

def export_names(manifest,catalog):
    by_hash={row['sha256']:row['id'] for row in catalog}
    rows=[]
    for entry in manifest['entries']:
        ident=by_hash.get(entry['sha256']) if not entry['is_metadata'] else None
        rows.append({'entry_index':entry['entry_index'],
                     'raw_zip_name_hex':entry['raw_zip_name_hex'],
                     'decoded_japanese_path':entry['decoded_japanese_path'],
                     'source_sha256':entry['sha256'],
                     'android_pack_path':f'game/{ident}/data.efp' if ident else None,
                     'preserved_original_path':entry['stored_path']})
    text=json.dumps(rows,ensure_ascii=False,indent=2)
    (OUT/'filename-map.json').write_text(text)
    (ROOT/'research/android_filename_map.json').write_text(text)

if __name__=='__main__':
    if '--names-only' in sys.argv:
        export_names(json.loads((ROOT/'research/filename_manifest.json').read_text()),json.loads((OUT/'catalog.json').read_text()))
    else:main()
