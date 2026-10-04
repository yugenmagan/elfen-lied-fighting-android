#!/usr/bin/env python3
from pathlib import Path
import subprocess,json,hashlib,re,sys
ROOT=Path(__file__).resolve().parents[1]
def run(cmd,log):
 with (ROOT/'research'/log).open('w') as f:p=subprocess.run(list(map(str,cmd)),cwd=ROOT,stdout=f,stderr=subprocess.STDOUT)
 print((ROOT/'research'/log).read_text()[-5000:],flush=True);p.check_returncode()
def main():
 before={p.name:p.read_bytes() for p in (ROOT/'app/src/main/assets/localization').iterdir()}
 run([sys.executable,'tools/build_localization.py'],'localization_compile_008.log')
 assert before=={p.name:p.read_bytes() for p in (ROOT/'app/src/main/assets/localization').iterdir()},'Generated translations are stale'
 for a in json.loads((ROOT/'localization/source_manifest.json').read_text()):
  p=ROOT/'app/src/main/assets/game'/a['pack']/f"{a['image']:04}.png"
  assert hashlib.sha256(p.read_bytes()).hexdigest()==a['source_sha256'],p
 keys={x[0] for x in json.loads((ROOT/'localization/ui.json').read_text())}
 s=(ROOT/'app/src/main/java/org/elfen/fighting/MainActivity.java').read_text()
 for literal in re.findall(r'\btr\(("(?:[^"\\]|\\.)*")\)',s):assert json.loads(literal) in keys,literal
 out=ROOT/'builds/localization008-tests';out.mkdir(parents=True,exist_ok=True)
 sources=sorted((ROOT/'runtime/src').rglob('*.java'))+sorted((ROOT/'tests/desktop_graphics').rglob('*.java'))+[ROOT/'app/src/main/java/org/elfen/fighting/LocalizedSprites.java',ROOT/'tests/Localization008Test.java',ROOT/'tests/StoryDataTest.java',ROOT/'tests/FinalBoss008Test.java',ROOT/'tests/FinalBossNaturalLoss008Test.java',ROOT/'tests/FinalBossNaturalWin008Test.java',ROOT/'tests/StoryInputPilotTest.java',ROOT/'tests/BattleRenderCheck.java',ROOT/'tests/LocalizedDemoRender.java']
 run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','-encoding','UTF-8','-d',out,*sources],'localization_javac_008.log')
 run(['java','-Xmx512m','-Djava.awt.headless=true','-cp',out,'org.elfen.fighting.Localization008Test',ROOT/'app/src/main/assets',ROOT/'research/localization008/render'],'localization_render_008.log')
 run(['java','-Xmx512m','-cp',out,'org.elfen.engine.FinalBoss008Test',ROOT/'app/src/main/assets/game'],'final_boss_008.log')
 run(['java','-Xmx512m','-cp',out,'org.elfen.engine.FinalBossNaturalLoss008Test',ROOT/'app/src/main/assets/game'],'final_boss_natural_loss_008.log')
 run(['java','-Xmx512m','-cp',out,'org.elfen.engine.FinalBossNaturalWin008Test',ROOT/'app/src/main/assets/game'],'final_boss_natural_win_008.log')
 run(['java','-Xmx512m','-Djava.awt.headless=true','-cp',out,'LocalizedDemoRender',ROOT/'app/src/main/assets',ROOT/'research/localization008/render',ROOT/'docs/localization008-render'],'localization_scenes_008.log')
 print('PASS generated-data reproducibility, original text-image SHA256, all Activity translation keys, text fitting and boss transitions')
if __name__=='__main__':main()
