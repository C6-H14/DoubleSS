from pathlib import Path
import json
import cv2
import numpy as np
from PIL import Image, ImageDraw

ROOT=Path(r"D:\StSmod\DoubleSS")
BASE=ROOT/"art_candidates/video_source/idle"
SRC=BASE/"short_blink_final_v21_solid_rgb_repair/alpha_frames"
RAW=BASE/"loop_v2_short_blink_180f/raw_frames"
OUT=BASE/"test_hair_hand_v22"
PICKS=[1,16,41,57,81,101,120,151,180]
HAIR_ROI=(270,35,835,410)
HAND_ROI=(355,300,715,590)

def pink(rgb):
    hsv=cv2.cvtColor(rgb,cv2.COLOR_RGB2HSV)
    return (hsv[:,:,0]>=140)&(hsv[:,:,0]<=179)&(hsv[:,:,1]>=45)&(hsv[:,:,2]>=45)

def smooth_hair_alpha(rgba):
    out=rgba.copy();a=rgba[:,:,3].astype(np.float32)
    hsv=cv2.cvtColor(rgba[:,:,:3],cv2.COLOR_RGB2HSV)
    yy,xx=np.indices(a.shape);x1,y1,x2,y2=HAIR_ROI
    area=(xx>=x1)&(xx<x2)&(yy>=y1)&(yy<y2)
    hair=(a>0)&area&(hsv[:,:,0]>=8)&(hsv[:,:,0]<=35)&(hsv[:,:,1]>=35)&(hsv[:,:,2]>=70)
    # Include dark hair outlines adjacent to gold, without touching face/eyes.
    hair_zone=cv2.dilate(hair.astype(np.uint8),np.ones((5,5),np.uint8))>0
    solid=a>=128
    external=(cv2.dilate(solid.astype(np.uint8),np.ones((5,5),np.uint8))>0)&(~cv2.erode(solid.astype(np.uint8),np.ones((5,5),np.uint8)).astype(bool))
    target=hair_zone&external&area
    # Supersampled alpha filtering reduces pixel stair-steps while preserving
    # the centerline and openings of thin strands.
    hi=cv2.resize(a,None,fx=4,fy=4,interpolation=cv2.INTER_CUBIC)
    hi=cv2.GaussianBlur(hi,(0,0),1.15)
    smooth=cv2.resize(hi,(a.shape[1],a.shape[0]),interpolation=cv2.INTER_AREA)
    # Limit movement: no boundary pixel changes by more than 72 alpha levels.
    smooth=np.clip(smooth,a-72,a+72)
    changed=target&(np.abs(smooth-a)>=3)
    out[changed,3]=np.clip(np.rint(smooth[changed]),0,255).astype(np.uint8)
    return out,changed

def enclosed_holes(alpha,roi,max_area=90):
    x1,y1,x2,y2=roi;crop=(alpha[y1:y2,x1:x2]<72).astype(np.uint8)
    n,labels,stats,_=cv2.connectedComponentsWithStats(crop,8);mask=np.zeros_like(crop,bool)
    for lab in range(1,n):
        x,y,w,h,area=stats[lab]
        if x==0 or y==0 or x+w==crop.shape[1] or y+h==crop.shape[0] or area>max_area:continue
        mask|=labels==lab
    full=np.zeros(alpha.shape,bool);full[y1:y2,x1:x2]=mask
    return full

def repair_hands(raw,rgba):
    out=rgba.copy();x1,y1,x2,y2=HAND_ROI
    area=np.zeros(rgba.shape[:2],bool);area[y1:y2,x1:x2]=True
    solid=area&(rgba[:,:,3]>=224)&(~pink(raw))
    out[solid,:3]=raw[solid]
    holes=enclosed_holes(rgba[:,:,3],HAND_ROI)&(~pink(raw))
    hsv=cv2.cvtColor(raw,cv2.COLOR_RGB2HSV)
    holes&=hsv[:,:,2]>=30
    support=cv2.filter2D(holes.astype(np.uint8),-1,np.ones((3,3),np.uint8))
    holes&=support>=3
    out[holes,:3]=raw[holes];out[holes,3]=255
    return out,solid,holes

