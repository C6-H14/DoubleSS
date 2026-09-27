from pathlib import Path
import cv2
import numpy as np

ROOT=Path(r"D:\StSmod\DoubleSS\art_candidates\video_source\death_splice_analysis\death_splice_frames_review_v3_uniform_bg")
BG=np.array([213,0,132],np.float32)


def metric(im):
    d=np.linalg.norm(im.astype(np.float32)-BG,axis=2)
    m=(d>32).astype(np.uint8)
    m=cv2.morphologyEx(m,cv2.MORPH_OPEN,np.ones((3,3),np.uint8))
    ys,xs=np.where(m>0)
    # Robust percentiles suppress the long thin sword from scale measurement.
    x0,x1=np.percentile(xs,[2,98]); y0,y1=np.percentile(ys,[1,99])
    return x0,y0,x1,y1,len(xs),(x0+x1)/2,(y0+y1)/2


for i in range(45,82):
    im=cv2.imread(str(ROOT/f'frame_{i:04d}.png'))
    x0,y0,x1,y1,area,cx,cy=metric(im)
    print(f'{i:03d} w={x1-x0:7.1f} h={y1-y0:7.1f} area={area:7d} center=({cx:7.1f},{cy:7.1f})')
