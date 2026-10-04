#!/usr/bin/env python3
"""Recovery gates using packaged assets/fixtures only; no Fighting.zip dependency."""
from pathlib import Path
import subprocess,sys
ROOT=Path(__file__).resolve().parents[1]
def run(args,logname):
    result=subprocess.run([str(x) for x in args],cwd=ROOT,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
    (ROOT/'research/recovery'/logname).write_text(result.stdout)
    print(result.stdout,end='',flush=True);result.check_returncode()
def main():
    run([sys.executable,'tools/check_apk.py'],'exact-apk-check.log')
    run([sys.executable,'tools/test_combat.py'],'combat-regression.log')
    out=ROOT/'builds/asset_tests';out.mkdir(parents=True,exist_ok=True)
    sources=sorted((ROOT/'runtime/src').rglob('*.java'))+[ROOT/'tests'/n for n in ('EngineTest.java','MotionScenarios.java','CombatInputScenarios.java')]
    run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','-encoding','UTF-8','-d',out,*sources],'asset-tests-compile.log')
    for name in ('EngineTest','MotionScenarios','CombatInputScenarios'):
        run(['java','-Xmx512m','-cp',out,name,ROOT/'app/src/main/assets/game'],name+'.log')
if __name__=='__main__':main()
