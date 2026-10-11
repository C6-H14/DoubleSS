"""Append front death with a fixed idle-aligned transform and purple matte."""
from pathlib import Path
import json, shutil, hashlib
import cv2, numpy as np
from PIL import Image, ImageDraw
import build_character_runtime_v25_idle as packer
import build_character_runtime_v46_alt_cast_s_bridge as flow
import bridge_alt_buff_v3 as media

ROOT=Path(r'D:\StSmod\DoubleSS')
SOURCE=ROOT.parent/'art_preview/front_character_death/death_v2_part1_0p8s/frames_60fps'
ALPHA=ROOT.parent/'art_preview/front_character_death/death_v3_alpha/alpha_frames_60fps'
OUT=ROOT/'art_candidates/character_runtime_v54_front_death'
CURRENT=ROOT/'src/main/resources/img/char/spine'
IDLE=Path(r'E:\图与psd\img\DoubleSS\动画最终美术资源_20260924\art_candidates\character_runtime_v32_idle_shoes\images\idle_001.png')

def matte(rgb):
    f=rgb.astype(np.float32);hsv=cv2.cvtColor(rgb,cv2.COLOR_RGB2HSV)
    purple=(hsv[:,:,0]>=125)&(hsv[:,:,0]<=175)&(hsv[:,:,1]>55)&(f[:,:,2]>f[:,:,1]+8)&(f[:,:,0]>f[:,:,1]+8)
    bg=np.median(f[:80,:180].reshape(-1,3),axis=0)
    alpha=np.clip((np.linalg.norm(f-bg,axis=2)-10)/35,0,1);alpha[purple]=0
    count,labels,stats,_=cv2.connectedComponentsWithStats((alpha>.1).astype(np.uint8),8)
    keep=np.zeros(alpha.shape,bool)
    for i in range(1,count):
        if stats[i,cv2.CC_STAT_AREA]>=5:keep|=labels==i
    alpha[~keep]=0
    clean=np.clip((f-bg[None,None,:]*(1-alpha[:,:,None]))/np.maximum(alpha[:,:,None],.03),0,255)
    clean[alpha==0]=0
    return Image.fromarray(np.uint8(np.dstack((clean,np.rint(alpha*255)))),'RGBA')

def landmarks(a):
    mask=(a[:,:,3]>180).astype(np.uint8)
    _,labels,stats,_=cv2.connectedComponentsWithStats(mask,8)
    index=1+np.argmax(stats[1:,cv2.CC_STAT_AREA]);y,x=np.where(labels==index)
    bottom=y.max();sy,sx=np.where((labels==index)&(np.indices(mask.shape)[0]>bottom-22))
    return float(y.min()),float(bottom),float(sx.min()),float(sx.max())

def main():
    if OUT.exists():raise RuntimeError('Preserve build; choose new version')
    export=OUT/'export';export.mkdir(parents=True);ALPHA.mkdir(parents=True,exist_ok=True)
    for p in CURRENT.iterdir():
        if p.is_file():shutil.copy2(p,export/p.name)
    before=json.loads((CURRENT/'character.json').read_text());data=json.loads(json.dumps(before))
    attachments=data['skins']['default']['character_frame']
    paths=sorted(SOURCE.glob('death_*.png'));assert len(paths)==295
    idle=np.asarray(Image.open(IDLE).convert('RGBA'),dtype=np.float32)
    first=np.asarray(matte(np.asarray(Image.open(paths[0]).convert('RGB'))))
    top,bottom,left,right=landmarks(first);it,ib,il,ir=landmarks(idle)
    sx=(ir-il)/(right-left);sy=(ib-it)/(bottom-top)
    matrix=np.float32([[sx,0,il-sx*left],[0,sy,ib-sy*bottom]])
    pixels=[]
    for i,p in enumerate(paths,1):
        im=matte(np.asarray(Image.open(p).convert('RGB')));im.save(ALPHA/f'death_alpha_{i:04d}.png')
        a=cv2.warpAffine(media.to_premultiplied(im),matrix,(1024,1024),flags=cv2.INTER_LINEAR,borderMode=cv2.BORDER_CONSTANT)
        pixels.append(np.uint8(np.clip(a,0,255)))
    target=pixels[2].astype(np.float32)
    f,b=flow.flow_between(idle,target),flow.flow_between(target,idle)
    pixels[0]=np.uint8(idle);pixels[1]=np.uint8(np.clip(flow.morph(idle,target,f,b,.5),0,255))
    items=[];keys=[];hashes={}
    for i,a in enumerate(pixels):
        digest=hashlib.sha256(a.tobytes()).hexdigest()
        if digest not in hashes:
            name=f'death_front_{len(hashes)+1:03d}';hashes[digest]=name
            im=Image.fromarray(a,'RGBA');bbox=im.getchannel('A').getbbox();assert bbox
            items.append((name,im,bbox))
            attachments[name]={'path':name,'x':0,'y':210.0,'width':1024,'height':1024}
        keys.append({'time':round(i/60,6),'name':hashes[digest]})
    keys.append({'time':len(pixels)/60,'name':keys[-1]['name']})
    data['animations']['death']={'slots':{'character_frame':{'attachment':keys}}}
    packer.EXPORT=export;page=1+max(int(p.stem.replace('character','') or '1') for p in export.glob('character*.png'))
    while items:
        items,section,_,_,_=packer.pack_page(items,page)
        with (export/'character.atlas').open('a',encoding='utf-8') as stream:stream.write(section)
        page+=1
    assert all(data['animations'][k]==v for k,v in before['animations'].items())
    (export/'character.json').write_text(json.dumps(data,separators=(',',':')),encoding='utf-8')
    sheet=Image.new('RGB',(1500,700),(32,32,32));draw=ImageDraw.Draw(sheet)
    for k,i in enumerate((0,2,23,47,55,90,140,185,230,294)):
        a=pixels[i].astype(np.float32);rgb=np.uint8(np.clip(a[:,:,:3]+32*(1-a[:,:,3:4]/255),0,255))
        im=Image.fromarray(rgb).crop((260,350,950,800));im.thumbnail((300,325))
        x,y=k%5*300,k//5*350;sheet.paste(im,(x,y));draw.text((x,y+330),str(i+1),fill='white')
    sheet.save(OUT/'matte_runtime_review.png')
    report={'frames':295,'fps':60,'duration':295/60,'mapping':matrix.tolist(),'idle_landmarks':[it,ib,il,ir],'source_landmarks':[top,bottom,left,right],'unique_regions':len(hashes),'other_animations_preserved':True,'final_frame_held':True}
    (OUT/'report.json').write_text(json.dumps(report,indent=2));print(json.dumps(report))

if __name__=='__main__':main()
