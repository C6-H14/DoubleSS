"""Correct measured shoe-span/baseline drift, then rebuild ultimate joins."""
from pathlib import Path
import json,shutil
import cv2,numpy as np
from PIL import Image,ImageDraw
from scipy.signal import savgol_filter
import build_character_runtime_v36_alt_attack_basic as runtime
import build_character_runtime_v46_alt_cast_s_bridge as flow
import bridge_alt_buff_v3 as media

ROOT=Path(r'D:\StSmod\DoubleSS')
SOURCE=ROOT/'art_candidates/character_runtime_v51_ultimate/images'
OUT=ROOT/'art_candidates/character_runtime_v52_ultimate_aligned'
CURRENT=ROOT/'src/main/resources/img/char/spine_inner'
STAGED=ROOT/'art_candidates/ultimate_staged_v52'
def landmarks(a):
    y,x=np.where((a[:,:,3]>128)&(np.indices(a.shape[:2])[0]>700))
    return np.array([x.min(),x.max(),y.max()],dtype=float)

def main():
    if OUT.exists():raise RuntimeError('Inspect existing version before rebuilding')
    paths=sorted(SOURCE.glob('ultimate_alpha_*.png'))
    assert len(paths)==81
    frames=[np.asarray(Image.open(p).convert('RGBA'),dtype=np.float32) for p in paths]
    idle=np.asarray(Image.open(flow.IDLE).convert('RGBA'),dtype=np.float32)
    target=landmarks(idle)
    raw=np.array([landmarks(a) for a in frames[3:-3]])
    smooth=savgol_filter(raw,7,2,axis=0)
    # Source standing head 393 / shoes 733, runtime idle head 393 / shoes 726.
    # Preserve deliberate crouching instead of forcing a fixed head elevation.
    sy=(726-393)/(733-393)
    matrices=[]
    corrected=[]
    for i,a in enumerate(frames):
        l,r,b=smooth[min(max(i-3,0),len(smooth)-1)]
        sx=(target[1]-target[0])/(r-l)
        m=np.float32([[sx,0,target[0]-sx*l],[0,sy,target[2]-sy*b]])
        corrected.append(cv2.warpAffine(a,m,(1024,1024),flags=cv2.INTER_LINEAR,borderMode=cv2.BORDER_CONSTANT,borderValue=(0,0,0,0)))
        matrices.append(m.tolist())
    first,last=corrected[3].copy(),corrected[-4].copy()
    fi,bi=flow.flow_between(idle,first),flow.flow_between(first,idle)
    fo,bo=flow.flow_between(last,idle),flow.flow_between(idle,last)
    for j in range(4):
        t=j/3
        corrected[j]=idle if j==0 else first if j==3 else flow.morph(idle,first,fi,bi,t)
        corrected[-4+j]=last if j==0 else idle if j==3 else flow.morph(last,idle,fo,bo,t)
    STAGED.mkdir(parents=True,exist_ok=True)
    runtime.SOURCE=SOURCE
    runtime.CURRENT=CURRENT
    runtime.OUT=OUT
    runtime.IMAGES=OUT/'images'
    runtime.EXPORT=OUT/'export'
    runtime.EXPECTED=81
    runtime.FIRST_PAGE=44
    runtime.PREFIX='ultimate_alpha_'
    runtime.ANIMATION='attack_ultimate'
    for i,a in enumerate(corrected,1):
        p=STAGED/f'ultimate_{i:03d}.png'
        media.to_straight(a.astype(np.float32)).save(p)
        runtime.FRAME_OVERRIDES[i]=p
    previous=json.loads((CURRENT/'character.json').read_text(encoding='utf-8'))
    runtime.main()
    new=json.loads((runtime.EXPORT/'character.json').read_text(encoding='utf-8'))
    assert all(new['animations'][k]==v for k,v in previous['animations'].items() if k!='attack_ultimate')
    for p in runtime.EXPORT.iterdir():
        if p.is_file():shutil.copy2(p,CURRENT/p.name)
    picks=[0,3,14,29,42,49,65,77,80]
    sheet=Image.new('RGB',(9*310,2*395),(32,32,32))
    draw=ImageDraw.Draw(sheet)
    for row,seq in enumerate((frames,corrected)):
        for col,i in enumerate(picks):
            a=seq[i]
            rgb=np.uint8(np.clip(a[:,:,:3]+32*(1-a[:,:,3:4]/255),0,255))
            im=Image.fromarray(rgb).crop((350,375,655,755))
            sheet.paste(im,(col*310,row*395))
            draw.text((col*310,row*395+380),f'{"before" if row==0 else "after"} {i+1}',fill='white')
    sheet.save(OUT/'alignment_review.png')
    metrics=np.array([landmarks(a) for a in corrected[3:-3]])
    report={'frames':81,'fps':60,'duration':1.35,'target_feet':target.tolist(),'vertical_scale':sy,'before_landmarks':raw.tolist(),'after_landmarks':metrics.tolist(),'matrices':matrices,'other_animations_preserved':True}
    (OUT/'alignment.json').write_text(json.dumps(report,indent=2),encoding='utf-8')
    print('target:',target,'after ranges:',metrics.min(axis=0),metrics.max(axis=0))
    assert np.max(np.abs(metrics[:,2]-target[2]))<=2
    print(OUT)

if __name__=='__main__':main()
