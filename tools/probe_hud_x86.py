#!/usr/bin/env python3
"""Original EXE HUD clipping and timer placement, without Windows execution."""
import json, struct
from probe_x86 import ROOT, machine, put, get
from inspect_portable import pack
from unicorn import UC_HOOK_CODE
from unicorn.x86_const import *

def main():
    u=machine();k=pack(ROOT/'app/src/main/assets/game/0116/data.efp')
    u.mem_write(0x435470,k['tail'])
    task=0x1000000;stack=0x13ff000;stop=0x13ffff0
    clips=[]
    for kind,width in ((10,206),(11,206),(20,118),(21,118)):
        for maximum in (1,200,400):
            for value in sorted({0,1,maximum//4,maximum//2,maximum-1,maximum}):
                u.mem_write(task,bytes(0x200));put(u,task+0x156,kind)
                put(u,0x470058,1)
                for p in range(2):
                    c=0x4d1d80+p*0xe03f
                    for off,v in ((0xdf05,value),(0xdf11,maximum),(0xdf1d,value),(0xdf21,maximum)):put(u,c+off,v)
                put(u,stack+0x14,0);u.reg_write(UC_X86_REG_ESP,stack)
                u.reg_write(UC_X86_REG_ECX,task);u.reg_write(UC_X86_REG_EBP,width);u.reg_write(UC_X86_REG_ESI,0)
                u.emu_start(0x40dd95,0x40de7f,count=500)
                assert u.reg_read(UC_X86_REG_EIP)==0x40de7f
                clips.append([kind,width,value,maximum,u.reg_read(UC_X86_REG_ESI),u.reg_read(UC_X86_REG_EBP),get(u,stack+0x14)])
    out=ROOT/'research/hud';out.mkdir(exist_ok=True)
    (out/'clip-x86.tsv').write_text('\n'.join('\t'.join(map(str,r)) for r in clips)+'\n')

    # Supply original skill records/instructions; hook only task allocation.
    table=0x1020000;code=0x1030000
    rows=bytearray()
    for s in k['skills']:rows+=s['name'].encode('cp932').ljust(32,b'\0')[:32]+struct.pack('<HBI',s['start'],0,s['type'])
    u.mem_write(table,bytes(rows));u.mem_write(code,k['code'])
    put(u,0x433350,table);put(u,0x433354,code)
    allocated=[]
    def allocate(u,a,size,data):
        sp=u.reg_read(UC_X86_REG_ESP);ptr=0x1040000+len(allocated)*0x200
        u.mem_write(ptr,bytes(0x200));put(u,ptr+8,get(u,sp+12));put(u,ptr+12,get(u,sp+16));allocated.append(ptr)
        u.reg_write(UC_X86_REG_EAX,ptr);u.reg_write(UC_X86_REG_ESP,sp+4);u.reg_write(UC_X86_REG_EIP,get(u,sp))
    u.hook_add(UC_HOOK_CODE,allocate,begin=0x406570,end=0x406570)
    def run():
        put(u,stack,stop);u.reg_write(UC_X86_REG_ESP,stack);u.emu_start(0x40a620,stop,count=10000)
        assert u.reg_read(UC_X86_REG_EIP)==stop
    timers=[]
    for value in (-1,0,1,9,10,19,30,60,99,100,199,999):
        allocated.clear();u.mem_write(task,bytes(0x200));put(u,0x4cfa00,task);put(u,0x470050,-1 if value<0 else value*100)
        run()
        if value<0:run()  # Initial negative-time branch schedules the infinity task next pass.
        digits=[]
        for ptr in allocated:
            x=get(u,ptr+8)//65536
            if abs(x)<1000:digits.append([get(u,ptr+0x30),x,get(u,ptr+12)//65536])
        timers.append({'seconds':value,'draw_tasks':digits})
    (out/'timer-x86.json').write_text(json.dumps(timers,indent=2)+'\n')
    (out/'timer-x86.tsv').write_text('\n'.join('\t'.join(map(str,[t['seconds'],*sum(t['draw_tasks'],[])])) for t in timers)+'\n')
    print('PASS',len(clips),'original x86 gauge crops;',len(timers),'timer layouts; allocation-only hook; not Windows/Android execution')
if __name__=='__main__':main()
