from pathlib import Path
import csv, shutil
import cv2, numpy as np
from PIL import Image
from scipy.ndimage import distance_transform_edt

ROOT=Path(r'D:\StSmod\DoubleSS\art_candidates\video_source\death_splice_analysis')
SRC=ROOT/'death_splice_v11_alpha_refined_v2'/'alpha_frames'
OUT=ROOT/'death_splice_v11_alpha_refined_v3_emblem'
FRAMES=OUT/'alpha_frames'


def gold_mask(rgb):
    hsv=cv2.cvtColor(rgb,cv2.COLOR_RGB2HSV);h,s,v=cv2.split(hsv)
    return (h>=8)&(h<=32)&(s>=65)&(v>=75)


def bow_box(rgb):
    m=gold_mask(rgb).astype(np.uint8)
    gate=np.zeros_like(m);gate[240:390,520:735]=1;m*=gate
    n,l,st,_=cv2.connectedComponentsWithStats(m,8)
    choices=[]
    for k in range(1,n):
        x,y,w,h,a=st[k]
        if a>=180 and w>=18 and h>=16: choices.append((a,x,y,w,h))
    if not choices:return None
    _,x,y,w,h=max(choices)
    return np.array([x,y,w,h],np.float32)


def main():
    if OUT.exists():shutil.rmtree(OUT)
    FRAMES.mkdir(parents=True)
    paths=sorted(SRC.glob('*.png'))
    rgba=[np.asarray(Image.open(p).convert('RGBA'),np.uint8).copy() for p in paths]
    ref=rgba[22][...,:3]; rb=bow_box(ref)
    # Last correct S-S: gold pixels below the bow, excluding skirt/button.
    glyph=np.zeros(ref.shape[:2],np.uint8)
    gm=gold_mask(ref)
    glyph[312:382,548:650]=gm[312:382,548:650].astype(np.uint8)*255
    # Keep meaningful glyph strokes while retaining the hyphen.
    n,l,st,_=cv2.connectedComponentsWithStats((glyph>0).astype(np.uint8),8)
    keep=np.zeros_like(glyph,bool)
    for k in range(1,n):
        if st[k,cv2.CC_STAT_AREA]>=3:keep|=l==k
    glyph=(keep*255).astype(np.uint8)
    color=np.zeros_like(ref);color[glyph>0]=ref[glyph>0]

    report=[]
    bg=np.array([132,0,213],np.float32)
    for i,arr in enumerate(rgba):
        rgb=arr[...,:3].copy();alpha=arr[...,3].copy()
        # Exact/near-exact background colour can never belong to the character.
        residue=(np.linalg.norm(rgb.astype(np.float32)-bg,axis=2)<=18)&(alpha>0)
        alpha[residue]=0;rgb[residue]=0
        repaired=0
        if i>=23:
            tb=bow_box(rgb)
            if tb is not None:
                sx=float(np.clip(tb[2]/rb[2],.72,1.32));sy=float(np.clip(tb[3]/rb[3],.72,1.32))
                rc=np.array([rb[0]+rb[2]/2,rb[1]+rb[3]/2]);tc=np.array([tb[0]+tb[2]/2,tb[1]+tb[3]/2])
                tx=tc[0]-sx*rc[0];ty=tc[1]-sy*rc[1]
                M=np.array([[sx,0,tx],[0,sy,ty]],np.float32)
                wg=cv2.warpAffine(glyph,M,(rgb.shape[1],rgb.shape[0]),flags=cv2.INTER_LINEAR)
                wc=cv2.warpAffine(color,M,(rgb.shape[1],rgb.shape[0]),flags=cv2.INTER_LINEAR)
                ys,xs=np.where(wg>6)
                if len(xs):
                    x0,x1=max(xs.min()-6,0),min(xs.max()+7,rgb.shape[1]);y0,y1=max(ys.min()-6,0),min(ys.max()+7,rgb.shape[0])
                    oldgold=gold_mask(rgb)&(np.indices(rgb.shape[:2])[0]>=int(tb[1]+tb[3]-3))
                    region=np.zeros_like(oldgold);region[y0:y1,x0:x1]=1;oldgold&=region
                    # Replace degenerated yellow dots with the nearest shirt texel.
                    hsv=cv2.cvtColor(rgb,cv2.COLOR_RGB2HSV);hh,ss,vv=cv2.split(hsv)
                    shirt=(hh>=70)&(hh<=115)&(ss>=35)&(vv<=155)&(~oldgold)
                    if shirt.any():
                        _,near=distance_transform_edt(~shirt,return_indices=True)
                        rgb[oldgold]=rgb[near[0],near[1]][oldgold]
                    hsv=cv2.cvtColor(rgb,cv2.COLOR_RGB2HSV);hh,ss,vv=cv2.split(hsv)
                    shirt=(hh>=70)&(hh<=115)&(ss>=28)&(vv<=175)
                    allowed=(shirt|oldgold)&(alpha>0)&(wg>6)
                    blend=(wg.astype(np.float32)/255.0)[...,None]
                    mixed=np.uint8(np.clip(rgb*(1-blend)+wc*blend,0,255))
                    rgb[allowed]=mixed[allowed];repaired=int(allowed.sum())
        arr=np.dstack((rgb,alpha));Image.fromarray(arr,'RGBA').save(FRAMES/f'death_{i:04d}.png',optimize=True)
        report.append((i,int(residue.sum()),repaired))
    with (OUT/'repair_report.csv').open('w',newline='',encoding='utf-8-sig') as fp:
        wr=csv.writer(fp);wr.writerow(['frame','purple_residue_removed','emblem_pixels_restored']);wr.writerows(report)
    print(OUT,'frames',len(rgba),'emblem repaired',sum(x[2]>0 for x in report))

if __name__=='__main__':main()
