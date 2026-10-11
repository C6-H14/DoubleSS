"""Rebuild prayer from the un-yellowed v56 frames; correct pink only."""
from pathlib import Path
import json, shutil
import cv2
import numpy as np
from PIL import Image
import audit_front_animations as audit
import repair_front_animations_v56 as helper
import build_character_runtime_v25_idle as packer

ROOT=Path(r'D:\StSmod\DoubleSS')
BASE=ROOT/'art_candidates/character_runtime_v56_front_repaired/export'
OUT=ROOT/'art_candidates/character_runtime_v61b_prayer_from_v56'

def correct(raw):
    rgba=helper.straight(raw,True)
    rgb=rgba[:,:,:3].astype(np.int16);alpha=rgba[:,:,3]
    hsv=cv2.cvtColor(rgba[:,:,:3],cv2.COLOR_RGB2HSV)
    # Pink has a blue channel above green, unlike the original gold aura,
    # brown shoes, white socks, and the neutral steel sword.
    pink=(rgb[:,:,0]>rgb[:,:,1]+12)&(rgb[:,:,2]>rgb[:,:,1]+16)
    pink&=(hsv[:,:,1]>=40)&(alpha>0)
    fixed=hsv.copy();fixed[pink,0]=23;fixed[pink,1]=np.maximum(fixed[pink,1],75)
    converted=cv2.cvtColor(fixed,cv2.COLOR_HSV2RGB)
    out=raw.copy();pma=helper.prem(np.dstack((converted,alpha)))
    out[pink,:3]=pma[pink,:3]
    assert np.array_equal(out[:,:,3],raw[:,:,3])
    # Source-identical neutral sword/stocking pixels are more important than
    # guessing where a moving limb lies at a particular frame.
    neutral=(np.abs(rgb[:,:,0]-rgb[:,:,1])<=12)&(np.abs(rgb[:,:,2]-rgb[:,:,1])<=16)
    assert np.array_equal(out[neutral],raw[neutral])
    return out,int(pink.sum())

def main():
    if OUT.exists():raise RuntimeError('Preserve existing version')
    OUT.mkdir(parents=True)
    export=OUT/'export';shutil.copytree(BASE,export)
    shutil.copy2(Path(r'E:\SteamLibrary\steamapps\common\SlayTheSpire\mods\DoubleSS.jar'),OUT/'DoubleSS_before.jar')
    audit.CURRENT=BASE;audit.atlas_util.CURRENT=BASE;audit.PAGES.clear()
    data=json.loads((BASE/'character.json').read_text());original=json.loads(json.dumps(data))
    regions=audit.atlas_util.regions();keys=data['animations']['cast_prayer']['slots']['character_frame']['attachment']
    idle=audit.raw_frame(data['animations']['idle']['slots']['character_frame']['attachment'][0]['name'],data,regions)
    pixels=[];cache={};items=[];report=[]
    for i,k in enumerate(keys):
        old=k['name']
        if old not in cache:
            raw=audit.raw_frame(old,original,regions)
            fixed,count=correct(raw) if 16<=i<=56 else (raw,0)
            if count:
                name=f'front61_prayer_{len(cache)+1:03d}'
                im=Image.fromarray(fixed);items.append((name,im,im.getchannel('A').getbbox()))
                data['skins']['default']['character_frame'][name]={'path':name,'x':0,'y':210,'width':1024,'height':1024}
            else:name=old
            cache[old]=(name,fixed,count)
        name,fixed,count=cache[old];k['name']=name
        if i<len(keys)-1:pixels.append(fixed)
        report.append({'frame':i+1,'pink_corrected':count})
    packer.EXPORT=export;page=packer.next_page_number(export);atlas=(export/'character.atlas').read_text()
    while items:
        items,section,_,_,_=packer.pack_page(items,page);atlas+=section;page+=1
    (export/'character.atlas').write_text(atlas,encoding='utf-8')
    (export/'character.json').write_text(json.dumps(data,separators=(',',':')),encoding='utf-8')
    assert all(data['animations'][n]==original['animations'][n] for n in data['animations'] if n!='cast_prayer')
    assert [k['time'] for k in keys]==[k['time'] for k in original['animations']['cast_prayer']['slots']['character_frame']['attachment']]
    assert np.array_equal(pixels[0],idle) and np.array_equal(pixels[-1],idle)
    helper.make_preview('cast_prayer',pixels,1.2,OUT/'preview',idle)
    (OUT/'report.json').write_text(json.dumps(report,indent=2),encoding='utf-8')
    print('COMPLETE',OUT,'changed',sum(r['pink_corrected'] for r in report))

if __name__=='__main__':main()
