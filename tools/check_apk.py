#!/usr/bin/env python3
"""Check RC1 packaging, assets, identity and signing without pretending to run Android."""
from pathlib import Path
import hashlib,json,zipfile,subprocess,sys,io,re
from bootstrap_android import SDK
ROOT=Path(__file__).resolve().parents[1]
def main():
 apk=Path(sys.argv[1]) if len(sys.argv)>1 else ROOT/'builds/elfen-fighting-v1.0.0-rc1.apk'
 old=json.loads((ROOT/'research/recovery/reference_inventory.json').read_text());assets=0;renamed=False;reencoded=[]
 with zipfile.ZipFile(apk) as z:
  assert z.testzip() is None
  resourceHashes={hashlib.sha256(z.read(n)).hexdigest() for n in z.namelist() if n.startswith('res/')}
  for row in old['entries']:
   n=row['path']
   if n.startswith('assets/') or n.startswith('res/'):
    if n.startswith('res/') and n not in z.namelist():
     assert row['sha256'] in resourceHashes,n;renamed=True # AGP resource-path shortening
    else:
     actual=z.read(n)
     if n.startswith('res/') and n.endswith('.png') and hashlib.sha256(actual).hexdigest()!=row['sha256']:
      # Debug AAPT2 crunch rewrites PNG encoding. Reference pixels were captured
      # only after matching encoded release resources to the original APK hashes.
      from PIL import Image
      directory=Path(n).parts[1];directory=re.sub(r'-v[0-9]+$','',directory)
      source=ROOT/'app/src/main/res'/directory/Path(n).name
      ref=json.loads((ROOT/'research/launcher_pixel_reference.json').read_text())[n]
      assert ref['encoded_sha256']==row['sha256'],n
      a=Image.open(io.BytesIO(actual)).convert('RGBA');b=Image.open(source).convert('RGBA')
      assert a.size==b.size==(ref['width'],ref['height']),n
      assert hashlib.sha256(a.tobytes()).hexdigest()==hashlib.sha256(b.tobytes()).hexdigest()==ref['rgba_sha256'],n
      reencoded.append(n)
     else:assert hashlib.sha256(actual).hexdigest()==row['sha256'],n
    if n.startswith('assets/'):assets+=1;assert z.read(n)==(ROOT/'app/src/main'/n).read_bytes(),n
  assert assets==2881
  assert not any(n.endswith('.so') for n in z.namelist())
  expectedAssets={p.relative_to(ROOT/'app/src/main').as_posix() for p in (ROOT/'app/src/main/assets').rglob('*') if p.is_file()}
  assert {n for n in z.namelist() if n.startswith('assets/')}==expectedAssets
  assert z.read('assets/game/story-index.tsv')==(ROOT/'app/src/main/assets/game/story-index.tsv').read_bytes()
  assert hashlib.sha256(z.read('classes.dex')).hexdigest()!=next(r['sha256'] for r in old['entries'] if r['path']=='classes.dex')
 bt=SDK/'build-tools/35.0.0'
 badging=subprocess.check_output([bt/'aapt','dump','badging',apk],text=True)
 assert "name='org.elfen.fighting.nativeport' versionCode='14' versionName='1.0.0-rc1'" in badging
 version=re.search(r"versionCode='(\d+)' versionName='([^']+)'",badging)
 assert version
 assert "sdkVersion:'29'" in badging and "targetSdkVersion:'35'" in badging
 permissions=[line for line in badging.splitlines() if line.startswith('uses-permission:')]
 assert permissions==["uses-permission: name='android.permission.INTERNET'"],permissions
 signature=subprocess.check_output(['java','-jar',bt/'lib/apksigner.jar','verify','--verbose','--print-certs',apk],text=True)
 assert '12f2989f0ab609532bdf881cfcb54093343a5707874af00a0ee8f47ce460d086' in signature
 subprocess.run([bt/'zipalign','-c','-p','4',apk],check=True)
 report={'apk':apk.name,'bytes':apk.stat().st_size,'sha256':hashlib.sha256(apk.read_bytes()).hexdigest(),'original_assets_unchanged':assets,'total_assets':len(expectedAssets),'new_assets':sorted(expectedAssets-{r['path'] for r in old['entries'] if r['path'].startswith('assets/')}),'launcher_resources_unchanged':True,'launcher_resource_paths_renamed':renamed,'signature_matches_test003':True,'application_id':'org.elfen.fighting.nativeport','version_code':int(version.group(1)),'version_name':version.group(2),'min_sdk':29,'target_sdk':35,'permissions':['android.permission.INTERNET'],'native_libraries':[],'zip_crc_ok':True,'android_execution_performed':False}
 report['launcher_png_reencoded']=reencoded
 report['launcher_pixels_unchanged']=True
 report['launcher_resources_unchanged']=not reencoded
 reportPath=Path(sys.argv[2]) if len(sys.argv)>2 else ROOT/'research/apk_validation_rc1a.json'
 reportPath.write_text(json.dumps(report,indent=2));print(json.dumps(report,indent=2))
if __name__=='__main__':main()
