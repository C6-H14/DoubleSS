from pathlib import Path
import json
import cv2
import numpy as np
from PIL import Image, ImageDraw

ROOT=Path(r"D:\StSmod\DoubleSS")
BASE=ROOT/"art_candidates/video_source/idle"
SRC=BASE/"short_blink_final_v21_solid_rgb_repair/alpha_frames/idle_073.png"
MASK=BASE/"test_hand_user_mask_v29/frame073_user_mask.png"
OUT=BASE/"test_hand_user_denoise_v31"
VIEW=(455,360,570,450)

def comp(a):
    return Image.alpha_composite(Image.new("RGBA",(a.shape[1],a.shape[0]),(45,45,45,255)),Image.fromarray(a,"RGBA"))

def make_variant(rgba,mask,kind):
    rgb=rgba[:,:,:3]
    if kind=="median3": donor=cv2.medianBlur(rgb,3)
    elif kind=="gaussian": donor=cv2.GaussianBlur(rgb,(3,3),0.65)
    elif kind=="bilateral": donor=cv2.bilateralFilter(rgb,5,24,2.2)
    else: raise ValueError(kind)
    out=rgba.copy();out[mask,:3]=donor[mask]
    return out

def main():
    OUT.mkdir(parents=True,exist_ok=True)
    rgba=np.asarray(Image.open(SRC).convert("RGBA"),np.uint8).copy();alpha=rgba[:,:,3]
    mask=(np.asarray(Image.open(MASK).convert("L"),np.uint8)>0)&(alpha>=150)
    variants={k:make_variant(rgba,mask,k) for k in ("median3","gaussian","bilateral")}
    x1,y1,x2,y2=VIEW;s=8;w=(x2-x1)*s;h=(y2-y1)*s
    cells=[("BEFORE",rgba)]+[(k.upper(),v) for k,v in variants.items()]
    sheet=Image.new("RGB",(w*2,h*2),(28,28,28));d=ImageDraw.Draw(sheet)
    reports={}
    for idx,(name,a) in enumerate(cells):
        im=comp(a).crop(VIEW).resize((w,h),Image.Resampling.NEAREST);px=(idx%2)*w;py=(idx//2)*h;sheet.paste(im,(px,py));d.text((px+7,py+7),name,fill="white")
        if idx:
            changed=np.any(a[:,:,:3]!=rgba[:,:,:3],axis=2)
            reports[name]={"changed":int(changed.sum()),"outside":int((changed&~mask).sum()),"alpha":int(np.count_nonzero(a[:,:,3]!=alpha))}
            Image.fromarray(a,"RGBA").save(OUT/f"idle_073_{name.lower()}.png",optimize=True)
    sheet.save(OUT/"frame073_three_denoise_variants_8x.png",optimize=True)
    preview=sheet.copy();preview.thumbnail((1800,1400),Image.Resampling.NEAREST);preview.save(OUT/"frame073_preview.png",optimize=True)
    (OUT/"v31_report.json").write_text(json.dumps(reports,indent=2)+"\n",encoding="utf-8");print(json.dumps(reports,indent=2))

if __name__=="__main__":main()
