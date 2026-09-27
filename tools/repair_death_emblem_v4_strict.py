from pathlib import Path
import csv,shutil
import cv2,numpy as np
from PIL import Image
from scipy.ndimage import distance_transform_edt

ROOT=Path(r'D:\StSmod\DoubleSS\art_candidates\video_source\death_splice_analysis')
SRC=ROOT/'death_splice_v11_alpha_refined_v2'/'alpha_frames'
OUT=ROOT/'death_splice_v11_alpha_refined_v4_strict_emblem';FRAMES=OUT/'alpha_frames'

def gold(rgb):
    h,s,v=cv2.split(cv2.cvtColor(rgb,cv2.COLOR_RGB2HSV))
    return (h>=14)&(h<=34)&(s>=115)&(v>=85)

def components(rgb):
    m=gold(rgb).astype(np.uint8);n,l,st,c=cv2.connectedComponentsWithStats(m,8);out=[]
    for k in range(1,n):
        x,y,w,h,a=st[k]
        if 25<=a<=2600 and w<=100 and h<=100:out.append((k,float(c[k,0]),float(c[k,1]),int(a),x,y,w,h,l))
    return out

def main():
    if OUT.exists():shutil.rmtree(OUT)
    FRAMES.mkdir(parents=True)
    arr=[np.asarray(Image.open(p).convert('RGBA'),np.uint8).copy() for p in sorted(SRC.glob('*.png'))]
    ref=arr[22][...,:3]
    # Fixed reference bow center, verified on the last correct source frame.
    ref_center=np.array([597.0,292.0])
    gm=gold(ref);core=np.zeros(gm.shape,bool)
    for y0,y1,x0,x1 in ((298,320,550,581),(306,316,580,598),(298,324,598,623)):
        core[y0:y1,x0:x1]=gm[y0:y1,x0:x1]
    # Keep the anti-aliased dark rim immediately around the gold strokes.
    glyph_mask=cv2.dilate(core.astype(np.uint8),np.ones((3,3),np.uint8))>0
    glyph_color=ref.copy()

    prev=ref_center.copy();report=[];bg=np.array([132,0,213],np.float32)
    for i,a in enumerate(arr):
        rgb=a[...,:3].copy();alpha=a[...,3].copy()
        residue=(np.linalg.norm(rgb.astype(np.float32)-bg,axis=2)<=18)&(alpha>0)
        alpha[residue]=0;rgb[residue]=0
        restored=0
        if i>=23:
            cand=components(rgb)
            # Track locally from the prior bow; prevent jumps to hair/ornaments.
            near=[q for q in cand if 500<q[1]<735 and 230<q[2]<430 and np.linalg.norm(np.array(q[1:3])-prev)<75]
            if near:
                q=min(near,key=lambda z:(np.linalg.norm(np.array(z[1:3])-prev),-z[3]))
                center=np.array(q[1:3]);prev=center
                dx,dy=center-ref_center
                M=np.array([[1,0,dx],[0,1,dy]],np.float32)
                mask=cv2.warpAffine((glyph_mask*255).astype(np.uint8),M,(rgb.shape[1],rgb.shape[0]),flags=cv2.INTER_NEAREST)>0
                col=cv2.warpAffine(glyph_color,M,(rgb.shape[1],rgb.shape[0]),flags=cv2.INTER_NEAREST)
                yy,xx=np.where(mask)
                if len(xx):
                    x0,x1=max(xx.min()-5,0),min(xx.max()+6,rgb.shape[1]);y0,y1=max(yy.min()-5,0),min(yy.max()+6,rgb.shape[0])
                    old=gold(rgb);box=np.zeros_like(old);box[y0:y1,x0:x1]=1;old&=box
                    h,s,v=cv2.split(cv2.cvtColor(rgb,cv2.COLOR_RGB2HSV))
                    shirt=(h>=70)&(h<=112)&(s>=28)&(v<=175)&(~old)&(alpha>0)
                    if shirt.any():
                        _,nearidx=distance_transform_edt(~shirt,return_indices=True)
                        rgb[old]=rgb[nearidx[0],nearidx[1]][old]
                    h,s,v=cv2.split(cv2.cvtColor(rgb,cv2.COLOR_RGB2HSV))
                    shirt=(h>=70)&(h<=112)&(s>=24)&(v<=185)
                    allowed=mask&shirt&(alpha>0)
                    rgb[allowed]=col[allowed];restored=int(allowed.sum())
        Image.fromarray(np.dstack((rgb,alpha)),'RGBA').save(FRAMES/f'death_{i:04d}.png',optimize=True)
        report.append((i,int(residue.sum()),restored,float(prev[0]),float(prev[1])))
    with (OUT/'repair_report.csv').open('w',newline='',encoding='utf-8-sig') as fp:
        wr=csv.writer(fp);wr.writerow(['frame','purple_removed','emblem_pixels','bow_x','bow_y']);wr.writerows(report)
    print(OUT,'restored frames',sum(x[2]>0 for x in report))
if __name__=='__main__':main()
