#!/usr/bin/env python3
"""Nana money-can evidence from the supplied EXE; no Windows/API stubs.

Executes original M/integration and FA-vs-FD collision routines with original
Nana FA/R instructions. Synthetic task positions/FD isolate contact semantics;
this is not a complete replay of the Windows scheduler.
"""
import json
from inspect_portable import pack
from probe_combat_x86 import machine, setup, put, get, TASK, STRIDE, CHAR, CS, STACK, STOP, FA, FD, R, ROOT
from unicorn.x86_const import *

def main():
    nana=pack(ROOT/'app/src/main/assets/game/0104/data.efp')
    code=nana['code'];instruction=lambda pc:code[pc*16:(pc+1)*16]
    u=machine();contacts=[];motion=[]
    for fa_pc,r_pc,trigger in ((818,817,93),(1038,1037,93),(794,793,0)):
        for face in (0,1):
            for guard in (0,1):
                for distance in (-100,-50,0,50,100):
                    setup(u,guard=guard,ax=640,bx=640+distance,af=face,bf=1-face)
                    projectile=TASK+2*STRIDE
                    u.mem_write(projectile,bytes(u.mem_read(TASK,STRIDE)))
                    put(u,TASK+0x89,0)
                    put(u,projectile+0x15a,1);put(u,projectile+0x68,trigger)
                    u.mem_write(FA,instruction(fa_pc));u.mem_write(R,instruction(r_pc))
                    u.emu_start(0x40f010,STOP,count=2000000)
                    assert u.reg_read(UC_X86_REG_EIP)==STOP
                    result=[get(u,CHAR+CS+0xdf05),get(u,TASK+STRIDE+0x38),get(u,projectile+0x38),get(u,projectile+0x15e),get(u,TASK+STRIDE+0x15e)]
                    # Repeat without rearming: one contact cannot deal damage twice.
                    u.reg_write(UC_X86_REG_ESP,STACK);put(u,STACK,STOP)
                    u.emu_start(0x40f010,STOP,count=2000000)
                    assert u.reg_read(UC_X86_REG_EIP)==STOP
                    result.append(get(u,CHAR+CS+0xdf05))
                    contacts.append([fa_pc,r_pc,trigger,face,guard,distance,*result])
    task,game,block=0x1000000,0x1100000,0x1200000
    put(u,0x541f78,655);put(u,0x445700,393)
    for pc in (795,1025,1041,1052):
        for face in (0,1):
            u.mem_write(task,bytes(0x200));put(u,task+0x5c,face)
            put(u,task+8,640<<16);put(u,task+12,420<<16)
            u.mem_write(block,instruction(pc))
            u.reg_write(UC_X86_REG_ESI,task);u.reg_write(UC_X86_REG_EDI,block);u.reg_write(UC_X86_REG_EBX,game)
            u.emu_start(0x41282d,0x4125ae,count=1000)
            assert u.reg_read(UC_X86_REG_EIP)==0x4125ae
            for tick in range(1,121):
                u.reg_write(UC_X86_REG_EDI,task);u.emu_start(0x40f96a,0x40f9dc,count=100)
                assert u.reg_read(UC_X86_REG_EIP)==0x40f9dc
                motion.append([pc,face,tick,*[get(u,task+off) for off in (8,12,0x18,0x1c,0x20,0x24)]])
    output={'scope':__doc__,'contacts':contacts,'motion':motion}
    (ROOT/'research/nana_can_x86_007a.json').write_text(json.dumps(output,indent=2))
    print('PASS original Nana can:',len(contacts),'contacts incl. re-hit,',len(motion),'motion frames')
    print('Contact outcomes:',sorted(set(tuple(row[6:]) for row in contacts)))

if __name__=='__main__':main()
