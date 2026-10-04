#!/usr/bin/env python3
"""Isolated original single-VS dispatcher. Only presentation/task creation is stubbed."""
import json,struct
from pathlib import Path
from probe_x86 import machine,put,get,ROOT
from unicorn import UC_HOOK_CODE
from unicorn.x86_const import *
class Efp:
 def __init__(self,b):self.b=b;self.p=0
 def n(self):v=struct.unpack_from('>i',self.b,self.p)[0];self.p+=4;return v
 def blob(self):n=self.n();b=self.b[self.p:self.p+n];self.p+=n;return b
 def ints(self):return [self.n() for _ in range(self.n())]
def load():
 r=Efp((ROOT/'app/src/main/assets/game/0116/data.efp').read_bytes());assert r.n()==0x45465031
 for _ in range(3):r.blob()
 starts=[]
 for _ in range(r.n()):r.blob();starts.append(r.n());r.n()
 code=r.blob()
 for _ in range(r.n()):r.n();r.n()
 for _ in range(r.n()):r.blob();r.n()
 r.ints()
 for _ in range(r.n()):r.blob();r.n();r.ints();r.blob();r.ints()
 for _ in range(r.n()):r.n();r.n()
 r.blob();r.n();r.n();r.n();tail=r.blob();assert r.p==len(r.b)
 return tail,{i:struct.unpack_from('<H',code,p*16+1)[0] for i,p in enumerate(starts) if p*16<len(code)}
def main():
 tail,durations=load();u=machine();u.mem_write(0x435470,tail);task=0x1000000;stack=0x13ff000;stop=0x13ffff0;started=[];created=[]
 def hook(u,address,size,user):
  if address not in (0x415190,0x406450,0x4068e0,0x406570):return
  sp=u.reg_read(UC_X86_REG_ESP);v=0
  if address==0x4068e0:
   skill=get(u,sp+4);started.append(skill);v=durations[skill]
  if address==0x406570:created.append(get(u,sp+4));v=0x1010000
  ret=get(u,sp);u.reg_write(UC_X86_REG_EAX,v);u.reg_write(UC_X86_REG_ESP,sp+4);u.reg_write(UC_X86_REG_EIP,ret)
 u.hook_add(UC_HOOK_CODE,hook);rows=[]
 phases=(110,111,112,200,300,510,511,520,521,522,523,530,531,532,533,540,541,900,901,902)
 for phase in phases:
  for wait in (0,1,200):
   for hp1,hp2 in ((400,400),(250,400),(400,250),(0,400),(400,0),(0,0)):
    for timer in ((-1,0,1,100) if phase==200 else (100,)):
     for scores in (((0,0),(3,1)) if phase==902 else ((0,0),)):
      started.clear();created.clear();u.mem_write(task,bytes(0x200));u.mem_write(0x4701e0,bytes(0x17e*1024));u.mem_write(0x4d1d80,bytes(0xe03f*8))
      for a,v in ((0x4701bc,0),(0x4280d8,0),(0x424718,0),(0x4cfa00,task),(task+0x152,phase),(task+0x156,wait),(0x470058,1),(0x470044,1),(0x470048,3),(0x470050,timer),(0x4dfc6d,scores[0]),(0x4edcac,scores[1])):put(u,a,v)
      for p,hp in enumerate((hp1,hp2)):
       c=0x4d1d80+p*0xe03f;t=0x4701e0+p*0x17e
       for a,v in ((c+0xdf05,hp),(c+0xdf11,400),(t,4),(t+0x156,p),(t+0x15a,0)):put(u,a,v)
      u.reg_write(UC_X86_REG_ESP,stack);put(u,stack,stop);u.emu_start(0x4086a0,stop,count=200000)
      assert u.reg_read(UC_X86_REG_EIP)==stop
      out=[get(u,a) for a in (task+0x152,task+0x156,0x470050,0x4dfc6d,0x4edcac)]
      if 10 in created:out[0]=1000
      rows.append({'input':[phase,wait,timer,hp1,hp2,*scores],'output':out,'skills':started.copy()})
 (ROOT/'research/match_x86_results.json').write_text(json.dumps({'scope':'original 0x4086a0 single-VS, stubbed presentation only','cases':rows},indent=2))
 print('PASS native dispatcher executed',len(rows),'cases')
if __name__=='__main__':main()
