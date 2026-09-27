from pathlib import Path
import json
import cv2
import numpy as np
from PIL import Image,ImageDraw

ROOT=Path(r"D:\StSmod\DoubleSS");BASE=ROOT/"art_candidates/video_source/idle"
SRC=BASE/"test_hand_manual_propagation_v39_dual_anchor_cyclic/alpha_frames";RAW=BASE/"loop_v2_short_blink_180f/raw_frames"
OUT=BASE/"short_blink_final_v41_gap_repaired_pixel_evidence";FRAMES=OUT/"alpha_frames"

def comp(a):return Image.alpha_composite(Image.new("RGBA",(a.shape[1],a.shape[0]),(45,45,45,255)),Image.fromarray(a,"RGBA"))

def repair(src,raw):
    lab=cv2.cvtColor(raw,cv2.COLOR_RGB2LAB).astype(np.float32);corn=np.concatenate([lab[:80,:80].reshape(-1,3),lab[:80,-80:].reshape(-1,3),lab[-80:,:80].reshape(-1,3),lab[-80:,-80:].reshape(-1,3)])
    bg=np.median(corn,axis=0);dist=np.linalg.norm(lab-bg,axis=2);raw_fg=dist>32
    trans=(src[:,:,3]==0).astype(np.uint8);n,labels,stats,_=cv2.connectedComponentsWithStats(trans,8);fill=np.zeros(trans.shape,bool);kept=[];accepted=[];H,W=trans.shape
    for i in range(1,n):
        x,y,w,h,area=map(int,stats[i])
        if x==0 or y==0 or x+w==W or y+h==H:continue
        cm=labels==i;ratio=float(raw_fg[cm].mean());item={"area":area,"bbox":[x,y,w,h],"raw_foreground_ratio":round(ratio,3)}
        proven=cm&raw_fg
        if proven.any():
            fill|=proven;item["filled_pixels"]=int(proven.sum());accepted.append(item)
        if (~raw_fg&cm).any():
            item2=dict(item);item2["preserved_pixels"]=int((~raw_fg&cm).sum());kept.append(item2)
    out=src.copy();out[fill,:3]=raw[fill];out[fill,3]=255
    return out,fill,accepted,kept

def main():
    FRAMES.mkdir(parents=True,exist_ok=True);reports=[];samples=[];sample_ids={1,20,36,60,73,90,120,150,176,179,180}
    for n in range(1,181):
        src=np.asarray(Image.open(SRC/f"idle_{n:03d}.png").convert("RGBA"),np.uint8);raw=np.asarray(Image.open(RAW/f"idle_{n:03d}.png").convert("RGB"),np.uint8)
        out,fill,accepted,kept=repair(src,raw);Image.fromarray(out,"RGBA").save(FRAMES/f"idle_{n:03d}.png",optimize=True)
        reports.append({"frame":n,"filled_components":len(accepted),"filled_pixels":int(fill.sum()),"preserved_holes":len(kept),"accepted":accepted,"preserved":kept})
        if n in sample_ids:
            # Red overlay identifies exactly what V40 restores.
            ov=np.asarray(comp(src).convert("RGB"));mark=ov.copy();mark[fill]=np.rint(.2*mark[fill]+.8*np.array([255,40,40])).astype(np.uint8)
            crop=(350,120,850,780);s=1;b=comp(src).crop(crop);m=Image.fromarray(mark).crop(crop);a=comp(out).crop(crop)
            row=Image.new("RGB",(b.width*3,b.height+20),(28,28,28));row.paste(b,(0,20));row.paste(m,(b.width,20));row.paste(a,(b.width*2,20));d=ImageDraw.Draw(row);d.text((4,3),f"F{n:03d} BEFORE",fill="white");d.text((b.width+4,3),"RED=RESTORED",fill="white");d.text((b.width*2+4,3),"AFTER",fill="white");samples.append(row)
    # Compact contact sheet.
    thumb=[]
    for r in samples:
        rr=r.copy();rr.thumbnail((1200,540),Image.Resampling.LANCZOS);thumb.append(rr)
    sw=max(r.width for r in thumb);sheet=Image.new("RGB",(sw,sum(r.height for r in thumb)),(28,28,28));yy=0
    for r in thumb:sheet.paste(r,(0,yy));yy+=r.height
    sheet.save(OUT/"sample_gap_evidence.png",optimize=True)
    totals={"frames":180,"frames_changed":sum(r["filled_pixels"]>0 for r in reports),"filled_components":sum(r["filled_components"] for r in reports),"filled_pixels":sum(r["filled_pixels"] for r in reports),"preserved_holes":sum(r["preserved_holes"] for r in reports)}
    (OUT/"v41_report.json").write_text(json.dumps({"summary":totals,"frames":reports},indent=2)+"\n",encoding="utf-8");print(json.dumps(totals,indent=2))
    # Full-body 60 fps preview.
    gif=[]
    for n in range(1,181):
        a=np.asarray(Image.open(FRAMES/f"idle_{n:03d}.png").convert("RGBA"),np.uint8);im=comp(a);im.thumbnail((556,417),Image.Resampling.LANCZOS);gif.append(im.convert("RGB"))
    gif[0].save(OUT/"idle_v41_gap_repaired_3s_60fps.gif",save_all=True,append_images=gif[1:],duration=17,loop=0,optimize=False,disposal=2)

if __name__=="__main__":main()
