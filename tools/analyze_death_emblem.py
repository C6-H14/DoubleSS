from pathlib import Path
import cv2,numpy as np

root=Path(r'D:\StSmod\DoubleSS\art_candidates\video_source\death_splice_analysis')
src=root/'death_splice_frames_review_v10_support_20f'
for fi in [18,20,22,23,24,26,28,30,32,34,40,50,70]:
    im=cv2.imread(str(src/f'frame_{fi:04d}.png'))
    hsv=cv2.cvtColor(im,cv2.COLOR_BGR2HSV);h,s,v=cv2.split(hsv)
    # gold/orange pixels in torso band only
    m=((h>=8)&(h<=32)&(s>=80)&(v>=80)).astype(np.uint8)
    m[:260]=0;m[470:]=0;m[:,:470]=0;m[:,760:]=0
    n,l,st,c=cv2.connectedComponentsWithStats(m,8)
    comps=[]
    for k in range(1,n):
        a=st[k,cv2.CC_STAT_AREA]
        if a>=3: comps.append((int(a),*[int(st[k,z]) for z in [cv2.CC_STAT_LEFT,cv2.CC_STAT_TOP,cv2.CC_STAT_WIDTH,cv2.CC_STAT_HEIGHT]]))
    print(fi,sorted(comps,reverse=True)[:15])
