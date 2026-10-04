#!/usr/bin/env python3
"""Original command matcher fixtures: actual commands, holds, mash and rotations."""
import json,random,struct
from probe_cpu_x86 import put_player
from probe_combat_x86 import machine,put,get,setup,TASK,STRIDE,CHAR,CS,STACK,STOP,ROOT
from unicorn.x86_const import *
DIR=[0,0,2,10,8,9,1,5,4,6,1,4,2,8,0,0]
def swap(m):return (m&~3)|((m&1)<<1)|((m&2)>>1)
def command_input(c):
 seq=[0]*20;steps=bytes.fromhex(c['steps_raw'])
 for step in range(10):
  f=struct.unpack_from('<H',steps,step*2)[0]
  if not f&0x2000:continue
  mask=DIR[f&15]|(f&0x3f0);amount=c['amounts_raw'][step]
  if f>>14==0:seq+=[mask]*2
  elif f>>14==1:seq+=[v for _ in range(amount) for v in (mask,0)]
  elif f>>14==2:seq+=[mask]*amount
  else:seq+=[v|(f&0x3f0) for _ in range(amount) for v in (2,8,1,4)]
 return seq

def main():
 u=machine();rows=[];rng=random.Random(149721)
 def case(ident,hist,reverse=0,air=0,start=0,custom=None):
  setup(u);put_player(u,ident,0)
  if custom:
   u.mem_write(CHAR+0x4da2,bytes(82*100));u.mem_write(CHAR+0x4da2,bytes(32)+struct.pack('<5H',120,11,12,13,14)+struct.pack('<10H',*custom['steps'])+struct.pack('<10H',*custom['amounts']))
  hist=(hist+[0]*1024)[:1024];put(u,0x4cfa00,TASK);put(u,0x47004c,1);put(u,0x447ee0,1023);put(u,TASK+0x5c,reverse);put(u,CHAR+0x7cb6,8 if reverse else 0)
  put(u,CHAR+0xdf69,TASK+STRIDE);put(u,CHAR+0xdf41,390<<16);put(u,CHAR+CS+0xdf41,800<<16)
  if air:put(u,TASK+12,800<<16)
  for age,v in enumerate(hist):put(u,0x4280e0+(1023-age)*4,v)
  put(u,CHAR+0xdf55,0);u.reg_write(UC_X86_REG_ESP,STACK);put(u,STACK,STOP);put(u,STACK+4,start);u.emu_start(0x410060,STOP,count=4000000)
  if u.reg_read(UC_X86_REG_EIP)!=STOP:raise RuntimeError('Command matcher did not return')
  rows.append({'id':ident,'reverse':reverse,'stance':0 if air else 3 if hist[0]&8 else 2,'start':start,'custom':custom,'history':hist,'skill':u.reg_read(UC_X86_REG_EAX),'chosen':get(u,CHAR+0xdf55),'after':[get(u,0x4280e0+(1023-age)*4) for age in range(1024)]})
 for ident in ('0170','0104','0128'):
  for c in json.loads((ROOT/f'research/containers/{ident}.player.json').read_text())['specific']['commands']:
   seq=command_input(c)
   for lag in (0,1,5,12):
    for reverse in (0,1):case(ident,[(swap(v) if reverse else v) for v in reversed(seq+[0]*lag)],reverse)
   case(ident,list(reversed(seq)),0,1)
  for _ in range(40):case(ident,[rng.randrange(1024) for _ in range(100)],rng.randrange(2),rng.randrange(2))
 for mode in range(4):
  for direction in range(14):
   for amount in (1,3,8):
    custom={'steps':[0x2000|(mode<<14)|direction|16]+[0]*9,'amounts':[amount]*10};c={'steps_raw':struct.pack('<10H',*custom['steps']).hex(),'amounts_raw':custom['amounts']};seq=command_input(c)
    case('0170',list(reversed(seq)),custom=custom);case('0170',[rng.randrange(32) for _ in range(128)],custom=custom)
 (ROOT/'research/commands_x86_results.json').write_text(json.dumps(rows,separators=(',',':')));print('Original matcher:',len(rows),'scenarios')
if __name__=='__main__':main()
