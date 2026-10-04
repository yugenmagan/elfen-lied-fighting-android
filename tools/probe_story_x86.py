#!/usr/bin/env python3
"""Original story/DEMO control flow; OS, audio and task allocation are bounded hooks.
No Java implementation is consulted to generate these differential fixtures.
"""
import struct,json,itertools
from probe_x86 import machine,put,get,ROOT
from probe_match_x86 import load
from inspect_portable import pack,story
from unicorn import UC_HOOK_CODE
from unicorn.x86_const import *
def main():
 u=machine();tail,durations=load();u.mem_write(0x435470,tail)
 task=0x1000000;stack=0x13ff000;stop=0x13ffff0;calls=[];skills=[]
 def hook(u,a,size,data):
  if a not in (0x415190,0x4179d0,0x406450,0x4068e0,0x406570,0x4039f0,0x403fc0,0x406790,0x4069b0):return
  sp=u.reg_read(UC_X86_REG_ESP);v=0
  if a==0x4068e0:skill=get(u,sp+4);skills.append(skill);v=durations[skill]
  if a==0x406570:calls.append([get(u,sp+j*4) for j in range(1,5)]);v=0x1010000
  if a==0x4069b0:calls.append([1001,0,0,0])
  ret=get(u,sp);u.reg_write(UC_X86_REG_EAX,v);u.reg_write(UC_X86_REG_ESP,sp+4);u.reg_write(UC_X86_REG_EIP,ret)
 u.hook_add(UC_HOOK_CODE,hook)
 def run(address):
  calls.clear();skills.clear();put(u,stack,stop);u.reg_write(UC_X86_REG_ESP,stack);u.emu_start(address,stop,count=200000);assert u.reg_read(UC_X86_REG_EIP)==stop
 rows=[]
 for phase,guard,latch,remaining,mask in itertools.product([0,1,2],[0,9,10],[0,1],[0,1,2],[0,1,16,1024]):
  u.mem_write(task,bytes(0x200));put(u,0x4cfa00,task)
  for a,v in [(task+0x152,phase),(task+0x156,guard),(task+0x15a,latch),(0x424f08,remaining),(0x4280d8,mask),(0x424f04,1)]:put(u,a,v)
  run(0x406c10);out=[get(u,a) for a in (task+0x152,task+0x156,task+0x15a,0x424f08)]+[int(bool(calls))]
  rows.append([phase,guard,latch,remaining,mask,*out])
 (ROOT/'research/demo_x86_fixtures.txt').write_text('\n'.join(' '.join(map(str,r)) for r in rows)+'\n')
 print('PASS native DEMO manager',len(rows),'cases',flush=True)
 record=bytes([1])+bytes.fromhex(story(pack(ROOT/'app/src/main/assets/game/0170/data.efp'))[1]['raw'])
 rows=[]
 for phase,wait,hps,points,timer in itertools.product([110,111,112,200,300,410,411,420,421,430,431,900,901,902],[0,1],[(400,400),(0,400),(400,0),(0,0)],[(0,0),(100,0),(0,100),(100,100)],[-1,0,1]):
  u.mem_write(task,bytes(0x200));u.mem_write(0x4d1d80,bytes(0xe03f*8));u.mem_write(0x4701e0,bytes(0x17e*1024));u.mem_write(0x4d9a49,record)
  hp1,hp2=hps;p1,p2=points
  for a,v in [(0x4701bc,0),(0x4280d8,0),(0x424718,0),(0x4cfa00,task),(task+0x152,phase),(task+0x156,wait),(0x470058,0),(0x470044,1),(0x470048,1),(0x470050,timer),(0x424f24,0),(0x424f28,0),(0x424e64,0),(0x4dfd3b,p1),(0x4edd7a,p2),(0x4dfc6d,1 if p1>p2 else 0),(0x4edcac,1 if p2>p1 else 0)]:put(u,a,v)
  for p,hp in enumerate(hps):
   c=0x4d1d80+p*0xe03f
   for a,v in [(c+0xdf05,hp),(c+0xdf11,400),(c+0xdf0d,p),(c+0xdf5d,p)]:put(u,a,v)
  run(0x4086a0)
  before=[phase,wait,timer,hp1,hp2,p1,p2,1 if p1>p2 else 0,1 if p2>p1 else 0]
  after=[get(u,a) for a in (task+0x152,task+0x156,0x470050,0x4dfc6d,0x4edcac,0x4dfd3b,0x4edd7a,0x424e64)]
  if any(c[0]==1001 for c in calls):after[0]=1000
  if any(c[0]==13 for c in calls):after[0]=1001
  rows.append(before+after+[len(skills)]+skills)
 (ROOT/'research/story_round_x86_fixtures.txt').write_text('\n'.join(' '.join(map(str,r)) for r in rows)+'\n')
 print('PASS native story round',len(rows),'cases')
if __name__=='__main__':main()
