from pathlib import Path
import json
import cv2
import numpy as np
from PIL import Image, ImageDraw
from scipy.ndimage import distance_transform_edt

ROOT=Path(r"D:\StSmod\DoubleSS")
BASE=ROOT/"art_candidates/video_source/idle"
SRC=BASE/"short_blink_final_v21_solid_rgb_repair/alpha_frames"
OUT=BASE/"test_contour_rebuild_v23"
PICKS=[1,16,41,57,81,101,120,151,180]
HAIR_ROI=(270,35,835,410)
HAND_ROI=(355,300,715,590)

def comp(a):
    return Image.alpha_composite(Image.new('RGBA',(a.shape[1],a.shape[0]),(45,45,45,255)),Image.fromarray(a,'RGBA'))

def rebuild_hair(rgba):
    out=rgba.copy();a=rgba[:,:,3].astype(np.float32);rgb=rgba[:,:,:3]
    hsv=cv2.cvtColor(rgb,cv2.COLOR_RGB2HSV);yy,xx=np.indices(a.shape);x1,y1,x2,y2=HAIR_ROI
    area=(xx>=x1)&(xx<x2)&(yy>=y1)&(yy<y2)
    gold=(a>0)&area&(hsv[:,:,0]>=7)&(hsv[:,:,0]<=38)&(hsv[:,:,1]>=25)&(hsv[:,:,2]>=55)
    zone=cv2.dilate(gold.astype(np.uint8),np.ones((7,7),np.uint8))>0
    binary=(a>=96).astype(np.uint8)*255
    # Smooth the silhouette as a signed coverage field. Sigma 0.9 visibly
    # rounds one-pixel stairs but cannot move the contour more than ~1 px.
    smooth=cv2.GaussianBlur(binary,(0,0),0.9).astype(np.float32)
    boundary=(cv2.dilate((a>=96).astype(np.uint8),np.ones((5,5),np.uint8))>
              cv2.erode((a>=96).astype(np.uint8),np.ones((5,5),np.uint8)))
    target=area&zone&boundary
    proposed=np.clip(smooth,a-128,a+128)
    changed=target&(np.abs(proposed-a)>=4)
    out[changed,3]=np.rint(proposed[changed]).astype(np.uint8)
    # Newly visible coverage inherits nearest existing hair/outline color.
    donor=(a>=96)&zone
    _,nearest=distance_transform_edt(~donor,return_indices=True)
    nearest_rgb=rgb[nearest[0],nearest[1]]
    new_edge=changed&(out[:,:,3]>a)
    out[new_edge,:3]=nearest_rgb[new_edge]
    return out,changed

def clean_hand_teeth(rgba):
    out=rgba.copy();x1,y1,x2,y2=HAND_ROI
    rgb=rgba[:,:,:3];a=rgba[:,:,3]
    hsv=cv2.cvtColor(rgb,cv2.COLOR_RGB2HSV);lab=cv2.cvtColor(rgb,cv2.COLOR_RGB2LAB)
    L=lab[:,:,0]
    area=np.zeros(a.shape,bool);area[y1:y2,x1:x2]=True
    skin=area&(a>=180)&(hsv[:,:,0]<=18)&(hsv[:,:,1]>=12)&(hsv[:,:,1]<=125)&(hsv[:,:,2]>=105)
    white=area&(a>=180)&(hsv[:,:,1]<=72)&(hsv[:,:,2]>=145)
    gold=area&(a>=180)&(hsv[:,:,0]>=8)&(hsv[:,:,0]<=35)&(hsv[:,:,1]>=65)&(hsv[:,:,2]>=70)
    light=skin|white|gold
    dark=area&(a>=180)&(L<92)
    kernel=np.ones((3,3),np.uint8)
    light_n=cv2.filter2D(light.astype(np.uint8),-1,kernel)
    dark_n=cv2.filter2D(dark.astype(np.uint8),-1,kernel)
    # Teeth are dark texels embedded in a light semantic region with too
    # little support to be part of a continuous outline.
    teeth=dark&(light_n>=5)&(dark_n<=4)
    # Also catch two-pixel tips attached to an otherwise valid outline.
    teeth|=dark&(light_n>=6)&(dark_n<=5)
    n,labels,stats,_=cv2.connectedComponentsWithStats(teeth.astype(np.uint8),8)
    keep=np.zeros_like(teeth)
    for labid in range(1,n):
        if stats[labid,cv2.CC_STAT_AREA]<=10:
            keep|=labels==labid
    teeth=keep
    # Choose nearest compatible light region; this reconstructs the semantic
    # surface rather than applying a generic blur.
    _,near=distance_transform_edt(~light,return_indices=True)
    donor=rgb[near[0],near[1]]
    out[teeth,:3]=donor[teeth]
    # Blend only the immediate repaired boundary to avoid a hard pasted dot.
    ring=(cv2.dilate(teeth.astype(np.uint8),kernel)>0)&(~teeth)&light
    blur=cv2.GaussianBlur(out[:,:,:3],(3,3),0.45)
    out[ring,:3]=np.rint(0.8*out[ring,:3]+0.2*blur[ring]).astype(np.uint8)
    return out,teeth

def main():
    OUT.mkdir(parents=True,exist_ok=True);rows=[];report={}
    for f in PICKS:
        before=np.asarray(Image.open(SRC/f'idle_{f:03d}.png').convert('RGBA'),np.uint8).copy()
        mid,hc=rebuild_hair(before);after,teeth=clean_hand_teeth(mid);after[after[:,:,3]==0,:3]=0
        Image.fromarray(after,'RGBA').save(OUT/f'idle_{f:03d}_v23.png',optimize=True)
        report[str(f)]={'hair_alpha_changed':int(hc.sum()),'hand_dark_teeth_removed':int(teeth.sum())}
        rows.append((f,before,after,hc,teeth))
    details={'hair':(275,120,535,355),'hands':(365,315,675,535)}
    for name,(x1,y1,x2,y2) in details.items():
        h,w=rows[0][1].shape[:2];x2=min(x2,w);y2=min(y2,h);cw,ch=x2-x1,y2-y1;s=4
        sheet=Image.new('RGB',(cw*s*3,ch*s*len(rows)),(28,28,28));d=ImageDraw.Draw(sheet);yy=0
        for f,b,a,hc,teeth in rows:
            bi=comp(b).crop((x1,y1,x2,y2)).resize((cw*s,ch*s),Image.Resampling.NEAREST)
            ai=comp(a).crop((x1,y1,x2,y2)).resize((cw*s,ch*s),Image.Resampling.NEAREST)
            change=hc if name=='hair' else teeth;m=np.zeros((ch,cw,3),np.uint8);m[change[y1:y2,x1:x2]]=(255,40,40)
            mi=Image.fromarray(m).resize((cw*s,ch*s),Image.Resampling.NEAREST)
            sheet.paste(bi,(0,yy));sheet.paste(ai,(cw*s,yy));sheet.paste(mi,(cw*s*2,yy));d.text((5,yy+4),f'{f:03d} BEFORE',fill='white');d.text((cw*s+5,yy+4),f'{f:03d} V23',fill='white');d.text((cw*s*2+5,yy+4),'CHANGED',fill='white');yy+=ch*s
        sheet.save(OUT/f'{name}_pixel_before_after.png',optimize=True)
        preview=sheet.copy();preview.thumbnail((1800,2200),Image.Resampling.NEAREST);preview.save(OUT/f'{name}_pixel_preview.png',optimize=True)
    (OUT/'v23_report.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(report,indent=2))
if __name__=='__main__':main()
