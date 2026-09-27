from pathlib import Path
import json
import cv2
import numpy as np
from PIL import Image,ImageDraw
from scipy.ndimage import distance_transform_edt

ROOT=Path(r"D:\StSmod\DoubleSS")
BASE=ROOT/"art_candidates/video_source/idle"
SRC=BASE/"short_blink_final_v21_solid_rgb_repair/alpha_frames/idle_073.png"
OUT=BASE/"test_hand_topology_v24"
ROI=(430,345,610,500)

def comp(a):
    return Image.alpha_composite(Image.new('RGBA',(a.shape[1],a.shape[0]),(45,45,45,255)),Image.fromarray(a,'RGBA'))

def main():
    OUT.mkdir(parents=True,exist_ok=True)
    rgba=np.asarray(Image.open(SRC).convert('RGBA'),np.uint8).copy();rgb=rgba[:,:,:3];a=rgba[:,:,3]
    x1,y1,x2,y2=ROI;area=np.zeros(a.shape,bool);area[y1:y2,x1:x2]=True
    hsv=cv2.cvtColor(rgb,cv2.COLOR_RGB2HSV);lab=cv2.cvtColor(rgb,cv2.COLOR_RGB2LAB);L=lab[:,:,0]
    skin=area&(a>=224)&(hsv[:,:,0]<=18)&(hsv[:,:,1]>=10)&(hsv[:,:,1]<=125)&(hsv[:,:,2]>=105)
    white=area&(a>=224)&(hsv[:,:,1]<=75)&(hsv[:,:,2]>=145)
    gold=area&(a>=224)&(hsv[:,:,0]>=7)&(hsv[:,:,0]<=35)&(hsv[:,:,1]>=60)&(hsv[:,:,2]>=65)
    semantic=skin|white|gold
    dark=area&(a>=224)&(L<92)
    # Connected components of dark ink; long strokes are legitimate outlines.
    n,labels,stats,_=cv2.connectedComponentsWithStats(dark.astype(np.uint8),8)
    local_light=cv2.filter2D(semantic.astype(np.uint8),-1,np.ones((5,5),np.uint8))
    candidates=np.zeros_like(dark);components=[]
    for labid in range(1,n):
        mask=labels==labid;size=int(stats[labid,cv2.CC_STAT_AREA])
        ys,xs=np.where(mask)
        if not len(xs):continue
        w=int(xs.max()-xs.min()+1);h=int(ys.max()-ys.min()+1)
        adjacency=float(local_light[mask].mean()/25.0)
        # Tiny near-isotropic pieces surrounded by light colors are noise;
        # elongated or large pieces are real line art.
        suspect=size<=7 and max(w,h)<=5 and adjacency>=0.30
        if suspect:candidates|=mask
        components.append({'id':labid,'pixels':size,'width':w,'height':h,
                           'light_neighbour_fraction':round(adjacency,3),'suspect':suspect})
    # Additional non-dark color outliers: compare with local median, but only
    # inside a semantic surface and only retain tiny connected patches.
    med=cv2.medianBlur(rgb,5);d=np.linalg.norm(cv2.cvtColor(rgb,cv2.COLOR_RGB2LAB).astype(np.float32)-
                                              cv2.cvtColor(med,cv2.COLOR_RGB2LAB).astype(np.float32),axis=2)
    outlier=area&(a>=224)&semantic&(d>=24)
    nn,ll,ss,_=cv2.connectedComponentsWithStats(outlier.astype(np.uint8),8)
    color_noise=np.zeros_like(outlier)
    for i in range(1,nn):
        if ss[i,cv2.CC_STAT_AREA]<=6:color_noise|=ll==i
    repair=candidates|color_noise
    # Donor must be nearby, opaque, semantic, and not dark/noisy.
    donor=semantic&(~dark)&(~color_noise)
    _,near=distance_transform_edt(~donor,return_indices=True)
    nearest=rgb[near[0],near[1]]
    after=rgba.copy();after[repair,:3]=nearest[repair]
    # The circled major defects are attached to six long contour components,
    # not isolated dots. Reconstruct only the outer contour near skin/cuff/gold.
    zone=cv2.dilate(semantic.astype(np.uint8),np.ones((7,7),np.uint8))>0
    solid=a>=96
    boundary=(cv2.dilate(solid.astype(np.uint8),np.ones((5,5),np.uint8))>
              cv2.erode(solid.astype(np.uint8),np.ones((5,5),np.uint8)))
    alpha_smooth=cv2.GaussianBlur((solid.astype(np.uint8)*255),(0,0),0.85).astype(np.float32)
    contour_target=area&zone&boundary
    proposed=np.clip(alpha_smooth,a.astype(np.float32)-112,a.astype(np.float32)+112)
    contour_changed=contour_target&(np.abs(proposed-a)>=4)
    olda=after[:,:,3].copy()
    after[contour_changed,3]=np.rint(proposed[contour_changed]).astype(np.uint8)
    # Newly exposed subpixels inherit nearest semantic color, never background.
    new_visible=contour_changed&(after[:,:,3]>olda)
    after[new_visible,:3]=nearest[new_visible]
    Image.fromarray(after,'RGBA').save(OUT/'idle_073_hand_local_test.png',optimize=True)

    crop=(x1,y1,x2,y2);cw,ch=x2-x1,y2-y1;s=6
    sheet=Image.new('RGB',(cw*s*3,ch*s),(28,28,28));dr=ImageDraw.Draw(sheet)
    b=comp(rgba).crop(crop).resize((cw*s,ch*s),Image.Resampling.NEAREST)
    z=comp(after).crop(crop).resize((cw*s,ch*s),Image.Resampling.NEAREST)
    m=np.zeros((ch,cw,3),np.uint8);m[repair[y1:y2,x1:x2]]=(255,40,40);m[contour_changed[y1:y2,x1:x2]]=(40,180,255)
    mi=Image.fromarray(m).resize((cw*s,ch*s),Image.Resampling.NEAREST)
    sheet.paste(b,(0,0));sheet.paste(z,(cw*s,0));sheet.paste(mi,(cw*s*2,0));dr.text((5,5),'FRAME 073 BEFORE',fill='white');dr.text((cw*s+5,5),'LOCAL TEST',fill='white');dr.text((cw*s*2+5,5),'REPAIRED PIXELS',fill='white')
    sheet.save(OUT/'frame073_hand_before_after_6x.png',optimize=True)
    preview=sheet.copy();preview.thumbnail((2000,1000),Image.Resampling.NEAREST);preview.save(OUT/'frame073_hand_preview.png',optimize=True)
    suspicious=[c for c in components if c['suspect']]
    report={'frame':73,'roi':ROI,'dark_components_total':len(components),
            'suspect_dark_components':len(suspicious),'suspect_dark_pixels':int(candidates.sum()),
            'color_noise_pixels':int(color_noise.sum()),'total_repaired':int(repair.sum()),
            'contour_alpha_pixels_resampled':int(contour_changed.sum()),
            'suspect_component_metrics':suspicious,
            'alpha_pixels_changed':int(contour_changed.sum())}
    (OUT/'frame073_hand_analysis.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(report,indent=2))
if __name__=='__main__':main()
