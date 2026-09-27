from pathlib import Path
import json
import cv2
import numpy as np
from PIL import Image

ROOT=Path(r"D:\StSmod\DoubleSS")
BASE=ROOT/"art_candidates/video_source/idle"
FRAMES=BASE/"short_blink_final_v21_solid_rgb_repair/alpha_frames"
ANNOT=Path(r"C:\Users\C6H14\AppData\Local\Temp\codex-clipboard-43b89843-cdea-47db-aaa1-80489c0d31c2.png")
OUT=BASE/"diagnostics_hand_redcircles_v24"

def main():
    OUT.mkdir(parents=True,exist_ok=True)
    ann=cv2.cvtColor(cv2.imread(str(ANNOT),cv2.IMREAD_COLOR),cv2.COLOR_BGR2RGB)
    # Exclude red markup and a small halo around it from matching.
    red=(ann[:,:,0]>180)&(ann[:,:,1]<110)&(ann[:,:,2]<110)
    red=cv2.dilate(red.astype(np.uint8),np.ones((9,9),np.uint8))>0
    valid=(~red).astype(np.uint8)*255
    best=[]
    scales=np.arange(0.32,0.43,0.02)
    for p in sorted(FRAMES.glob('idle_*.png')):
        rgba=np.asarray(Image.open(p).convert('RGBA'),np.uint8)
        bg=np.full_like(rgba[:,:,:3],45)
        af=rgba[:,:,3:4].astype(np.float32)/255
        frame=np.rint(rgba[:,:,:3]*af+bg*(1-af)).astype(np.uint8)
        # Hands remain inside this stable window in every idle frame.
        search=frame[270:620,320:750]
        for s in scales:
            tw=max(20,int(ann.shape[1]*s));th=max(20,int(ann.shape[0]*s))
            if tw>=search.shape[1] or th>=search.shape[0]:continue
            tpl=cv2.resize(ann,(tw,th),interpolation=cv2.INTER_AREA)
            mask=cv2.resize(valid,(tw,th),interpolation=cv2.INTER_NEAREST)
            # masked SQDIFF normalized: lower is better
            res=cv2.matchTemplate(search,tpl,cv2.TM_SQDIFF_NORMED,mask=mask)
            _,v,_,loc=cv2.minMaxLoc(res)
            best.append((float(v),int(p.stem[-3:]),float(s),loc[0]+320,loc[1]+270,tw,th))
    best.sort(key=lambda x:x[0]);top=best[:20]
    (OUT/'matches.json').write_text(json.dumps(top,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(top[:10],indent=2))
if __name__=='__main__':main()
