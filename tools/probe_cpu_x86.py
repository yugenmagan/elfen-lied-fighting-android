#!/usr/bin/env python3
import json,struct
from probe_combat_x86 import machine,put,get,setup,TASK,STRIDE,CHAR,CS,STACK,STOP,ROOT
from unicorn.x86_const import *
def put_player(u,ident,index):
 p=json.loads((ROOT/f'research/containers/{ident}.player.json').read_text());sp=p['specific'];c=CHAR+index*CS
 raw=b''.join(bytes.fromhex(r['raw']) for r in sp['cpu_slots'])+struct.pack('<24H',*sp['builtin_skill_indices']);u.mem_write(c+0x2246,raw)
 for i,v in enumerate(sp['commands']):
  data=v['name'].encode('cp932').ljust(32,b'\0')+struct.pack('<5H',v['time_raw'],*v['skill_indices'])+bytes.fromhex(v['steps_raw'])+struct.pack('<10H',*v['amounts_raw']);u.mem_write(c+0x4d50+(i+1)*82,data)
 return p

def main():
 out=[]
 for ident in ('0170','0104','0128'):
  for level,distance,self_y,other_y in [(20,80,920,920),(50,400,920,920),(80,150,800,920),(100,230,800,800)]:
   u=machine();setup(u);put_player(u,ident,0);c=CHAR
   put(u,0x4cfa00,TASK);put(u,0x47004c,1);put(u,0x41fb1c,1)
   for off,val in [(0xdf5d,1),(0xdf61,level),(0xdf65,1),(0xdf69,TASK+STRIDE),(0xdf41,390<<16),(0xdf45,self_y<<16)]:put(u,c+off,val)
   put(u,CHAR+CS+0xdf41,(390+distance)<<16);put(u,CHAR+CS+0xdf45,other_y<<16);rows=[]
   for tick in range(120):
    put(u,0x447ee0,tick&1023);u.reg_write(UC_X86_REG_ESP,STACK);put(u,STACK,STOP);u.emu_start(0x411270,STOP,count=500000)
    if u.reg_read(UC_X86_REG_EIP)!=STOP:raise RuntimeError('CPU did not return')
    rows.append({'state':[get(u,c+off) for off in (0xdf75,0xdf81,0xdf79,0xdf7d)],'rng':get(u,0x41fb1c)&0xffffffff,'history':[get(u,0x4280e0+((tick-age)&1023)*4) for age in range(1024)]})
   out.append({'id':ident,'level':level,'distance':distance,'self_y':self_y,'other_y':other_y,'rows':rows});print('CPU',ident,level,flush=True)
 (ROOT/'research/cpu_x86_results.json').write_text(json.dumps(out,separators=(',',':')))
if __name__=='__main__':main()
