from pathlib import Path
import json
import cv2
import numpy as np
from PIL import Image,ImageDraw
from scipy.ndimage import distance_transform_edt

ROOT=Path(r"D:\StSmod\DoubleSS")
BASE=ROOT/"art_candidates/video_source/idle"
SRC=BASE/"short_blink_final_v21_solid_rgb_repair/alpha_frames/idle_073.png"
OUT=BASE/"test_hand_manual_regions_v26"
VIEW=(430,345,610,485)

# Coordinates mapped from the user's annotated crop to frame 73.
PROTECT_RED=(460,355,488,397)
REGIONS={
    'left_cuff_under':(476,376,499,399),
    'between_hands':(489,389,515,419),
    'right_cuff_under':(510,385,551,425),
    'guard_junction':(535,402,570,438),
}

def comp(a):
    return Image.alpha_composite(Image.new('RGBA',(a.shape[1],a.shape[0]),(45,45,45,255)),Image.fromarray(a,'RGBA'))

def main():
    OUT.mkdir(parents=True,exist_ok=True)
    before=np.asarray(Image.open(SRC).convert('RGBA'),np.uint8).copy();after=before.copy()
    rgb=before[:,:,:3];alpha=before[:,:,3]
    hsv=cv2.cvtColor(rgb,cv2.COLOR_RGB2HSV);lab=cv2.cvtColor(rgb,cv2.COLOR_RGB2LAB);L=lab[:,:,0]
    yy,xx=np.indices(alpha.shape)
    protect=(xx>=PROTECT_RED[0])&(xx<PROTECT_RED[2])&(yy>=PROTECT_RED[1])&(yy<PROTECT_RED[3])
    skin=(alpha>=224)&(hsv[:,:,0]<=18)&(hsv[:,:,1]>=8)&(hsv[:,:,1]<=130)&(hsv[:,:,2]>=100)
    white=(alpha>=224)&(hsv[:,:,1]<=78)&(hsv[:,:,2]>=142)
    gold=(alpha>=224)&(hsv[:,:,0]>=7)&(hsv[:,:,0]<=35)&(hsv[:,:,1]>=55)&(hsv[:,:,2]>=60)
    dark=(alpha>=180)&(L<105)
    changed=np.zeros(alpha.shape,bool);counts={}
    for name,(x1,y1,x2,y2) in REGIONS.items():
        region=(xx>=x1)&(xx<x2)&(yy>=y1)&(yy<y2)&(~protect)
        allowed=(skin|white) if name!='guard_junction' else (skin|white|gold)
        # Remove only dark teeth for which a valid semantic surface exists
        # within four pixels. Long dark strokes outside the hand-facing side
        # are not selected.
        _,near=distance_transform_edt(~allowed,return_indices=True)
        dist=distance_transform_edt(~allowed)
        candidate=region&dark&(dist<=4.2)
        # Keep dense continuous ink; remove protruding tips with high nearby
        # light support, including tips attached to a long component.
        support=cv2.filter2D(allowed.astype(np.uint8),-1,np.ones((5,5),np.uint8))
        candidate&=support>=7
        donor=rgb[near[0],near[1]]
        after[candidate,:3]=donor[candidate]
        changed|=candidate;counts[name]=int(candidate.sum())
    assert not np.any(changed&protect)
    assert np.array_equal(after[:,:,3],before[:,:,3])
    Image.fromarray(after,'RGBA').save(OUT/'idle_073_manual_regions_test.png',optimize=True)
    x1,y1,x2,y2=VIEW;cw,ch=x2-x1,y2-y1;s=8
    sheet=Image.new('RGB',(cw*s*3,ch*s),(28,28,28));d=ImageDraw.Draw(sheet)
    b=comp(before).crop(VIEW).resize((cw*s,ch*s),Image.Resampling.NEAREST)
    a=comp(after).crop(VIEW).resize((cw*s,ch*s),Image.Resampling.NEAREST)
    m=np.zeros((ch,cw,3),np.uint8);m[changed[y1:y2,x1:x2]]=(40,190,255);m[protect[y1:y2,x1:x2]]=(255,40,40)
    mi=Image.fromarray(m).resize((cw*s,ch*s),Image.Resampling.NEAREST)
    sheet.paste(b,(0,0));sheet.paste(a,(cw*s,0));sheet.paste(mi,(cw*s*2,0));d.text((5,5),'V21 BEFORE',fill='white');d.text((cw*s+5,5),'V26 LOCAL TEST',fill='white');d.text((cw*s*2+5,5),'BLUE=EDIT RED=PROTECTED',fill='white')
    sheet.save(OUT/'frame073_manual_regions_before_after_8x.png',optimize=True)
    preview=sheet.copy();preview.thumbnail((2100,1000),Image.Resampling.NEAREST);preview.save(OUT/'frame073_manual_regions_preview.png',optimize=True)
    report={'frame':73,'region_changes':counts,'total_rgb_pixels_changed':int(changed.sum()),
            'protected_red_pixels_changed':0,'alpha_pixels_changed':0}
    (OUT/'v26_report.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(report,indent=2))
if __name__=='__main__':main()
