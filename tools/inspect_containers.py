#!/usr/bin/env python3
"""Strict structural inspection, NOT an FM2K runtime.

Format research reference: Xem85/fm2ndparser (MIT), commit
7266d65b9ca486a619b6b10836ee0cfed734fefb. See vendor LICENSE.
Unknown execution semantics remain raw bytes; no placeholder game behavior.
"""
from __future__ import annotations
import collections
import hashlib
import json
from pathlib import Path
import struct


class FormatError(ValueError): pass


class Reader:
    def __init__(self,data): self.data=data; self.p=0
    def take(self,n):
        if n<0 or self.p+n>len(self.data):
            raise FormatError(f'truncated at {self.p:#x}, requested {n} bytes')
        b=self.data[self.p:self.p+n]; self.p+=n; return b
    def u8(self): return self.take(1)[0]
    def u16(self): return int.from_bytes(self.take(2),'little')
    def u32(self): return int.from_bytes(self.take(4),'little')
    def text(self,n): return self.take(n).split(b'\0',1)[0].decode('cp932')


def unpack_sprite(source,expected):
    r=Reader(source); out=bytearray(); token_counts=collections.Counter()
    while r.p<len(source):
        token=r.u8(); mode=token>>6; count=token&63; token_counts[mode]+=1
        if count==0:
            count=r.u8()
            if count: count+=63
            else: count=int.from_bytes(r.take(3),'little')+319
        if len(out)+count>expected:
            raise FormatError(f'image output overrun: {len(out)}+{count}>{expected}')
        if mode==0: out.extend(b'\0'*count)
        elif mode==1: out.extend(r.take(count))
        elif mode==2: out.extend(r.take(1)*count)
        else:
            distance=r.u8()
            if distance==0:
                distance=(r.u8()+1)<<8
                r.take(1) # present in both upstream C# and the older Pascal decoder
            if distance>len(out): raise FormatError('back reference before start')
            for _ in range(count): out.append(out[-distance])
    if len(out)!=expected: raise FormatError(f'image size mismatch {len(out)} != {expected}')
    return bytes(out),dict(token_counts)


def digest(b): return hashlib.sha256(b).hexdigest()


