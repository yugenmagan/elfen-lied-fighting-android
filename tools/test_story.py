#!/usr/bin/env python3
"""Portable TEST005 gates; shipped fixtures allow testing without Windows tools."""
from pathlib import Path
import subprocess
ROOT=Path(__file__).resolve().parents[1]
def run(args,name):
 with (ROOT/'research'/name).open('w') as log:
  p=subprocess.run([str(a) for a in args],cwd=ROOT,stdout=log,stderr=subprocess.STDOUT)
 print(name,'PASS' if p.returncode==0 else 'FAIL',flush=True)
 if p.returncode:print((ROOT/'research'/name).read_text()[-8000:]);p.check_returncode()
def main():
 out=ROOT/'builds/story_tests';out.mkdir(parents=True,exist_ok=True)
 names=['StoryDataTest','StoryPlayTest','StoryBranchesTest','BattleRenderCheck','DemoRenderCheck']
 sources=sorted((ROOT/'runtime/src').rglob('*.java'))+[ROOT/'tests'/(n+'.java') for n in names]
 run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','-encoding','UTF-8','-d',out,*sources],'story_compile_005.log')
 assets=ROOT/'app/src/main/assets/game'
 for name,extra,log in [('org.elfen.engine.StoryDataTest',[ROOT/'research/story_round_x86_fixtures.txt',ROOT/'research/demo_x86_fixtures.txt'],'story_data_005.log'),('org.elfen.engine.StoryBranchesTest',[],'story_branches_005.log'),('org.elfen.engine.StoryPlayTest',['0170','80'],'story_play_005.log'),('DemoRenderCheck',[ROOT/'docs/story005-render'],'story_render_005.log')]:
  run(['java','-Xmx512m','-Djava.awt.headless=true','-cp',out,name,assets,*extra],log)
if __name__=='__main__':main()
