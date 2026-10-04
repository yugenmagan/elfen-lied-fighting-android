#!/usr/bin/env python3
"""TEST007 selection and regression gates. Shipped fixtures need no Windows."""
from pathlib import Path
import subprocess,sys
ROOT=Path(__file__).resolve().parents[1]
def run(args,log):
 with (ROOT/'research'/log).open('w') as f:p=subprocess.run(list(map(str,args)),cwd=ROOT,stdout=f,stderr=subprocess.STDOUT)
 print((ROOT/'research'/log).read_text()[-3500:],end='',flush=True);p.check_returncode()
def main():
 out=ROOT/'builds/selection_tests';out.mkdir(parents=True,exist_ok=True)
 names=['CharacterSelectTest','StoryDataTest','BattleRenderCheck','SelectionRenderCheck','MatchRollbackTest','MatchBranchesTest','EngineTest','StoryPlayTest']
 sources=sorted((ROOT/'runtime/src').rglob('*.java'))+[ROOT/'tests'/(n+'.java') for n in names]
 run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','-encoding','UTF-8','-d',out,*sources],'selection_compile_007.log');assets=ROOT/'app/src/main/assets/game'
 tests=[('org.elfen.engine.CharacterSelectTest',[ROOT/'research'],'selection_tests_007.log'),('SelectionRenderCheck',[ROOT/'docs/selection007-render'],'selection_render_007.log')]
 if '--regression' in sys.argv:tests.extend([('org.elfen.engine.MatchRollbackTest',[],'regression_rollback_007.log'),('org.elfen.engine.MatchBranchesTest',[],'regression_rounds_007.log'),('EngineTest',[],'regression_assets_007.log'),('org.elfen.engine.StoryPlayTest',['0170','80'],'regression_lucy_story_007.log')])
 for name,extra,log in tests:run(['java','-Xmx512m','-Djava.awt.headless=true','-cp',out,name,assets,*extra],log)
if __name__=='__main__':main()
