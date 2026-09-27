from pathlib import Path
import json
import shutil

import cv2
import numpy as np
from PIL import Image, ImageDraw
from scipy.ndimage import distance_transform_edt


ROOT = Path(r"D:\StSmod\DoubleSS")
SOURCE = ROOT / "art_candidates" / "video_source" / "idle" / "short_blink_alpha_v1" / "alpha_frames"
OUT = ROOT / "art_candidates" / "video_source" / "idle" / "short_blink_alpha_v2_nearest_color"
FRAMES = OUT / "alpha_frames"
COUNT = 180


def replace_outline(path: Path):
    rgba = np.asarray(Image.open(path).convert("RGBA"), dtype=np.uint8).copy()
    rgb = rgba[:, :, :3]
    alpha = rgba[:, :, 3]
    fg = alpha > 0
    inward = cv2.distanceTransform(fg.astype(np.uint8), cv2.DIST_L2, 5)

    # The main-color donor starts at least 3.6 px inside the silhouette. This
    # deliberately ignores every baked fringe texel, regardless of hue or
    # saturation. Alpha is never edited.
    donor = fg & (inward >= 3.6) & (alpha >= 245)
    if not np.any(donor):
        raise RuntimeError(f"No donor pixels in {path}")
    _, nearest = distance_transform_edt(~donor, return_indices=True)
    outline = fg & (inward <= 2.4)
    rgb[outline] = rgb[nearest[0], nearest[1]][outline]
    rgb[~fg] = 0
    return Image.fromarray(rgba, "RGBA"), int(np.count_nonzero(outline))


def make_contact(frames, name, color, picks):
    cols, cw, ch = 6, 240, 205
    sheet = Image.new("RGB", (cols * cw, 5 * ch), (18, 18, 18))
    draw = ImageDraw.Draw(sheet)
    for n, index in enumerate(picks):
        view = Image.alpha_composite(Image.new("RGBA", frames[index].size, color), frames[index])
        thumb = view.resize((240, 180), Image.Resampling.LANCZOS).convert("RGB")
        x, y = (n % cols) * cw, (n // cols) * ch
        sheet.paste(thumb, (x, y + 25))
        draw.text((x + 4, y + 4), f"{index + 1:03d}  {index/60:.2f}s", fill="white")
    sheet.save(OUT / f"idle_{name}_contact.jpg", quality=98)


def make_head_zoom(frames, picks):
    # Full-resolution crop around hair, ornaments, face and shoulders.
    crop_box = (330, 65, 855, 500)
    cell_w, cell_h = 630, 555
    sheet = Image.new("RGB", (cell_w, len(picks) * cell_h), (45, 45, 45))
    draw = ImageDraw.Draw(sheet)
    for row, index in enumerate(picks):
        view = Image.alpha_composite(Image.new("RGBA", frames[index].size, (45, 45, 45, 255)), frames[index])
        crop = view.crop(crop_box).resize((630, 522), Image.Resampling.NEAREST).convert("RGB")
        y = row * cell_h
        sheet.paste(crop, (0, y + 33))
        draw.text((8, y + 8), f"frame {index + 1:03d} / {index/60:.2f}s / nearest-neighbor 120%", fill="white")
    sheet.save(OUT / "idle_head_outline_zoom_darkgray.png", optimize=True)


def main():
    if OUT.exists():
        shutil.rmtree(OUT)
    FRAMES.mkdir(parents=True)
    paths = sorted(SOURCE.glob("idle_*.png"))
    if len(paths) != COUNT:
        raise RuntimeError(f"Expected {COUNT} frames, found {len(paths)}")
    frames, changed = [], []
    alpha_mismatches = 0
    for i, path in enumerate(paths, 1):
        before = np.asarray(Image.open(path).convert("RGBA"), dtype=np.uint8)
        frame, pixels = replace_outline(path)
        after = np.asarray(frame, dtype=np.uint8)
        alpha_mismatches += int(np.count_nonzero(before[:, :, 3] != after[:, :, 3]))
        frame.save(FRAMES / f"idle_{i:03d}.png", optimize=True)
        frames.append(frame)
        changed.append(pixels)

    picks = np.linspace(0, COUNT - 1, 30).round().astype(int).tolist()
    for name, color in {
        "black": (0, 0, 0, 255), "white": (255, 255, 255, 255),
        "gray": (127, 127, 127, 255), "green": (0, 255, 0, 255),
    }.items():
        make_contact(frames, name, color, picks)
    make_head_zoom(frames, [0, 59, 111, 148, 179])

    for name, color in (("black", (0, 0, 0, 255)), ("gray", (45, 45, 45, 255))):
        views = [Image.alpha_composite(Image.new("RGBA", f.size, color), f)
                 .resize((533, 400), Image.Resampling.LANCZOS).convert("RGB") for f in frames]
        views[0].save(OUT / f"idle_{name}_3s_60fps.gif", save_all=True,
                      append_images=views[1:], duration=17, loop=0, disposal=2, optimize=False)

    report = {"frames": COUNT, "fps": 60, "duration": 3.0,
              "outline_rgb_pixels_replaced_total": sum(changed),
              "alpha_mismatch_pixels": alpha_mismatches,
              "geometry_changed": alpha_mismatches != 0}
    (OUT / "nearest_color_report.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    main()
