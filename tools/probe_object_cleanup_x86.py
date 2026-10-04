#!/usr/bin/env python3
"""Execute original 40e4a0; verify numbered OO links are cleared on retirement."""
import json
from probe_x86 import machine,put,get,ROOT
from unicorn.x86_const import UC_X86_REG_ESP,UC_X86_REG_EIP

def main():
 u=machine();task=0x4701e0+4*0x17e;other=task+0x17e;stack=0x13ff000;stop=0x13ffff0;rows=[]
 for player in (0,1):
  for kind in (0,1,2):
   for slot in (0,3,9):
    u.mem_write(task,bytes(0x17e));put(u,task,4);put(u,task+0x15a,kind);put(u,task+0x156,player);put(u,0x4cfa00,task)
    slots=0x4d1d80+player*0xe03f+0xdfbf
    before=[task if n==slot else other for n in range(10)]
    for n,v in enumerate(before):put(u,slots+n*4,v)
    put(u,stack,stop);u.reg_write(UC_X86_REG_ESP,stack);u.emu_start(0x40e4a0,stop,count=1000);assert u.reg_read(UC_X86_REG_EIP)==stop
    after=[get(u,slots+n*4) for n in range(10)]
    expected=[0 if kind==1 and v==task else v for v in before]
    assert after==expected and get(u,task)==1
    rows.append({'player':player,'kind':kind,'number':slot,'before':before,'after':after,'retired_task_type':get(u,task)})
 (ROOT/'research/object_cleanup_x86_005.json').write_text(json.dumps({'scope':'isolated original 40e4a0, not full Windows execution','cases':rows},indent=2))
 print('PASS original OO link retirement',len(rows),'cases')
if __name__=='__main__':main()
