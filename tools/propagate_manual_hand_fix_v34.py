from pathlib import Path
import json
import cv2
import numpy as np
from PIL import Image, ImageDraw

ROOT=Path(r"D:\StSmod\DoubleSS")
BASE=ROOT/"art_candidates/video_source/idle"
SRC_DIR=BASE/"short_blink_final_v21_solid_rgb_repair/alpha_frames"
ANCHOR_ORIG=SRC_DIR/"idle_073.png"
ANCHOR_FIX=SRC_DIR/"idlefix_073.png"
OUT=BASE/"test_hand_manual_propagation_v34"
FRAME_OUT=OUT/"alpha_frames"
ANCHOR_N=73
ROI=(450,365,590,465)  # contains both hands, cuffs, pommel and guard

def dark_comp(a):
    return Image.alpha_composite(Image.new("RGBA",(a.shape[1],a.shape[0]),(45,45,45,255)),Image.fromarray(a,"RGBA"))

def gray_roi(a):
    rgb=np.asarray(dark_comp(a).convert("RGB"),np.uint8)
    x1,y1,x2,y2=ROI
    return cv2.cvtColor(rgb[y1:y2,x1:x2],cv2.COLOR_RGB2GRAY)

def main():
    FRAME_OUT.mkdir(parents=True,exist_ok=True)
    orig=np.asarray(Image.open(ANCHOR_ORIG).convert("RGBA"),np.uint8)
    fixed=np.asarray(Image.open(ANCHOR_FIX).convert("RGBA"),np.uint8)
    H,W=orig.shape[:2];x1,y1,x2,y2=ROI;rh,rw=y2-y1,x2-x1
    delta=fixed.astype(np.float32)-orig.astype(np.float32)
    support=np.any(orig!=fixed,axis=2).astype(np.uint8)
    # Preserve the exact manual support, with a one-pixel feather only for warped sampling.
    support=cv2.dilate(support,np.ones((3,3),np.uint8),iterations=1).astype(np.float32)
    anchor_gray=gray_roi(orig)
    files=sorted(SRC_DIR.glob("idle_[0-9][0-9][0-9].png"))
    metrics=[];sample_frames={1,20,40,60,73,90,120,150,180};sample_rows=[]

    for path in files:
        n=int(path.stem.split("_")[-1]);target=np.asarray(Image.open(path).convert("RGBA"),np.uint8)
        if n==ANCHOR_N:
            out=fixed.copy();flow=np.zeros((rh,rw,2),np.float32);conf=np.ones((rh,rw),np.float32)
        else:
            target_gray=gray_roi(target)
            # target -> anchor mapping, followed by forward/backward consistency.
            flow=cv2.calcOpticalFlowFarneback(target_gray,anchor_gray,None,.5,5,21,4,7,1.5,0)
            back=cv2.calcOpticalFlowFarneback(anchor_gray,target_gray,None,.5,5,21,4,7,1.5,0)
            gy,gx=np.mgrid[0:rh,0:rw].astype(np.float32);mx=gx+flow[:,:,0];my=gy+flow[:,:,1]
            bx=cv2.remap(back[:,:,0],mx,my,cv2.INTER_LINEAR,borderMode=cv2.BORDER_CONSTANT,borderValue=99)
            by=cv2.remap(back[:,:,1],mx,my,cv2.INTER_LINEAR,borderMode=cv2.BORDER_CONSTANT,borderValue=99)
            fb=np.sqrt((flow[:,:,0]+bx)**2+(flow[:,:,1]+by)**2)
            conf=np.clip(1.0-fb/2.0,0,1)
            # Sample the manual correction layer at corresponding anchor coordinates.
            warped_delta=np.zeros((rh,rw,4),np.float32)
            for c in range(4):
                warped_delta[:,:,c]=cv2.remap(delta[y1:y2,x1:x2,c],mx,my,cv2.INTER_LINEAR,borderMode=cv2.BORDER_CONSTANT,borderValue=0)
            warped_support=cv2.remap(support[y1:y2,x1:x2],mx,my,cv2.INTER_LINEAR,borderMode=cv2.BORDER_CONSTANT,borderValue=0)
            weight=np.clip(warped_support*conf,0,1)
            out=target.copy();roi=out[y1:y2,x1:x2].astype(np.float32)
            roi=np.clip(roi+warped_delta*weight[:,:,None],0,255)
            out[y1:y2,x1:x2]=np.rint(roi).astype(np.uint8)
        Image.fromarray(out,"RGBA").save(FRAME_OUT/f"idle_{n:03d}.png",optimize=True)
        changed=np.any(out!=target,axis=2)
        metrics.append({"frame":n,"changed_pixels":int(changed.sum()),"max_flow":round(float(np.linalg.norm(flow,axis=2).max()),3),"mean_confidence":round(float(conf.mean()),4)})
        if n in sample_frames:
            crop=(455,375,570,450);s=5
            b=dark_comp(target).crop(crop).resize(((crop[2]-crop[0])*s,(crop[3]-crop[1])*s),Image.Resampling.NEAREST)
            a=dark_comp(out).crop(crop).resize(b.size,Image.Resampling.NEAREST)
            row=Image.new("RGB",(b.width*2,b.height+20),(28,28,28));row.paste(b,(0,20));row.paste(a,(b.width,20));d=ImageDraw.Draw(row);d.text((4,3),f"F{n:03d} BEFORE",fill="white");d.text((b.width+4,3),f"F{n:03d} PROPAGATED",fill="white");sample_rows.append(row)

    # Contact sheet.
    if sample_rows:
        sw=max(r.width for r in sample_rows);sh=sum(r.height for r in sample_rows);sheet=Image.new("RGB",(sw,sh),(28,28,28));yy=0
        for r in sample_rows:sheet.paste(r,(0,yy));yy+=r.height
        sheet.save(OUT/"sample_before_after_5x.png",optimize=True)

    # 60 fps dynamic hand crop preview, before and after side by side.
    gif=[];crop=(445,360,585,465);scale=3
    for path in files:
        n=int(path.stem.split("_")[-1]);before=np.asarray(Image.open(path).convert("RGBA"),np.uint8);after=np.asarray(Image.open(FRAME_OUT/f"idle_{n:03d}.png").convert("RGBA"),np.uint8)
        b=dark_comp(before).crop(crop).resize(((crop[2]-crop[0])*scale,(crop[3]-crop[1])*scale),Image.Resampling.NEAREST)
        a=dark_comp(after).crop(crop).resize(b.size,Image.Resampling.NEAREST)
        frame=Image.new("RGB",(b.width*2,b.height+18),(28,28,28));frame.paste(b,(0,18));frame.paste(a,(b.width,18));d=ImageDraw.Draw(frame);d.text((4,2),f"BEFORE F{n:03d}",fill="white");d.text((b.width+4,2),"PROPAGATED",fill="white");gif.append(frame)
    gif[0].save(OUT/"hand_before_after_3s_60fps.gif",save_all=True,append_images=gif[1:],duration=17,loop=0,optimize=False)

    report={"anchor":str(ANCHOR_FIX),"source_frames":len(files),"roi":ROI,"manual_changed_pixels":int(np.any(orig!=fixed,axis=2).sum()),"metrics":metrics}
    (OUT/"v34_report.json").write_text(json.dumps(report,indent=2)+"\n",encoding="utf-8")
    vals=[m["changed_pixels"] for m in metrics];confs=[m["mean_confidence"] for m in metrics]
    print(json.dumps({"frames":len(files),"changed_pixels_min":min(vals),"changed_pixels_max":max(vals),"confidence_min":min(confs),"confidence_mean":round(sum(confs)/len(confs),4)},indent=2))

if __name__=="__main__":main()
