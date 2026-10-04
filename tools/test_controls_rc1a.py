#!/usr/bin/env python3
"""Layout matrix and real control paint methods; Java2D is not Android."""
from pathlib import Path
import subprocess
from test_release_flow import method
ROOT=Path(__file__).resolve().parents[1]
def main():
 source=(ROOT/'app/src/main/java/org/elfen/fighting/MainActivity.java').read_text()
 text=(ROOT/'tests/ControlPreview.java.in').read_text().replace('/*DRAW_METHODS*/','\n'.join(method(source,m) for m in ['void drawControls(Canvas c)','void drawStartButton(Canvas c)']))
 out=ROOT/'builds/control-tests';out.mkdir(parents=True,exist_ok=True);java=out/'ControlPreview.java';java.write_text(text)
 files=[java,ROOT/'runtime/src/org/elfen/engine/Input.java',*sorted((ROOT/'app/src/main/java/org/elfen/controls').glob('*.java')),*sorted((ROOT/'tests/desktop_graphics').rglob('*.java'))]
 subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','-encoding','UTF-8','-d',str(out),*map(str,files)],check=True)
 subprocess.run(['java','-Djava.awt.headless=true','-cp',str(out),'ControlPreview',str(ROOT/'docs/hud007d-render/0170-0104-desktop.png'),str(ROOT/'docs/controls-rc1a')],check=True)
if __name__=='__main__':main()
