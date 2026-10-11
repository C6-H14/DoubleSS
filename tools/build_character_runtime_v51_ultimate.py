"""Matte approved green-screen ultimate, append an isolated inner animation."""
from pathlib import Path
import json, shutil
import cv2,numpy as np
from PIL import Image
from scipy.ndimage import distance_transform_edt
import build_character_runtime_v36_alt_attack_basic as runtime
import bridge_alt_buff_v3 as media
import build_character_runtime_v46_alt_cast_s_bridge as flow

ROOT=Path(r'D:\StSmod\DoubleSS')
SOURCE=ROOT.parent/'art_preview/alternate_fencer_ultimate/ultimate_v3_81f_60fps/frames_60fps'
ALPHA=ROOT.parent/'art_preview/alternate_fencer_ultimate/ultimate_v4_alpha_81f_60fps'
FRAMES=ALPHA/'alpha_frames_60fps'
CURRENT=ROOT/'src/main/resources/img/char/spine_inner'
OUT=ROOT/'art_candidates/character_runtime_v51_ultimate'

def matte(rgb):
    f=rgb.astype(np.float32)
    hsv=cv2.cvtColor(rgb,cv2.COLOR_RGB2HSV)
    green=(hsv[:,:,0]>=40)&(hsv[:,:,0]<=95)&(f[:,:,1]>np.maximum(f[:,:,0],f[:,:,2])+4)&(hsv[:,:,1]>35)
    # Local background statistics follow the low-level video green variations.
    bg=np.median(f[:90,:180].reshape(-1,3),axis=0)
    distance=np.linalg.norm(f-bg,axis=2)
    alpha=np.clip((distance-9)/30,0,1)
    alpha[green]=0
    # Green key alone must not classify the black mesh face guard as background.
    foreground=(alpha>.06).astype(np.uint8)
    count,labels,stats,_=cv2.connectedComponentsWithStats(foreground,8)
    keep=np.zeros(foreground.shape,bool)
    for i in range(1,count):
        if stats[i,cv2.CC_STAT_AREA]>=4:
            keep|=labels==i
    alpha[~keep]=0
    # Unmix the keyed background from partially transparent fire/edge pixels.
    clean=np.clip((f-bg[None,None,:]*(1-alpha[:,:,None]))/np.maximum(alpha[:,:,None],.02),0,255)
    clean[alpha==0]=0
    return Image.fromarray(np.uint8(np.dstack((clean,np.rint(alpha*255)))),'RGBA')

def main():
    if OUT.exists(): raise RuntimeError('New output already exists; inspect before rebuilding')
    FRAMES.mkdir(parents=True,exist_ok=True)
    paths=sorted(SOURCE.glob('ultimate_*.png'))
    assert len(paths)==81
    alpha=[]
    for i,path in enumerate(paths,1):
        im=matte(np.asarray(Image.open(path).convert('RGB')))
        im.save(FRAMES/f'ultimate_alpha_{i:03d}.png',optimize=True)
        alpha.append(im)
    media.contact(alpha,ALPHA/'contact_dark.png',(32,32,32,255))
    media.contact(alpha,ALPHA/'contact_white.png',(255,255,255,255))
    # Scale from actual body endpoints, never from the floating sword/fire bounds.
    bounds=[alpha[i].getchannel('A').getbbox() for i in (0,80)]
    top=sum(b[1] for b in bounds)/2
    bottom=sum(b[3] for b in bounds)/2
    left=sum(b[0] for b in bounds)/2
    scale=341/(bottom-top)
    matrix=np.float32([[scale,0,379-left*scale],[0,scale,731-bottom*scale]])
    pixels=[cv2.warpAffine(media.to_premultiplied(im),matrix,(1024,1024),flags=cv2.INTER_LINEAR,borderMode=cv2.BORDER_CONSTANT,borderValue=(0,0,0,0)) for im in alpha]
    idle=np.asarray(Image.open(flow.IDLE).convert('RGBA'),dtype=np.float32)
    # Retain 81 frames; first/last four meet the exact runtime idle artwork.
    first,last=pixels[3].copy(),pixels[-4].copy()
    fi,bi=flow.flow_between(idle,first),flow.flow_between(first,idle)
    fo,bo=flow.flow_between(last,idle),flow.flow_between(idle,last)
    for j in range(4):
        t=j/3
        pixels[j]=idle if j==0 else first if j==3 else flow.morph(idle,first,fi,bi,t)
        pixels[-4+j]=last if j==0 else idle if j==3 else flow.morph(last,idle,fo,bo,t)
    staged=ROOT/'art_candidates/ultimate_staged_v51'
    staged.mkdir(parents=True,exist_ok=True)
    runtime.SOURCE=FRAMES
    runtime.CURRENT=CURRENT
    runtime.OUT=OUT
    runtime.IMAGES=OUT/'images'
    runtime.EXPORT=OUT/'export'
    runtime.EXPECTED=81
    runtime.FIRST_PAGE=44
    runtime.PREFIX='ultimate_alpha_'
    runtime.ANIMATION='attack_ultimate'
    for i,p in enumerate(pixels,1):
        path=staged/f'ultimate_{i:03d}.png'
        media.to_straight(p.astype(np.float32)).save(path)
        runtime.FRAME_OVERRIDES[i]=path
    before=json.loads((CURRENT/'character.json').read_text(encoding='utf-8'))
    runtime.main()
    after=json.loads((runtime.EXPORT/'character.json').read_text(encoding='utf-8'))
    assert all(after['animations'][k]==v for k,v in before['animations'].items())
    for path in runtime.EXPORT.iterdir():
        if path.is_file(): shutil.copy2(path,CURRENT/path.name)
    review=[]
    for i in (0,3,29,42,50,65,77,80):
        p=pixels[i]
        rgb=np.uint8(np.clip(p[:,:,:3]+32*(1-p[:,:,3:4]/255),0,255))
        review.append(Image.fromarray(rgb).crop((330,350,990,760)).resize((660,410)))
    sheet=Image.new('RGB',(660*4,410*2),(32,32,32))
    for j,im in enumerate(review):sheet.paste(im,(j%4*660,j//4*410))
    sheet.save(OUT/'runtime_review.png')
    (ALPHA/'report.json').write_text(json.dumps({'frames':81,'fps':60,'duration':1.35,'mapping':matrix.tolist(),'source_bounds':bounds,'animation':'attack_ultimate','previous_animations_preserved':True,'idle_bridge_frames':4},indent=2),encoding='utf-8')
    print('Preserved',len(before['animations']),'prior animations; added attack_ultimate')

if __name__=='__main__':main()
