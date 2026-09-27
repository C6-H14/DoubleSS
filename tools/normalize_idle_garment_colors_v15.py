from pathlib import Path
import json
import shutil

import cv2
import numpy as np
from PIL import Image, ImageDraw


ROOT = Path(r"D:\StSmod\DoubleSS")
SOURCE = ROOT / "art_candidates" / "video_source" / "idle" / "short_blink_final_v14_smooth_underlay" / "alpha_frames"
OUT = ROOT / "art_candidates" / "video_source" / "idle" / "short_blink_final_v15_garment_color_stable"
FRAMES = OUT / "alpha_frames"


def masks(rgba):
    rgb = rgba[:, :, :3]
    alpha = rgba[:, :, 3]
    hsv = cv2.cvtColor(rgb, cv2.COLOR_RGB2HSV)
    yy, _ = np.indices(alpha.shape)
    teal_core = ((alpha > 245) & (hsv[:, :, 0] >= 75) & (hsv[:, :, 0] <= 105) &
                 (hsv[:, :, 1] >= 45) & (hsv[:, :, 2] >= 25) &
                 (hsv[:, :, 2] <= 175) & (yy > 220) & (yy < 610))
    teal_broad = ((alpha > 0) & (hsv[:, :, 0] >= 68) & (hsv[:, :, 0] <= 112) &
                  (hsv[:, :, 1] >= 20) & (hsv[:, :, 2] <= 195) &
                  (yy > 215) & (yy < 620))
    shoe_core = ((alpha > 245) & (hsv[:, :, 0] >= 3) & (hsv[:, :, 0] <= 25) &
                 (hsv[:, :, 1] >= 45) & (hsv[:, :, 2] >= 25) &
                 (hsv[:, :, 2] <= 175) & (yy > 650))
    shoe_broad = ((alpha > 0) & (hsv[:, :, 0] >= 0) & (hsv[:, :, 0] <= 30) &
                  (hsv[:, :, 1] >= 20) & (hsv[:, :, 2] <= 195) & (yy > 630))
    teal_weight = np.maximum(teal_core.astype(np.float32),
                             cv2.GaussianBlur(teal_core.astype(np.float32), (0,0), 1.1) * teal_broad)
    shoe_weight = np.maximum(shoe_core.astype(np.float32),
                             cv2.GaussianBlur(shoe_core.astype(np.float32), (0,0), 1.1) * shoe_broad)
    return teal_core, shoe_core, np.clip(teal_weight,0,1), np.clip(shoe_weight,0,1)


def median_lab(rgba, mask):
    lab = cv2.cvtColor(rgba[:, :, :3], cv2.COLOR_RGB2LAB)
    return np.median(lab[mask], axis=0).astype(np.float32)


def apply_delta(rgba, weight, delta_uint_lab):
    rgbf = rgba[:, :, :3].astype(np.float32) / 255.0
    lab = cv2.cvtColor(rgbf, cv2.COLOR_RGB2LAB)
    delta = np.array([delta_uint_lab[0] * 100.0 / 255.0,
                      delta_uint_lab[1], delta_uint_lab[2]], np.float32)
    lab += weight[:, :, None] * delta[None, None, :]
    lab[:, :, 0] = np.clip(lab[:, :, 0], 0, 100)
    converted = cv2.cvtColor(lab, cv2.COLOR_LAB2RGB)
    rgba[:, :, :3] = np.clip(np.rint(converted * 255), 0, 255).astype(np.uint8)


def main():
    if OUT.exists():
        shutil.rmtree(OUT)
    FRAMES.mkdir(parents=True)
    paths = sorted(SOURCE.glob("idle_*.png"))
    arrays = [np.asarray(Image.open(p).convert("RGBA"), dtype=np.uint8).copy() for p in paths]
    before = []
    all_masks = []
    for rgba in arrays:
        tm, sm, tw, sw = masks(rgba)
        before.append((median_lab(rgba, tm), median_lab(rgba, sm)))
        all_masks.append((tm, sm, tw, sw))
    stable = before[15:165]
    teal_target = np.median(np.stack([x[0] for x in stable]), axis=0)
    shoe_target = np.median(np.stack([x[1] for x in stable]), axis=0)

    images, rows = [], []
    for i, (rgba, (tm,sm,tw,sw)) in enumerate(zip(arrays, all_masks), 1):
        teal_before, shoe_before = before[i-1]
        apply_delta(rgba, tw, teal_target - teal_before)
        apply_delta(rgba, sw, shoe_target - shoe_before)
        tm2, sm2, _, _ = masks(rgba)
        teal_after = median_lab(rgba, tm2)
        shoe_after = median_lab(rgba, sm2)
        image = Image.fromarray(rgba, "RGBA")
        image.save(FRAMES / f"idle_{i:03d}.png", optimize=True)
        images.append(image)
        rows.append({"frame":i,"teal_before":teal_before.tolist(),"teal_after":teal_after.tolist(),
                     "shoe_before":shoe_before.tolist(),"shoe_after":shoe_after.tolist(),
                     "teal_pixels":int(tm.sum()),"shoe_pixels":int(sm.sum())})

    for name,color in (("game_dark",(36,25,25,255)),("black",(0,0,0,255))):
        views=[Image.alpha_composite(Image.new("RGBA",im.size,color),im).resize((533,400),Image.Resampling.LANCZOS).convert("RGB") for im in images]
        views[0].save(OUT/f"idle_{name}_3s_60fps.gif",save_all=True,append_images=views[1:],duration=17,loop=0,disposal=2,optimize=False)

    picks=[1,16,60,120,165,180]
    sheet=Image.new("RGB",(556*3,417*2),(30,30,30));draw=ImageDraw.Draw(sheet)
    for k,n in enumerate(picks):
        rgba=arrays[n-1];tm,sm,_,_=all_masks[n-1]
        overlay=rgba.copy(); overlay[tm,:3]=[0,255,255];overlay[sm,:3]=[255,0,255]
        im=Image.fromarray(overlay,"RGBA").resize((556,417),Image.Resampling.LANCZOS)
        bg=Image.new("RGBA",im.size,(40,40,40,255));bg.alpha_composite(im)
        x,y=(k%3)*556,(k//3)*417;sheet.paste(bg.convert("RGB"),(x,y));draw.text((x+5,y+5),f"frame {n}: cyan=clothes magenta=shoes",fill="white")
    sheet.save(OUT/"garment_mask_audit.png",optimize=True)
    report={"teal_target":teal_target.tolist(),"shoe_target":shoe_target.tolist(),"rows":rows}
    (OUT/"garment_color_report.json").write_text(json.dumps(report,indent=2)+"\n",encoding="utf-8")
    print(json.dumps({"teal_target":teal_target.tolist(),"shoe_target":shoe_target.tolist(),
                      "frame1_after":rows[0],"frame180_after":rows[-1]},indent=2))


if __name__ == "__main__":
    main()