def parse(data,ext,export_dir=None):
    r=Reader(data); sections={}; signature=r.take(12)
    if signature[:7] not in (b'2DKGT2G',b'2DKGT2K'):
        raise FormatError('unexpected signature')
    flags=r.u32(); title=r.text(256)
    sections['skills_count']=r.p; count=r.u32()
    if count>65536: raise FormatError('unreasonable skill count')
    skills=[]
    for i in range(count):
        pos=r.p; name=r.text(32); block=r.u16(); extra=r.u8(); kind=r.u32()
        skills.append(dict(index=i,offset=pos,name=name,block_start=block,extra=extra,type=kind))
    sections['blocks_count']=r.p; count=r.u32()
    raw_blocks=r.take(count*16)
    blocks=[raw_blocks[i:i+16] for i in range(0,len(raw_blocks),16)]
    if any(s['block_start']>count for s in skills): raise FormatError('skill block start out of bounds')
    sections['images_count']=r.p; count=r.u32(); images=[]; raw_images=[]
    if count>100000: raise FormatError('unreasonable image count')
    for i in range(count):
        offset=r.p; pointer=r.u32(); w=r.u32(); h=r.u32(); palette_type=r.u32(); packed=r.u32()
        if palette_type not in (0,1) or w*h>100000000: raise FormatError('invalid image descriptor')
        expected=w*h+(1024 if palette_type else 0)
        # A completely empty slot has no pixel or palette payload.
        if not packed and not w and not h: expected=0
        payload=r.take(packed or expected)
        pixels,tokens=unpack_sprite(payload,expected) if packed else (payload,{})
        raw_images.append(pixels)
        images.append(dict(index=i,offset=offset,pointer=pointer,width=w,height=h,palette_type=palette_type,packed_size=packed,unpacked_size=len(pixels),unpacked_sha256=digest(pixels),compression_tokens=tokens))
    sections['palettes']=r.p; palettes=[r.take(1056) for _ in range(8)]
    sections['sounds_count']=r.p; count=r.u32(); sounds=[]
    if count>100000: raise FormatError('unreasonable sound count')
    for i in range(count):
        offset=r.p; pointer=r.u32(); name=r.text(32); size=r.u32(); flags_sound=r.u8(); track=r.u8(); payload=r.take(size)
        sounds.append(dict(index=i,offset=offset,pointer=pointer,name=name,size=size,flags=flags_sound,type=flags_sound&3,loop=bool(flags_sound&32),cdda_track=track,magic=payload[:12].hex(),sha256=digest(payload)))
    sections['type_specific']=r.p
    specific={}
    if ext=='.kgt':
        specific['reserved4']=r.take(4).hex()
        specific['characters']=[r.text(256) for _ in range(50)]
        specific['hit_junctions']=[dict(name=r.text(32),flag=r.u32()) for _ in range(200)]
        specific['unknown_u32']=r.u32(); specific['reserved1']=r.u8()
        specific['stiff_time']=dict(hit=r.u8(),guard=r.u8(),offset=r.u8())
        specific['stages']=[r.text(256) for _ in range(50)]
        specific['demos']=[r.text(256) for _ in range(100)]
        specific['screens']=dict(zip(['title','p1_cpu','p1_p2','team','game_over','opening'],r.take(6)))
    elif ext in ('.demo','.stage'):
        specific['reserved4']=r.take(4).hex(); specific['bgm_index']=r.u16()
        if specific['bgm_index']>=len(sounds): raise FormatError('BGM index out of bounds')
        if ext=='.demo':
            specific['skip_input_raw']=r.u16(); specific['reserved1']=r.u8(); specific['time_raw']=r.u32()
    elif ext=='.player':
        specific['reserved4']=r.take(4).hex(); sections['commands_count']=r.p; n=r.u32(); commands=[]
        for i in range(n):
            offset=r.p; name=r.text(32); time=r.u16(); refs=[r.u16() for _ in range(4)]; raw_steps=r.take(20); amounts=[r.u16() for _ in range(10)]
            commands.append(dict(index=i,offset=offset,name=name,time_raw=time,skill_indices=refs,steps_raw=raw_steps.hex(),amounts_raw=amounts))
            if any(x>=len(skills) for x in refs):raise FormatError('command skill index out of bounds')
        specific['commands']=commands
        n=r.u32();specific['hit_junctions']=[dict(hit_skill=r.u16(),spark_skill=r.u16()) for _ in range(n)]
        n=r.u32();specific['common_images']=[r.take(6).hex() for _ in range(n)]
        specific['reserved10']=r.take(10).hex(); sections['cpu']=r.p
        specific['cpu_slots']=[dict(index=i,offset=r.p,raw=r.take(111).hex()) for i in range(100)]
        specific['builtin_skill_indices']=[r.u16() for _ in range(24)]
        specific['reserved38']=r.take(38).hex();sections['player_settings']=r.p
        specific['player_settings_raw']=r.take(1785).hex();sections['story']=r.p;story=[]
        for slot in range(100):
            offset=r.p;kind=r.u8()
            if kind not in (0,1,2,3,4):raise FormatError(f'unknown story type {kind} at {offset:#x}')
            payload=r.take(205) if kind else b''
            event=dict(slot=slot,offset=offset,type=kind,raw=payload.hex())
            if kind==2:event['demo_index']=int.from_bytes(payload[:2],'little')
            elif kind==1:
                event['stage_index']=payload[0];event['rounds']=payload[1];event['time_raw']=int.from_bytes(payload[5:7],'little')
            elif kind==3:event['condition_raw']=payload[0];event['value_raw']=payload[1];event['destination_raw']=int.from_bytes(payload[4:5],'little',signed=True)
            story.append(event)
        specific['story_slots']=story
    parsed_until=r.p;remaining=r.take(len(data)-r.p)
    result=dict(title=title,signature=signature.hex(),flags=flags,file_size=len(data),sections=sections,skills=skills,block_count=len(blocks),block_type_counts=dict(collections.Counter(b[0] for b in blocks)),blocks_hex=[b.hex() for b in blocks],images=images,palettes_sha256=[digest(p) for p in palettes],sounds=sounds,specific=specific,parsed_until=parsed_until,trailing_bytes=len(remaining),trailing_nonzero=sum(x!=0 for x in remaining),trailing_sha256=digest(remaining),execution_semantics_verified=False)
    if export_dir is not None:
        from PIL import Image
        export_dir.mkdir(parents=True,exist_ok=True)
        for d,pixels in zip(images,raw_images):
            w,h=d['width'],d['height']
            if not w or not h:continue
            if d['palette_type']: pal=pixels[:1024]; pixels=pixels[1024:]
            else:pal=palettes[0][:1024]
            im=Image.frombytes('P',(w,h),pixels)
            im.putpalette(bytes(c for k in range(256) for c in [pal[k*4+2],pal[k*4+1],pal[k*4]]))
            im.save(export_dir/f'image_{d["index"]:04d}.png')
    return result


def main():
    root=Path(__file__).resolve().parents[1];manifest=json.loads((root/'research/filename_manifest.json').read_text())
    out=root/'research/containers';out.mkdir(parents=True,exist_ok=True)
    seen=set();summaries=[];errors=[]
    for row in manifest['entries']:
        if not row['stored_path'] or row['is_metadata']:continue
        p=root/row['stored_path']
        if p.suffix not in ('.kgt','.player','.stage','.demo') or row['sha256'] in seen:continue
        seen.add(row['sha256'])
        try: result=parse(p.read_bytes(),p.suffix)
        except (FormatError,UnicodeError) as exc:
            errors.append(dict(entry_index=row['entry_index'],title=row['internal_title'],error=str(exc)));continue
        result['entry_index']=row['entry_index'];result['source_sha256']=row['sha256']
        (out/(p.name+'.json')).write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf8')
        summaries.append(dict(entry_index=row['entry_index'],title=result['title'],extension=p.suffix,skills=len(result['skills']),blocks=result['block_count'],images=len(result['images']),sounds=len(result['sounds']),trailing_bytes=result['trailing_bytes'],trailing_nonzero=result['trailing_nonzero']))
    output=dict(parsed=summaries,errors=errors,scope='structural inspection, not runtime behavior')
    (root/'research/container_summary.json').write_text(json.dumps(output,ensure_ascii=False,indent=2),encoding='utf8')
    print(json.dumps(output,ensure_ascii=False,indent=2))
    if errors:raise SystemExit(1)


if __name__=='__main__':main()
