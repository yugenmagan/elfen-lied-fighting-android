#!/usr/bin/env python3
"""Standalone server JAR from the checkpoint; no third-party dependencies or assets."""
from pathlib import Path
import subprocess,zipfile,shutil
ROOT=Path(__file__).resolve().parents[1]
out=ROOT/'builds/server_classes'
if out.exists():shutil.rmtree(out)
out.mkdir(parents=True)
sources=[p for folder in ['runtime/src','network/src','server/src'] for p in (ROOT/folder).rglob('*.java')]
subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','--release','8','-encoding','UTF-8','-d',str(out),*[str(p) for p in sources]],check=True)
target=ROOT/'builds/elfen-room-server.jar'
with zipfile.ZipFile(target,'w',compression=zipfile.ZIP_DEFLATED) as z:
    z.writestr('META-INF/MANIFEST.MF','Manifest-Version: 1.0\nMain-Class: org.elfen.server.RoomServer\n\n')
    for p in sorted(out.rglob('*.class')):z.write(p,p.relative_to(out).as_posix())
print(target)
