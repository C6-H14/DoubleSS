from pathlib import Path
import csv, cv2, numpy as np
from PIL import Image

ROOT=Path(r'D:\StSmod\DoubleSS\art_candidates\video_source\death_splice_analysis')
SRC=ROOT/'death_splice_frames_review_v10_support_20f'
rows=[]
for fi,p in enumerate(sorted(SRC.glob('frame_*.png'))):
    rgb=np.asarray(Image.open(p).convert('RGB'),np.uint8)
    hsv=cv2.cvtColor(rgb,cv2.COLOR_RGB2HSV);h,s,v=cv2.split(hsv)
    r,g,b=(rgb[...,i].astype(np.float32) for i in range(3))
    bias=np.minimum(r-g,b-g)
    cand=((h>=126)&(h<=168)&(s>=55)&(v>=45)&(bias>=28)).astype(np.uint8)
    n,lab,stats,cent=cv2.connectedComponentsWithStats(cand,8)
    border=set(np.unique(np.concatenate((lab[0],lab[-1],lab[:,0],lab[:,-1]))))
    for k in range(1,n):
        if k in border: continue
        area=int(stats[k,cv2.CC_STAT_AREA])
        if area<8: continue
        x,y,w,h0=[int(stats[k,z]) for z in (cv2.CC_STAT_LEFT,cv2.CC_STAT_TOP,cv2.CC_STAT_WIDTH,cv2.CC_STAT_HEIGHT)]
        cx,cy=cent[k]
        rows.append((fi,k,area,x,y,w,h0,round(float(cx),1),round(float(cy),1)))
with (ROOT/'death_internal_purple_components.csv').open('w',newline='',encoding='utf-8-sig') as fp:
    wr=csv.writer(fp);wr.writerow(['frame','label','area','x','y','w','h','cx','cy']);wr.writerows(rows)
print('components',len(rows),'frames',len(set(r[0] for r in rows)))
print('largest')
for r in sorted(rows,key=lambda x:x[2],reverse=True)[:40]:print(r)
