#!/usr/bin/env python3
"""Original EXE menu-input/grid fixtures. No OS or compatibility runtime."""
import random,struct
from probe_x86 import ROOT,machine,put,get
from inspect_portable import pack
from unicorn.x86_const import UC_X86_REG_ESP
def main():
 u=machine();u.mem_write(0x435470,pack(ROOT/'app/src/main/assets/game/0116/data.efp')['tail'])
 assert get(u,0x41e3fc)==50 and get(u,0x41e400)==5
 masks=[0]*3+[2]*73+[10]*63+[8]*20+[0]*2+[1]*57+[2,18,16,0,4,12,8,0,1023,0]
 rng=random.Random(7007)
 for _ in range(50):masks.extend([rng.randrange(1024)]*rng.randrange(1,65))
 lines=[]
 for m in masks:
  put(u,0x4259c0,m);u.emu_start(0x414770,0x414829,count=1000);lines.append(f'{m} {get(u,0x447f60)} {get(u,0x447f40)}')
 (ROOT/'research/select_input_x86_007.txt').write_text('\n'.join(lines)+'\n')
 cols,rows=struct.unpack('<hh',u.mem_read(0x4452b8,4));coord,stack,stop=0x1000000,0x13ff000,0x13ffff0;nav=[]
 for cell in range(cols*rows):
  for mask in range(16):
   put(u,coord,cell%cols);put(u,coord+4,cell//cols)
   for bit,dx,dy in ((4,0,-1),(8,0,1),(1,-1,0),(2,1,0)):
    if mask&bit:
     u.mem_write(stack,struct.pack('<IIii',stop,coord,dx,dy));u.reg_write(UC_X86_REG_ESP,stack);u.emu_start(0x406e70,stop,count=1000)
   nav.append(f'{cell} {mask} {get(u,coord+4)*cols+get(u,coord)}')
 (ROOT/'research/select_grid_x86_007.txt').write_text('\n'.join(nav)+'\n')
 print(f'PASS original EXE: {len(lines)} input frames, {len(nav)} wrap cases, repeat50/5 ticks; isolated routines, not Windows execution')
if __name__=='__main__':main()
