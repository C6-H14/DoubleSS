from pathlib import Path
import json
import cv2
import numpy as np
from PIL import Image,ImageDraw

ROOT=Path(r"D:\StSmod\DoubleSS")
BASE=ROOT/"art_candidates/video_source/idle"
SRC=BASE/"short_blink_final_v21_solid_rgb_repair/alpha_frames/idle_073.png"
OUT=BASE/"test_hand_narrow_band_v27"
VIEW=(430,345,610,485)

def comp(a):
    return Image.alpha_composite(Image.new('RGBA',(a.shape[1],a.shape[0]),(45,45,45,255)),Image.fromarray(a,'RGBA'))

def main():
    OUT.mkdir(parents=True,exist_ok=True)
    rgba=np.asarray(Image.open(SRC).convert('RGBA'),np.uint8).copy();rgb=rgba[:,:,:3];alpha=rgba[:,:,3]
    hsv=cv2.cvtColor(rgb,cv2.COLOR_RGB2HSV)

    # One narrow, articulated ribbon following only the reported bad boundary.
    paths=[
        np.array([[482,387],[489,394],[495,403],[501,412]],np.int32),
        np.array([[501,394],[511,393],[521,397],[531,404],[539,413]],np.int32),
        np.array([[537,406],[542,414],[548,423],[553,430]],np.int32),
    ]
    band=np.zeros(alpha.shape,np.uint8)
    for pts in paths:cv2.polylines(band,[pts],False,255,thickness=7,lineType=cv2.LINE_AA)
    band=band>20
    # Absolute exclusions: user's red region and weapon materials.
    yy,xx=np.indices(alpha.shape)
    red_protect=(xx>=458)&(xx<490)&(yy>=352)&(yy<397)
    gold_brown=(alpha>=160)&(hsv[:,:,0]>=3)&(hsv[:,:,0]<=35)&(hsv[:,:,1]>=45)&(hsv[:,:,2]>=30)
    weapon_protect=cv2.dilate(gold_brown.astype(np.uint8),np.ones((7,7),np.uint8))>0
    mask=band&(~red_protect)&(~weapon_protect)&(alpha>=150)

    # Redraw the narrow strip from its immediate surroundings, then restore
    # three intentional smooth outline strokes. Alpha stays untouched.
    inpaint_mask=(mask.astype(np.uint8)*255)
    redrawn=cv2.inpaint(rgb,inpaint_mask,2.2,cv2.INPAINT_TELEA)
    after=rgba.copy();after[mask,:3]=redrawn[mask]
    # Continuous intended ink, rendered antialiased at native resolution.
    canvas=after[:,:,:3].copy()
    ink=(72,48,43)
    cv2.polylines(canvas,[paths[0]],False,ink,1,cv2.LINE_AA)
    cv2.polylines(canvas,[paths[1]],False,(74,70,73),1,cv2.LINE_AA)
    cv2.polylines(canvas,[paths[2]],False,(72,60,55),1,cv2.LINE_AA)
    line_mask=np.zeros(alpha.shape,np.uint8)
    for pts in paths:cv2.polylines(line_mask,[pts],False,255,1,cv2.LINE_AA)
    line_use=(line_mask>0)&mask
    after[line_use,:3]=canvas[line_use]
    assert np.array_equal(after[:,:,3],alpha)
    assert not np.any(mask&red_protect)
    assert not np.any(mask&weapon_protect)
    Image.fromarray(after,'RGBA').save(OUT/'idle_073_narrow_band_test.png',optimize=True)

    x1,y1,x2,y2=VIEW;cw,ch=x2-x1,y2-y1;s=8
    before_img=comp(rgba).crop(VIEW).resize((cw*s,ch*s),Image.Resampling.NEAREST)
    after_img=comp(after).crop(VIEW).resize((cw*s,ch*s),Image.Resampling.NEAREST)
    overlay=np.asarray(comp(rgba).crop(VIEW).convert('RGB')).copy()
    local=mask[y1:y2,x1:x2];overlay[local]=np.rint(0.35*overlay[local]+0.65*np.array([20,190,255])).astype(np.uint8)
    rp=red_protect[y1:y2,x1:x2];overlay[rp]=np.rint(0.35*overlay[rp]+0.65*np.array([255,40,40])).astype(np.uint8)
    overlay_img=Image.fromarray(overlay).resize((cw*s,ch*s),Image.Resampling.NEAREST)
    sheet=Image.new('RGB',(cw*s*3,ch*s),(28,28,28));d=ImageDraw.Draw(sheet)
    sheet.paste(before_img,(0,0));sheet.paste(overlay_img,(cw*s,0));sheet.paste(after_img,(cw*s*2,0));d.text((5,5),'V21 BEFORE',fill='white');d.text((cw*s+5,5),'CYAN=REDRAW RED=LOCKED',fill='white');d.text((cw*s*2+5,5),'V27 LOCAL REDRAW',fill='white')
    sheet.save(OUT/'frame073_narrow_band_before_mask_after_8x.png',optimize=True)
    preview=sheet.copy();preview.thumbnail((2100,1000),Image.Resampling.NEAREST);preview.save(OUT/'frame073_narrow_band_preview.png',optimize=True)
    report={'frame':73,'band_pixels':int(mask.sum()),'red_protected_pixels_changed':0,
            'weapon_protected_pixels_changed':0,'alpha_pixels_changed':0,'paths':[p.tolist() for p in paths]}
    (OUT/'v27_report.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(report,indent=2))
if __name__=='__main__':main()
