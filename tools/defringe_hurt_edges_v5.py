from pathlib import Path
import json
import shutil

import cv2
import numpy as np
from PIL import Image, ImageDraw
from scipy.ndimage import distance_transform_edt


ROOT = Path(r"D:\StSmod\DoubleSS")
SOURCE = ROOT / "art_candidates" / "video_source" / "hurt" / "final_v4_42f_700ms" / "alpha_frames"
OUT = ROOT / "art_candidates" / "video_source" / "hurt" / "final_v5_42f_defringed"
FRAMES = OUT / "alpha_frames"


def clean_frame(path: Path):
    rgba = np.asarray(Image.open(path).convert("RGBA"), dtype=np.uint8).copy()
    rgb, alpha = rgba[:, :, :3], rgba[:, :, 3]
    fg = alpha > 0
    inward = cv2.distanceTransform(fg.astype(np.uint8), cv2.DIST_L2, 5)
    edge2 = fg & (inward <= 2.2)
    edge4 = fg & (inward <= 4.2)

    hsv = cv2.cvtColor(rgb, cv2.COLOR_RGB2HSV)
    h, s, v = cv2.split(hsv)
    pink = (h >= 140) & (h <= 179) & (s >= 28) & (v >= 65)
    strong_pink = (h >= 148) & (h <= 176) & (s >= 48) & (v >= 85)

    # Trim only strongly pink pixels on the outer two-pixel contour.
    trim = edge2 & strong_pink
    alpha[trim] = 0

    # On the next two pixels, retain the exact alpha/shape but replace only
    # pink-contaminated RGB with the nearest uncontaminated opaque body color.
    safe = (alpha >= 245) & ~pink
    if np.any(safe):
        _, nearest = distance_transform_edt(~safe, return_indices=True)
        recolor = edge4 & pink & (alpha > 0)
        rgb[recolor] = rgb[nearest[0], nearest[1]][recolor]

    rgb[alpha == 0] = 0
    result = Image.fromarray(np.dstack((rgb, alpha)), "RGBA")
    return result, int(np.count_nonzero(edge4 & pink)), int(np.count_nonzero(trim))


def make_preview(frames, name, color):
    views = [Image.alpha_composite(Image.new("RGBA", f.size, color), f)
             .resize((533, 400), Image.Resampling.LANCZOS).convert("RGB") for f in frames]
    views[0].save(OUT / f"hurt_{name}_42f_450ms.gif", save_all=True,
                  append_images=views[1:], duration=11, loop=0, disposal=2, optimize=False)
    cols, cw, ch = 7, 190, 160
    sheet = Image.new("RGB", (cols * cw, 6 * ch), (18, 18, 18))
    draw = ImageDraw.Draw(sheet)
    for i, view in enumerate(views, 1):
        thumb = view.resize((190, 143), Image.Resampling.LANCZOS)
        x, y = ((i - 1) % cols) * cw, ((i - 1) // cols) * ch
        sheet.paste(thumb, (x, y + 17))
        draw.text((x + 3, y + 2), f"{i:02d}", fill="white")
    sheet.save(OUT / f"hurt_{name}_contact.jpg", quality=96)


def main():
    if OUT.exists():
        shutil.rmtree(OUT)
    FRAMES.mkdir(parents=True)
    paths = sorted(SOURCE.glob("hurt_*.png"))
    if len(paths) != 42:
        raise RuntimeError(f"Expected 42 frames, found {len(paths)}")
    frames, stats = [], []
    for i, path in enumerate(paths, 1):
        frame, pink_before, trimmed = clean_frame(path)
        frame.save(FRAMES / f"hurt_{i:03d}.png", optimize=True)
        frames.append(frame)
        stats.append({"frame": i, "pink_edge_candidates": pink_before, "trimmed_pixels": trimmed})
    make_preview(frames, "black", (0, 0, 0, 255))
    make_preview(frames, "white", (255, 255, 255, 255))
    make_preview(frames, "gray", (127, 127, 127, 255))
    summary = {"frames": 42, "duration": 0.45,
               "trimmed_pixels_total": sum(s["trimmed_pixels"] for s in stats),
               "per_frame": stats}
    (OUT / "cleanup_report.json").write_text(json.dumps(summary, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({k: v for k, v in summary.items() if k != "per_frame"}, indent=2))


if __name__ == "__main__":
    main()