def comp(a):
    return Image.alpha_composite(Image.new('RGBA',(a.shape[1],a.shape[0]),(45,45,45,255)),Image.fromarray(a,'RGBA'))

def main():
    OUT.mkdir(parents=True,exist_ok=True);records=[];report={}
    for f in PICKS:
        before=np.asarray(Image.open(SRC/f'idle_{f:03d}.png').convert('RGBA'),np.uint8).copy()
        raw=np.asarray(Image.open(RAW/f'idle_{f:03d}.png').convert('RGB'),np.uint8)
        mid,hair_changed=smooth_hair_alpha(before)
        after,hand_rgb,hand_holes=repair_hands(raw,mid)
        after[after[:,:,3]==0,:3]=0
        Image.fromarray(after,'RGBA').save(OUT/f'idle_{f:03d}_test.png',optimize=True)
        report[str(f)]={'hair_alpha_pixels_smoothed':int(hair_changed.sum()),
                        'hand_solid_rgb_restored':int(hand_rgb.sum()),
                        'hand_hole_pixels_filled':int(hand_holes.sum())}
        records.append((f,before,after,hair_changed,hand_holes))
    for name,roi in [('hair',HAIR_ROI),('hands',HAND_ROI)]:
        x1,y1,x2,y2=roi;h,w=records[0][1].shape[:2];x2=min(x2,w);y2=min(y2,h);roi=(x1,y1,x2,y2)
        cw,ch=x2-x1,y2-y1;s=3
        sheet=Image.new('RGB',(cw*s*3,ch*s*len(records)),(28,28,28));d=ImageDraw.Draw(sheet);yy=0
        for f,b,a,hc,hh in records:
            bi=comp(b).crop(roi).resize((cw*s,ch*s),Image.Resampling.NEAREST)
            ai=comp(a).crop(roi).resize((cw*s,ch*s),Image.Resampling.NEAREST)
            change=hc|hh;m=np.zeros((ch,cw,3),np.uint8);m[change[y1:y2,x1:x2]]=(255,40,40)
            mi=Image.fromarray(m).resize((cw*s,ch*s),Image.Resampling.NEAREST)
            sheet.paste(bi,(0,yy));sheet.paste(ai,(cw*s,yy));sheet.paste(mi,(cw*s*2,yy))
            d.text((5,yy+4),f'{f:03d} BEFORE',fill='white');d.text((cw*s+5,yy+4),f'{f:03d} V22',fill='white');d.text((cw*s*2+5,yy+4),'ALPHA/HOLES',fill='white');yy+=ch*s
        sheet.save(OUT/f'{name}_before_after.png',optimize=True)
        preview=sheet.copy();preview.thumbnail((1600,2000),Image.Resampling.LANCZOS);preview.save(OUT/f'{name}_preview.png',optimize=True)
    # Pixel-level closeups for the two reported failure types.
    details={'hair_loop':(280,135,520,340),'wrist_cuff_guard':(370,320,660,525)}
    for name,roi in details.items():
        x1,y1,x2,y2=roi;h,w=records[0][1].shape[:2];x2=min(x2,w);y2=min(y2,h);cw,ch=x2-x1,y2-y1;s=3
        sheet=Image.new('RGB',(cw*s*2,ch*s*len(records)),(28,28,28));d=ImageDraw.Draw(sheet);yy=0
        for f,b,a,hc,hh in records:
            bi=comp(b).crop((x1,y1,x2,y2)).resize((cw*s,ch*s),Image.Resampling.NEAREST)
            ai=comp(a).crop((x1,y1,x2,y2)).resize((cw*s,ch*s),Image.Resampling.NEAREST)
            sheet.paste(bi,(0,yy));sheet.paste(ai,(cw*s,yy));d.text((5,yy+4),f'{f:03d} BEFORE',fill='white');d.text((cw*s+5,yy+4),f'{f:03d} V22',fill='white');yy+=ch*s
        sheet.save(OUT/f'{name}_pixel_closeup.png',optimize=True)
        preview=sheet.copy();preview.thumbnail((1600,2000),Image.Resampling.NEAREST);preview.save(OUT/f'{name}_pixel_preview.png',optimize=True)
    (OUT/'v22_test_report.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(report,indent=2))
if __name__=='__main__':main()
