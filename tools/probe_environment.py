#!/usr/bin/env python3
"""Report build/execution prerequisites; never changes host permissions."""
import argparse
import json
from pathlib import Path
import platform
import shutil
import socket
import subprocess
import tempfile

p=argparse.ArgumentParser();p.add_argument('--wine');p.add_argument('--output',required=True);a=p.parse_args()
report={'platform':platform.platform(),'tools':{k:shutil.which(k) for k in ['java','javac','gradle','adb','emulator','sdkmanager','wine','cmake','ninja']},'kvm_available':Path('/dev/kvm').exists()}
with tempfile.TemporaryDirectory() as tmp:
    for label,family,address in [('tcp_loopback',socket.AF_INET,('127.0.0.1',0)),('unix_domain',socket.AF_UNIX,str(Path(tmp)/'probe.sock'))]:
        s=None
        try:
            s=socket.socket(family,socket.SOCK_STREAM);s.bind(address);s.listen(1)
            report[label]={'status':'ok'}
        except OSError as exc:report[label]={'status':'failed','errno':exc.errno,'message':str(exc)}
        finally:
            if s is not None:s.close()
    if a.wine:
        import os
        env=dict(os.environ,WINEPREFIX=str(Path(tmp)/'prefix'),WINEDLLOVERRIDES='winemenubuilder.exe=d',WINEDEBUG='err+all')
        cp=subprocess.run([a.wine,'cmd','/c','ver'],env=env,capture_output=True,text=True,timeout=45)
        report['wine_probe']={'executable':a.wine,'command':['wine','cmd','/c','ver'],'returncode':cp.returncode,'stdout':cp.stdout,'stderr':cp.stderr}
Path(a.output).write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf8')
print(json.dumps(report,ensure_ascii=False,indent=2))
