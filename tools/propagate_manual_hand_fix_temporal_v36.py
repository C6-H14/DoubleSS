from pathlib import Path
import json
import cv2
import numpy as np
from PIL import Image, ImageDraw

ROOT=Path(r"D:\StSmod\DoubleSS");BASE=ROOT/"art_candidates/video_source/idle"
SRC=BASE/"short_blink_final_v21_solid_rgb_repair/alpha_frames";ORIG=SRC/"idle_073.png";FIX=SRC/"idlefix_073.png"
OUT=BASE/"test_hand_manual_propagation_v36_temporal";FRAMES=OUT/"alpha_frames";ROI=(450,365,590,465);ANCHOR=73

def comp(a):return Image.alpha_composite(Image.new("RGBA",(a.shape[1],a.shape[0]),(45,45,45,255)),Image.fromarray(a,"RGBA"))
def premul(a):
    x=a.astype(np.float32);al=x[:,:,3:4]/255.;return np.concatenate([x[:,:,:3]*al,x[:,:,3:4]],2)
def unpremul(p):
    a=np.clip(p[:,:,3:4],0,255);rgb=np.zeros_like(p[:,:,:3]);np.divide(p[:,:,:3]*255.,a,out=rgb,where=a>1e-4);return np.rint(np.clip(np.concatenate([rgb,a],2),0,255)).astype(np.uint8)
def gray(a):
    x1,y1,x2,y2=ROI;return cv2.cvtColor(np.asarray(comp(a).convert("RGB"))[y1:y2,x1:x2],cv2.COLOR_RGB2GRAY)

def main():
    FRAMES.mkdir(parents=True,exist_ok=True);orig=np.asarray(Image.open(ORIG).convert("RGBA"),np.uint8);fix=np.asarray(Image.open(FIX).convert("RGBA"),np.uint8)
    x1,y1,x2,y2=ROI;rh,rw=y2-y1,x2-x1;ag=gray(orig);files=sorted(SRC.glob("idle_[0-9][0-9][0-9].png"));targets=[];flows=[];confs=[]
    for f in files:
        t=np.asarray(Image.open(f).convert("RGBA"),np.uint8);targets.append(t);tg=gray(t)
        fl=cv2.calcOpticalFlowFarneback(tg,ag,None,.5,5,21,4,7,1.5,0);bk=cv2.calcOpticalFlowFarneback(ag,tg,None,.5,5,21,4,7,1.5,0)
        gy,gx=np.mgrid[0:rh,0:rw].astype(np.float32);mx=gx+fl[:,:,0];my=gy+fl[:,:,1];bx=cv2.remap(bk[:,:,0],mx,my,cv2.INTER_LINEAR,borderMode=cv2.BORDER_CONSTANT,borderValue=99);by=cv2.remap(bk[:,:,1],mx,my,cv2.INTER_LINEAR,borderMode=cv2.BORDER_CONSTANT,borderValue=99)
        flows.append(fl);confs.append(np.clip(1-np.sqrt((fl[:,:,0]+bx)**2+(fl[:,:,1]+by)**2)/1.5,0,1))
    flows=np.stack(flows);confs=np.stack(confs)
    # Smooth only through time; spatial flow detail is retained.
    sf=cv2.GaussianBlur(flows.reshape(len(files),-1),(1,9),0,2.0,borderType=cv2.BORDER_REFLECT).reshape(flows.shape)
    sc=cv2.GaussianBlur(confs.reshape(len(files),-1),(1,9),0,2.0,borderType=cv2.BORDER_REFLECT).reshape(confs.shape)
    support=np.any(orig!=fix,axis=2).astype(np.float32);donor=premul(fix)[y1:y2,x1:x2];metrics=[];samples=[];wanted={1,20,28,29,40,60,73,90,120,150,180}
    for idx,(f,t) in enumerate(zip(files,targets)):
        n=int(f.stem[-3:])
        if n==ANCHOR:out=fix.copy();weight=np.ones((rh,rw),np.float32)
        else:
            fl=sf[idx];gy,gx=np.mgrid[0:rh,0:rw].astype(np.float32);mx=gx+fl[:,:,0];my=gy+fl[:,:,1];wd=np.empty_like(donor)
            for c in range(4):wd[:,:,c]=cv2.remap(donor[:,:,c],mx,my,cv2.INTER_LINEAR,borderMode=cv2.BORDER_CONSTANT,borderValue=0)
            ws=cv2.remap(support[y1:y2,x1:x2],mx,my,cv2.INTER_LINEAR,borderMode=cv2.BORDER_CONSTANT,borderValue=0)
            weight=np.clip((ws-.12)/.76,0,1)*np.clip((sc[idx]-.12)/.36,0,1)
            tp=premul(t);r=tp[y1:y2,x1:x2];tp[y1:y2,x1:x2]=r*(1-weight[:,:,None])+wd*weight[:,:,None];out=unpremul(tp)
        Image.fromarray(out,"RGBA").save(FRAMES/f"idle_{n:03d}.png",optimize=True);ch=np.any(out!=t,axis=2);metrics.append({"frame":n,"changed":int(ch.sum()),"weight_sum":round(float(weight.sum()),2)})
        if n in wanted:
            crop=(455,375,570,450);s=5;b=comp(t).crop(crop).resize(((crop[2]-crop[0])*s,(crop[3]-crop[1])*s),Image.Resampling.NEAREST);a=comp(out).crop(crop).resize(b.size,Image.Resampling.NEAREST);row=Image.new("RGB",(b.width*2,b.height+20),(28,28,28));row.paste(b,(0,20));row.paste(a,(b.width,20));d=ImageDraw.Draw(row);d.text((4,3),f"F{n:03d} BEFORE",fill="white");d.text((b.width+4,3),"TEMPORAL PREMUL",fill="white");samples.append(row)
    sw=max(r.width for r in samples);sheet=Image.new("RGB",(sw,sum(r.height for r in samples)),(28,28,28));yy=0
    for r in samples:sheet.paste(r,(0,yy));yy+=r.height
    sheet.save(OUT/"sample_before_after_5x.png",optimize=True)
    c=np.array([m['changed'] for m in metrics]);j=np.abs(np.diff(c));report={"frames":len(files),"roi":ROI,"metrics":metrics,"largest_changed_jump":int(j.max()),"largest_jump_between":[int(j.argmax()+1),int(j.argmax()+2)],"loop_changed_jump":int(abs(c[-1]-c[0]))};(OUT/"v36_report.json").write_text(json.dumps(report,indent=2)+"\n",encoding="utf-8");print(json.dumps({k:report[k] for k in ('frames','largest_changed_jump','largest_jump_between','loop_changed_jump')},indent=2))

if __name__=="__main__":main()
