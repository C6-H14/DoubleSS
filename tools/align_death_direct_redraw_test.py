from pathlib import Path
import cv2
import numpy as np

ROOT=Path(r"D:\StSmod\DoubleSS\art_candidates\video_source\death_splice_analysis")
target=cv2.imread(str(ROOT/'death_splice_frames_review_v4_repaired'/'frame_0036.png'))
redraw=cv2.imread(str(ROOT/'death_local_repair_tests'/'frame_0036_direct_redraw_raw.png'))
h,w=target.shape[:2]
redraw=cv2.resize(redraw,(w,h),interpolation=cv2.INTER_LANCZOS4)
BG=np.array([213,0,132],np.float32)


def box(im):
    d=np.linalg.norm(im.astype(np.float32)-BG,axis=2)
    y,x=np.where(d>34)
    return np.percentile(x,[2,98]),np.percentile(y,[1,99])


(tx,ty)=box(target); (rx,ry)=box(redraw)
sw=(tx[1]-tx[0])/(rx[1]-rx[0]); sh=(ty[1]-ty[0])/(ry[1]-ry[0])
s=float(np.sqrt(sw*sh))
tc=((tx[0]+tx[1])/2,(ty[0]+ty[1])/2)
rc=((rx[0]+rx[1])/2,(ry[0]+ry[1])/2)
M=np.array([[s,0,tc[0]-s*rc[0]],[0,s,tc[1]-s*rc[1]]],np.float32)
aligned=cv2.warpAffine(redraw,M,(w,h),flags=cv2.INTER_LANCZOS4,
                       borderMode=cv2.BORDER_CONSTANT,borderValue=(213,0,132))
out=ROOT/'death_local_repair_tests'/'frame_0036_direct_redraw_aligned.png'
cv2.imwrite(str(out),aligned)

tw,th=560,416
sheet=np.empty((th,2*tw,3),np.uint8)
sheet[:,:tw]=cv2.resize(target,(tw,th),interpolation=cv2.INTER_AREA)
sheet[:,tw:]=cv2.resize(aligned,(tw,th),interpolation=cv2.INTER_AREA)
cv2.putText(sheet,'original',(10,28),cv2.FONT_HERSHEY_SIMPLEX,.75,(255,255,255),2,cv2.LINE_AA)
cv2.putText(sheet,'direct redraw test',(tw+10,28),cv2.FONT_HERSHEY_SIMPLEX,.75,(255,255,255),2,cv2.LINE_AA)
cmp=ROOT/'death_local_repair_tests'/'frame_0036_direct_redraw_comparison.png'
cv2.imwrite(str(cmp),sheet)
print(out); print(cmp); print('scale',s)
