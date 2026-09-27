from pathlib import Path
import json
import cv2
import numpy as np
from PIL import Image, ImageDraw

ROOT=Path(r"D:\StSmod\DoubleSS");BASE=ROOT/"art_candidates/video_source/idle"
SRC=BASE/"short_blink_final_v21_solid_rgb_repair/alpha_frames";OUT=BASE/"test_hand_manual_propagation_v39_dual_anchor_cyclic";FRAMES=OUT/"alpha_frames"
ROI=(450,365,590,465);ANCHORS=(36,73)

def comp(a):return Image.alpha_composite(Image.new("RGBA",(a.shape[1],a.shape[0]),(45,45,45,255)),Image.fromarray(a,"RGBA"))
def premul(a):
    x=a.astype(np.float32);al=x[:,:,3:4]/255.;return np.concatenate([x[:,:,:3]*al,x[:,:,3:4]],2)
def unpremul(p):
    a=np.clip(p[:,:,3:4],0,255);rgb=np.zeros_like(p[:,:,:3]);np.divide(p[:,:,:3]*255.,a,out=rgb,where=a>1e-4);return np.rint(np.clip(np.concatenate([rgb,a],2),0,255)).astype(np.uint8)
def gray(a):
    x1,y1,x2,y2=ROI;return cv2.cvtColor(np.asarray(comp(a).convert("RGB"))[y1:y2,x1:x2],cv2.COLOR_RGB2GRAY)

def anchor_data(n):
    orig=np.asarray(Image.open(SRC/f"idle_{n:03d}.png").convert("RGBA"),np.uint8);fix=np.asarray(Image.open(SRC/f"idlefix_{n:03d}.png").convert("RGBA"),np.uint8)
    x1,y1,x2,y2=ROI;support=np.any(orig!=fix,axis=2).astype(np.float32)
    return {"n":n,"orig":orig,"fix":fix,"gray":gray(orig),"donor":premul(fix)[y1:y2,x1:x2],"support":support[y1:y2,x1:x2]}

def propagated(a,target,tgray):
    x1,y1,x2,y2=ROI;rh,rw=y2-y1,x2-x1
    flow=cv2.calcOpticalFlowFarneback(tgray,a["gray"],None,.5,5,21,4,7,1.5,0);back=cv2.calcOpticalFlowFarneback(a["gray"],tgray,None,.5,5,21,4,7,1.5,0)
    gy,gx=np.mgrid[0:rh,0:rw].astype(np.float32);mx=gx+flow[:,:,0];my=gy+flow[:,:,1]
    bx=cv2.remap(back[:,:,0],mx,my,cv2.INTER_LINEAR,borderMode=cv2.BORDER_CONSTANT,borderValue=99);by=cv2.remap(back[:,:,1],mx,my,cv2.INTER_LINEAR,borderMode=cv2.BORDER_CONSTANT,borderValue=99)
    conf=np.clip(1-np.sqrt((flow[:,:,0]+bx)**2+(flow[:,:,1]+by)**2)/1.5,0,1)
    donor=np.empty_like(a["donor"])
    for c in range(4):donor[:,:,c]=cv2.remap(a["donor"][:,:,c],mx,my,cv2.INTER_LINEAR,borderMode=cv2.BORDER_CONSTANT,borderValue=0)
    support=cv2.remap(a["support"],mx,my,cv2.INTER_LINEAR,borderMode=cv2.BORDER_CONSTANT,borderValue=0)
    weight=np.clip((support-.20)/.65,0,1)*conf
    return donor,weight,conf

def main():
    FRAMES.mkdir(parents=True,exist_ok=True);ads=[anchor_data(n) for n in ANCHORS];files=sorted(SRC.glob("idle_[0-9][0-9][0-9].png"));x1,y1,x2,y2=ROI;metrics=[];samples=[];wanted={1,20,28,32,36,40,44,54,60,73,90,120,150,180}
    for f in files:
        n=int(f.stem[-3:]);target=np.asarray(Image.open(f).convert("RGBA"),np.uint8);tgray=gray(target)
        # Anchor frames use the same blend as their neighbours, preventing a
        # one-frame discontinuity at frame 36 or 73.
        circular=lambda a:min(abs(n-a),180-abs(n-a))
        td=np.array([np.exp(-circular(a["n"])/18.0) for a in ads],np.float32);td/=td.sum();anchor_mix=td.tolist()
        props=[propagated(a,target,tgray) for a in ads]
        us=[props[i][1]*td[i] for i in range(len(ads))];sumu=np.sum(us,axis=0);total=np.clip(sumu,0,1)
        mix=np.zeros_like(props[0][0]);den=np.maximum(sumu,1e-6)
        for u,(donor,_,_) in zip(us,props):mix+=donor*(u/den)[:,:,None]
        tp=premul(target);r=tp[y1:y2,x1:x2];tp[y1:y2,x1:x2]=r*(1-total[:,:,None])+mix*total[:,:,None];out=unpremul(tp)
        Image.fromarray(out,"RGBA").save(FRAMES/f"idle_{n:03d}.png",optimize=True);ch=np.any(out!=target,axis=2);metrics.append({"frame":n,"changed":int(ch.sum()),"anchor_mix":[round(float(v),4) for v in anchor_mix]})
        if n in wanted:
            crop=(455,375,570,450);s=5;b=comp(target).crop(crop).resize(((crop[2]-crop[0])*s,(crop[3]-crop[1])*s),Image.Resampling.NEAREST);aa=comp(out).crop(crop).resize(b.size,Image.Resampling.NEAREST);row=Image.new("RGB",(b.width*2,b.height+20),(28,28,28));row.paste(b,(0,20));row.paste(aa,(b.width,20));d=ImageDraw.Draw(row);d.text((4,3),f"F{n:03d} BEFORE",fill="white");d.text((b.width+4,3),f"DUAL {anchor_mix[0]:.2f}/{anchor_mix[1]:.2f}",fill="white");samples.append(row)
    sw=max(r.width for r in samples);sheet=Image.new("RGB",(sw,sum(r.height for r in samples)),(28,28,28));yy=0
    for r in samples:sheet.paste(r,(0,yy));yy+=r.height
    sheet.save(OUT/"sample_before_after_5x.png",optimize=True)
    c=np.array([m['changed'] for m in metrics]);j=np.abs(np.diff(c));report={"anchors":ANCHORS,"frames":len(files),"metrics":metrics,"largest_changed_jump":int(j.max()),"largest_jump_between":[int(j.argmax()+1),int(j.argmax()+2)],"loop_changed_jump":int(abs(c[-1]-c[0]))};(OUT/"v39_report.json").write_text(json.dumps(report,indent=2)+"\n",encoding="utf-8")
    print(json.dumps({k:report[k] for k in ('anchors','frames','largest_changed_jump','largest_jump_between','loop_changed_jump')},indent=2))

if __name__=="__main__":main()
