"""Prayer pink-fringe pass using tighter subject protection around marked areas."""
from pathlib import Path
import json, shutil
import cv2
import numpy as np
from PIL import Image
import audit_front_animations as audit
import repair_front_animations_v56 as helper
import build_character_runtime_v25_idle as packer

ROOT=Path(r'D:\StSmod\DoubleSS')
CURRENT=audit.CURRENT
OUT=ROOT/'art_candidates/character_runtime_v60_prayer_marked_fringe'

def correct(raw):
    a=helper.straight(raw,True)
    rgb=a[:,:,:3].astype(np.int16);alpha=a[:,:,3]
    hsv=cv2.cvtColor(a[:,:,:3],cv2.COLOR_RGB2HSV)
    yy,xx=np.indices(alpha.shape)
    # Six small regions matching the user's blue circles in the runtime canvas.
    # Never color-grade the entire spell or character: it yellowed the hands.
    circles=[(594,472,29,35),(444,661,23,27),(460,719,29,20),
             (558,663,25,42),(607,701,26,38),(551,730,22,18)]
    marked=np.zeros(alpha.shape,bool)
    for cx,cy,rx,ry in circles:marked|=((xx-cx)/rx)**2+((yy-cy)/ry)**2<=1
    skin=((xx>=510)&(xx<=575)&(yy>=405)&(yy<=477))
    skin|=((xx>=572)&(xx<=604)&(yy>=492)&(yy<=528))
    skin|=((xx>=534)&(xx<=578)&(yy>=575)&(yy<=643))
    skin|=((xx>=467)&(xx<=526)&(yy>=630)&(yy<=706))
    # Keep dark leather/line art while neutralizing only light pink fringe.
    leather=(hsv[:,:,2]<145)&(hsv[:,:,1]>35)
    warm=(hsv[:,:,0]<20)&(rgb[:,:,0]>rgb[:,:,1]+7)&(rgb[:,:,2]>rgb[:,:,1]+5)
    magenta=(hsv[:,:,0]>=125)&(hsv[:,:,0]<=179)&(hsv[:,:,1]>25)
    change=(warm|magenta)&marked&~skin&~leather&(alpha>0)
    fixed=hsv.copy();fixed[change,0]=24
    fixed[change,1]=np.maximum(fixed[change,1],80)
    new_rgb=cv2.cvtColor(fixed,cv2.COLOR_HSV2RGB)
    colored=helper.prem(np.dstack((new_rgb,alpha)))
    result=raw.copy();result[change,:3]=colored[change,:3]
    assert np.array_equal(result[:,:,3],raw[:,:,3])
    assert np.array_equal(result[skin],raw[skin])
    return result,int(change.sum())

def main():
    if OUT.exists():raise RuntimeError('Preserve existing version')
    OUT.mkdir(parents=True)
    export=OUT/'export';shutil.copytree(CURRENT,export)
    shutil.copy2(ROOT/'target/DoubleSS.jar',OUT/'DoubleSS_before.jar')
    data=json.loads((CURRENT/'character.json').read_text());original=json.loads(json.dumps(data))
    reg=audit.atlas_util.regions();keys=data['animations']['cast_prayer']['slots']['character_frame']['attachment']
    idle=audit.raw_frame(data['animations']['idle']['slots']['character_frame']['attachment'][0]['name'],data,reg)
    items=[];cache={};pixels=[];report=[]
    for i,k in enumerate(keys):
        old=k['name']
        if old not in cache:
            raw=audit.raw_frame(old,original,reg)
            fixed,count=correct(raw) if 17<=i<=54 else (raw,0)
            if count:
                name='front60_prayer_'+str(len(cache)+1).zfill(3)
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
