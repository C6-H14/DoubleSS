from pathlib import Path
import json

import cv2
import numpy as np
from PIL import Image, ImageDraw


ROOT = Path(r"D:\StSmod\DoubleSS")
SOURCE = ROOT / "art_candidates" / "video_source"
OUT = ROOT / "art_candidates" / "matting_tests" / "latest_magenta_inspection"


def main():
    videos = sorted(SOURCE.glob("*.mp4"), key=lambda p: p.stat().st_mtime, reverse=True)
    if not videos:
        raise RuntimeError(f"No MP4 files in {SOURCE}")
    video = videos[0]
    cap = cv2.VideoCapture(str(video))
    fps = float(cap.get(cv2.CAP_PROP_FPS))
    count = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
    width = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
    height = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
    frames = []
    while True:
        ok, bgr = cap.read()
        if not ok:
            break
        frames.append(cv2.cvtColor(bgr, cv2.COLOR_BGR2RGB))
    if not frames:
        raise RuntimeError(f"Cannot decode {video}")

    OUT.mkdir(parents=True, exist_ok=True)
    picks = np.linspace(0, len(frames) - 1, 12).round().astype(int)
    thumbs = []
    metrics = []
    for index, rgb in enumerate(frames):
        border = np.concatenate((rgb[0], rgb[-1], rgb[:, 0], rgb[:, -1]), axis=0)
        bg = np.median(border, axis=0)
        distance = np.linalg.norm(rgb.astype(np.float32) - bg.reshape(1, 1, 3), axis=2)
        foreground = distance > 55
        n, labels, stats, _ = cv2.connectedComponentsWithStats(foreground.astype(np.uint8), 8)
        keep = np.zeros_like(foreground)
        for label in range(1, n):
            if stats[label, cv2.CC_STAT_AREA] >= 30:
                keep |= labels == label
        ys, xs = np.where(keep)
        bbox = None if len(xs) == 0 else [int(xs.min()), int(ys.min()), int(xs.max()+1), int(ys.max()+1)]
        metrics.append({
            "frame": index,
            "background_rgb": [round(float(x), 2) for x in bg],
            "border_std": [round(float(x), 2) for x in border.std(axis=0)],
            "bbox": bbox,
            "foreground_fraction": round(float(keep.mean()), 6),
        })

    for index in picks:
        im = Image.fromarray(frames[index], "RGB")
        im.thumbnail((384, 288), Image.Resampling.LANCZOS)
        canvas = Image.new("RGB", (384, 320), (24, 24, 24))
        canvas.paste(im, ((384 - im.width)//2, 28))
        draw = ImageDraw.Draw(canvas)
        draw.text((8, 7), f"frame {index:03d}  {index/fps:.3f}s", fill=(255,255,255))
        thumbs.append(canvas)
        im.save(OUT / f"sample_{index:03d}.png")
    sheet = Image.new("RGB", (4*384, 3*320), (16,16,16))
    for i, im in enumerate(thumbs):
        sheet.paste(im, ((i%4)*384, (i//4)*320))
    sheet.save(OUT / "contact_sheet.png")

    bboxes = [m["bbox"] for m in metrics if m["bbox"]]
    centers_x = [(b[0]+b[2])/2 for b in bboxes]
    centers_y = [(b[1]+b[3])/2 for b in bboxes]
    widths = [b[2]-b[0] for b in bboxes]
    heights = [b[3]-b[1] for b in bboxes]
    summary = {
        "video": str(video), "fps": fps, "frames": len(frames),
        "duration": len(frames)/fps, "size": [width,height],
        "background_rgb_median": np.median([m["background_rgb"] for m in metrics], axis=0).round(2).tolist(),
        "border_std_median": np.median([m["border_std"] for m in metrics], axis=0).round(2).tolist(),
        "bbox_center_x_range": [round(min(centers_x),2),round(max(centers_x),2)],
        "bbox_center_y_range": [round(min(centers_y),2),round(max(centers_y),2)],
        "bbox_width_range": [min(widths),max(widths)],
        "bbox_height_range": [min(heights),max(heights)],
    }
    (OUT / "inspection.json").write_text(json.dumps({"summary":summary,"frames":metrics},ensure_ascii=False,indent=2),encoding="utf-8")
    print(json.dumps(summary,ensure_ascii=False,indent=2))


if __name__ == "__main__":
    main()
