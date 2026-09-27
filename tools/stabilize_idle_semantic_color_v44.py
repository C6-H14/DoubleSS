from pathlib import Path
import json
import os

import cv2
import numpy as np
from PIL import Image, ImageDraw


ROOT = Path(r"D:\StSmod\DoubleSS")
BASE = ROOT / "art_candidates/video_source/idle"
SOURCE = Path(os.environ.get(
    "IDLE_COLOR_SOURCE",
    BASE / "short_blink_final_v43_saturation_stable/alpha_frames",
))
OUT = Path(os.environ.get(
    "IDLE_COLOR_OUT",
    BASE / "short_blink_final_v44_semantic_color_stable",
))
FRAMES = OUT / "alpha_frames"
COUNT = 180


def circular_gaussian(values, sigma=3.2):
    radius = int(np.ceil(sigma * 3))
    x = np.arange(-radius, radius + 1, dtype=np.float32)
    kernel = np.exp(-(x * x) / (2 * sigma * sigma))
    kernel /= kernel.sum()
    padded = np.r_[values[-radius:], values, values[:radius]]
    return np.convolve(padded, kernel, mode="same")[radius:radius + len(values)]


def robust_mean(values):
    if values.size == 0:
        return 0.0
    lo, hi = np.percentile(values, (10, 90))
    kept = values[(values >= lo) & (values <= hi)]
    return float(kept.mean()) if kept.size else float(values.mean())


def masks_for(hsv, alpha):
    h, s, v = [hsv[:, :, i] for i in range(3)]
    solid = alpha >= 220
    return {
        "all": solid & (v >= 25) & (v <= 248),
        "teal": solid & (h >= 70) & (h <= 105) & (s >= 45) & (v >= 35) & (v <= 175),
        "hair": solid & (h >= 8) & (h <= 32) & (s >= 28) & (s <= 155) & (v >= 125),
        "skin": solid & ((h <= 14) | (h >= 175)) & (s >= 12) & (s <= 105) & (v >= 145),
        "white": solid & (s <= 42) & (v >= 145),
        "gold": solid & (h >= 10) & (h <= 35) & (s >= 70) & (v >= 95),
        "brown": solid & ((h <= 25) | (h >= 175)) & (s >= 50) & (v >= 35) & (v <= 170),
    }


def dark_preview(array):
    image = Image.fromarray(array, "RGBA")
    return Image.alpha_composite(Image.new("RGBA", image.size, (45, 45, 45, 255)), image)


def collect(frames):
    result = {name: {"s": [], "v": []} for name in ("all", "teal", "hair", "skin", "white", "gold", "brown")}
    for rgba in frames:
        hsv = cv2.cvtColor(rgba[:, :, :3], cv2.COLOR_RGB2HSV)
        masks = masks_for(hsv, rgba[:, :, 3])
        for name, mask in masks.items():
            result[name]["s"].append(robust_mean(hsv[:, :, 1][mask].astype(np.float32)))
            result[name]["v"].append(robust_mean(hsv[:, :, 2][mask].astype(np.float32)))
    return {name: {channel: np.asarray(values, np.float32) for channel, values in channels.items()} for name, channels in result.items()}


def transition_summary(stats):
    rows = []
    for name, channels in stats.items():
        for channel, values in channels.items():
            jumps = np.r_[np.diff(values), values[0] - values[-1]]
            order = np.argsort(np.abs(jumps))[::-1][:6]
            rows.append({
                "metric": f"{name}_{channel}",
                "max_abs_jump": round(float(np.max(np.abs(jumps))), 4),
                "loop_jump": round(float(jumps[-1]), 4),
                "worst_transitions": [f"{int(i + 1)}->{int((i + 1) % COUNT + 1)}" for i in order],
            })
    return rows


