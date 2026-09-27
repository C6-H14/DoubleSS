from pathlib import Path
import json
import cv2
import numpy as np
from PIL import Image, ImageDraw

ROOT=Path(r"D:\StSmod\DoubleSS")
BASE=ROOT/"art_candidates/video_source/idle"
SRC=BASE/"short_blink_final_v21_solid_rgb_repair/alpha_frames/idle_073.png"
MASK=BASE/"test_hand_user_mask_v29/frame073_user_mask.png"
OUT=BASE/"test_hand_vector_boundaries_v32"
VIEW=(455,360,570,450); SCALE=8

def comp(a):
    return Image.alpha_composite(Image.new("RGBA",(a.shape[1],a.shape[0]),(45,45,45,255)),Image.fromarray(a,"RGBA"))

def catmull(points,steps=18):
    p=np.asarray(points,np.float32); out=[]
    q=np.vstack([p[0],p,p[-1]])
    for i in range(1,len(q)-2):
        p0,p1,p2,p3=q[i-1:i+3]
        for t in np.linspace(0,1,steps,endpoint=False):
            t2=t*t;t3=t2*t
            out.append(.5*((2*p1)+(-p0+p2)*t+(2*p0-5*p1+4*p2-p3)*t2+(-p0+3*p1-3*p2+p3)*t3))
    out.append(p[-1]);return np.asarray(out)

def paint_boundary(canvas,allowed,points,neg_color,pos_color,ink_strength):
    curve=catmull(points)*SCALE
    h,w=allowed.shape; yy,xx=np.indices((h,w),dtype=np.float32)
    best=np.full((h,w),1e9,np.float32); sign=np.zeros((h,w),np.float32)
    for a,b in zip(curve[:-1],curve[1:]):
        vx,vy=b-a; den=vx*vx+vy*vy+1e-6
        t=np.clip(((xx-a[0])*vx+(yy-a[1])*vy)/den,0,1)
        px=a[0]+t*vx;py=a[1]+t*vy;dx=xx-px;dy=yy-py;d2=dx*dx+dy*dy
        take=d2<best;best[take]=d2[take];sign[take]=(vx*dy-vy*dx)[take]
    near=allowed & (best <= (3.2*SCALE)**2)
    canvas[near&(sign<0)]=neg_color;canvas[near&(sign>=0)]=pos_color
    if ink_strength>0:
        dist=np.sqrt(best)/SCALE
        width=.42+.38*ink_strength
        aa=np.clip((width+.55-dist)/.75,0,1)*ink_strength
        use=allowed&(aa>0)
        ink=np.array([72,55,54],np.float32)
        a=aa[use,None]
        canvas[use]=np.rint(canvas[use]*(1-a)+ink*a).astype(np.uint8)

def render(rgba,mask,strength):
    x1,y1,x2,y2=VIEW
    crop=rgba[y1:y2,x1:x2,:3]
    hi=np.asarray(Image.fromarray(crop).resize(((x2-x1)*SCALE,(y2-y1)*SCALE),Image.Resampling.BICUBIC),np.uint8).copy()
    allowed=cv2.resize(mask[y1:y2,x1:x2].astype(np.uint8),None,fx=SCALE,fy=SCALE,interpolation=cv2.INTER_NEAREST)>0
    def local(ps):return [(x-x1,y-y1) for x,y in ps]
    # Left cuff/skin seam; above is cuff, below is skin.
    paint_boundary(hi,allowed,local([(489,401),(497,402),(504,403),(509,406)]),(220,217,220),(232,196,180),strength)
    # Left hand outer contour; left is skin, right is dark clothing/background.
    paint_boundary(hi,allowed,local([(509,405),(511,411),(513,417),(515,423)]),(232,196,180),(42,57,61),strength)
    # Right cuff/skin seam.
    paint_boundary(hi,allowed,local([(521,429),(528,424),(536,422),(542,425),(547,431)]),(230,224,232),(237,199,185),strength)
    # Right outer edge approaching the guard; northwest side is hand/cuff, southeast side is dark/gold transition.
    paint_boundary(hi,allowed,local([(544,440),(548,437),(552,433),(555,429),(558,426)]),(229,205,199),(82,68,61),strength)
    low=np.asarray(Image.fromarray(hi).resize((x2-x1,y2-y1),Image.Resampling.LANCZOS),np.uint8)
    out=rgba.copy();local_mask=mask[y1:y2,x1:x2];out[y1:y2,x1:x2,:3][local_mask]=low[local_mask]
    return out

def main():
    OUT.mkdir(parents=True,exist_ok=True)
    rgba=np.asarray(Image.open(SRC).convert("RGBA"),np.uint8).copy();alpha=rgba[:,:,3]
    mask=(np.asarray(Image.open(MASK).convert("L"),np.uint8)>0)&(alpha>=150)
    variants={"no_ink":render(rgba,mask,0.0),"soft_ink":render(rgba,mask,0.42),"medium_ink":render(rgba,mask,0.72)}
    x1,y1,x2,y2=VIEW;s=8;w=(x2-x1)*s;h=(y2-y1)*s
    sheet=Image.new("RGB",(w*2,h*2),(28,28,28));d=ImageDraw.Draw(sheet)
    cells=[("BEFORE",rgba)]+[(k.upper(),v) for k,v in variants.items()];report={}
    for idx,(name,a) in enumerate(cells):
        px=idx%2*w;py=idx//2*h;sheet.paste(comp(a).crop(VIEW).resize((w,h),Image.Resampling.NEAREST),(px,py));d.text((px+7,py+7),name,fill="white")
        if idx:
            changed=np.any(a[:,:,:3]!=rgba[:,:,:3],axis=2);report[name]={"changed":int(changed.sum()),"outside":int((changed&~mask).sum()),"alpha":int(np.count_nonzero(a[:,:,3]!=alpha))}
            Image.fromarray(a,"RGBA").save(OUT/f"idle_073_{name.lower()}.png",optimize=True)
    sheet.save(OUT/"frame073_vector_variants_8x.png",optimize=True);preview=sheet.copy();preview.thumbnail((1800,1400),Image.Resampling.NEAREST);preview.save(OUT/"frame073_preview.png",optimize=True)
    (OUT/"v32_report.json").write_text(json.dumps(report,indent=2)+"\n",encoding="utf-8");print(json.dumps(report,indent=2))

if __name__=="__main__":main()
