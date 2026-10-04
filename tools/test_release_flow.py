#!/usr/bin/env python3
"""Execute production Activity methods with queued-dialog fakes and real game states.
Not an Android emulator: tests ordering, input gates and real snapshots on the JVM.
"""
from pathlib import Path
import subprocess
ROOT=Path(__file__).resolve().parents[1]
def method(text,marker):
 a=text.index(marker);i=text.index('{',a)+1;depth=1
 while depth:
  if text[i]=='{':depth+=1
  if text[i]=='}':depth-=1
  i+=1
 return text[a:i].replace('@Override ','')
def main():
 source=(ROOT/'app/src/main/java/org/elfen/fighting/MainActivity.java').read_text()
 markers=['private void route(','private void show(AlertDialog d){','private void show(AlertDialog d,Runnable back)','private boolean canPlayAudio()','private void showTitle()','private void endCurrentGame()','private void titleAction(','private void titleNavigate(','private void pauseMenu()','private void resumeBattle()','private void controlMenu()','private void clearInputs()','private void chooseLanguage(','private void openSelection(boolean storyMode,boolean fromModes)','private void selectionBack()','private void loadSession()','private void loadStory()','protected void onResume()','protected void onPause()','protected void onDestroy()','public void onWindowFocusChanged(']
 guard=source.split('&&(selection!=null||!replayDone)')[1].split('{',1)[0];assert guard.startswith('&&') and guard.endswith(')');guard=guard[2:-1]
 startup=method(source,'public void onCreate(')
 assert 'loadSession()' not in startup and 'loadSavedStory' not in source and 'saveStoryToDisk' not in source
 assert '"en"' in startup and 'releaseLanguageChosen' in startup and 'story-save.bin")).delete()' in startup
 draw=method(source,'protected void onDraw(');assert 'TEST 008' not in draw and 'lastCommand' not in draw and '%03X' not in draw
 assert 'new Handler().post' not in source
 template=(ROOT/'tests/ReleaseFlowHarness.java.in').read_text().replace('/*ACTIVITY_METHODS*/','\n'.join(method(source,m) for m in markers)).replace('/*FRAME_GUARD*/',guard).replace('/*TITLE_TOUCH*/',method(source,'boolean titleTouch(MotionEvent e)'))
 out=ROOT/'builds/rc1-flow';out.mkdir(parents=True,exist_ok=True);java=out/'ReleaseFlowHarness.java';java.write_text(template)
 sources=list((ROOT/'runtime/src').rglob('*.java'))+list((ROOT/'app/src/main/java/org/elfen/controls').rglob('*.java'))+[ROOT/'tests/StoryDataTest.java',java]
 subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','-encoding','UTF-8','-d',str(out),*map(str,sources)],check=True)
 subprocess.run(['java','-Xmx512m','-cp',str(out),'org.elfen.engine.ReleaseFlowHarness',str(ROOT/'app/src/main/assets')],check=True)
 print('PASS cold-start/no-disk-save/debug-overlay source gates; 22 production Activity/touch methods executed with lifecycle/dialog fakes')
if __name__=='__main__':main()
