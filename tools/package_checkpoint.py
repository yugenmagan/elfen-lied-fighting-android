#!/usr/bin/env python3
"""Create a secret-free RC1 build14 checkpoint and optionally verify a rebuild.
Signing identity is supplied externally; no signing key enters the archive.
"""
import argparse,hashlib,json,os,shutil,subprocess,zipfile
from pathlib import Path
from bootstrap_android import SDK
ROOT=Path(__file__).resolve().parents[1]
EXCLUDE={'.deps','.gradle','.git','.idea','build','builds','__pycache__'}
SECRET_SUFFIXES={'.p12','.pfx','.jks','.keystore','.key','.pem','.pk8'}
def include(p):
 q=p.relative_to(ROOT)
 if not p.is_file() or any(x in EXCLUDE for x in q.parts):return False
 if q.parts[:2]==('research','android-smoke'):return False
 if p.name in ('local.properties','keystore.properties','signing.properties','credentials.json','gradle_rc1a.log','build_rc1a.log'):return False
 if p.name.startswith('.env') or p.name.startswith('service-account'):return False
 return p.suffix not in SECRET_SUFFIXES|{'.pyc','.idsig','.apk','.aab','.zip','.bundle'}
def main():
 parser=argparse.ArgumentParser();parser.add_argument('--verify',action='store_true');args=parser.parse_args()
 apk=ROOT/'builds/elfen-fighting-v1.0.0-rc1.apk'
 expected=hashlib.sha256(apk.read_bytes()).hexdigest()
 target=ROOT/'builds/Elfen_Lied_Android_RC1_build14_project.zip'
 files=sorted(p for p in ROOT.rglob('*') if include(p))
 with zipfile.ZipFile(target,'w',zipfile.ZIP_DEFLATED,compresslevel=6) as z:
  for p in files:
   if p.is_symlink():raise ValueError('Symlink in checkpoint')
   info=zipfile.ZipInfo('elfen_rc1a/'+p.relative_to(ROOT).as_posix(),(2026,10,4,0,0,0));info.create_system=3;info.external_attr=(p.stat().st_mode&0xffff)<<16;info.compress_type=zipfile.ZIP_DEFLATED;z.writestr(info,p.read_bytes())
 with zipfile.ZipFile(target) as z:assert z.testzip() is None
 report={'zip':target.name,'zip_bytes':target.stat().st_size,'zip_sha256':hashlib.sha256(target.read_bytes()).hexdigest(),'files':len(files),'crc_all_entries_passed':True,'private_signing_key_included':False,'rebuild_verified':False}
 if args.verify:
  if not os.environ.get('ELFEN_KEYSTORE'):raise RuntimeError('External signing identity required for exact signed APK comparison')
  destination=ROOT.parent/'rc1a_zip_verification'
  if destination.exists():shutil.rmtree(destination)
  with zipfile.ZipFile(target) as z:
   for e in z.infolist():
    q=Path(e.filename)
    if q.is_absolute() or '..' in q.parts:raise ValueError('Unsafe checkpoint path')
    p=destination/q;p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(z.read(e));p.chmod((e.external_attr>>16)&0o777)
  extracted=destination/'elfen_rc1a';env=dict(os.environ,ANDROID_SDK_ROOT=str(SDK.resolve()))
  with (ROOT/'builds/unpacked-build-rc1a.log').open('w') as log:
   for cmd in (['python3','tools/build_android.py'],['python3','tools/check_apk.py']):subprocess.run(cmd,cwd=extracted,env=env,stdout=log,stderr=subprocess.STDOUT,check=True)
  actual=hashlib.sha256((extracted/'builds/elfen-fighting-v1.0.0-rc1.apk').read_bytes()).hexdigest()
  assert actual==expected,'Unpacked APK differs'
  report.update(rebuild_verified=True,unpacked_apk_sha256=actual,same_as_delivered_apk=True)
 (ROOT/'builds/RC1A_CHECKPOINT_VERIFICATION.json').write_text(json.dumps(report,indent=2));print(json.dumps(report,indent=2))
if __name__=='__main__':main()
