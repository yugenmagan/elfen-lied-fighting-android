#!/usr/bin/env python3
"""Pinned official Android SDK packages; uses no system package manager."""
import hashlib
import os
from pathlib import Path
import shutil
import urllib.request
import zipfile

ROOT = Path(__file__).resolve().parents[1]
SDK = Path(os.environ.get('ANDROID_SDK_ROOT', ROOT / '.deps/android-sdk'))
PACKAGES = [
    ('build-tools_r35_linux.zip', '2cfaa0bbb2336e9ec18ed3ecea84fa2e2af607bc', 'build-tools/35.0.0'),
    ('platform-35_r02.zip', '0bb560a90a7a2cbd0dd8348224d518b638fe7949', 'platforms/android-35'),
]

def main():
    for name, sha1, directory in PACKAGES:
        dest = SDK / directory
        if (dest / 'source.properties').exists():
            continue
        archive = ROOT / '.deps/downloads' / name
        archive.parent.mkdir(parents=True, exist_ok=True)
        if not archive.exists():
            print('Downloading official SDK:', name, flush=True)
            with urllib.request.urlopen('https://dl.google.com/android/repository/' + name, timeout=120) as r, archive.open('wb') as f:
                shutil.copyfileobj(r, f)
        if hashlib.sha1(archive.read_bytes()).hexdigest() != sha1:
            raise ValueError('SDK integrity mismatch: ' + name)
        with zipfile.ZipFile(archive) as z:
            for item in z.infolist():
                relative = Path(*Path(item.filename).parts[1:])
                if '..' in relative.parts:
                    raise ValueError('Unsafe SDK path')
                target = dest / relative
                if item.is_dir():
                    target.mkdir(parents=True, exist_ok=True)
                else:
                    target.parent.mkdir(parents=True, exist_ok=True)
                    target.write_bytes(z.read(item))
                    target.chmod((item.external_attr >> 16) & 0o777 or 0o644)
        print('Verified and extracted:', directory, flush=True)
    print(SDK)

if __name__ == '__main__':
    main()
