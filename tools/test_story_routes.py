#!/usr/bin/env python3
"""Long natural CPU routes; failures are reported, never converted to wins."""
from pathlib import Path
import concurrent.futures,json,subprocess,sys,time
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'builds/story_tests'
IDS=['0104','0108','0110','0114','0122','0128','0136','0156','0168']

def run_route(pid):
    log=ROOT/'research'/f'story_route_{pid}_006.log'
    command=['java','-Xmx384m','-cp',str(OUT),'org.elfen.engine.StoryPlayTest',str(ROOT/'app/src/main/assets/game'),pid,'80','19','500000','1']
    start=time.monotonic()
    with log.open('w') as f:
        result=subprocess.run(command,cwd=ROOT,stdout=f,stderr=subprocess.STDOUT)
    text=log.read_text()
    complete=result.returncode==0 and 'PASS full route replay 0' in text
    row={'character':pid,'exit':result.returncode,'complete':complete,'seconds':round(time.monotonic()-start,2),'log':str(log.relative_to(ROOT)),'last_lines':text.splitlines()[-6:]}
    print(json.dumps(row,ensure_ascii=False),flush=True)
    return row

def main():
    OUT.mkdir(parents=True,exist_ok=True)
    sources=list((ROOT/'runtime/src').rglob('*.java'))+[ROOT/'tests/StoryDataTest.java',ROOT/'tests/StoryPlayTest.java']
    subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','-encoding','UTF-8','-d',str(OUT),*[str(p) for p in sources]],cwd=ROOT,check=True)
    with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
        rows=list(pool.map(run_route,sys.argv[1:] or IDS))
    (ROOT/'research/story_routes_006.json').write_text(json.dumps(rows,ensure_ascii=False,indent=2)+'\n')
    return 1 if not all(r['complete'] for r in rows) else 0
if __name__=='__main__':sys.exit(main())
