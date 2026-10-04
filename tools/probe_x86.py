#!/usr/bin/env python3
"""Execute isolated, pure original x86 routines for differential evidence.

No OS/runtime is emulated; unexpected accesses/syscalls fail the probe.
Requires unicorn==2.1.4, pefile==2024.8.26 (research only, not APK).
"""
import json
from pathlib import Path
import struct
import sys
ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / '.deps/python'))
import pefile
from unicorn import Uc, UC_ARCH_X86, UC_MODE_32
from unicorn.x86_const import UC_X86_REG_ESP, UC_X86_REG_EBX, UC_X86_REG_ESI, UC_X86_REG_EDI

def machine():
    pe = pefile.PE(str(ROOT/'source_original/entries/0186.exe'))
    u = Uc(UC_ARCH_X86, UC_MODE_32)
    u.mem_map(0x400000, (pe.OPTIONAL_HEADER.SizeOfImage+4095)&~4095)
    u.mem_write(0x400000, pe.get_memory_mapped_image())
    u.mem_map(0x1000000, 0x400000)
    u.reg_write(UC_X86_REG_ESP, 0x13ff000)
    u.mem_write(0x13ff000, struct.pack('<I', 0x13ffff0))
    return u

def put(u,a,v): u.mem_write(a,struct.pack('<I',v&0xffffffff))
def get(u,a): return struct.unpack('<i',u.mem_read(a,4))[0]

def main():
    u=machine(); result={'scope':'isolated x86 handlers, not Windows or Android execution','speed':[],'motion':[]}
    for speed in (0,5,10,15,20):
        put(u,0x430104,speed);u.reg_write(UC_X86_REG_ESP,0x13ff000)
        u.emu_start(0x406450,0x13ffff0,count=30000)
        c=speed*10 if speed>10 else 50+speed*5
        actual=[get(u,a) for a in (0x445704,0x541f78,0x445700)]
        assert actual == [c,65536//c,3932160//(c*c)],actual
        result['speed'].append({'setting':speed,'timing_velocity_acceleration':actual})
    put(u,0x541f78,655);put(u,0x445700,393)
    task,game,block=0x1000000,0x1100000,0x1200000
    for facing in (0,1):
        for flags in (0,1,2,4,8,16,31):
            u.mem_write(task,bytes(0x200));put(u,task+0x5c,facing)
            for offset in (0x18,0x1c,0x20,0x24):put(u,task+offset,10000)
            b=struct.pack('<BhhhhB6x',1,-10,700,-2000,90,flags)
            u.mem_write(block,b)
            u.reg_write(UC_X86_REG_ESI,task);u.reg_write(UC_X86_REG_EDI,block);u.reg_write(UC_X86_REG_EBX,game)
            u.emu_start(0x41282d,0x4125ae,count=1000)
            actual=[get(u,task+o) for o in (0x18,0x1c,0x20,0x24)]
            mult=-1 if facing else 1
            values=[700*655*mult,-2000*655,-10*393*mult,90*393]
            expect=[10000 if flags&(2<<i) else (10000 if flags&1 else 0)+v for i,v in enumerate(values)]
            assert actual==expect,(actual,expect)
            result['motion'].append({'facing':facing,'flags':flags,'actual':actual})
    for wait in (0,1,5,15,325):
        u.mem_write(block,struct.pack('<BH13x',12,wait));put(u,task+0x3c,-100)
        put(u,task+0x15a,2);put(u,0x445704,100)
        u.reg_write(UC_X86_REG_ESI,task);u.reg_write(UC_X86_REG_EDI,block);u.reg_write(UC_X86_REG_EBX,game)
        u.emu_start(0x4127e3,0x4125ae,count=1000)
        assert get(u,task+0x3c)==(-1 if not wait else wait*100-100)
    result['wait_cases']=5
    result['integration']=[]
    def signed(v): return (v+2**31)%2**32-2**31
    for x,y,vx,vy,ax,ay in ((100,200,10,-20,3,7),(0,920<<16,0,-2000*655,0,90*393),(2**31-3,0,5,0,0,0)):
        u.mem_write(task,bytes(0x200))
        for offset,value in ((8,x),(12,y),(0x18,vx),(0x1c,vy),(0x20,ax),(0x24,ay)):put(u,task+offset,value)
        u.reg_write(UC_X86_REG_EDI,task)
        u.emu_start(0x40f96a,0x40f9dc,count=100)
        actual=[get(u,task+o) for o in (8,12,0x18,0x1c)]
        expect=list(map(signed,(x+vx+ax,y+vy+ay,vx+ax,vy+ay)))
        assert actual==expect,(actual,expect)
        result['integration'].append({'initial':[x,y,vx,vy,ax,ay],'position_velocity':actual})
    result['attachment']=[]
    for facing in (0,1):
        u.mem_write(task,bytes(0x200));u.mem_write(game,bytes(0x200))
        put(u,task+0x18,12345);put(u,task+0x1c,67890)
        put(u,task+0x28,0x20000000);put(u,task+0x17a,game)
        u.mem_write(task+0x12d,struct.pack('<hh',7,-9))
        put(u,game+8,100<<16);put(u,game+12,200<<16);put(u,game+0x5c,facing)
        u.reg_write(UC_X86_REG_EDI,task);u.emu_start(0x40f96a,0x40f9dc,count=100)
        actual=[get(u,task+o) for o in (8,12)]
        assert actual==[((100-7 if facing else 100+7)<<16),191<<16],actual
        result['attachment'].append({'facing':facing,'position':actual})
    result['passed']=True
    (ROOT/'research/x86_probe_results.json').write_text(json.dumps(result,indent=2))
    print('PASS: 29 original x86 cases (5 speed, 14 M, 5 I, 3 integration, 2 parent attachment)')

if __name__=='__main__':main()
