#!/usr/bin/env python3
"""Compile reviewed JP/EN/RU text and presentation regions. Original game assets untouched."""
from pathlib import Path
import json,sys,hashlib
sys.path.insert(0,str(Path(__file__).resolve().parent))
from inspect_portable import pack
ROOT=Path(__file__).resolve().parents[1]
def esc(s):return str(s).replace('\\','\\\\').replace('\t','\\t').replace('\n','\\n')
def unesc(s):return s.replace('\\n','\n')
def main():
 out=ROOT/'app/src/main/assets/localization';out.mkdir(exist_ok=True)
 ui=json.loads((ROOT/'localization/ui.json').read_text());assert len({r[0] for r in ui})==len(ui)
 (out/'ui.tsv').write_text('ELF-UI-1\n'+'\n'.join('\t'.join(map(esc,r)) for r in ui)+'\n')
 cards={'0060:5','0070:6','0076:4','0078:1','0088:6','0094:7','0096:8','0112:6','0118:6','0130:5','0178:6'}
 overlay={
 '0060:2':[dict(rect=[32,165,238,215],font=38,bg='mask:ff000008:142:165:132:41',color='ffff3366',align='center')],
 '0060:3':[dict(rect=[69,45,193,282],font=33,bg='ff000008')],
 '0060:4':[dict(rect=[42,54,180,295],font=31,bg='ff000008')],
 '0072:5':[dict(rect=[0,0,300,74],font=37,bg='ff10101a',en='Only your head...',ru='Только голова...'),dict(rect=[164,106,446,66],font=33,bg='ff10101a',en='...is left now.',ru='...вот всё, что осталось.')],
 '0072:12':[dict(rect=[32,133,218,97],font=36,bg='ff10101a',en='You too...',ru='И ты тоже...'),dict(rect=[0,310,613,110],font=37,bg='ff10101a',en='Keep that letter safe.',ru='Сбереги это письмо.')],
 '0124:0':[dict(rect=[0,0,76,480],font=44,bg='clear')],
 '0124:7':[dict(rect=[0,0,366,100],font=39,bg='ff10101a',en='Daddy...',ru='Папа...'),dict(rect=[290,165,337,94],font=31,bg='ff10101a',en='I love you.',ru='Я люблю тебя.')],
 '0072:18':[dict(rect=[58,250,522,144],font=48,bg='ff59ff33',align='center',en='ELFEN FIGHT\nG2',ru='ELFEN FIGHT\nG2'),dict(rect=[425,103,132,96],font=30,bg='ff59ff33',align='center',en='STORY\nVS',ru='СЮЖЕТ\nVS')],
 '0074:11':[dict(rect=[58,250,522,144],font=48,bg='ff59ff33',align='center',en='ELFEN FIGHT\nG2',ru='ELFEN FIGHT\nG2'),dict(rect=[425,103,132,96],font=30,bg='ff59ff33',align='center',en='STORY\nVS',ru='СЮЖЕТ\nVS')],
 }
 rows=[];manifest=[]
 for line in (ROOT/'localization/scenes_source.tsv').read_text().splitlines():
  pid,number,ja,en,ru=line.split('\t');idx=int(number);ja,en,ru=map(unesc,(ja,en,ru));p=pack(ROOT/'app/src/main/assets/game'/pid/'data.efp');w,h=p['images'][idx];assert w and h
  key=pid+':'+str(idx);mode='replace';rect=[1,1,w-2,h-2];font=32;color='ffffffff';bg='none';align='left'
  if key in cards:mode='card';rect=[32,140,570,195];font=46;color='ff55eaa0';align='center'
  if pid=='0116':font=min(86,h-4);align='center';color='ffff3030' if idx in (0,1,17,18,23,24) else 'ffffcf38' if idx in (14,15,16) else 'ffffffff'
  if p['kind']=='.player':font=min(60,h-4);color='ff90edc0';align='center'
  if key in ('0124:2','0124:3'):font=58;align='center'
  if key in ('0092:1','0124:5'):font=52;color='ff45f3a8';align='center'
  if key=='0222:0':rect=[24,24,592,432];font=27;bg='ff000008';align='center'
  if key=='0086:0':rect=[1,0,w-2,62];font=52;color='fffbe037';align='center'
  # These original text sprites extend to/past the frame's right border.
  # Keep the new lettering inside the same drawn dialogue window.
  if key=='0076:3':rect=[4,1,376,h-2]
  if key=='0088:4':rect=[4,1,374,h-2]
  if key=='0178:4':rect=[20,1,374,h-2]
  if key=='0072:0':rect=[15,195,610,80];font=25;align='center';bg='ff000008'
  if key=='0072:15':rect=[10,135,620,225];font=70;color='ff6b52ff';align='center'
  if key=='0072:23':font=300;align='center';color='ffffffff';bg='ff14ff0c'
  if key in overlay:mode='overlay'
  blocks=overlay.get(key,[{}])
  if key=='0086:0':blocks=[dict(en='CONTINUE?',ru='ПРОДОЛЖИТЬ?')]
  for block in blocks:
   r=block.get('rect',rect);assert r[0]>=0 and r[1]>=0 and r[0]+r[2]<=w and r[1]+r[3]<=h,(key,r,w,h)
   row=[pid,idx,mode,*r,block.get('font',font),block.get('color',color),block.get('bg',bg),block.get('align',align),ja,block.get('en',en),block.get('ru',ru)];rows.append(row)
  image=ROOT/'app/src/main/assets/game'/pid/f'{idx:04}.png';manifest.append({'pack':pid,'image':idx,'width':w,'height':h,'source_sha256':hashlib.sha256(image.read_bytes()).hexdigest(),'blocks':len(blocks),'mode':mode})
 (out/'scenes.tsv').write_text('ELF-SCENES-1\n'+'\n'.join('\t'.join(map(esc,r)) for r in rows)+'\n')
 (ROOT/'localization/source_manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n')
 print('Compiled',len(ui),'UI entries;',len(manifest),'localized sprite images;',len(rows),'text regions')
if __name__=='__main__':main()
