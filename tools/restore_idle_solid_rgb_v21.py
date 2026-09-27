from pathlib import Path
import json
import shutil

import cv2
import numpy as np
from PIL import Image, ImageDraw

ROOT = Path(r"D:\StSmod\DoubleSS")
BASE = ROOT / "art_candidates/video_source/idle"
SOURCE = BASE / "short_blink_final_v15_garment_color_stable/alpha_frames"
RAW = BASE / "loop_v2_short_blink_180f/raw_frames"
OUT = BASE / "short_blink_final_v21_solid_rgb_repair"
FRAMES = OUT / "alpha_frames"
COUNT = 180
DELTA_E = 10.0


def pink(rgb):
    hsv = cv2.cvtColor(rgb, cv2.COLOR_RGB2HSV)
    return ((hsv[:, :, 0] >= 140) & (hsv[:, :, 0] <= 179)
            & (hsv[:, :, 1] >= 45) & (hsv[:, :, 2] >= 45))


def repair(raw, rgba):
    out = rgba.copy()
    lab_raw = cv2.cvtColor(raw, cv2.COLOR_RGB2LAB).astype(np.float32)
    lab_cur = cv2.cvtColor(rgba[:, :, :3], cv2.COLOR_RGB2LAB).astype(np.float32)
    delta = np.linalg.norm(lab_raw - lab_cur, axis=2)
    candidate = (rgba[:, :, 3] >= 224) & (~pink(raw)) & (delta >= DELTA_E)
    # Preserve line-shaped details while rejecting lone codec specks.
    neighbours = cv2.filter2D(candidate.astype(np.uint8), -1, np.ones((3, 3), np.uint8))
    use = candidate & (neighbours >= 2)
    out[use, :3] = raw[use]
    out[rgba[:, :, 3] == 0, :3] = 0
    if not np.array_equal(out[:, :, 3], rgba[:, :, 3]):
        raise RuntimeError("alpha changed")
    return out, use, delta


def composite(a, color):
    return Image.alpha_composite(Image.new("RGBA", (a.shape[1], a.shape[0]), color), Image.fromarray(a, "RGBA"))


def main():
    if OUT.exists():
        shutil.rmtree(OUT)
    FRAMES.mkdir(parents=True)
    srcs = sorted(SOURCE.glob("idle_*.png"))
    raws = sorted(RAW.glob("idle_*.png"))
    if len(srcs) != COUNT or len(raws) != COUNT:
        raise RuntimeError(f"expected {COUNT}, found {len(srcs)}/{len(raws)}")
    frames, masks, rows = [], [], []
    for index, (sp, rp) in enumerate(zip(srcs, raws), 1):
        before = np.asarray(Image.open(sp).convert("RGBA"), np.uint8).copy()
        raw = np.asarray(Image.open(rp).convert("RGB"), np.uint8)
        after, use, delta = repair(raw, before)
        image = Image.fromarray(after, "RGBA")
        image.save(FRAMES / f"idle_{index:03d}.png", optimize=True)
        frames.append(image); masks.append(use)
        rows.append({"frame": index, "rgb_pixels_restored": int(use.sum()),
                     "alpha_pixels_changed": 0,
                     "mean_delta_restored": float(delta[use].mean()) if use.any() else 0.0})

    for name, color in (("game_dark", (36,25,25,255)), ("gray", (45,45,45,255)),
                        ("white", (235,235,235,255))):
        views = [composite(np.asarray(im), color).resize((533,400), Image.Resampling.LANCZOS).convert("RGB") for im in frames]
        views[0].save(OUT / f"idle_{name}_3s_60fps.gif", save_all=True,
                      append_images=views[1:], duration=17, loop=0, disposal=2, optimize=False)

    picks = [1,16,41,57,81,101,120,151,180]
    sample_h, sample_w = np.asarray(frames[0]).shape[:2]
    crop = (250,35,min(975,sample_w),min(840,sample_h))
    cw,ch = crop[2]-crop[0], crop[3]-crop[1]
    thumb=(360,400)
    sheet=Image.new("RGB",(thumb[0]*3,thumb[1]*len(picks)),(28,28,28));d=ImageDraw.Draw(sheet)
    for row,n in enumerate(picks):
        before=np.asarray(Image.open(srcs[n-1]).convert("RGBA"),np.uint8)
        after=np.asarray(frames[n-1],np.uint8)
        b=composite(before,(36,25,25,255)).crop(crop).resize(thumb,Image.Resampling.LANCZOS)
        a=composite(after,(36,25,25,255)).crop(crop).resize(thumb,Image.Resampling.LANCZOS)
        m=np.zeros((ch,cw,3),np.uint8);m[masks[n-1][crop[1]:crop[3],crop[0]:crop[2]]]=(255,45,45)
        mi=Image.fromarray(m).resize(thumb,Image.Resampling.NEAREST)
        y=row*thumb[1];sheet.paste(b,(0,y));sheet.paste(a,(thumb[0],y));sheet.paste(mi,(thumb[0]*2,y))
        d.text((5,y+4),f"{n:03d} V15",fill="white");d.text((thumb[0]+5,y+4),f"{n:03d} V21",fill="white");d.text((thumb[0]*2+5,y+4),"RESTORED RGB",fill="white")
    sheet.save(OUT / "v15_v21_sample_contact.png", optimize=True)

    report={"source":str(SOURCE),"frames":COUNT,"delta_e_threshold":DELTA_E,
            "alpha_pixels_changed_total":0,"rgb_pixels_restored_total":sum(r["rgb_pixels_restored"] for r in rows),
            "rows":rows}
    (OUT / "v21_report.json").write_text(json.dumps(report,indent=2)+"\n",encoding="utf-8")
    print(json.dumps({"frames":COUNT,"rgb_pixels_restored_total":report["rgb_pixels_restored_total"],
                      "min_per_frame":min(r["rgb_pixels_restored"] for r in rows),
                      "max_per_frame":max(r["rgb_pixels_restored"] for r in rows),
                      "alpha_pixels_changed_total":0},indent=2))


if __name__ == "__main__":
    main()
