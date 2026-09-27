from pathlib import Path
import csv,shutil
import cv2,numpy as np
from PIL import Image,ImageDraw,ImageFont
from scipy.ndimage import distance_transform_edt

ROOT=Path(r'D:\StSmod\DoubleSS\art_candidates\video_source\death_splice_analysis')
SRC=ROOT/'death_splice_v11_alpha_refined_v2'/'alpha_frames'
OUT=ROOT/'death_splice_v11_alpha_refined_v5_drawn_emblem';FRAMES=OUT/'alpha_frames'
FONT=ImageFont.truetype(r'C:\Windows\Fonts\georgiai.ttf',26)

def gold(rgb):
    h,s,v=cv2.split(cv2.cvtColor(rgb,cv2.COLOR_RGB2HSV));return (h>=14)&(h<=34)&(s>=105)&(v>=80)
def comps(rgb):
    m=gold(rgb).astype(np.uint8);n,l,st,c=cv2.connectedComponentsWithStats(m,8);out=[]
    for k in range(1,n):
        x,y,w,h,a=st[k]
        if 200<=a<=2500 and 20<=w<=90 and 18<=h<=90:out.append((float(c[k,0]),float(c[k,1]),int(a)))
    return out

def main():
    if OUT.exists():shutil.rmtree(OUT)
    FRAMES.mkdir(parents=True)
    arr=[np.asarray(Image.open(p).convert('RGBA'),np.uint8).copy() for p in sorted(SRC.glob('*.png'))]
    # Clean standalone emblem, approximately matching the original serif S-S.
    em=Image.new('RGBA',(90,42),(0,0,0,0));d=ImageDraw.Draw(em)
    d.text((3,2),'S-S',font=FONT,fill=(214,151,43,255),stroke_width=1,stroke_fill=(112,72,22,255))
    ea=np.asarray(em,np.uint8);emask=ea[...,3]>0;ecol=ea[...,:3]
    prev=np.array([597.,292.]);bg=np.array([132,0,213],np.float32);report=[]
    for i,a in enumerate(arr):
        rgb=a[...,:3].copy();alpha=a[...,3].copy()
        residue=(np.linalg.norm(rgb.astype(np.float32)-bg,axis=2)<=18)&(alpha>0);alpha[residue]=0;rgb[residue]=0
        if i==22:
            hh,ss,vv=cv2.split(cv2.cvtColor(rgb,cv2.COLOR_RGB2HSV))
            local=np.zeros(alpha.shape,bool);local[295:345,610:670]=1
            purple22=local&(hh>=125)&(hh<=172)&(ss>=45)&(alpha>0)
            alpha[purple22]=0;rgb[purple22]=0;residue|=purple22
        restored=0
        if i>=23:
            cs=[q for q in comps(rgb) if 520<q[0]<720 and 230<q[1]<430 and np.linalg.norm(np.array(q[:2])-prev)<70]
            if cs:
                q=min(cs,key=lambda z:np.linalg.norm(np.array(z[:2])-prev));prev=np.array(q[:2])
                # Original emblem sits 47 px left and 6 px below bow center.
                x0=int(round(prev[0]-47));y0=int(round(prev[1]+6));x1=x0+em.width;y1=y0+em.height
                if 0<=x0 and x1<=rgb.shape[1] and 0<=y0 and y1<=rgb.shape[0]:
                    box=np.zeros(alpha.shape,bool);box[y0:y1,x0:x1]=1
                    old=gold(rgb)&box
                    h,s,v=cv2.split(cv2.cvtColor(rgb,cv2.COLOR_RGB2HSV));shirt=(h>=70)&(h<=112)&(s>=25)&(v<=180)&(~old)&(alpha>0)
                    if shirt.any():
                        _,ni=distance_transform_edt(~shirt,return_indices=True);rgb[old]=rgb[ni[0],ni[1]][old]
                    h,s,v=cv2.split(cv2.cvtColor(rgb,cv2.COLOR_RGB2HSV));shirt=(h>=70)&(h<=112)&(s>=22)&(v<=190)
                    local_allowed=emask&shirt[y0:y1,x0:x1]&(alpha[y0:y1,x0:x1]>0)
                    patch=rgb[y0:y1,x0:x1];patch[local_allowed]=ecol[local_allowed];rgb[y0:y1,x0:x1]=patch
                    restored=int(local_allowed.sum())
        Image.fromarray(np.dstack((rgb,alpha)),'RGBA').save(FRAMES/f'death_{i:04d}.png',optimize=True)
        report.append((i,int(residue.sum()),restored,float(prev[0]),float(prev[1])))
    with (OUT/'repair_report.csv').open('w',newline='',encoding='utf-8-sig') as fp:
        wr=csv.writer(fp);wr.writerow(['frame','purple_removed','emblem_pixels','bow_x','bow_y']);wr.writerows(report)
    print(OUT,'restored',sum(x[2]>0 for x in report))
if __name__=='__main__':main()
