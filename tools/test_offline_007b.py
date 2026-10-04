#!/usr/bin/env python3
"""Offline patch acceptance gates; original assets are read-only."""
from pathlib import Path
import subprocess,sys
ROOT=Path(__file__).resolve().parents[1]
def run(args,name):
    log=ROOT/'research'/name
    with log.open('w') as f:p=subprocess.run(list(map(str,args)),cwd=ROOT,stdout=f,stderr=subprocess.STDOUT)
    print(log.read_text()[-4500:],end='',flush=True);p.check_returncode()
def main():
    out=ROOT/'builds/offline007b_tests';out.mkdir(parents=True,exist_ok=True)
    names=['Offline007bTest','StoryDataTest','StoryPlayTest','MatchRollbackTest','MatchBranchesTest','EngineTest','CharacterSelectTest']
    source=sorted((ROOT/'runtime/src').rglob('*.java'))+sorted((ROOT/'app/src/main/java/org/elfen/controls').glob('*.java'))+[ROOT/'tests'/(n+'.java') for n in names]
    run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','-encoding','UTF-8','-d',out,*source],'offline_compile_007b.log')
    assets=ROOT/'app/src/main/assets/game'
    for name,args,log in [('org.elfen.engine.Offline007bTest',[],'offline_tests_007b.log'),('org.elfen.engine.MatchRollbackTest',[],'rollback_regression_007b.log'),('org.elfen.engine.MatchRollbackTest',['--classic'],'rollback_classic_007b.log'),('EngineTest',[],'assets_regression_007b.log'),('org.elfen.engine.CharacterSelectTest',[ROOT/'research'],'selection_regression_007b.log')]:
        run(['java','-Xmx512m','-cp',out,name,assets,*args],log)
    if '--story' in sys.argv:
        run(['java','-Xmx512m','-cp',out,'org.elfen.engine.StoryPlayTest',assets,'0110','80','19','900000','3','true'],'story_mayu_first2_007b.log')
if __name__=='__main__':main()
