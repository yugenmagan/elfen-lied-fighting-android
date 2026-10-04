#!/usr/bin/env python3
"""Focused HUD/native-data tests and reviewable desktop renders."""
from pathlib import Path
import subprocess,sys
ROOT=Path(__file__).resolve().parents[1]
def run(cmd,name):
    with (ROOT/'research'/name).open('w') as f:r=subprocess.run(list(map(str,cmd)),cwd=ROOT,stdout=f,stderr=subprocess.STDOUT)
    print((ROOT/'research'/name).read_text()[-3500:],end='',flush=True);r.check_returncode()
def main():
    if '--probe' in sys.argv:run(['python3','tools/probe_hud_x86.py'],'hud_native_007d.log')
    out=ROOT/'builds/hud_tests';out.mkdir(parents=True,exist_ok=True)
    source=[p for p in (ROOT/'runtime/src').rglob('*.java') if not any(n.startswith('.') for n in p.relative_to(ROOT).parts[:-1])]
    source += [ROOT/'tests'/n for n in ('BattleHudTest.java','BattleRenderCheck.java','HudRenderCheck.java')]
    run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','-encoding','UTF-8','-d',out,*source],'hud_compile_007d.log')
    run(['java','-Xmx512m','-cp',out,'org.elfen.engine.BattleHudTest',ROOT/'app/src/main/assets/game',ROOT/'research/hud'],'hud_tests_007d.log')
    run(['java','-Xmx512m','-Djava.awt.headless=true','-cp',out,'HudRenderCheck',ROOT/'app/src/main/assets/game',ROOT/'docs/hud007d-render'],'hud_render_007d.log')
if __name__=='__main__':main()
