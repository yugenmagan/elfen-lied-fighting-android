#!/usr/bin/env python3
"""Compare RC1 with the exact accepted TEST008 source checkpoint."""
from pathlib import Path
import zipfile,hashlib,json,sys
from test_release_flow import method
ROOT=Path(__file__).resolve().parents[1]
base=Path(sys.argv[1]) if len(sys.argv)>1 else ROOT.parent/'test008_downloads/Elfen_Lied_Android_008_project.zip'
assert hashlib.sha256(base.read_bytes()).hexdigest()=='d07462d60e90015dbd552533349d4b969964815e1b1e6b5b87ffea15f6b972c9'
counts={};prefix='elfen_test008/'
with zipfile.ZipFile(base) as z:
 for folder in ['runtime/src','network/src','app/src/main/assets/game','app/src/main/res']:
  entries=[n for n in z.namelist() if n.startswith(prefix+folder+'/') and not n.endswith('/')]
  for n in entries:assert z.read(n)==(ROOT/n[len(prefix):]).read_bytes(),n
  counts[folder]=len(entries)
 for name in ['app/src/main/java/org/elfen/fighting/Audio.java','app/src/main/java/org/elfen/controls/ArcadeStick.java','app/src/main/java/org/elfen/controls/FrameInput.java','app/src/main/java/org/elfen/controls/PauseGate.java','app/src/main/assets/localization/scenes.tsv','localization/scenes_source.tsv']:
  assert z.read(prefix+name)==(ROOT/name).read_bytes(),name
 name='app/src/main/java/org/elfen/fighting/MainActivity.java';before=z.read(prefix+name).decode();after=(ROOT/name).read_text()
 methods=['void layoutControls()','void drawControls(Canvas c)','void drawStartButton(Canvas c)','int activeButtons()','float buttonX(int i)','float buttonY(int i)']
 for marker in methods:assert method(before,marker)==method(after,marker),marker
 original=method(before,'public boolean onTouchEvent(MotionEvent e)');actual=method(after,'public boolean onTouchEvent(MotionEvent e)').replace('            if(flow.atTitle())return titleTouch(e);\n','')
 assert original==actual,'Combat touch branch changed'
report={'base_zip':base.name,'base_sha256':hashlib.sha256(base.read_bytes()).hexdigest(),'unchanged_file_counts':counts,'audio_unchanged':True,'combat_touch_branch_unchanged':True,'controls_rendering_and_layout_unchanged':True,'scene_translations_unchanged':True,'game_logic_unchanged':True,'note':'New title and lifecycle/dialog presentation only; original source assets untouched.'}
(ROOT/'research/preservation_rc1.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report,indent=2))
