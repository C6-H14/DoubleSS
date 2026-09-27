from pathlib import Path
import json
import cv2
import numpy as np
from PIL import Image, ImageDraw

ROOT=Path(r"D:\StSmod\DoubleSS")
BASE=ROOT/"art_candidates/video_source/idle"
SRC=BASE/"short_blink_final_v21_solid_rgb_repair/alpha_frames"
ORIG=SRC/"idle_073.png"; FIX=SRC/"idlefix_073.png"
OUT=BASE/"test_hand_manual_propagation_v35_premul"; FRAMES=OUT/"alpha_frames"
ROI=(450,365,590,465); ANCHOR=73

def comp(a):return Image.alpha_composite(Image.new("RGBA",(a.shape[1],a.shape[0]),(45,45,45,255)),Image.fromarray(a,"RGBA"))
def premul(a):
    x=a.astype(np.float32);al=x[:,:,3:4]/255.;return np.concatenate([x[:,:,:3]*al,x[:,:,3:4]],axis=2)
def unpremul(p):
    a=np.clip(p[:,:,3:4],0,255);rgb=np.zeros_like(p[:,:,:3]);np.divide(p[:,:,:3]*255.,a,out=rgb,where=a>1e-4);return np.rint(np.clip(np.concatenate([rgb,a],axis=2),0,255)).astype(np.uint8)
def gray(a):
    x1,y1,x2,y2=ROI;return cv2.cvtColor(np.asarray(comp(a).convert("RGB"))[y1:y2,x1:x2],cv2.COLOR_RGB2GRAY)

def main():
    FRAMES.mkdir(parents=True,exist_ok=True)
    orig=np.asarray(Image.open(ORIG).convert("RGBA"),np.uint8);fix=np.asarray(Image.open(FIX).convert("RGBA"),np.uint8)
    x1,y1,x2,y2=ROI;rh,rw=y2-y1,x2-x1;ag=gray(orig)
    support=np.any(orig!=fix,axis=2).astype(np.uint8)
    # Close pinholes but do not expand into unrelated artwork.
    support=cv2.morphologyEx(support,cv2.MORPH_CLOSE,np.ones((3,3),np.uint8)).astype(np.float32)
    donor=premul(fix)[y1:y2,x1:x2]
    files=sorted(SRC.glob("idle_[0-9][0-9][0-9].png"));metrics=[];samples=[];wanted={1,20,40,60,73,90,120,150,180}
    for f in files:
        n=int(f.stem[-3:]);t=np.asarray(Image.open(f).convert("RGBA"),np.uint8)
        if n==ANCHOR:out=fix.copy();conf=np.ones((rh,rw),np.float32);flow=np.zeros((rh,rw,2),np.float32)
        else:
            tg=gray(t);flow=cv2.calcOpticalFlowFarneback(tg,ag,None,.5,5,21,4,7,1.5,0);back=cv2.calcOpticalFlowFarneback(ag,tg,None,.5,5,21,4,7,1.5,0)
            gy,gx=np.mgrid[0:rh,0:rw].astype(np.float32);mx=gx+flow[:,:,0];my=gy+flow[:,:,1]
            bx=cv2.remap(back[:,:,0],mx,my,cv2.INTER_LINEAR,borderMode=cv2.BORDER_CONSTANT,borderValue=99);by=cv2.remap(back[:,:,1],mx,my,cv2.INTER_LINEAR,borderMode=cv2.BORDER_CONSTANT,borderValue=99)
            conf=np.clip(1-np.sqrt((flow[:,:,0]+bx)**2+(flow[:,:,1]+by)**2)/1.5,0,1)
            wd=np.empty_like(donor)
            for c in range(4):wd[:,:,c]=cv2.remap(donor[:,:,c],mx,my,cv2.INTER_LINEAR,borderMode=cv2.BORDER_CONSTANT,borderValue=0)
            ws=cv2.remap(support[y1:y2,x1:x2],mx,my,cv2.INTER_LINEAR,borderMode=cv2.BORDER_CONSTANT,borderValue=0)
            # Sharper support avoids ghost seams; confidence prevents drift.
            weight=np.clip((ws-.20)/.65,0,1)*conf
            tp=premul(t);r=tp[y1:y2,x1:x2];r=r*(1-weight[:,:,None])+wd*weight[:,:,None];tp[y1:y2,x1:x2]=r;out=unpremul(tp)
        Image.fromarray(out,"RGBA").save(FRAMES/f"idle_{n:03d}.png",optimize=True)
        ch=np.any(out!=t,axis=2);metrics.append({"frame":n,"changed":int(ch.sum()),"confidence":round(float(conf.mean()),4)})
        if n in wanted:
            crop=(455,375,570,450);s=5;b=comp(t).crop(crop).resize(((crop[2]-crop[0])*s,(crop[3]-crop[1])*s),Image.Resampling.NEAREST);a=comp(out).crop(crop).resize(b.size,Image.Resampling.NEAREST)
            row=Image.new("RGB",(b.width*2,b.height+20),(28,28,28));row.paste(b,(0,20));row.paste(a,(b.width,20));d=ImageDraw.Draw(row);d.text((4,3),f"F{n:03d} BEFORE",fill="white");d.text((b.width+4,3),"PREMUL PROPAGATED",fill="white");samples.append(row)
    sw=max(r.width for r in samples);sheet=Image.new("RGB",(sw,sum(r.height for r in samples)),(28,28,28));yy=0
    for r in samples:sheet.paste(r,(0,yy));yy+=r.height
    sheet.save(OUT/"sample_before_after_5x.png",optimize=True)
    report={"frames":len(files),"roi":ROI,"manual_support":int(support.sum()),"metrics":metrics};(OUT/"v35_report.json").write_text(json.dumps(report,indent=2)+"\n",encoding="utf-8")
    print(json.dumps({"frames":len(files),"changed_min":min(m['changed'] for m in metrics),"changed_max":max(m['changed'] for m in metrics),"confidence_min":min(m['confidence'] for m in metrics)},indent=2))

if __name__=="__main__":main()
