from pathlib import Path
import json
import cv2
import numpy as np
from PIL import Image, ImageDraw

ROOT=Path(r"D:\StSmod\DoubleSS")
BASE=ROOT/"art_candidates/video_source/idle"
SRC=BASE/"short_blink_final_v21_solid_rgb_repair/alpha_frames/idlefix_073.png"
RAW=BASE/"loop_v2_short_blink_180f/raw_frames/idle_073.png"
OUT=BASE/"test_anchor_gap_repair_v33"

def composite(a,bg=(45,45,45,255)):
    return Image.alpha_composite(Image.new("RGBA",(a.shape[1],a.shape[0]),bg),Image.fromarray(a,"RGBA"))

def main():
    OUT.mkdir(parents=True,exist_ok=True)
    src=np.asarray(Image.open(SRC).convert("RGBA"),np.uint8).copy()
    raw=np.asarray(Image.open(RAW).convert("RGB"),np.uint8)
    lab=cv2.cvtColor(raw,cv2.COLOR_RGB2LAB).astype(np.float32)
    corners=np.concatenate([lab[:80,:80].reshape(-1,3),lab[:80,-80:].reshape(-1,3),lab[-80:,:80].reshape(-1,3),lab[-80:,-80:].reshape(-1,3)])
    bg=np.median(corners,axis=0);dist=np.linalg.norm(lab-bg,axis=2);raw_fg=dist>24

    transparent=(src[:,:,3]==0).astype(np.uint8)
    n,labels,stats,cents=cv2.connectedComponentsWithStats(transparent,8)
    fill=np.zeros(transparent.shape,bool);accepted=[];rejected=[]
    H,W=transparent.shape
    for i in range(1,n):
        x,y,w,h,area=map(int,stats[i]);
        if x==0 or y==0 or x+w==W or y+h==H: continue
        cm=labels==i;ratio=float(raw_fg[cm].mean());item={"area":area,"bbox":[x,y,w,h],"raw_foreground_ratio":round(ratio,3)}
        if ratio>=0.80:
            fill|=cm;accepted.append(item)
        else:rejected.append(item)

    out=src.copy();out[fill,:3]=raw[fill];out[fill,3]=255
    Image.fromarray(out,"RGBA").save(OUT/"idlefix_073_gap_repaired.png",optimize=True)

    # Evidence views: red = repaired, cyan boxes = intentionally preserved holes.
    before=np.asarray(composite(src).convert("RGB"));after=np.asarray(composite(out).convert("RGB"));overlay=before.copy()
    overlay[fill]=np.rint(.2*overlay[fill]+.8*np.array([255,40,40])).astype(np.uint8)
    ov=Image.fromarray(overlay);d=ImageDraw.Draw(ov)
    for it in rejected:
        x,y,w,h=it["bbox"];d.rectangle((x-2,y-2,x+w+1,y+h+1),outline=(20,220,255),width=2)
    sheet=Image.new("RGB",(W*3,H),(28,28,28));sheet.paste(Image.fromarray(before),(0,0));sheet.paste(ov,(W,0));sheet.paste(Image.fromarray(after),(W*2,0));sd=ImageDraw.Draw(sheet)
    sd.text((8,8),"MANUAL ANCHOR",fill="white");sd.text((W+8,8),"RED=RAW-PROVEN CHARACTER GAP; CYAN=PRESERVED",fill="white");sd.text((W*2+8,8),"V33 REPAIRED",fill="white")
    preview=sheet.copy();preview.thumbnail((2400,900),Image.Resampling.LANCZOS);preview.save(OUT/"idlefix_073_gap_evidence_preview.png",optimize=True)
    # Detail sheet of every accepted hole, sorted by size.
    details=[]
    for idx,it in enumerate(sorted(accepted,key=lambda q:q["area"],reverse=True)):
        x,y,w,h=it["bbox"];pad=10;box=(max(0,x-pad),max(0,y-pad),min(W,x+w+pad),min(H,y+h+pad));
        b=Image.fromarray(before).crop(box).resize(((box[2]-box[0])*6,(box[3]-box[1])*6),Image.Resampling.NEAREST)
        a=Image.fromarray(after).crop(box).resize(b.size,Image.Resampling.NEAREST);details.append((it,b,a))
    if details:
        cw=max(b.width for _,b,_ in details);rows=[]
        for it,b,a in details:
            row=Image.new("RGB",(cw*2,max(b.height,a.height)+22),(28,28,28));row.paste(b,(0,22));row.paste(a,(cw,22));rd=ImageDraw.Draw(row);rd.text((4,4),f"before area={it['area']} bbox={it['bbox']}",fill="white");rd.text((cw+4,4),"after",fill="white");rows.append(row)
        dh=sum(r.height for r in rows);ds=Image.new("RGB",(cw*2,dh),(28,28,28));yy=0
        for r in rows:ds.paste(r,(0,yy));yy+=r.height
        ds.save(OUT/"accepted_holes_details.png",optimize=True)
    report={"source":str(SRC),"accepted_components":len(accepted),"accepted_pixels":int(fill.sum()),"accepted":accepted,"preserved_components":len(rejected),"preserved":rejected,"alpha0_to_255":int(fill.sum())}
    (OUT/"v33_report.json").write_text(json.dumps(report,indent=2)+"\n",encoding="utf-8");print(json.dumps({k:report[k] for k in ('accepted_components','accepted_pixels','preserved_components')},indent=2))

if __name__=="__main__":main()
