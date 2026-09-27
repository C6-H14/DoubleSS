from pathlib import Path
import json
import shutil

import cv2
import numpy as np
from PIL import Image, ImageDraw
from scipy.ndimage import distance_transform_edt


ROOT = Path(r"D:\StSmod\DoubleSS")
SOURCE = ROOT / "art_candidates" / "video_source" / "idle" / "loop_v2_short_blink_180f" / "raw_frames"
OUT = ROOT / "art_candidates" / "video_source" / "idle" / "short_blink_alpha_v1"
FRAMES = OUT / "alpha_frames"
COUNT = 180


def matte(path: Path):
    rgb = np.asarray(Image.open(path).convert("RGB"), dtype=np.uint8)
    hsv = cv2.cvtColor(rgb, cv2.COLOR_RGB2HSV)
    h, s, v = cv2.split(hsv)

    # The generated background shifts from hot pink to darker magenta across
    # the canvas. Skin/hair/shoes live outside this hue range; the sword is
    # low-saturation silver, so saturation protects it.
    bg = (h >= 142) & (h <= 179) & (s >= 34) & (v >= 55)
    bg = cv2.morphologyEx(bg.astype(np.uint8), cv2.MORPH_CLOSE,
                          np.ones((3, 3), np.uint8)).astype(bool)
    fg = (~bg).astype(np.uint8)

    # Keep the principal connected subject. Tiny isolated compression flecks
    # are background; internal hair gaps remain transparent because they were
    # already classified as background.
    count, labels, stats, _ = cv2.connectedComponentsWithStats(fg, 8)
    if count <= 1:
        raise RuntimeError(f"No foreground in {path}")
    principal = 1 + int(np.argmax(stats[1:, cv2.CC_STAT_AREA]))
    fg = labels == principal

    inward = cv2.distanceTransform(fg.astype(np.uint8), cv2.DIST_L2, 5)
    alpha = np.uint8(np.clip(inward / 1.15, 0.0, 1.0) * 255)
    alpha[~fg] = 0
    alpha[alpha > 248] = 255

    # Edge decontamination: preserve geometry and alpha, replace only RGB on
    # antialiased contour pixels with the nearest clean opaque subject color.
    opaque = (alpha == 255) & ~((h >= 135) & (h <= 179) & (s >= 24))
    _, nearest = distance_transform_edt(~opaque, return_indices=True)
    clean = rgb.copy()
    contour = (alpha > 0) & (alpha < 255)
    clean[contour] = rgb[nearest[0], nearest[1]][contour]

    # Generated footage can bake magenta into fully opaque contour texels.
    # Alpha-only cleanup cannot catch those. Replace their RGB (not alpha) in
    # the outer four pixels with the nearest verified non-magenta body texel.
    outer4 = fg & (inward <= 4.2)
    baked_magenta = outer4 & (h >= 140) & (h <= 179) & (s >= 24)
    clean[baked_magenta] = rgb[nearest[0], nearest[1]][baked_magenta]

    # Final one-pixel pink-fringe guard. This never touches interior pixels.
    edge = fg & (inward <= 1.2)
    pink_edge = edge & (h >= 148) & (h <= 176) & (s >= 48)
    alpha[pink_edge] = 0
    clean[alpha == 0] = 0
    return Image.fromarray(np.dstack((clean, alpha)), "RGBA"), int(np.count_nonzero(pink_edge))


def contact(frames, name, color, picks):
    cols, cw, ch = 6, 240, 205
    sheet = Image.new("RGB", (cols * cw, 5 * ch), (18, 18, 18))
    draw = ImageDraw.Draw(sheet)
    for n, index in enumerate(picks):
        view = Image.alpha_composite(Image.new("RGBA", frames[index].size, color), frames[index])
        thumb = view.resize((240, 180), Image.Resampling.LANCZOS).convert("RGB")
        x, y = (n % cols) * cw, (n // cols) * ch
        sheet.paste(thumb, (x, y + 25))
        draw.text((x + 4, y + 4), f"{index + 1:03d}  {index/60:.2f}s", fill="white")
    sheet.save(OUT / f"idle_{name}_contact.jpg", quality=97)


def main():
    if OUT.exists():
        shutil.rmtree(OUT)
    FRAMES.mkdir(parents=True)
    paths = sorted(SOURCE.glob("idle_*.png"))
    if len(paths) != COUNT:
        raise RuntimeError(f"Expected {COUNT} frames, found {len(paths)}")
    frames, trimmed = [], []
    for i, path in enumerate(paths, 1):
        frame, pixels = matte(path)
        frame.save(FRAMES / f"idle_{i:03d}.png", optimize=True)
        frames.append(frame)
        trimmed.append(pixels)

    picks = np.linspace(0, COUNT - 1, 30).round().astype(int).tolist()
    colors = {
        "black": (0, 0, 0, 255), "white": (255, 255, 255, 255),
        "gray": (127, 127, 127, 255), "red": (255, 0, 0, 255),
        "green": (0, 255, 0, 255), "blue": (0, 0, 255, 255),
    }
    for name, color in colors.items():
        contact(frames, name, color, picks)

    for name, color in (("black", colors["black"]), ("white", colors["white"])):
        views = [Image.alpha_composite(Image.new("RGBA", f.size, color), f)
                 .resize((533, 400), Image.Resampling.LANCZOS).convert("RGB") for f in frames]
        views[0].save(OUT / f"idle_{name}_3s_60fps.gif", save_all=True,
                      append_images=views[1:], duration=17, loop=0, disposal=2, optimize=False)

    report = {"frames": COUNT, "fps": 60, "duration": 3.0,
              "pink_edge_pixels_trimmed_total": sum(trimmed),
              "pink_edge_pixels_trimmed_max_frame": max(trimmed),
              "review_frame_indices": [i + 1 for i in picks]}
    (OUT / "matte_report.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    main()
