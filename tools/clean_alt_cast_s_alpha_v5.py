from pathlib import Path
import json

import cv2
import numpy as np
from PIL import Image, ImageDraw


ROOT = Path(r"D:\StSmod\art_preview\alternate_fencer_cast_s")
SOURCE = ROOT / "cast_s_v4_alpha_45f_60fps" / "alpha_frames_45f_60fps"
OUT = ROOT / "cast_s_v5_alpha_clean_45f_60fps"
FRAMES = OUT / "alpha_frames_45f_60fps"


def clean(rgba: np.ndarray, frame_no: int) -> tuple[np.ndarray, dict]:
    result = rgba.copy()
    rgb = result[:, :, :3].astype(np.int16)
    r, g, b = rgb[:, :, 0], rgb[:, :, 1], rgb[:, :, 2]
    a = result[:, :, 3].copy()
    height, width = a.shape
    xx = np.broadcast_to(np.arange(width), (height, width))

    # Purple video background mixed by H.264 into thin flames. Real flame
    # pixels are warm: red >= green > blue; remove magenta/purple admixture.
    purple = (r > g + 6) & (b > g + 6)
    subject_purple = (a > 0) & purple & (xx < 760)
    # The push/explosion happens from output frame 27 onward. Its right-hand
    # region contains only the fire effect, so warm color is a safe constraint.
    fire_region = (xx >= 760) & (frame_no >= 27)
    warm_fire = (r > g - 8) & (r > b + 12) & (r > 90) & (g > 42)
    # The bright blast has a red inner core. Keep its red/orange pixels even
    # where compression has pulled the blue channel slightly toward purple.
    core_restore = None
    if 27 <= frame_no <= 32:
        yy = np.broadcast_to(np.arange(height)[:, None], (height, width))
        hotspot = (xx > 765) & (xx < 850) & (yy > 300) & (yy < 430) & (r > 220) & (g > 140) & (g > b)
        if np.any(hotspot):
            cy = float(np.mean(yy[hotspot]))
            cx = float(np.mean(xx[hotspot]))
            core = ((xx - cx) ** 2 + (yy - cy) ** 2) < 82 ** 2
            core_red = core & (r > g + 22) & (r > b + 12) & (r > 90) & (g > 40)
            warm_fire |= core_red
            bright_core = (r >= g - 8) & (g >= b + 10) & (r > 135) & (g > 100)
            core_restore = core & (a > 0) & (((r > g + 8) & (r > 75)) | bright_core)
    fire_false = (a > 0) & fire_region & ~warm_fire
    result[:, :, 3][subject_purple | fire_false] = 0
    if core_restore is not None:
        result[:, :, 3][core_restore] = a[core_restore]
        # Correct the violet H.264 spill inside the blast without carving a
        # hole into its red core.
        blue = result[:, :, 2]
        contaminated = core_restore & (b > g + 5)
        blue[contaminated] = np.minimum(blue[contaminated], np.clip(g[contaminated] * 0.8, 0, 255).astype(np.uint8))

    # One-pixel magenta edge chips at the flame boundary can still survive a
    # hard color key. Pull their RGB from the nearest truly warm inner pixel.
    accepted_colors = warm_fire | core_restore if core_restore is not None else warm_fire
    accepted = (result[:, :, 3] > 0) & accepted_colors & fire_region
    if np.any(accepted):
        outside = ((result[:, :, 3] > 0) & fire_region & ~accepted)
        result[:, :, 3][outside] = 0

    # Soften the dim red compression fringe around the blast. Bright flame
    # cores remain opaque; weak warm pixels taper instead of forming a hard,
    # speckled silhouette against a dark game background.
    glow = (result[:, :, 3] > 0) & fire_region
    contrast = np.clip((r - b - 8).astype(np.float32) / 58.0, 0.0, 1.0)
    bright = (r > 220) & (g > 180) & (g > b + 8)
    contrast[bright] = 1.0
    result[:, :, 3][glow] = np.minimum(result[:, :, 3][glow],
                                       np.rint(255 * contrast[glow]).astype(np.uint8))
    spill = glow & (b > g - 4) & ~bright
    result[:, :, 2][spill] = np.minimum(result[:, :, 2][spill],
                                        np.clip(g[spill] * 0.84, 0, 255).astype(np.uint8))

    # Clear hidden RGB for clean compositing in games and image editors.
    result[:, :, :3][result[:, :, 3] == 0] = 0
    return result, {
        "frame": frame_no,
        "removed_subject_purple": int(subject_purple.sum()),
        "removed_fire_false_color": int(fire_false.sum()),
    }


def contact(frames: list[Image.Image], path: Path, background: tuple[int, int, int, int]) -> None:
    picks = np.linspace(0, len(frames) - 1, 20).round().astype(int)
    tw, th, cols = 256, 192, 5
    sheet = Image.new("RGB", (tw * cols, (th + 23) * 4), (25, 25, 25))
    draw = ImageDraw.Draw(sheet)
    for slot, idx in enumerate(picks):
        view = Image.alpha_composite(Image.new("RGBA", frames[idx].size, background), frames[idx])
        thumb = view.resize((tw, th), Image.Resampling.LANCZOS).convert("RGB")
        x, y = slot % cols * tw, slot // cols * (th + 23)
        sheet.paste(thumb, (x, y + 23))
        draw.text((x + 4, y + 3), f"frame {idx + 1:02d}", fill="white")
    sheet.save(path, quality=95)


def main() -> None:
    paths = sorted(SOURCE.glob("*.png"))
    if len(paths) != 45:
        raise RuntimeError(f"Expected 45 frames; found {len(paths)}")
    FRAMES.mkdir(parents=True, exist_ok=True)
    images = []
    report = []
    for frame_no, path in enumerate(paths, 1):
        src = np.array(Image.open(path).convert("RGBA"))
        cleaned, stats = clean(src, frame_no)
        img = Image.fromarray(cleaned, "RGBA")
        img.save(FRAMES / f"cast_s_alpha_{frame_no:03d}.png", optimize=True)
        images.append(img)
        report.append(stats)

    for name, background in {
        "black": (0, 0, 0, 255),
        "white": (255, 255, 255, 255),
        "gray": (127, 127, 127, 255),
    }.items():
        contact(images, OUT / f"contact_{name}.jpg", background)
    gif_frames = [img.resize((640, 480), Image.Resampling.LANCZOS) for img in images]
    gif_frames[0].save(OUT / "cast_s_alpha_clean_45f_60fps.gif", save_all=True,
                       append_images=gif_frames[1:], duration=17, loop=0, disposal=2)
    summary = {
        "frames": len(images), "fps": 60, "duration_seconds": 0.75,
        "removed_subject_purple_total": sum(x["removed_subject_purple"] for x in report),
        "removed_fire_false_color_total": sum(x["removed_fire_false_color"] for x in report),
        "per_frame": report,
    }
    (OUT / "cleanup_report.json").write_text(json.dumps(summary, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({k: v for k, v in summary.items() if k != "per_frame"}, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
