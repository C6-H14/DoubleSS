"""Append inner death, preserve all previous animation resources and timelines."""
from pathlib import Path
import json, shutil, hashlib
import numpy as np
import cv2
from PIL import Image, ImageDraw
import build_character_runtime_v25_idle as packer
import build_character_runtime_v51_ultimate as keyer
import build_character_runtime_v46_alt_cast_s_bridge as flow
import bridge_alt_buff_v3 as media

ROOT=Path(r'D:\StSmod\DoubleSS')
SOURCE=ROOT.parent/'art_preview/alternate_fencer_death/death_v1/frames_60fps'
ALPHA=ROOT.parent/'art_preview/alternate_fencer_death/death_v2_alpha/alpha_frames_60fps'
OUT=ROOT/'art_candidates/character_runtime_v53_death'
CURRENT=ROOT/'src/main/resources/img/char/spine_inner'

def main():
    if OUT.exists(): raise RuntimeError('Preserve existing build; use a new version')
    ALPHA.mkdir(parents=True,exist_ok=True)
    export=OUT/'export'; export.mkdir(parents=True)
    for p in CURRENT.iterdir():
        if p.is_file(): shutil.copy2(p,export/p.name)
    before=json.loads((CURRENT/'character.json').read_text())
    data=json.loads(json.dumps(before))
    attachments=data['skins']['default']['character_frame']
    paths=sorted(SOURCE.glob('death_*.png')); assert len(paths)==521
    idle=np.asarray(Image.open(flow.IDLE).convert('RGBA'),dtype=np.float32)
    first=np.asarray(keyer.matte(np.asarray(Image.open(paths[0]).convert('RGB'))))
    mask=(first[:,:,3]>180).astype(np.uint8)
    count,labels,stats,_=cv2.connectedComponentsWithStats(mask,8)
    label=1+np.argmax(stats[1:,cv2.CC_STAT_AREA])
    y,x=np.where(labels==label)
    top,bottom=y.min(),y.max()
    shoe_y,shoe_x=np.where((labels==label)&(np.indices(mask.shape)[0]>bottom-25))
    sx=240/(shoe_x.max()-shoe_x.min()); sy=333/(bottom-top)
    matrix=np.float32([[sx,0,382-sx*shoe_x.min()],[0,sy,726-sy*bottom]])
    # Use one mapping for all frames, including standing/bowing/disintegration.
    pixels=[]
    for i,p in enumerate(paths,1):
        im=keyer.matte(np.asarray(Image.open(p).convert('RGB')))
        if i>=510: im=Image.new('RGBA',im.size)
        im.save(ALPHA/f'death_alpha_{i:04d}.png')
        a=cv2.warpAffine(media.to_premultiplied(im),matrix,(1024,1024),flags=cv2.INTER_LINEAR,borderMode=cv2.BORDER_CONSTANT)
        pixels.append(a)
    target=pixels[5].copy()
    f,b=flow.flow_between(idle,target),flow.flow_between(target,idle)
    for i in range(6): pixels[i]=idle if i==0 else target if i==5 else flow.morph(idle,target,f,b,i/5)
    items=[]; keys=[]; hashes={}; samples=[]
    for i,a in enumerate(pixels):
        a=np.uint8(np.clip(a,0,255))
        digest=hashlib.sha256(a.tobytes()).hexdigest()
        if digest not in hashes:
            name=f'death_inner_{len(hashes)+1:03d}'; hashes[digest]=name
            im=Image.fromarray(a,'RGBA'); bbox=im.getchannel('A').getbbox() or (0,0,1,1)
            items.append((name,im,bbox))
            attachments[name]={'path':name,'x':0,'y':210.0,'width':1024,'height':1024}
        keys.append({'time':round(i/60,6),'name':hashes[digest]})
        if i in (0,5,120,240,248,300,360,420,480,520): samples.append((i,a))
    keys.append({'time':len(pixels)/60,'name':keys[-1]['name']})
    data['animations']['death']={'slots':{'character_frame':{'attachment':keys}}}
    packer.EXPORT=export
    page=1+max([int(p.stem.replace('character','') or '1') for p in export.glob('character*.png')])
    while items:
        items,section,_,_,_=packer.pack_page(items,page)
        with (export/'character.atlas').open('a',encoding='utf-8') as stream:stream.write(section)
        page+=1
    assert all(data['animations'][k]==v for k,v in before['animations'].items())
    (export/'character.json').write_text(json.dumps(data,separators=(',',':')),encoding='utf-8')
    sheet=Image.new('RGB',(1500,800),(32,32,32)); draw=ImageDraw.Draw(sheet)
    for k,(i,a) in enumerate(samples):
        rgb=np.uint8(np.clip(a[:,:,:3]+32*(1-a[:,:,3:4]/255),0,255))
        im=Image.fromarray(rgb).crop((320,350,850,780)); im.thumbnail((300,370))
        x,y=k%5*300,k//5*400;sheet.paste(im,(x,y));draw.text((x,y+375),str(i+1),fill='white')
    sheet.save(OUT/'matte_runtime_review.png')
    report={'frames':521,'fps':60,'duration':521/60,'unique_regions':len(hashes),'mapping':matrix.tolist(),'previous_animations_preserved':True,'final_frame_empty':not np.any(pixels[-1][:,:,3])}
    (OUT/'report.json').write_text(json.dumps(report,indent=2))
    print(json.dumps(report))

if __name__=='__main__':main()
