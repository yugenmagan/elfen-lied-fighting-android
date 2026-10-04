#!/usr/bin/env python3
"""Headless TEST004 gates. Packaged assets/fixtures suffice; no proprietary tools."""
from pathlib import Path
import json,subprocess
ROOT=Path(__file__).resolve().parents[1]
def run(args,name):
 p=subprocess.run([str(x) for x in args],cwd=ROOT,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
 (ROOT/'research'/name).write_text(p.stdout);print(p.stdout,end='',flush=True);p.check_returncode()
def main():
 out=ROOT/'builds/match_tests';out.mkdir(parents=True,exist_ok=True)
 cases=json.loads((ROOT/'research/match_x86_results.json').read_text())['cases'];fixture=out/'match-fixtures.txt'
 fixture.write_text('\n'.join(' '.join(map(str,c['input']+c['output']+[len(c['skills'])]+c['skills'])) for c in cases)+'\n')
 names=['MatchTest','MatchRollbackTest','MatchBranchesTest','EngineTest','MotionScenarios','CombatInputScenarios','BattleRenderCheck','MatchRenderCheck']
 sources=sorted((ROOT/'runtime/src').rglob('*.java'))+[ROOT/'tests'/(x+'.java') for x in names]
 run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','-encoding','UTF-8','-d',out,*sources],'match_compile_004.log')
 assets=ROOT/'app/src/main/assets/game'
 for name,extra,log in [('org.elfen.engine.MatchTest',[fixture,ROOT/'research/match-recording.efm'],'match_tests_004.log'),('org.elfen.engine.MatchRollbackTest',[],'match_rollback_004.log'),('org.elfen.engine.MatchBranchesTest',[],'match_branches_004.log'),('EngineTest',[],'match_assets_004.log'),('MotionScenarios',[],'match_motion_004.log'),('CombatInputScenarios',[],'match_input_004.log'),('MatchRenderCheck',[ROOT/'docs/match004-render'],'match_render_004.log')]:
  run(['java','-Xmx512m','-Djava.awt.headless=true','-cp',out,name,assets,*extra],log)
if __name__=='__main__':main()
