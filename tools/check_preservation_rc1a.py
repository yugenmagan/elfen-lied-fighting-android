#!/usr/bin/env python3
"""Compare the UI patch with the private RC1 source checkpoint."""
from pathlib import Path
import hashlib,json,sys,zipfile
from test_release_flow import method
ROOT=Path(__file__).resolve().parents[1]
def main():
 original=Path(sys.argv[1])
 assert hashlib.sha256(original.read_bytes()).hexdigest()=='e1b6ae8a6605b06a8f9d363706767c5015b5ff13959b4d6b67c9eb821eb4e9d6'
 counts={}
 with zipfile.ZipFile(original) as z:
  for folder in ['runtime/src','network/src','app/src/main/assets','app/src/main/res']:
   disk={p.relative_to(ROOT).as_posix():p for p in (ROOT/folder).rglob('*') if p.is_file()}
   old={x.removeprefix('elfen_rc1/') for x in z.namelist() if x.startswith('elfen_rc1/'+folder+'/') and not x.endswith('/')}
   assert set(disk)==old,folder
   for name,p in disk.items(): assert p.read_bytes()==z.read('elfen_rc1/'+name),name
   counts[folder]=len(disk)
  path='app/src/main/java/org/elfen/fighting/MainActivity.java'
  before=z.read('elfen_rc1/'+path).decode();after=(ROOT/path).read_text()
  assert method(before,'public boolean onTouchEvent(MotionEvent e)')==method(after,'public boolean onTouchEvent(MotionEvent e)')
  for marker in ['void layoutControls()','float buttonX(int i)','float buttonY(int i)','void drawControls(Canvas c)','protected void onDraw(Canvas c)']:
   before=before.replace(method(before,marker),'/*same*/');after=after.replace(method(after,marker),'/*same*/')
  after=after.replace('org.elfen.controls.CombatLayout controlLayout;','')
  assert ''.join(before.split())==''.join(after.split()),'Other Activity logic changed'
  for name in z.namelist():
   rel=name.removeprefix('elfen_rc1/')
   if rel.startswith('app/src/main/java/') and rel.endswith('.java') and rel!=path: assert z.read(name)==(ROOT/rel).read_bytes(),rel
 result={'unchanged_files':counts,'actual_touch_handler_identical':True,'all_existing_activity_logic_outside_five_layout_drawing_methods_identical':True,'combat_audio_network_lifecycle_changes':False}
 (ROOT/'research/preservation_rc1a.json').write_text(json.dumps(result,indent=2));print(json.dumps(result,indent=2))
if __name__=='__main__':main()
