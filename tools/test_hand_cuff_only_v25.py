from pathlib import Path
import json
import cv2
import numpy as np
from PIL import Image,ImageDraw
from scipy.ndimage import distance_transform_edt

ROOT=Path(r"D:\StSmod\DoubleSS")
BASE=ROOT/"art_candidates/video_source/idle"
SRC=BASE/"short_blink_final_v21_solid_rgb_repair/alpha_frames/idle_073.png"
V24=BASE/"test_hand_topology_v24/idle_073_hand_local_test.png"
OUT=BASE/"test_hand_cuff_only_v25"
ROI=(430,345,610,500)

def comp(a):
    return Image.alpha_composite(Image.new('RGBA',(a.shape[1],a.shape[0]),(45,45,45,255)),Image.fromarray(a,'RGBA'))

def main():
    OUT.mkdir(parents=True,exist_ok=True)
    rgba=np.asarray(Image.open(SRC).convert('RGBA'),np.uint8).copy();rgb=rgba[:,:,:3];alpha=rgba[:,:,3]
    x1,y1,x2,y2=ROI;area=np.zeros(alpha.shape,bool);area[y1:y2,x1:x2]=True
    hsv=cv2.cvtColor(rgb,cv2.COLOR_RGB2HSV);lab=cv2.cvtColor(rgb,cv2.COLOR_RGB2LAB);L=lab[:,:,0]
    skin=area&(alpha>=224)&(hsv[:,:,0]<=18)&(hsv[:,:,1]>=10)&(hsv[:,:,1]<=125)&(hsv[:,:,2]>=105)
    white=area&(alpha>=224)&(hsv[:,:,1]<=75)&(hsv[:,:,2]>=145)
    light=skin|white
    gold_brown=area&(alpha>=180)&(hsv[:,:,0]>=3)&(hsv[:,:,0]<=35)&(hsv[:,:,1]>=50)&(hsv[:,:,2]>=35)
    protected=cv2.dilate(gold_brown.astype(np.uint8),np.ones((13,13),np.uint8))>0
    # Protect the true outer silhouette: only internal opaque pixels qualify.
    inward=cv2.distanceTransform((alpha>=224).astype(np.uint8),cv2.DIST_L2,5)
    internal=inward>=2.0
    dark=area&(alpha>=224)&(L<92)&(~protected)&internal
    light_n=cv2.filter2D(light.astype(np.uint8),-1,np.ones((5,5),np.uint8))
    n,labels,stats,_=cv2.connectedComponentsWithStats(dark.astype(np.uint8),8)
    teeth=np.zeros_like(dark);component_rows=[]
    for i in range(1,n):
        m=labels==i;ys,xs=np.where(m);size=int(stats[i,cv2.CC_STAT_AREA])
        if not len(xs):continue
        w=int(xs.max()-xs.min()+1);h=int(ys.max()-ys.min()+1);adj=float(light_n[m].mean()/25)
        use=size<=7 and max(w,h)<=5 and adj>=0.45
        if use:teeth|=m
        component_rows.append({'pixels':size,'width':w,'height':h,'light_fraction':round(adj,3),'removed':use})
    # RGB outliers only if they lie inside skin/white, never in gold/brown or outlines.
    med=cv2.medianBlur(rgb,5)
    med_lab=cv2.cvtColor(med,cv2.COLOR_RGB2LAB).astype(np.float32)
    delta=np.linalg.norm(lab.astype(np.float32)-med_lab,axis=2)
    raw_noise=light&internal&(~protected)&(delta>=28)
    nn,ll,ss,_=cv2.connectedComponentsWithStats(raw_noise.astype(np.uint8),8)
    color_noise=np.zeros_like(raw_noise)
    for i in range(1,nn):
        if ss[i,cv2.CC_STAT_AREA]<=5:color_noise|=ll==i
    repair=teeth|color_noise
    donor=light&internal&(~protected)&(~repair)
    _,near=distance_transform_edt(~donor,return_indices=True)
    after=rgba.copy();after[repair,:3]=rgb[near[0],near[1]][repair]
    assert np.array_equal(after[:,:,3],alpha)
    # Explicit protection contract.
    assert not np.any(repair&protected)
    # Retain the visually successful V24 change only inside the user's blue
    # right-wrist area. Everything in the three red failure zones stays V21.
    v24=np.asarray(Image.open(V24).convert('RGBA'),np.uint8)
    blue_box=(478,374,558,447)
    bx1,by1,bx2,by2=blue_box
    blue=np.zeros(alpha.shape,bool);blue[by1:by2,bx1:bx2]=True
    v24_diff=np.any(v24!=rgba,axis=2)
    blue_use=blue&v24_diff&(~protected)
    after[blue_use]=v24[blue_use]
    Image.fromarray(after,'RGBA').save(OUT/'idle_073_cuff_only_test.png',optimize=True)
    cw,ch=x2-x1,y2-y1;s=7
    sheet=Image.new('RGB',(cw*s*3,ch*s),(28,28,28));d=ImageDraw.Draw(sheet)
    b=comp(rgba).crop(ROI).resize((cw*s,ch*s),Image.Resampling.NEAREST)
    a=comp(after).crop(ROI).resize((cw*s,ch*s),Image.Resampling.NEAREST)
    m=np.zeros((ch,cw,3),np.uint8);m[repair[y1:y2,x1:x2]]=(40,180,255);m[blue_use[y1:y2,x1:x2]]=(30,220,255)
    mi=Image.fromarray(m).resize((cw*s,ch*s),Image.Resampling.NEAREST)
    sheet.paste(b,(0,0));sheet.paste(a,(cw*s,0));sheet.paste(mi,(cw*s*2,0));d.text((5,5),'V21 BEFORE',fill='white');d.text((cw*s+5,5),'V25 CUFF ONLY',fill='white');d.text((cw*s*2+5,5),'CHANGED (BLUE)',fill='white')
    sheet.save(OUT/'frame073_cuff_only_before_after_7x.png',optimize=True)
    preview=sheet.copy();preview.thumbnail((2000,1000),Image.Resampling.NEAREST);preview.save(OUT/'frame073_cuff_only_preview.png',optimize=True)
    report={'frame':73,'alpha_pixels_changed':0,'gold_brown_pixels_changed':0,
            'dark_teeth_removed':int(teeth.sum()),'color_noise_repaired':int(color_noise.sum()),
            'total_rgb_pixels_changed':int(repair.sum()),
            'v24_blue_region_pixels_retained':int(blue_use.sum()),'components':component_rows}
    (OUT/'v25_report.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
    print(json.dumps({k:v for k,v in report.items() if k!='components'},indent=2))
if __name__=='__main__':main()
