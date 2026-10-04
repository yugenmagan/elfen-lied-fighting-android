#!/usr/bin/env python3
"""Read EFP1 without modifying the original or converted assets."""
import struct,json
from pathlib import Path
class Reader:
 def __init__(self,data):self.data=data;self.pos=0
 def integer(self):
  value=struct.unpack_from('>i',self.data,self.pos)[0];self.pos+=4;return value
 def blob(self):
  n=self.integer();assert 0<=n<=len(self.data)-self.pos
  b=self.data[self.pos:self.pos+n];self.pos+=n;return b
 def text(self):return self.blob().decode('utf8')
 def integers(self):return [self.integer() for _ in range(self.integer())]
def pack(path):
 r=Reader(Path(path).read_bytes());assert r.integer()==0x45465031
 p=dict(name=r.text(),kind=r.text(),hash=r.text());p['skills']=[dict(name=r.text(),start=r.integer(),type=r.integer()) for _ in range(r.integer())];p['code']=r.blob();p['images']=[(r.integer(),r.integer()) for _ in range(r.integer())];p['sounds']=[(r.text(),r.integer()) for _ in range(r.integer())];p['builtins']=r.integers()
 p['commands']=[dict(name=r.text(),time=r.integer(),skills=r.integers(),steps=r.blob(),amounts=r.integers()) for _ in range(r.integer())];p['junctions']=[(r.integer(),r.integer()) for _ in range(r.integer())];p['settings']=r.blob();p['bgm']=r.integer();p['time']=r.integer();p['skip']=r.integer();p['tail']=r.blob();assert r.pos==len(r.data)
 return p
def story(p):
 t=p['tail'];n=lambda off:struct.unpack_from('<I',t,off)[0]
 at=8+n(4)*82;at+=4+n(at)*4;at+=4+n(at)*6;at+=10+11100+48+38+1785
 out=[]
 for slot in range(100):
  kind=t[at];at+=1;assert kind in range(5)
  payload=t[at:at+205] if kind else b''
  if kind:at+=205
  out.append(dict(slot=slot,type=kind,raw=payload.hex()))
 return out
def tables(p):
 t=p['tail'];pos=4
 def names(n):
  nonlocal pos
  out=[t[pos+i*256:pos+(i+1)*256].split(b'\0')[0].decode('cp932') for i in range(n)];pos+=n*256;return out
 chars=names(50);pos+=200*36+8;stages=names(50);demos=names(100)
 return dict(characters=chars,stages=stages,demos=demos,screens=list(t[pos:pos+6]))
def main():
 root=Path(__file__).resolve().parents[1];base=root/'app/src/main/assets/game';out={}
 for folder in sorted(base.iterdir()):
  if not folder.is_dir():continue
  p=pack(folder/'data.efp')
  if p['kind']=='.player':out[folder.name]=dict(name=p['name'],events=[e for e in story(p) if e['type']]);print(folder.name,p['name'],[(e['slot'],e['type'],e['raw'][:28]) for e in out[folder.name]['events']])
  elif p['kind']=='.demo':print('DEMO',folder.name,p['name'],'BGM',p['bgm'],'time',p['time'],'skip',hex(p['skip']),'tail',p['tail'][:13].hex(),'scripts',[(i,s['name'],s['type']) for i,s in enumerate(p['skills']) if (s['type']&3)!=3])
  elif p['kind']=='.kgt':out['tables']=tables(p)
 (root/'research/story_tables_005.json').write_text(json.dumps(out,ensure_ascii=False,indent=2))
if __name__=='__main__':main()
