#!/usr/bin/env python3
"""Headless Android smoke worker; jobs share the emulator's network namespace.

Uses official Google x86_64 images. TCG is slow; this is not a latency/audio test.
Write {"args": [...], "binary": "optional-relative.png"} to .deps/smoke-jobs.
"""
from pathlib import Path
import concurrent.futures, hashlib, json, os, subprocess, time, urllib.request, zipfile
ROOT = Path(__file__).resolve().parents[1]
D = ROOT / '.deps'; SDK = D / 'smoke-sdk'; OUT = ROOT / 'research/android-smoke'
DOWNLOADS = [
 ('platform-tools_r37.0.1-linux.zip', 'repository/', '477254aa5f903c15cf51001717bdf347fb6b53e0', SDK),
 ('emulator-linux_x64-16433917.zip', 'repository/', '87d8d859df482b6d045d28e36fbccc20fc14e810', SDK),
 ('x86_64-29_r08-linux.zip', 'repository/sys-img/android/', 'e4b798d6fcddff90d528d74ef22ce3dd4a2ca798', SDK / 'system-images/android-29/default'),
]
def download(item):
 name, path, digest, dest = item
 file = D / name
 if not file.exists():
  with urllib.request.urlopen('https://dl.google.com/android/' + path + name, timeout=120) as r, file.open('wb') as f:
   while data := r.read(1024*1024): f.write(data)
 assert hashlib.sha1(file.read_bytes()).hexdigest() == digest, name
 marker=dest/('.verified-'+name)
 if marker.exists():return
 with zipfile.ZipFile(file) as z:
  for info in z.infolist():
   p = Path(z.extract(info, dest)); mode = info.external_attr >> 16
   if mode: p.chmod(mode)
 marker.touch()
 print('Verified official component:', name, flush=True)
def main():
 D.mkdir(exist_ok=True); OUT.mkdir(parents=True, exist_ok=True)
 with concurrent.futures.ThreadPoolExecutor(max_workers=3) as pool: list(pool.map(download, DOWNLOADS))
 lib = D / 'emulator-libs'
 if not (lib / 'usr/lib/x86_64-linux-gnu/libxkbfile.so.1').exists():
  deb = D / 'libxkbfile.deb'
  urllib.request.urlretrieve('https://archive.ubuntu.com/ubuntu/pool/main/libx/libxkbfile/libxkbfile1_1.1.0-1build5_amd64.deb', deb)
  subprocess.run(['dpkg-deb','-x',str(deb),str(lib)],check=True)
 avd = D / 'avd/elfen-smoke.avd'; avd.mkdir(parents=True, exist_ok=True)
 (avd / 'config.ini').write_text('''AvdId=elfen-smoke
avd.ini.displayname=Elfen RC smoke
abi.type=x86_64
hw.cpu.arch=x86_64
hw.cpu.ncore=2
hw.ramSize=1536
hw.lcd.width=1280
hw.lcd.height=720
hw.lcd.density=160
hw.initialOrientation=landscape
hw.gpu.enabled=yes
hw.gpu.mode=swiftshader
hw.keyboard=yes
hw.mainKeys=no
hw.audioInput=no
hw.audioOutput=no
hw.camera.back=none
hw.camera.front=none
disk.dataPartition.size=2048M
showDeviceFrame=no
fastboot.forceColdBoot=yes
image.sysdir.1=''' + str(SDK / 'system-images/android-29/default/x86_64') + '\n')
 (avd.parent/'elfen-smoke.ini').write_text('avd.ini.encoding=UTF-8\npath='+str(avd)+'\ntarget=android-29\n')
 env = dict(os.environ, ANDROID_SDK_ROOT=str(SDK), ANDROID_AVD_HOME=str(avd.parent), ANDROID_USER_HOME=str(D/'android-user'), LD_LIBRARY_PATH=str(lib/'usr/lib/x86_64-linux-gnu'))
 adb = str(SDK/'platform-tools/adb'); serial = 'emulator-5556'
 subprocess.run([adb,'start-server'],env=env,check=True)
 log = (OUT/'emulator.log').open('w')
 p = subprocess.Popen([str(SDK/'emulator/emulator'),'-avd','elfen-smoke','-no-window','-no-audio','-no-boot-anim','-no-snapshot','-no-accel','-gpu','swiftshader_indirect','-feature','-Vulkan','-cores','2','-memory','1536','-port','5556','-no-metrics'],env=env,stdout=log,stderr=subprocess.STDOUT)
 jobs = D/'smoke-jobs'; jobs.mkdir(exist_ok=True)
 try:
  start = time.monotonic(); announced = 0
  while time.monotonic()-start < 480:
   if p.poll() is not None: raise RuntimeError('Emulator exited; see log')
   r = subprocess.run([adb,'-s',serial,'shell','getprop','sys.boot_completed'],env=env,capture_output=True,timeout=15)
   if r.stdout.strip()==b'1': break
   if time.monotonic()-announced>30: print('Booting Android…',flush=True); announced=time.monotonic()
   time.sleep(2)
  else: raise RuntimeError('Android boot timeout')
  (OUT/'ready.json').write_text(json.dumps({'android':29,'serial':serial,'boot_seconds':time.monotonic()-start}))
  print('ANDROID READY',flush=True)
  while time.monotonic()-start < 3600 and not (jobs/'STOP').exists():
   for job in sorted(jobs.glob('*.json')):
    result = OUT/job.name
    if result.exists(): continue
    spec = json.loads(job.read_text()); t=time.monotonic()
    try:
     r=subprocess.run([adb,'-s',serial,*spec['args']],env=env,capture_output=True,timeout=spec.get('timeout',150))
     if spec.get('binary'): (OUT/spec['binary']).write_bytes(r.stdout)
     result.write_text(json.dumps({'args':spec['args'],'exit_code':r.returncode,'seconds':time.monotonic()-t,'stdout':'' if spec.get('binary') else r.stdout.decode(errors='replace'),'stderr':r.stderr.decode(errors='replace')},indent=2))
     print('Completed:',job.name,r.returncode,flush=True)
    except subprocess.TimeoutExpired:
     result.write_text(json.dumps({'args':spec['args'],'exit_code':124,'error':'timeout'}))
   time.sleep(.25)
 finally:
  p.terminate(); subprocess.run([adb,'kill-server'],env=env,capture_output=True); log.close()
if __name__ == '__main__': main()
