"""Keep gold aura from v58, restore unchanged character/steel from v56."""
from pathlib import Path
import json, shutil
import cv2
import numpy as np
from PIL import Image
import audit_front_animations as audit
import repair_front_animations_v56 as helper
import build_character_runtime_v25_idle as packer

ROOT=Path(r'D:\StSmod\DoubleSS')
GOLD=ROOT/'art_candidates/character_runtime_v58_prayer_fringe/export'
ORIG=ROOT/'art_candidates/character_runtime_v56_front_repaired/export'
OUT=ROOT/'art_candidates/character_runtime_v62_prayer_composite'

def read(folder):
    audit.CURRENT=folder;audit.atlas_util.CURRENT=folder;audit.PAGES.clear()
    data=json.loads((folder/'character.json').read_text())
    regions=audit.atlas_util.regions()
    keys=data['animations']['cast_prayer']['slots']['character_frame']['attachment']
    return data,[audit.raw_frame(k['name'],data,regions) for k in keys]

def fix(gold,orig):
    assert np.array_equal(gold[:,:,3],orig[:,:,3])
    raw=helper.straight(orig,True);hsv=cv2.cvtColor(raw[:,:,:3],cv2.COLOR_RGB2HSV)
    yy,xx=np.indices(raw.shape[:2]);alpha=orig[:,:,3]
    neutral=(hsv[:,:,1]<=66)&(hsv[:,:,2]>=70)&(alpha>=100)
    # Restore actual white stockings and the silvery sword from pristine v56.
    socks=(((xx>=409)&(xx<=510)&(yy>=635)&(yy<=719))|
           ((xx>=541)&(xx<=591)&(yy>=572)&(yy<=722)))&neutral
    sword=(xx>=577)&(xx<=616)&(yy>=515)&(yy<=740)&neutral
    hands=(xx>=562)&(xx<=610)&(yy>=476)&(yy<=530)&(alpha>=100)
    face=(xx>=510)&(xx<=575)&(yy>=405)&(yy<=482)&(alpha>=100)
    hair=(xx>=432)&(xx<=581)&(yy>=385)&(yy<=544)&(hsv[:,:,0]>=12)&(hsv[:,:,0]<=38)&(alpha>=180)
    restore=socks|sword|hands|face|hair
    out=gold.copy();out[restore]=orig[restore]
    assert np.array_equal(out[:,:,3],orig[:,:,3])
    assert np.array_equal(out[socks|sword],orig[socks|sword])
    return out,int(restore.sum())

def main():
    if OUT.exists():raise RuntimeError('Preserve existing version')
    OUT.mkdir(parents=True);export=OUT/'export';shutil.copytree(GOLD,export)
    shutil.copy2(Path(r'E:\SteamLibrary\steamapps\common\SlayTheSpire\mods\DoubleSS.jar'),OUT/'DoubleSS_before.jar')
    data,gold=read(GOLD);original= json.loads(json.dumps(data));_,orig=read(ORIG)
    assert len(gold)==len(orig)
    keys=data['animations']['cast_prayer']['slots']['character_frame']['attachment']
    idle=orig[0];items=[];pixels=[];report=[];cache={}
    for i,k in enumerate(keys):
        old=k['name']
        if old not in cache:
            fixed,count=fix(gold[i],orig[i]) if 15<=i<=57 else (gold[i],0)
            if count and not np.array_equal(fixed,gold[i]):
                name=f'front62_prayer_{len(cache)+1:03d}'
                im=Image.fromarray(fixed);items.append((name,im,im.getchannel('A').getbbox()))
                data['skins']['default']['character_frame'][name]={'path':name,'x':0,'y':210,'width':1024,'height':1024}
            else:name=old
            cache[old]=(name,fixed,count)
        name,fixed,count=cache[old];k['name']=name
        if i<len(keys)-1:pixels.append(fixed)
        report.append({'frame':i+1,'original_subject_pixels_restored':count})
    packer.EXPORT=export;page=packer.next_page_number(export);atlas=(export/'character.atlas').read_text()
    while items:
        items,section,_,_,_=packer.pack_page(items,page);atlas+=section;page+=1
    (export/'character.atlas').write_text(atlas,encoding='utf-8')
    (export/'character.json').write_text(json.dumps(data,separators=(',',':')),encoding='utf-8')
    assert all(data['animations'][n]==original['animations'][n] for n in data['animations'] if n!='cast_prayer')
    assert [k['time'] for k in keys]==[k['time'] for k in original['animations']['cast_prayer']['slots']['character_frame']['attachment']]
    assert np.array_equal(pixels[0],orig[0]) and np.array_equal(pixels[-1],orig[-2])
    helper.make_preview('cast_prayer',pixels,1.2,OUT/'preview',idle)
    (OUT/'report.json').write_text(json.dumps(report,indent=2),encoding='utf-8')
    print('COMPLETE',OUT)

if __name__=='__main__':main()
