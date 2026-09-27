from pathlib import Path
import cv2
import numpy as np
from PIL import Image,ImageDraw

ROOT=Path(r"D:\StSmod\DoubleSS");BASE=ROOT/"art_candidates/video_source/idle";DIR=BASE/"test_hand_manual_propagation_v39_dual_anchor_cyclic";TARGET=DIR/"alpha_frames/idle_176.png"

def dark(a):return Image.alpha_composite(Image.new("RGBA",(a.shape[1],a.shape[0]),(45,45,45,255)),Image.fromarray(a,"RGBA")).convert("RGB")

def main():
 a=np.asarray(Image.open(TARGET).convert("RGBA"),np.uint8).copy();before=a.copy();H,W=a.shape[:2]
 yy,xx=np.indices((H,W));sample_box=(xx>=478)&(xx<530)&(yy>=404)&(yy<438)&(a[:,:,3]>=240)
 rgb=a[:,:,:3];skin=sample_box&(rgb[:,:,0]>=205)&(rgb[:,:,1]>=160)&(rgb[:,:,1]<=218)&(rgb[:,:,2]>=145)&(rgb[:,:,2]<=205)&((rgb[:,:,0].astype(int)-rgb[:,:,1])>=20)
 # Exclude the contaminated upper band from plane fitting.
 skin&=~((yy<=421)&(yy>=406))
 X=np.column_stack([np.ones(int(skin.sum())),xx[skin]-500,yy[skin]-420]);coef=[]
 for c in range(3):coef.append(np.linalg.lstsq(X,rgb[:,:,c][skin].astype(float),rcond=None)[0])
 coef=np.asarray(coef)
 # Hand-drawn narrow polygon follows only the remaining grey crescent.
 mask=np.zeros((H,W),np.uint8);poly=np.array([[(479,408),(493,406),(508,408),(521,414),(521,419),(514,422),(496,419),(482,416)]],np.int32);cv2.fillPoly(mask,poly,255)
 mask=(mask>0)&(a[:,:,3]>=200)
 pred=np.stack([coef[c,0]+coef[c,1]*(xx-500)+coef[c,2]*(yy-420) for c in range(3)],2);pred=np.clip(pred,0,255)
 # One-pixel antialiasing at the polygon boundary; interior is fully rebuilt.
 soft=cv2.GaussianBlur((mask.astype(np.uint8)*255),(3,3),.55).astype(float)/255.;use=soft>0;w=soft[use,None]
 a[use,:3]=np.rint(a[use,:3]*(1-w)+pred[use]*w).astype(np.uint8)
 candidate=DIR/"idle_176_hand_fixed_v41.png";Image.fromarray(a,"RGBA").save(candidate,optimize=True)
 crop=(465,390,535,435);s=10;b=dark(before).crop(crop).resize(((crop[2]-crop[0])*s,(crop[3]-crop[1])*s),Image.Resampling.NEAREST);r=dark(a).crop(crop).resize(b.size,Image.Resampling.NEAREST)
 sheet=Image.new("RGB",(b.width*2,b.height+22),(28,28,28));sheet.paste(b,(0,22));sheet.paste(r,(b.width,22));d=ImageDraw.Draw(sheet);d.text((5,4),"BEFORE: GREY RESIDUE",fill="white");d.text((b.width+5,4),"V41 SKIN-GRADIENT REBUILD",fill="white");sheet.save(DIR/"frame176_gray_residue_before_after_10x.png",optimize=True)
 changed=np.any(a!=before,axis=2);print({"skin_samples":int(skin.sum()),"changed_pixels":int(changed.sum()),"candidate":str(candidate)})
if __name__=="__main__":main()
