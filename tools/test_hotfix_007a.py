#!/usr/bin/env python3
"""Run original-can differential fixtures and actual touch-stick adapter checks."""
import json,subprocess,sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
def run(args,log):
    result=subprocess.run([str(x) for x in args],cwd=ROOT,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True)
    (ROOT/'research'/log).write_text(result.stdout);print(result.stdout,flush=True);result.check_returncode()
def main():
    if '--probe' in sys.argv:run([sys.executable,ROOT/'tools/probe_nana_can_x86.py'],'nana_can_x86_007a.log')
    dest=ROOT/'builds/hotfix_tests';dest.mkdir(parents=True,exist_ok=True)
    native=json.loads((ROOT/'research/nana_can_x86_007a.json').read_text());rows=[]
    for key,tag in (('contacts','contact'),('motion','motion')):
        rows += [tag+' '+' '.join(map(str,row)) for row in native[key]]
    fixture=dest/'nana-can.txt';fixture.write_text('\n'.join(rows)+'\n')
    sources=sorted((ROOT/'runtime/src').rglob('*.java'))+sorted((ROOT/'app/src/main/java/org/elfen/controls').rglob('*.java'))
    sources += [ROOT/'tests'/(n+'.java') for n in ('ContactTest','NanaCanTest','ArcadeStickTest','ReplayTest')]
    run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','-encoding','UTF-8','-d',dest,*sources],'hotfix_compile_007a.log')
    assets=ROOT/'app/src/main/assets/game'
    for name,extra,log in [('org.elfen.engine.NanaCanTest',[fixture],'nana_can_007a.log'),('org.elfen.controls.ArcadeStickTest',[],'arcade_stick_007a.log'),('org.elfen.engine.ReplayTest',[ROOT/'research/replay_hotfix_007a.efr'],'replay_007a.log')]:
        run(['java','-Xmx512m','-cp',dest,name,assets,*extra],log)
if __name__=='__main__':main()
