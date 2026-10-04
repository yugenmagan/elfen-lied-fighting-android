#!/usr/bin/env python3
"""Reproducible standalone Java Android build. No Gradle daemon/Unix sockets required.

JDK 17, pinned official build-tools 35.0.0 / platform 35. Android Studio project
is at the project root. This equivalent build route uses the same sources and manifest.
"""
import hashlib
import os
from pathlib import Path
import shutil
import subprocess
import sys
import zipfile
import xml.etree.ElementTree as ET
from bootstrap_android import SDK,main as bootstrap
ROOT=Path(__file__).resolve().parents[1]

def run(args):
    print('+',str(args[0]),str(args[1]),flush=True)
    subprocess.run([str(x) for x in args],cwd=ROOT,check=True)

def main():
    bootstrap()
    if not (ROOT/'app/src/main/assets/game/catalog.json').exists() or '--convert' in sys.argv:
        run([sys.executable,ROOT/'tools/convert_game.py'])
    bt=SDK/'build-tools/35.0.0';androidjar=SDK/'platforms/android-35/android.jar'
    build=ROOT/'builds/work';build.mkdir(parents=True,exist_ok=True)
    classes=build/'classes';shutil.rmtree(classes,ignore_errors=True);classes.mkdir()
    # Source copies made by workspace synchronization are not project inputs.
    source=[p for base in ('runtime/src','network/src','app/src/main/java') for p in (ROOT/base).rglob('*.java')
            if not any(part.startswith('.') for part in p.relative_to(ROOT).parts[:-1]) and p.is_file()]
    run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','-encoding','UTF-8','--release','8','-cp',androidjar,'-d',classes,*source])
    jar=build/'classes.jar'
    with zipfile.ZipFile(jar,'w') as z:
        for f in sorted(classes.rglob('*.class')):z.write(f,f.relative_to(classes))
    dex=build/'dex';dex.mkdir(exist_ok=True)
    run(['java','-cp',bt/'lib/d8.jar','com.android.tools.r8.D8','--release','--min-api','29','--lib',androidjar,'--output',dex,jar])
    resources=build/'compiled-resources.zip'
    run([bt/'aapt2','compile','--dir',ROOT/'app/src/main/res','-o',resources])
    apk=build/'resources.apk'
    # AGP 8 gets namespace/applicationId from Gradle. AAPT2 needs a packaged copy.
    ET.register_namespace('android','http://schemas.android.com/apk/res/android')
    manifest=ET.parse(ROOT/'app/src/main/AndroidManifest.xml')
    manifest.getroot().set('package','org.elfen.fighting.nativeport')
    packaged_manifest=build/'AndroidManifest.xml';manifest.write(packaged_manifest,encoding='utf-8',xml_declaration=True)
    run([bt/'aapt2','link',resources,'--manifest',packaged_manifest,'-I',androidjar,'-A',ROOT/'app/src/main/assets','-o',apk,'--min-sdk-version','29','--target-sdk-version','35','-0','wav','-0','mid','-0','efp','-0','zlib'])
    with zipfile.ZipFile(apk,'a',compression=zipfile.ZIP_DEFLATED) as z:
        for f in sorted(dex.glob('*.dex')):
            info=zipfile.ZipInfo(f.name,(2026,9,21,0,0,0));info.compress_type=zipfile.ZIP_DEFLATED;z.writestr(info,f.read_bytes())
    aligned=build/'aligned.apk';run([bt/'zipalign','-f','-p','4',apk,aligned])
    key=os.environ.get('ELFEN_KEYSTORE')
    signing=['ELFEN_KEY_ALIAS','ELFEN_STORE_PASSWORD','ELFEN_KEY_PASSWORD']
    if key:
        if not all(os.environ.get(k) for k in signing):
            raise RuntimeError('Provide all ELFEN signing environment variables; see BUILDING.md')
        final=ROOT/'builds/elfen-fighting-v1.0.0-rc1.apk'
        run(['java','-jar',bt/'lib/apksigner.jar','sign','--ks',key,
             '--ks-pass','env:ELFEN_STORE_PASSWORD','--key-pass','env:ELFEN_KEY_PASSWORD',
             '--ks-key-alias',os.environ['ELFEN_KEY_ALIAS'],'--out',final,aligned])
        run(['java','-jar',bt/'lib/apksigner.jar','verify','--verbose','--print-certs',final])
    else:
        final=ROOT/'builds/elfen-fighting-v1.0.0-rc1-unsigned.apk'
        shutil.copyfile(aligned,final)
        print('Unsigned release: supply private signing environment variables to install.')
    run([bt/'zipalign','-c','-p','4',final])
    sha=hashlib.sha256(final.read_bytes()).hexdigest();final.with_suffix('.apk.sha256').write_text(sha+'  '+final.name+'\n')
    print('APK:',final,'bytes:',final.stat().st_size,'SHA256:',sha)

if __name__=='__main__':main()
