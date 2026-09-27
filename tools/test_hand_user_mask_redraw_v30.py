from pathlib import Path
import json
import cv2
import numpy as np
from PIL import Image, ImageDraw

ROOT=Path(r"D:\StSmod\DoubleSS")
BASE=ROOT/"art_candidates/video_source/idle"
SRC=BASE/"short_blink_final_v21_solid_rgb_repair/alpha_frames/idle_073.png"
MASK_PATH=BASE/"test_hand_user_mask_v29/frame073_user_mask.png"
OUT=BASE/"test_hand_user_redraw_v30"
VIEW=(455,360,570,450)

def comp(a):
    return Image.alpha_composite(Image.new("RGBA",(a.shape[1],a.shape[0]),(45,45,45,255)),Image.fromarray(a,"RGBA"))

def skeletonize(src):
    img=(src>0).astype(np.uint8)*255
    sk=np.zeros_like(img);el=cv2.getStructuringElement(cv2.MORPH_CROSS,(3,3))
    while cv2.countNonZero(img):
        er=cv2.erode(img,el);op=cv2.dilate(er,el);sk=cv2.bitwise_or(sk,cv2.subtract(img,op));img=er
    return sk>0

def nearest_fill(rgb,mask):
    out=rgb.copy();ys,xs=np.where(mask)
    for y,x in zip(ys,xs):
        best=None
        for r in range(1,6):
            yy0=max(0,y-r);yy1=min(mask.shape[0],y+r+1);xx0=max(0,x-r);xx1=min(mask.shape[1],x+r+1)
            yy,xx=np.where(~mask[yy0:yy1,xx0:xx1])
            if len(xx):
                ay=yy+yy0;ax=xx+xx0;d=(ay-y)**2+(ax-x)**2;k=int(np.argmin(d));best=(ay[k],ax[k]);break
        if best is not None:out[y,x]=rgb[best]
    return out

def main():
    OUT.mkdir(parents=True,exist_ok=True)
    rgba=np.asarray(Image.open(SRC).convert("RGBA"),np.uint8).copy();alpha=rgba[:,:,3].copy()
    mask=np.asarray(Image.open(MASK_PATH).convert("L"),np.uint8)>0
    mask &= alpha>=150
    n,lab,stats,_=cv2.connectedComponentsWithStats(mask.astype(np.uint8),8)
    filled=nearest_fill(rgba[:,:,:3],mask)
    after=rgba.copy();after[mask,:3]=filled[mask]

    # Rebuild one coherent centerline per user stroke after debris removal.
    # Thin antialiasing is produced by a lightly blurred 1 px skeleton, not by
    # blurring the hand/cuff artwork itself.
    for i in range(1,n):
        cm=lab==i
        if cm.sum()<2:continue
        sk=skeletonize(cm.astype(np.uint8)) & mask
        soft=cv2.GaussianBlur((sk.astype(np.uint8)*255),(3,3),0.45).astype(np.float32)/255.0
        # Component 1 contains cuff/skin seam plus the outer hand contour.
        yy,xx=np.indices(mask.shape)
        if i==1:
            top=sk&(yy<=407)
            side=sk&(yy>407)
            layers=[(top,(132,105,108)),(side,(70,51,48))]
        else:
            layers=[(sk,(72,57,57))]
        for line,color in layers:
            w=cv2.GaussianBlur((line.astype(np.uint8)*255),(3,3),0.40).astype(np.float32)/255.0
            use=(w>0)&mask
            a=np.clip(w[use]*0.88,0,0.88)[:,None]
            after[use,:3]=np.rint(after[use,:3]*(1-a)+np.array(color)*a).astype(np.uint8)

    assert np.array_equal(after[:,:,3],alpha)
    changed=np.any(after[:,:,:3]!=rgba[:,:,:3],axis=2)
    assert not np.any(changed&~mask)
    Image.fromarray(after,"RGBA").save(OUT/"idle_073_user_mask_redraw.png",optimize=True)

    x1,y1,x2,y2=VIEW;s=10
    before=comp(rgba).crop(VIEW).resize(((x2-x1)*s,(y2-y1)*s),Image.Resampling.NEAREST)
    result=comp(after).crop(VIEW).resize(((x2-x1)*s,(y2-y1)*s),Image.Resampling.NEAREST)
    overlay=np.asarray(comp(rgba).crop(VIEW).convert("RGB")).copy();local=mask[y1:y2,x1:x2]
    overlay[local]=np.rint(.25*overlay[local]+.75*np.array([20,205,255])).astype(np.uint8)
    overlay=Image.fromarray(overlay).resize(((x2-x1)*s,(y2-y1)*s),Image.Resampling.NEAREST)
    sheet=Image.new("RGB",((x2-x1)*s*3,(y2-y1)*s),(28,28,28));sheet.paste(before,(0,0));sheet.paste(overlay,((x2-x1)*s,0));sheet.paste(result,((x2-x1)*s*2,0))
    d=ImageDraw.Draw(sheet);d.text((8,8),"BEFORE",fill="white");d.text(((x2-x1)*s+8,8),"USER MASK",fill="white");d.text(((x2-x1)*s*2+8,8),"V30 REDRAW",fill="white")
    sheet.save(OUT/"frame073_before_mask_after_10x.png",optimize=True)
    preview=sheet.copy();preview.thumbnail((2100,900),Image.Resampling.NEAREST);preview.save(OUT/"frame073_preview.png",optimize=True)
    report={"frame":73,"mask_pixels":int(mask.sum()),"rgb_pixels_changed":int(changed.sum()),"changed_outside_mask":int((changed&~mask).sum()),"alpha_pixels_changed":0}
    (OUT/"v30_report.json").write_text(json.dumps(report,indent=2)+"\n",encoding="utf-8");print(json.dumps(report,indent=2))

if __name__=="__main__":main()
