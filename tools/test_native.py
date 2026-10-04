#!/usr/bin/env python3
"""Run local parser/native VM tests; persist exact output and fail on any error."""
from pathlib import Path
import subprocess
import sys
ROOT=Path(__file__).resolve().parents[1]

def run(args,log):
    result=subprocess.run([str(x) for x in args],cwd=ROOT,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True)
    (ROOT/'research'/log).write_text(result.stdout)
    print(result.stdout,end='',flush=True)
    result.check_returncode()

def main():
    milestone='003' if '--milestone-003' in sys.argv else '002'
    (ROOT/'builds/tests').mkdir(parents=True,exist_ok=True)
    sources=sorted((ROOT/'runtime/src').rglob('*.java'))+[ROOT/'tests'/name for name in ('EngineTest.java','MotionScenarios.java','RenderCheck.java')]
    run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','-encoding','UTF-8','-d','builds/tests',*sources],'java_compile_'+milestone+'.txt')
    run([sys.executable,'-m','unittest','discover','-s','tests','-v'],'static_tests_'+milestone+'.txt')
    run(['java','-cp','builds/tests','EngineTest','app/src/main/assets/game'],'engine_test_'+milestone+'.txt')
    run(['java','-cp','builds/tests','MotionScenarios','app/src/main/assets/game'],'motion_scenarios_'+milestone+'.txt')
    if '--x86' in sys.argv:run([sys.executable,'tools/probe_x86.py'],'x86_probe_002.txt')
    if '--render' in sys.argv:run(['java','-Djava.awt.headless=true','-cp','builds/tests','RenderCheck','app/src/main/assets/game','research/render_checks'],'render_check_002.txt')

if __name__=='__main__':main()