def main():
    FRAMES.mkdir(parents=True, exist_ok=True)
    source = [np.asarray(Image.open(SOURCE / f"idle_{i:03d}.png").convert("RGBA"), np.uint8) for i in range(1, COUNT + 1)]
    before = collect(source)
    targets = {
        name: {channel: circular_gaussian(values) for channel, values in channels.items()}
        for name, channels in before.items()
    }
    # Full-subject correction is deliberately weak; semantic regions carry the
    # correction so one changing region cannot make the entire character pulse.
    class_strength = {"all": 0.20, "teal": 0.95, "hair": 0.85, "skin": 0.85, "white": 0.75, "gold": 0.70, "brown": 0.75}
    output = []
    per_frame = []
    for index, rgba in enumerate(source):
        hsv_u8 = cv2.cvtColor(rgba[:, :, :3], cv2.COLOR_RGB2HSV)
        hsv = hsv_u8.astype(np.float32)
        masks = masks_for(hsv_u8, rgba[:, :, 3])
        sum_s = np.zeros(hsv.shape[:2], np.float32)
        sum_v = np.zeros(hsv.shape[:2], np.float32)
        sum_w = np.zeros(hsv.shape[:2], np.float32)
        corrections = {}
        for name, mask in masks.items():
            strength = class_strength[name]
            ds = float(np.clip(targets[name]["s"][index] - before[name]["s"][index], -7.0, 7.0))
            dv = float(np.clip(targets[name]["v"][index] - before[name]["v"][index], -6.0, 6.0))
            weight = mask.astype(np.float32) * strength
            sum_s += weight * ds
            sum_v += weight * dv
            sum_w += weight
            corrections[name] = {"ds": round(ds, 4), "dv": round(dv, 4)}
        visible = rgba[:, :, 3] > 0
        delta_s = np.divide(sum_s, sum_w, out=np.zeros_like(sum_s), where=sum_w > 0)
        delta_v = np.divide(sum_v, sum_w, out=np.zeros_like(sum_v), where=sum_w > 0)
        alpha_weight = rgba[:, :, 3].astype(np.float32) / 255.0
        hsv[:, :, 1] = np.clip(hsv[:, :, 1] + delta_s * alpha_weight, 0, 255)
        hsv[:, :, 2] = np.clip(hsv[:, :, 2] + delta_v * alpha_weight, 0, 255)
        rgb = cv2.cvtColor(np.rint(hsv).astype(np.uint8), cv2.COLOR_HSV2RGB)
        fixed = rgba.copy()
        fixed[visible, :3] = rgb[visible]
        Image.fromarray(fixed, "RGBA").save(FRAMES / f"idle_{index + 1:03d}.png", optimize=True)
        output.append(fixed)
        per_frame.append({"frame": index + 1, "corrections": corrections})

    after = collect(output)
    alpha_exact = all(np.array_equal(a[:, :, 3], b[:, :, 3]) for a, b in zip(source, output))
    rgb_changed_where_transparent = sum(int(np.any(a[:, :, :3] != b[:, :, :3], axis=2)[a[:, :, 3] == 0].sum()) for a, b in zip(source, output))
    report = {
        "frames": COUNT,
        "alpha_exact_all_frames": alpha_exact,
        "rgb_changes_in_fully_transparent_pixels": rgb_changed_where_transparent,
        "before_transitions": transition_summary(before),
        "after_transitions": transition_summary(after),
        "per_frame": per_frame,
    }
    (OUT / "v44_semantic_color_report.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")

    compare = []
    for i, (old, new) in enumerate(zip(source, output), 1):
        a = dark_preview(old)
        b = dark_preview(new)
        a.thumbnail((445, 334), Image.Resampling.LANCZOS)
        b.thumbnail((445, 334), Image.Resampling.LANCZOS)
        panel = Image.new("RGB", (a.width * 2, a.height + 18), (28, 28, 28))
        panel.paste(a.convert("RGB"), (0, 18))
        panel.paste(b.convert("RGB"), (a.width, 18))
        draw = ImageDraw.Draw(panel)
        draw.text((4, 2), f"V43 F{i:03d}", fill="white")
        draw.text((a.width + 4, 2), "V44 SEMANTIC STABLE", fill="white")
        compare.append(panel)
    compare[0].save(OUT / "v43_v44_compare_3s_60fps.gif", save_all=True, append_images=compare[1:], duration=17, loop=0, optimize=False, disposal=2)
    print(json.dumps({
        "alpha_exact_all_frames": alpha_exact,
        "rgb_changes_in_fully_transparent_pixels": rgb_changed_where_transparent,
        "output": str(OUT),
    }, indent=2))


if __name__ == "__main__":
    main()
