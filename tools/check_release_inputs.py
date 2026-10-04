#!/usr/bin/env python3
"""Verify the published application's source-input and APK identity manifest."""
from pathlib import Path
import hashlib,json,sys
ROOT=Path(__file__).resolve().parents[1]
def main():
 m=json.loads((ROOT/'SOURCE_INPUTS.json').read_text())
 for name,expected in m['inputs'].items():
  p=ROOT/name
  if not p.is_file() or hashlib.sha256(p.read_bytes()).hexdigest()!=expected:raise SystemExit('Source input mismatch: '+name)
 if len(sys.argv)>1:
  p=Path(sys.argv[1]);assert hashlib.sha256(p.read_bytes()).hexdigest()==m['apk_sha256'],'APK mismatch'
 print('PASS',len(m['inputs']),'source/assets/build inputs;',m['version'],'build',m['version_code'])
if __name__=='__main__':main()
