#!/usr/bin/env python3
"""Resolve CP932 KGT references through the preserved original filename map.
Conflicting versions keep the explicit TEST003/004 selection policy; no renames.
"""
from pathlib import Path
import json
from inspect_portable import pack,tables
ROOT=Path(__file__).resolve().parents[1]
def main():
 base=ROOT/'app/src/main/assets/game';mapping=json.loads((base/'filename-map.json').read_text());kgt=pack(base/'0116/data.efp');t=tables(kgt)
 preferred={'マリコ.player':'0128','クラフト.player':'0142'}
 lines=['EFSI1\t'+kgt['hash']];resolved=[]
 for group,ext in [('characters','.player'),('stages','.stage'),('demos','.demo')]:
  for i,name in enumerate(t[group],1):
   if not name:continue
   filename=name+ext
   ids={e['android_pack_path'].split('/')[1] for e in mapping if e['android_pack_path'] and e['decoded_japanese_path'].split('/')[-1]==filename}
   if len(ids)>1:
    assert filename in preferred and preferred[filename] in ids,(filename,ids)
    id=preferred[filename]
   else:
    assert len(ids)==1,(filename,ids)
    id=next(iter(ids))
   p=pack(base/id/'data.efp');assert p['kind']==ext
   lines.append('\t'.join([ext,str(i),id,p['hash'],filename]));resolved.append(dict(type=ext,index=i,id=id,filename=filename,alternatives=sorted(ids)))
 (base/'story-index.tsv').write_text('\n'.join(lines)+'\n',encoding='utf8')
 (ROOT/'research/story_references_005.json').write_text(json.dumps(resolved,ensure_ascii=False,indent=2))
 print('PASS resolved',len(resolved),'original KGT references; explicit variants',preferred)
if __name__=='__main__':main()
