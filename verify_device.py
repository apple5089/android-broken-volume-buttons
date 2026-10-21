"""Integration test: actual audio usages + simulated VoIP, no phone calls or audible sound."""
import json
import re
import subprocess
import time
from pathlib import Path

ROOT=Path(__file__).resolve().parent

def adb(*args):
    return subprocess.check_output(['adb',*map(str,args)],encoding='utf-8',errors='replace')

def volumes():
    dump=adb('shell','dumpsys','audio'); result={}
    for name in ['MUSIC','ALARM','VOICE_CALL','RING']:
        block=dump.split('- STREAM_'+name+':',1)[1].split('\n- ',1)[0]
        result[name]=int(re.search(r'streamVolume:(\d+)',block).group(1))
    return result

def button(direction):
    dump=adb('shell','dumpsys','window','windows')
    block=next(b for b in re.split(r'(?=  Window #)',dump) if 'EasyVolumeOverlay' in b and 'mFrame=' in b)
    x,y,x2,y2=map(int,re.search(r'mFrame=\[(\d+),(\d+)\]\[(\d+),(\d+)\]',block).groups())
    density=(x2-x)/44
    return round((x+x2)/2),round(y+(22 if direction>0 else 78)*density)

def press(direction,long=False):
    x,y=button(direction)
    if long:adb('shell','input','swipe',x,y,x,y,1350)
    else:adb('shell','input','tap',x,y)
    time.sleep(0.7)

results={'device':adb('shell','getprop','ro.product.model').strip(),'checks':[],'method':'Silent PCM usages; simulated VoIP audio mode. No real phone calls.'}
try:
    for scene,stream in [('media','MUSIC'),('alarm','ALARM'),('call','VOICE_CALL')]:
        adb('shell','am','start','-W','-n','cn.local.easyvolume.qa/.SceneActivity','--es','scene',scene)
        time.sleep(1)
        before=volumes(); direction=-1 if before[stream]>=6 else 1
        try:
            press(direction);after=volumes()
            changed=[k for k in before if before[k]!=after[k]]
            passed=after[stream]==before[stream]+direction and changed==[stream]
            row={'scene':scene,'before':before,'after':after,'changed':changed,'passed':passed}
            results['checks'].append(row);print(json.dumps(row),flush=True)
            if not passed:raise RuntimeError('Wrong audio stream selected for '+scene)
        finally:
            for _ in range(15):
                current=volumes()[stream]
                if current==before[stream]:break
                press(1 if current<before[stream] else -1)
        if scene=='media':
            time.sleep(4)
            before=volumes()['MUSIC']
            press(1,True);after=volumes()['MUSIC']
            row={'scene':'media_long_press','before':before,'after':after,'passed':after-before>=2}
            results['checks'].append(row);print(json.dumps(row),flush=True)
            for _ in range(15):
                if volumes()['MUSIC']<=before:break
                press(-1)
            if not row['passed']:raise RuntimeError('Long press failed')
finally:
    adb('shell','am','force-stop','cn.local.easyvolume.qa')
    results['final_volumes']=volumes()
    (ROOT/'dist/runtime-checks-1.1.json').write_text(json.dumps(results,ensure_ascii=False,indent=2),encoding='utf-8')
