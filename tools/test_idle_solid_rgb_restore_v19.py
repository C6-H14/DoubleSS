from pathlib import Path
import json
import cv2
import numpy as np
from PIL import Image, ImageDraw

ROOT=Path(r"D:\StSmod\DoubleSS")
BASE=ROOT/"art_candidates/video_source/idle"
SRC=BASE/"short_blink_final_v15_garment_color_stable/alpha_frames"
RAW=BASE/"loop_v2_short_blink_180f/raw_frames"
OUT=BASE/"test_solid_rgb_restore_v19"
PICKS=[16,57,120,180]
ROI=(430,150,720,370)

def pink(rgb):
    h=cv2.cvtColor(rgb,cv2.COLOR_RGB2HSV)
    return (h[:,:,0]>=140)&(h[:,:,0]<=179)&(h[:,:,1]>=45)&(h[:,:,2]>=45)

def restore(raw, rgba):
    out=rgba.copy(); x1,y1,x2,y2=ROI
    lab_raw=cv2.cvtColor(raw,cv2.COLOR_RGB2LAB).astype(np.float32)
    lab_cur=cv2.cvtColor(rgba[:,:,:3],cv2.COLOR_RGB2LAB).astype(np.float32)
    delta=np.linalg.norm(lab_raw-lab_cur,axis=2)
    area=np.zeros(delta.shape,bool);area[y1:y2,x1:x2]=True
    # Solid pixels only: alpha and silhouette are byte-for-byte frozen.
    # For the collar/neck block the raw same-frame image is the clean texture
    # authority. Replace the complete solid interior; freeze alpha and all
    # semitransparent/external edge pixels.
    use=area&(rgba[:,:,3]>=224)&(~pink(raw))
    out[use,:3]=raw[use]
    return out,use,delta

def comp(a):
    return Image.alpha_composite(Image.new('RGBA',(a.shape[1],a.shape[0]),(45,45,45,255)),Image.fromarray(a,'RGBA'))

def main():
    OUT.mkdir(parents=True,exist_ok=True); rows=[];report={}
    for f in PICKS:
        rgba=np.asarray(Image.open(SRC/f'idle_{f:03d}.png').convert('RGBA'),np.uint8).copy()
        raw=np.asarray(Image.open(RAW/f'idle_{f:03d}.png').convert('RGB'),np.uint8)
        out,use,delta=restore(raw,rgba)
        assert np.array_equal(out[:,:,3],rgba[:,:,3])
        out[rgba[:,:,3]==0,:3]=0
        Image.fromarray(out,'RGBA').save(OUT/f'idle_{f:03d}_rgb_test.png',optimize=True)
        report[str(f)]={'rgb_pixels_restored':int(use.sum()),'alpha_pixels_changed':0,
                        'mean_delta_restored':float(delta[use].mean()) if use.any() else 0}
        rows.append((f,rgba,out,use))
    x1,y1,x2,y2=ROI;cw,ch=x2-x1,y2-y1;s=4
    sheet=Image.new('RGB',(cw*s*3,ch*s*len(rows)),(30,30,30));d=ImageDraw.Draw(sheet);y=0
    for f,b,a,use in rows:
        bb=comp(b).crop(ROI).resize((cw*s,ch*s),Image.Resampling.NEAREST)
        aa=comp(a).crop(ROI).resize((cw*s,ch*s),Image.Resampling.NEAREST)
        m=np.zeros((ch,cw,3),np.uint8);m[use[y1:y2,x1:x2]]=(255,40,40)
        mm=Image.fromarray(m).resize((cw*s,ch*s),Image.Resampling.NEAREST)
        sheet.paste(bb,(0,y));sheet.paste(aa,(cw*s,y));sheet.paste(mm,(cw*s*2,y))
        d.text((5,y+5),f'{f:03d} V15',fill='white');d.text((cw*s+5,y+5),f'{f:03d} RGB RESTORED',fill='white');d.text((cw*s*2+5,y+5),'CHANGED',fill='white');y+=ch*s
    sheet.save(OUT/'collar_solid_rgb_before_after.png',optimize=True)
    (OUT/'solid_rgb_test_report.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(report,indent=2))
if __name__=='__main__':main()
