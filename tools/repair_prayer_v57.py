"""Prayer-only warm pink halo correction, keeping Alpha and timing exact."""
from pathlib import Path
import json, shutil, hashlib
import cv2
import numpy as np
from PIL import Image
import audit_front_animations as audit
import repair_front_animations_v56 as helper
import build_character_runtime_v25_idle as packer

ROOT=Path(r'D:\StSmod\DoubleSS')
OUT=ROOT/'art_candidates/character_runtime_v57b_prayer_gold'
CURRENT=audit.CURRENT

def correct(raw):
    a=helper.straight(raw,True)
    hsv=cv2.cvtColor(a[:,:,:3],cv2.COLOR_RGB2HSV)
    rgb=a[:,:,:3].astype(float);yy,xx=np.indices(a.shape[:2])
    # These bounded regions protect the face, clasped hands and exposed legs
    # throughout the stationary kneeling/effect portion, not just one keyframe.
    skin=((xx>=495)&(xx<=577)&(yy>=407)&(yy<=480))
    skin|=((xx>=558)&(xx<=612)&(yy>=478)&(yy<=545))
    skin|=((xx>=460)&(xx<=592)&(yy>=565)&(yy<=714))
    skin|=((xx>=395)&(xx<=453)&(yy>=645)&(yy<=730))
    skin|=((xx>=542)&(xx<=616)&(yy>=675)&(yy<=731))
    pink=((hsv[:,:,0]<19)|(hsv[:,:,0]>120))&(rgb[:,:,0]>rgb[:,:,1]+7)&(rgb[:,:,2]>rgb[:,:,1]*.76)&(a[:,:,3]>0)
    # Only the upright aura and its rune base; standing entry/exit stay exact.
    effect=(xx>=400)&(xx<=637)&(yy>=325)&(yy<=797)
    change=pink&effect&~skin
    fixed=hsv.copy();fixed[change,0]=24
    fixed[change,1]=np.maximum(fixed[change,1],95)
    new=cv2.cvtColor(fixed,cv2.COLOR_HSV2RGB)
    result=raw.copy();colored=helper.prem(np.dstack((new,a[:,:,3])))
    result[change,:3]=colored[change,:3]
    assert np.array_equal(result[:,:,3],raw[:,:,3])
    assert np.array_equal(result[skin],raw[skin])
    return result,int(change.sum())

def main():
    if OUT.exists():raise RuntimeError('Version already exists')
    OUT.mkdir(parents=True)
    export=OUT/'export';shutil.copytree(CURRENT,export)
    shutil.copy2(ROOT/'target/DoubleSS.jar',OUT/'DoubleSS_before.jar')
    data=json.loads((CURRENT/'character.json').read_text());original=json.loads(json.dumps(data))
    reg=audit.atlas_util.regions();keys=data['animations']['cast_prayer']['slots']['character_frame']['attachment']
    idle= audit.raw_frame(data['animations']['idle']['slots']['character_frame']['attachment'][0]['name'],data,reg)
    items=[];cache={};pixels=[];report=[]
    for i,k in enumerate(keys):
        old=k['name']
        if old not in cache:
            raw=audit.raw_frame(old,original,reg)
            # No aura in the entry/exit region: don't touch their normal colors.
            fixed,count=correct(raw) if 20<=i<=50 else (raw,0)
            if count:
                name='front57_prayer_'+str(len(cache)+1).zfill(3)
                image=Image.fromarray(fixed);items.append((name,image,image.getchannel('A').getbbox()))
                data['skins']['default']['character_frame'][name]={'path':name,'x':0,'y':210,'width':1024,'height':1024}
            else:name=old
            cache[old]=(name,fixed,count)
        name,fixed,count=cache[old];k['name']=name
        if i<len(keys)-1:pixels.append(fixed)
        report.append({'key':i+1,'recolored_pixels':count})
    packer.EXPORT=export;page=packer.next_page_number(export);atlas=(export/'character.atlas').read_text()
    while items:
        items,section,_,_,_=packer.pack_page(items,page);atlas+=section;page+=1
    (export/'character.atlas').write_text(atlas,encoding='utf-8')
    (export/'character.json').write_text(json.dumps(data,separators=(',',':')),encoding='utf-8')
    for name in original['animations']:
        if name!='cast_prayer':assert data['animations'][name]==original['animations'][name]
    assert [k.get('time',0) for k in keys]==[k.get('time',0) for k in original['animations']['cast_prayer']['slots']['character_frame']['attachment']]
    assert np.array_equal(pixels[0],idle) and np.array_equal(pixels[-1],idle)
    helper.make_preview('cast_prayer',pixels,1.2,OUT/'preview',idle)
    (OUT/'report.json').write_text(json.dumps(report,indent=2),encoding='utf-8')
    print('COMPLETE',OUT,'changed',sum(r['recolored_pixels'] for r in report))

if __name__=='__main__':main()
