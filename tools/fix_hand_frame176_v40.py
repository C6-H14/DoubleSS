from pathlib import Path
import cv2
import numpy as np
from PIL import Image,ImageDraw

ROOT=Path(r"D:\StSmod\DoubleSS");BASE=ROOT/"art_candidates/video_source/idle"
SRC=BASE/"short_blink_final_v21_solid_rgb_repair/alpha_frames";V39=BASE/"test_hand_manual_propagation_v39_dual_anchor_cyclic/alpha_frames";OUT=V39.parent
ROI=(465,385,535,435)

def comp(a):return np.asarray(Image.alpha_composite(Image.new("RGBA",(a.shape[1],a.shape[0]),(45,45,45,255)),Image.fromarray(a,"RGBA")).convert("RGB"))
def premul(a):
 x=a.astype(np.float32);al=x[:,:,3:4]/255.;return np.concatenate([x[:,:,:3]*al,x[:,:,3:4]],2)
def unpremul(p):
 a=np.clip(p[:,:,3:4],0,255);rgb=np.zeros_like(p[:,:,:3]);np.divide(p[:,:,:3]*255.,a,out=rgb,where=a>1e-4);return np.rint(np.clip(np.concatenate([rgb,a],2),0,255)).astype(np.uint8)

def main():
 src={n:np.asarray(Image.open(SRC/f"idle_{n:03d}.png").convert("RGBA"),np.uint8) for n in (175,176,177)}
 cur={n:np.asarray(Image.open(V39/f"idle_{n:03d}.png").convert("RGBA"),np.uint8) for n in (175,176,177)}
 x1,y1,x2,y2=ROI;h,w=y2-y1,x2-x1;g176=cv2.cvtColor(comp(src[176])[y1:y2,x1:x2],cv2.COLOR_RGB2GRAY);warped=[]
 gy,gx=np.mgrid[0:h,0:w].astype(np.float32)
 for n in (175,177):
  gn=cv2.cvtColor(comp(src[n])[y1:y2,x1:x2],cv2.COLOR_RGB2GRAY)
  flow=cv2.calcOpticalFlowFarneback(g176,gn,None,.5,5,17,4,7,1.4,0);mx=gx+flow[:,:,0];my=gy+flow[:,:,1];p=premul(cur[n])[y1:y2,x1:x2];wp=np.empty_like(p)
  for c in range(4):wp[:,:,c]=cv2.remap(p[:,:,c],mx,my,cv2.INTER_LINEAR,borderMode=cv2.BORDER_REFLECT)
  warped.append(wp)
 donor=(warped[0]+warped[1])*.5
 # Only the spurious grey arc below the left cuff is replaced.
 mask=np.zeros((h,w),np.uint8);poly=np.array([[(480-x1,405-y1),(507-x1,405-y1),(522-x1,412-y1),(526-x1,420-y1),(518-x1,425-y1),(488-x1,421-y1)]],np.int32);cv2.fillPoly(mask,poly,255);weight=cv2.GaussianBlur(mask.astype(np.float32)/255.,(5,5),.8)
 tp=premul(cur[176]);r=tp[y1:y2,x1:x2];r=r*(1-weight[:,:,None])+donor*weight[:,:,None];tp[y1:y2,x1:x2]=r;out=unpremul(tp)
 candidate=OUT/"idle_176_hand_fixed_candidate.png";Image.fromarray(out,"RGBA").save(candidate,optimize=True)
 # Evidence sheet.
 crop=(455,375,570,450);s=7
 def dark(a):return Image.alpha_composite(Image.new("RGBA",(a.shape[1],a.shape[0]),(45,45,45,255)),Image.fromarray(a,"RGBA")).convert("RGB").crop(crop).resize(((crop[2]-crop[0])*s,(crop[3]-crop[1])*s),Image.Resampling.NEAREST)
 b=dark(cur[176]);a=dark(out);sheet=Image.new("RGB",(b.width*2,b.height+22),(28,28,28));sheet.paste(b,(0,22));sheet.paste(a,(b.width,22));d=ImageDraw.Draw(sheet);d.text((5,4),"F176 V39 BEFORE",fill="white");d.text((b.width+5,4),"F176 LOCAL TEMPORAL FIX",fill="white");sheet.save(OUT/"frame176_hand_fix_before_after_7x.png",optimize=True)
 changed=np.any(out!=cur[176],axis=2);print({"changed_pixels":int(changed.sum()),"bbox":[int(np.where(changed)[1].min()),int(np.where(changed)[0].min()),int(np.where(changed)[1].max()+1),int(np.where(changed)[0].max()+1)],"candidate":str(candidate)})
if __name__=="__main__":main()
