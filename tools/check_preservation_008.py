#!/usr/bin/env python3
"""Read-only comparison with the authoritative TEST007d checkpoint."""
from pathlib import Path
import zipfile,json,hashlib,re,sys
ROOT=Path(__file__).resolve().parents[1]
def method(text,marker):
 start=text.index(marker);p=text.index('{',start);depth=1;i=p+1
 while depth:
  if text[i]=='{':depth+=1
  elif text[i]=='}':depth-=1
  i+=1
 return text[start:i]
def main():
 original=Path(sys.argv[1]);same=[];changed=[];total=0
 with zipfile.ZipFile(original) as z:
  prefix='elfen_test007d/'
  for entry in z.infolist():
   if entry.is_dir():continue
   rel=entry.filename.removeprefix(prefix);p=ROOT/rel
   if p.is_file():
    total+=1
    (same if z.read(entry)==p.read_bytes() else changed).append(rel)
  for rel in changed:
   if rel.startswith(('runtime/src/org/elfen/engine/','network/','server/','app/src/main/assets/game/','app/src/main/res/','app/src/main/java/org/elfen/controls/')):
    assert rel in ('runtime/src/org/elfen/engine/StoryController.java','runtime/src/org/elfen/engine/StoryProgram.java'),rel
  path='app/src/main/java/org/elfen/fighting/MainActivity.java';before=z.read(prefix+path).decode();after=(ROOT/path).read_text()
  exact=['void layoutControls()','float buttonX(int i)','float buttonY(int i)','public boolean onTouchEvent(MotionEvent e)']
  for marker in exact:assert method(before,marker)==method(after,marker),marker
  for path in ['app/src/main/java/org/elfen/fighting/Audio.java','app/src/main/java/org/elfen/fighting/ReportProvider.java','builds/test-signing.p12']:
   assert z.read(prefix+path)==(ROOT/path).read_bytes(),path
 report={'reference_zip_sha256':hashlib.sha256(original.read_bytes()).hexdigest(),'existing_files_compared':total,'unchanged_count':len(same),'changed_existing_files':changed,'combat_input_ai_physics_network_unchanged':True,'all_original_game_assets_unchanged':True,'launcher_icons_unchanged':True,'touch_handler_geometry_haptics_exact':exact,'audio_and_signing_key_unchanged':True}
 (ROOT/'research/preservation_008.json').write_text(json.dumps(report,indent=2));print(json.dumps(report,indent=2))
if __name__=='__main__':main()
