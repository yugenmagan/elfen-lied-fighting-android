#!/usr/bin/env python3
"""Compile headless combat and compare against original x86 fixtures and replay."""
from pathlib import Path
import json,subprocess
ROOT=Path(__file__).resolve().parents[1]

def run(args,name):
    p=subprocess.run([str(a) for a in args],cwd=ROOT,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
    (ROOT/'research'/name).write_text(p.stdout);print(p.stdout,flush=True);p.check_returncode()

def main():
    d=json.loads((ROOT/'research/combat_x86_results.json').read_text());lines=[]
    for v in d['damage']:lines.append('damage '+' '.join(map(str,v)))
    for r in d['rng']:lines.append('rng '+' '.join(map(str,[r['seed'],*r['values']])))
    defaults={'power':5,'rate':100,'guard':0,'flags':0,'combo':0,'ax':390,'bx':450,'af':0,'bf':1}
    for r in d['full_collision']:
        a=defaults|r['input'];lines.append('collision '+' '.join(str(a[k]) for k in defaults)+' '+' '.join(str(r['output'][k]) for k in ['life','pending_skill','attacker_freeze','defender_freeze','combo','attacker_flags','defender_flags']))
    dest=ROOT/'builds/combat_tests';dest.mkdir(parents=True,exist_ok=True);fixture=dest/'x86-fixtures.txt';fixture.write_text('\n'.join(lines)+'\n')
    cpu=dest/'cpu-fixtures.txt';rows=[]
    for case in json.loads((ROOT/'research/cpu_x86_results.json').read_text()):
        rows.append('case '+' '.join(str(case[k]) for k in ('id','level','distance','self_y','other_y')))
        for row in case['rows']:rows.append(' '.join(map(str,[*row['state'],row['rng'],*row['history']])))
    cpu.write_text('\n'.join(rows)+'\n')
    cmd=dest/'command-fixtures.txt';rows=[]
    for case in json.loads((ROOT/'research/commands_x86_results.json').read_text()):
        values=[case[k] for k in ('id','reverse','stance','start','skill','chosen')]+[int(case['custom'] is not None)]
        if case['custom']:values+=case['custom']['steps']+case['custom']['amounts']
        rows.append(' '.join(map(str,values+case['history']+case['after'])))
    cmd.write_text('\n'.join(rows)+'\n')
    contacts=dest/'contacts-fixtures.txt';contact_data=json.loads((ROOT/'research/contacts_x86_results.json').read_text());rows=[]
    for case in contact_data['clashes']:rows.append('clash '+' '.join(map(str,case['input']+case['output'])))
    for case in contact_data['reaction_errors']:rows.append('reaction '+' '.join(str(case[k]) for k in ('absent_r','life','pending','attacker_flags','defender_flags')))
    contacts.write_text('\n'.join(rows)+'\n')
    names=['CombatTest','CpuTest','CommandRecognizerTest','ContactTest','BattleCpuTest','ReplayTest','AllCpuCharacters']
    sources=sorted((ROOT/'runtime/src').rglob('*.java'))+[ROOT/'tests'/(name+'.java') for name in names]
    run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','-encoding','UTF-8','-d',dest,*sources],'combat_compile.txt')
    assets=ROOT/'app/src/main/assets/game'
    for name,extra,log in [('CombatTest',[fixture],'combat_tests.txt'),('CpuTest',[cpu],'cpu_tests.txt'),('CommandRecognizerTest',[cmd],'command_tests.txt'),('ContactTest',[contacts],'contact_tests.txt'),('BattleCpuTest',[],'battle_cpu_tests.txt'),('ReplayTest',[ROOT/'research/combat-input-recording.efr'],'replay_tests.txt'),('AllCpuCharacters',[],'all_cpu_characters.txt')]:
        run(['java','-Xmx512m','-cp',dest,'org.elfen.engine.'+name,assets,*extra],log)

if __name__=='__main__':main()
