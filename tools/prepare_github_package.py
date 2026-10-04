#!/usr/bin/env python3
"""Create a clean local Git checkpoint and a ready-to-publish prerelease package.
No network mutation and no signing material enters the public Git history.
"""
from pathlib import Path
import hashlib,json,os,re,shutil,subprocess,zipfile
from package_checkpoint import ROOT,include,SECRET_SUFFIXES
TAG='v1.0.0-rc1'
def git(tree,*args):
 return subprocess.check_output(['git','-C',str(tree),*args],text=True).strip()
def scan(files,base):
 count=0
 forbidden=re.compile(rb'(?:gh[pousr]_[A-Za-z0-9]{30,}|github_pat_[A-Za-z0-9_]{40,}|-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----)')
 private=[os.environ[k].encode() for k in ('ELFEN_STORE_PASSWORD','ELFEN_KEY_PASSWORD') if os.environ.get(k)]
 for p in files:
  if p.suffix.lower() in SECRET_SUFFIXES or p.name=='local.properties' or p.name.startswith('.env'):raise RuntimeError('Forbidden file: '+str(p.relative_to(base)))
  data=p.read_bytes()
  if forbidden.search(data) or any(value in data for value in private):raise RuntimeError('Secret pattern found in '+str(p.relative_to(base)))
  count+=1
 return count
def main():
 tree=ROOT/'builds/github-source'
 if tree.exists():raise RuntimeError('Prepared Git tree exists: inspect and reuse it; do not overwrite its history')
 files=sorted(p for p in ROOT.rglob('*') if include(p) and p.relative_to(ROOT).parts[0]!='source_original');count=scan(files,ROOT)
 for p in files:
  q=tree/p.relative_to(ROOT);q.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(p,q)
 git(tree,'init','-b','main');git(tree,'config','user.name','Asagi');git(tree,'config','user.email','144571390+yugenmagan@users.noreply.github.com')
 git(tree,'add','.')
 tracked=[x for x in git(tree,'ls-files','-z').split('\0') if x];scan([tree/n for n in tracked],tree)
 git(tree,'commit','-m','Prepare v1.0.0-rc1: clear HUD margins and labelled touch controls')
 commit=git(tree,'rev-parse','HEAD');git(tree,'tag','-a',TAG,'-m','Elfen Lied Fighting Android RC1 — build 14');assert git(tree,'status','--porcelain')==''
 stage=ROOT/'builds/github-release-package';release=stage/'release';release.mkdir(parents=True,exist_ok=True)
 bundle=stage/'repository.bundle';git(tree,'bundle','create',str(bundle),'--all');git(tree,'bundle','verify',str(bundle))
 shutil.copy2(ROOT/'builds/elfen-fighting-v1.0.0-rc1.apk',release/'elfen-fighting-v1.0.0-rc1.apk')
 for name in ['SHA256SUMS.txt','RELEASE_NOTES.md']:shutil.copy2(ROOT/name,release/name)
 metadata={'tag_name':TAG,'name':'Elfen Lied Fighting '+TAG,'target_commitish':commit,'draft':False,'prerelease':True,'repository':None,'proposed_repository_name':'elfen-lied-fighting-android','owner':'yugenmagan','published':False,'apk':'release/elfen-fighting-v1.0.0-rc1.apk','sha256':hashlib.sha256((release/'elfen-fighting-v1.0.0-rc1.apk').read_bytes()).hexdigest(),'secret_scan_files':count,'tracked_files':len(tracked)}
 (stage/'RELEASE_METADATA.json').write_text(json.dumps(metadata,indent=2)+'\n')
 (stage/'PUBLISHING.md').write_text('''# Ready GitHub prerelease package

Complete source and Git history are in `repository.bundle`, including the annotated tag `v1.0.0-rc1`. The signed APK, SHA256SUMS.txt and release notes are under `release/`. No private signing material or machine-local configuration is included. Nothing has been published yet.

For the release operator: clone repository.bundle, create or select the authorized GitHub repository, push main and v1.0.0-rc1, then create a prerelease at this exact tag using RELEASE_NOTES.md. Attach the APK and SHA256SUMS.txt. Check the uploaded APK hash against RELEASE_METADATA.json.

The user is on Android and is not being asked to use Git or Terminal. This bundle preserves the exact checkpoint for an authorized publication path. Follow the environment's browser-fallback approval and account handoff rules. Never request passwords or tokens in chat, and never put them into the repository.

The separate project ZIP opens directly in Android Studio. Public-room-server deployment and real internet battle validation remain separate release gates.
''')
 out=ROOT/'builds/Elfen_Lied_v1.0.0-rc1_GitHub_package.zip'
 with zipfile.ZipFile(out,'w',zipfile.ZIP_DEFLATED,6) as z:
  for p in sorted(stage.rglob('*')):
   if p.is_file():z.write(p,p.relative_to(stage))
 with zipfile.ZipFile(out) as z:assert z.testzip() is None
 metadata.update(package=out.name,package_sha256=hashlib.sha256(out.read_bytes()).hexdigest(),package_bytes=out.stat().st_size,zip_crc_ok=True,git_bundle_verified=True)
 (ROOT/'builds/GITHUB_PREPARATION.json').write_text(json.dumps(metadata,indent=2)+'\n');print(json.dumps(metadata,indent=2))
if __name__=='__main__':main()
