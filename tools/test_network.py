#!/usr/bin/env python3
"""Reproducible local TLS relay and paired rollback gates; no public service needed."""
from pathlib import Path
import subprocess
ROOT=Path(__file__).resolve().parents[1]
def main():
    out=ROOT/'builds/network_tests';out.mkdir(parents=True,exist_ok=True)
    cert=ROOT/'builds/network-cert';cert.mkdir(parents=True,exist_ok=True)
    subprocess.run(['openssl','req','-x509','-newkey','rsa:2048','-nodes','-subj','/CN=localhost','-addext','subjectAltName=DNS:localhost','-keyout',str(cert/'key.pem'),'-out',str(cert/'cert.pem'),'-days','2'],check=True,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
    subprocess.run(['openssl','pkcs12','-export','-inkey',str(cert/'key.pem'),'-in',str(cert/'cert.pem'),'-out',str(cert/'server.p12'),'-passout','pass:local-test-only'],check=True)
    sources=[p for directory in ['runtime/src','network/src','server/src'] for p in (ROOT/directory).rglob('*.java')]+[ROOT/'tests'/(n+'.java') for n in ['StoryDataTest','MatchRollbackTest','RollbackPeerTest','NetworkTest']]
    subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','--release','8','-encoding','UTF-8','-d',str(out),*[str(p) for p in sources]],cwd=ROOT,check=True)
    for name,extra,log in [('RollbackPeerTest',[],'rollback_peers_006.log'),('NetworkTest',[cert/'server.p12',cert/'cert.pem'],'network_tls_006.log')]:
        path=ROOT/'research'/log
        with path.open('w') as f:r=subprocess.run(['java','-Xmx384m','-cp',str(out),'org.elfen.engine.'+name,str(ROOT/'app/src/main/assets/game'),*[str(p) for p in extra]],cwd=ROOT,stdout=f,stderr=subprocess.STDOUT)
        text=path.read_text();print(text,flush=True);r.check_returncode()
        if 'PASS '+name+' checks=' not in text:raise RuntimeError('Missing completion marker: '+name)
if __name__=='__main__':main()
