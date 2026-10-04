#!/usr/bin/env python3
"""Recover the exact TEST003 using an APK plus independently verified source candidate.

No source archive is used. Source provenance is accepted only after D8 output
and every packaged entry have been compared with the reference APK.
"""
import hashlib, json, shutil, zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent
APK = ROOT/'upload/elfen-fighting-003-combat.apk'
SOURCE = ROOT/'elfen_android'
OUT = ROOT/'recovered_test003'
EXPECTED = '6709395d45df9876d476a3795aa67758d807ce5b80f404d04bf976ab2a1ac7dd'

def sha(data): return hashlib.sha256(data).hexdigest()
def copy(a,b):
    b.parent.mkdir(parents=True,exist_ok=True)
    shutil.copy2(a,b)

def main():
    if sha(APK.read_bytes()) != EXPECTED: raise ValueError('Unexpected reference APK')
    if OUT.exists(): raise FileExistsError('Refusing to overwrite recovery directory')
    OUT.mkdir()
    (OUT/'research/recovery').mkdir(parents=True)
    inventory=[]
    with zipfile.ZipFile(APK) as z:
        if z.testzip() is not None: raise ValueError('Reference APK CRC failure')
        if len(z.namelist())!=len(set(z.namelist())): raise ValueError('Duplicate APK entries')
        for entry in z.infolist():
            p=Path(entry.filename)
            if p.is_absolute() or '..' in p.parts: raise ValueError('Unsafe APK path')
            data=z.read(entry)
            inventory.append({'path':entry.filename,'bytes':len(data),'sha256':sha(data)})
            target=(OUT/'app/src/main'/p) if p.parts[0]=='assets' else OUT/'research/recovery/apk'/p
            target.parent.mkdir(parents=True,exist_ok=True)
            target.write_bytes(data)
    for directory in ['runtime','tests','docs']:
        shutil.copytree(SOURCE/directory,OUT/directory)
    shutil.copytree(SOURCE/'android/app/src/main/java',OUT/'app/src/main/java')
    shutil.copytree(SOURCE/'android/app/src/main/res',OUT/'app/src/main/res')
    copy(SOURCE/'android/app/src/main/AndroidManifest.xml',OUT/'app/src/main/AndroidManifest.xml')
    for name in ['build.gradle','settings.gradle','gradle.properties','gradlew','gradlew.bat']:
        copy(SOURCE/'android'/name,OUT/name)
    shutil.copytree(SOURCE/'android/gradle',OUT/'gradle')
    text=(SOURCE/'android/app/build.gradle').read_text().replace("'../../runtime/src'","'../runtime/src'").replace("'../../builds/test-signing.p12'","'../builds/test-signing.p12'")
    (OUT/'app/build.gradle').write_text(text)
    # Signing keys stay external; never include them in a source checkpoint.
    for name in ['PORTING_NOTES.md','FORMAT_NOTES.md','THIRD_PARTY_NOTICES.md','TESTS.md','KNOWN_ISSUES.md','PHONE_TEST_003_RU.md']:
        copy(SOURCE/name,OUT/name)
    for file in (SOURCE/'tools').glob('*'):
        if not file.is_file():continue
        copy(file,OUT/'tools'/file.name)
        if file.suffix=='.py':
            p=OUT/'tools'/file.name;p.write_text(p.read_text().replace('android/app/src/main','app/src/main'))
    builder=OUT/'tools/build_android.py'
    builder.write_text(builder.read_text().replace('elfen-fighting-003-combat.apk','elfen-fighting-003-recovered.apk'))
    check=OUT/'tools/check_apk.py';check.write_text(check.read_text().replace('elfen-fighting-003-combat.apk','elfen-fighting-003-recovered.apk'))
    for file in (SOURCE/'research').iterdir():
        if file.is_file() and file.suffix in ('.json','.txt','.log','.efr'):
            copy(file,OUT/'research'/file.name)
    # Source inventory is evidence, not an assertion of equivalence before compilation.
    source_files=[]
    for directory in ['runtime/src','app/src/main/java']:
        for f in sorted((OUT/directory).rglob('*.java')):
            source_files.append({'path':str(f.relative_to(OUT)),'sha256':sha(f.read_bytes())})
    result={'reference_apk_sha256':EXPECTED,'reference_apk_bytes':APK.stat().st_size,'entries':inventory,
            'candidate_sources':source_files,'source_origin':'surviving working tree; no damaged project ZIP used',
            'equivalence':'pending independent rebuild and APK payload comparison'}
    (OUT/'research/recovery/reference_inventory.json').write_text(json.dumps(result,ensure_ascii=False,indent=2))
    copy(Path(__file__),OUT/'tools/recovery_provenance_script.py')
    print(json.dumps({'project':str(OUT),'apk_entries':len(inventory),'assets':sum(x['path'].startswith('assets/') for x in inventory),'java_sources':len(source_files)},indent=2))

if __name__=='__main__':main()
