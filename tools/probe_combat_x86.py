#!/usr/bin/env python3
"""Run pure collision/damage/RNG code from the supplied EXE; no Wine/API stubs."""
import json,struct
from pathlib import Path
from probe_x86 import machine,put,get,ROOT
from inspect_portable import pack
from unicorn import UC_HOOK_CODE
from unicorn.x86_const import *
TASK=0x4701e0;STRIDE=0x17e;CHAR=0x4d1d80;CS=0xe03f
STACK=0x13ff000;STOP=0x13ffff0;FA=0x1200000;FD=FA+0x20;R=FA+0x40
NANA_JUNCTIONS=pack(ROOT/'app/src/main/assets/game/0104/data.efp')['junctions']

def box(op,x,y,w,h,flags=0,rate=100,power=0):
 b=bytearray(16);struct.pack_into('<BhhhhBBB',b,0,op,x,y,w,h,0,flags,rate);b[12]=power;return bytes(b)

def setup(u,power=5,rate=100,guard=0,flags=0,combo=0,ax=390,bx=450,af=0,bf=1):
 u.mem_write(TASK,bytes(STRIDE*1024));u.mem_write(CHAR,bytes(CS*2))
 u.mem_write(FA,box(24,54,-110,30,23,flags,0,power));u.mem_write(FD,box(25,1,-79,54,84,3,rate))
 u.mem_write(R,struct.pack('<B6H3x',23,1,2,3,7,8,9))
 put(u,0x447ee0,0);put(u,0x4280e0,0);put(u,0x4290e0,guard);put(u,0x470058,1)
 u.mem_write(0x43a299,bytes([7,9,6]))
 for index in range(1,13):u.mem_write(0x438694+index*36,bytes([1 if index<7 else 0]))
 for i,x,face in [(0,ax,af),(1,bx,bf)]:
  task=TASK+i*STRIDE;c=CHAR+i*CS
  for off,val in ((0,4),(8,x<<16),(12,920<<16),(0x58,920<<16),(0x5c,face),(0x60,task),(0x156,i)):
   put(u,task+off,val)
  for off,val in ((0xdef5,task),(0xdf05,400),(0xdf11,400),(0xdfb7,1<<(1-i)),(0x7cae,400),(0x7cb2,200),(0x7cb6,2)):
   put(u,c+off,val)
  for j in range(1,13):u.mem_write(c+0x6daa+4*j,struct.pack('<HH',NANA_JUNCTIONS[j][0],0))
  u.mem_write(c+0x75a0,struct.pack('<H',20))
 u.mem_write(CHAR+0x7ca5,bytes([3,0,0,2]));u.mem_write(CHAR+CS+0x7ca5,bytes([5,0,0,3]))
 put(u,CHAR+CS+0xdf01,combo)
 put(u,TASK+0x89,FA);put(u,TASK+0x129,R);put(u,TASK+STRIDE+0xd9,FD)
 u.reg_write(UC_X86_REG_ESP,STACK);put(u,STACK,STOP)

def full_case(u,**args):
 setup(u,**args);u.emu_start(0x40f010,STOP,count=2000000)
 assert u.reg_read(UC_X86_REG_EIP)==STOP,'probe did not reach return'
 v=TASK+STRIDE;c=CHAR+CS
 return {'life':get(u,c+0xdf05),'pending_skill':get(u,v+0x38),'attacker_freeze':get(u,TASK+0x40),'defender_freeze':get(u,v+0x40),'combo':get(u,c+0xdf01),'attacker_flags':get(u,TASK+0x15e),'defender_flags':get(u,v+0x15e)}

def damage_only(u,power,combo,correction,rate):
 # Enter after meter updates; leave immediately before life modification CALL.
 setup(u);u.reg_write(UC_X86_REG_ESP,STACK);put(u,STACK+0x28,0);put(u,STACK+0x44,FD)
 u.reg_write(UC_X86_REG_EBX,CHAR+CS);u.reg_write(UC_X86_REG_EBP,TASK+STRIDE);u.reg_write(UC_X86_REG_ESI,power)
 put(u,CHAR+CS+0xdf01,combo);u.mem_write(CHAR+0x7ca8,bytes([correction]));u.mem_write(FD+11,bytes([rate]))
 u.emu_start(0x40f786,0x40f7f9,count=100)
 return -get(u,u.reg_read(UC_X86_REG_ESP)+4)

def main():
 u=machine();out={'scope':'isolated original x86 only','full_collision':[],'damage':[],'rng':[]}
 for args in ({},{'bx':650},{'guard':1},{'guard':9},{'guard':1,'flags':64},{'guard':1,'flags':4},{'rate':50},{'power':255},{'power':0},{'ax':510,'bx':450,'af':1,'bf':0}):
  actual=full_case(u,**args);print(args,actual);out['full_collision'].append({'input':args,'output':actual})
 for p in (1,5,55,255):
  for count in (0,1,5,40):
   for correction in (0,2,9):
    for rate in (0,50,100,255):
     damage=damage_only(u,p,count,correction,rate)
     expected=max(1,(max(1,p-int(p*count*correction/100))*rate)//100)
     assert damage==expected,(p,count,correction,rate,damage,expected)
     out['damage'].append([p,count,correction,rate,damage])
 for seed in (0,1,0xffffffff,0x80000000):
  state=seed;put(u,0x41fb1c,state);values=[]
  for i in range(64):
   u.reg_write(UC_X86_REG_ESP,STACK);put(u,STACK,STOP);u.emu_start(0x417a22,STOP,count=20)
   state=(state*214013+2531011)&0xffffffff;value=(state>>16)&32767
   assert u.reg_read(UC_X86_REG_EAX)==value and (get(u,0x41fb1c)&0xffffffff)==state
   values.append(value)
  out['rng'].append({'seed':seed,'values':values,'final_state':state})
 (ROOT/'research/combat_x86_results.json').write_text(json.dumps(out,indent=2))
 print('PASS damage',len(out['damage']),'RNG',sum(len(r['values']) for r in out['rng']))
if __name__=='__main__':main()
