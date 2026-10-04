#!/usr/bin/env python3
"""Clash and absent-reaction behavior, executed from the supplied PE image."""
import json,itertools,struct
from probe_combat_x86 import *

def main():
 u=machine();rows=[]
 for power_a,power_b,trigger_a,trigger_b,apart,flag_b in itertools.product((0,5),(0,7),(0,61),(0,62),(0,1),(0,8)):
  setup(u);put(u,0x4438a4,13)
  for task,addr,power,trigger,x in ((TASK,FA,power_a,trigger_a,390),(TASK+STRIDE,FD,power_b,trigger_b,590 if apart else 410)):
   u.mem_write(addr,box(24,0,-60,30,40,0,0,power));put(u,task+8,x<<16);put(u,task+0x89,addr);put(u,task+0x74,trigger)
  put(u,TASK+STRIDE+0x15e,flag_b)
  u.emu_start(0x40eb60,STOP,count=1000000)
  assert u.reg_read(UC_X86_REG_EIP)==STOP
  out=[]
  for task in (TASK,TASK+STRIDE):out.extend([int(get(u,task+0x89)!=0),get(u,task+0x38),get(u,task+0x74)])
  rows.append({'input':[power_a,power_b,trigger_a,trigger_b,apart,flag_b],'output':out})
 errors=[]
 for empty in (0,1):
  setup(u,guard=1);put(u,0x424744,0)
  if empty:put(u,TASK+0x129,0)
  else:u.mem_write(R,struct.pack('<B6H3x',23,1,2,3,0,0,0))
  u.emu_start(0x40f010,STOP,count=2000000);assert u.reg_read(UC_X86_REG_EIP)==STOP
  errors.append({'absent_r':empty,'life':get(u,CHAR+CS+0xdf05),'pending':get(u,TASK+STRIDE+0x38),'attacker_flags':get(u,TASK+0x15e),'defender_flags':get(u,TASK+STRIDE+0x15e)})
 (ROOT/'research/contacts_x86_results.json').write_text(json.dumps({'clashes':rows,'reaction_errors':errors},indent=2));print('Original clash',len(rows),'and reaction error',len(errors),'cases')
if __name__=='__main__':main()
