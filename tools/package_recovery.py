#!/usr/bin/env python3
"""Package maintained sources/assets; CRC-check, unpack and rebuild this exact ZIP."""
import hashlib,json,os,shutil,subprocess,zipfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
def include(p):
    q=p.relative_to(ROOT);parts=q.parts
    if any(x in ('.deps','.gradle','.git','__pycache__') for x in parts):return False
    if parts[0]=='builds':return p.name in ('elfen-fighting-003-recovered.apk.sha256',)
    if parts[:2]==('app','build') or parts[0]=='build':return False
    return p.is_file() and p.name!='local.properties' and p.suffix not in ('.pyc','.idsig')
def main():
    target=ROOT/'builds/Elfen_Lied_Android_003_RECOVERED_project.zip'
    files=sorted(p for p in ROOT.rglob('*') if include(p))
    with zipfile.ZipFile(target,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=9) as z:
        for p in files:
            if p.is_symlink():raise ValueError('Symlink in checkpoint')
            entry=zipfile.ZipInfo('recovered_test003/'+p.relative_to(ROOT).as_posix(),(2026,9,23,0,0,0))
            entry.create_system=3;entry.external_attr=(p.stat().st_mode&0xffff)<<16;entry.compress_type=zipfile.ZIP_DEFLATED
            z.writestr(entry,p.read_bytes())
    destination=ROOT.parent/'recovery_zip_verification'
    if destination.exists():shutil.rmtree(destination)
    destination.mkdir()
    with zipfile.ZipFile(target) as z:
        bad=z.testzip()
        if bad:raise ValueError('ZIP CRC failure: '+bad)
        for entry in z.infolist():
            q=Path(entry.filename)
            if q.is_absolute() or '..' in q.parts:raise ValueError('Unsafe checkpoint path')
            p=destination/q;p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(z.read(entry))
            p.chmod((entry.external_attr>>16)&0o777)
    extracted=destination/'recovered_test003'
    # SDK may be shared, but no compiled class, DEX, APK or build cache is copied.
    env=dict(os.environ)
    if 'ANDROID_SDK_ROOT' not in env:
        env['ANDROID_SDK_ROOT']=str(ROOT/'.deps/android-sdk')
    buildlog=ROOT/'builds/recovery-unpacked-build.log'
    with buildlog.open('w') as log:
        for command in (['python3','tools/build_android.py'],['python3','tools/check_apk.py']):
            result=subprocess.run(command,cwd=extracted,env=env,stdout=log,stderr=subprocess.STDOUT)
            if result.returncode:raise RuntimeError('Unpacked build failed; see '+str(buildlog))
    apk=extracted/'builds/elfen-fighting-003-recovered.apk'
    expected=json.loads((ROOT/'research/recovery/reference_inventory.json').read_text())['reference_apk_sha256']
    actual=hashlib.sha256(apk.read_bytes()).hexdigest()
    if actual!=expected:raise ValueError('Unpacked APK differs from reference')
    digest=hashlib.sha256(target.read_bytes()).hexdigest()
    target.with_suffix('.zip.sha256').write_text(digest+'  '+target.name+'\n')
    report={'zip':target.name,'zip_bytes':target.stat().st_size,'zip_sha256':digest,'files':len(files),
            'crc_all_entries_passed':True,'unpacked_into_separate_directory':True,'rebuilt_from_unpacked_sources':True,
            'unpacked_apk_sha256':actual,'same_as_original_test003':True,'android_execution_performed':False}
    (ROOT/'builds/RECOVERY_CHECKPOINT_VERIFICATION.json').write_text(json.dumps(report,indent=2))
    print(json.dumps(report,indent=2))
if __name__=='__main__':main()
