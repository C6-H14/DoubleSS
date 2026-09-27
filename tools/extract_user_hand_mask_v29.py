from pathlib import Path
import json
import cv2
import numpy as np
from PIL import Image, ImageDraw

ROOT = Path(r"D:\StSmod\DoubleSS")
BASE = ROOT / "art_candidates/video_source/idle"
SRC = BASE / "short_blink_final_v21_solid_rgb_repair/alpha_frames/idle_073.png"
MARKED = Path(r"C:\Users\C6H14\AppData\Local\Temp\codex-clipboard-4f5fd1c9-5af9-4028-98c3-af45b3fe9264.png")
OUT = BASE / "test_hand_user_mask_v29"
VIEW = (455, 360, 570, 450)


def comp(a):
    return Image.alpha_composite(Image.new("RGBA", (a.shape[1], a.shape[0]), (45,45,45,255)), Image.fromarray(a, "RGBA"))


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    rgba = np.asarray(Image.open(SRC).convert("RGBA"), np.uint8)
    mark = np.asarray(Image.open(MARKED).convert("RGB"), np.uint8)
    # User paint is saturated red; tolerate antialiasing/compression around it.
    red = (mark[:,:,0] >= 210) & (mark[:,:,0] >= mark[:,:,1] * 1.7) & (mark[:,:,0] >= mark[:,:,2] * 1.7)
    red = cv2.morphologyEx(red.astype(np.uint8), cv2.MORPH_CLOSE, np.ones((3,3),np.uint8))
    x1,y1,x2,y2 = VIEW
    native = cv2.resize(red, (x2-x1,y2-y1), interpolation=cv2.INTER_AREA) >= 0.20
    full = np.zeros(rgba.shape[:2], bool); full[y1:y2,x1:x2] = native

    # Split disconnected user strokes for later semantic redraw.
    n, labels, stats, cents = cv2.connectedComponentsWithStats(full.astype(np.uint8), 8)
    comps=[]
    for i in range(1,n):
        if stats[i,cv2.CC_STAT_AREA] < 2: continue
        comps.append({"id":i,"area":int(stats[i,cv2.CC_STAT_AREA]),"bbox":[int(v) for v in stats[i,:4]],"centroid":[float(v) for v in cents[i]]})

    crop = np.asarray(comp(rgba).crop(VIEW).convert("RGB")).copy()
    crop[native] = np.rint(.25*crop[native] + .75*np.array([20,205,255])).astype(np.uint8)
    s=10
    before=comp(rgba).crop(VIEW).resize(((x2-x1)*s,(y2-y1)*s),Image.Resampling.NEAREST)
    overlay=Image.fromarray(crop).resize(((x2-x1)*s,(y2-y1)*s),Image.Resampling.NEAREST)
    sheet=Image.new("RGB",((x2-x1)*s*2,(y2-y1)*s),(30,30,30));sheet.paste(before,(0,0));sheet.paste(overlay,((x2-x1)*s,0))
    d=ImageDraw.Draw(sheet);d.text((8,8),"SOURCE",fill="white");d.text(((x2-x1)*s+8,8),"CYAN = USER RED STROKES MAPPED TO FRAME 73",fill="white")
    sheet.save(OUT/"frame073_user_mask_mapping_10x.png",optimize=True)
    Image.fromarray((full*255).astype(np.uint8),"L").save(OUT/"frame073_user_mask.png",optimize=True)
    report={"frame":73,"view":VIEW,"marked_size":[int(mark.shape[1]),int(mark.shape[0])],"native_mask_pixels":int(full.sum()),"components":comps}
    (OUT/"mask_report.json").write_text(json.dumps(report,indent=2)+"\n",encoding="utf-8")
    print(json.dumps(report,indent=2))

if __name__ == "__main__": main()
