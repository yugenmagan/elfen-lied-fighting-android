#!/usr/bin/env python3
from pathlib import Path
import subprocess
ROOT=Path(__file__).resolve().parents[1]
out=ROOT/'builds/rc1-title';out.mkdir(parents=True,exist_ok=True)
src=list((ROOT/'runtime/src').rglob('*.java'))+list((ROOT/'tests/desktop_graphics').rglob('*.java'))+[ROOT/p for p in ['app/src/main/java/org/elfen/controls/TitleLayout.java','app/src/main/java/org/elfen/fighting/TitleScreen.java','tests/TitleScreenPreview.java']]
subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','-encoding','UTF-8','-d',str(out),*map(str,src)],check=True)
subprocess.run(['java','-Djava.awt.headless=true','-cp',str(out),'org.elfen.fighting.TitleScreenPreview',str(ROOT/'app/src/main/assets'),str(ROOT/'research/rc1-title')],check=True)
