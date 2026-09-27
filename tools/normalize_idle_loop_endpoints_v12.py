from pathlib import Path
import json
import shutil

import cv2
import numpy as np
from PIL import Image


ROOT = Path(r"D:\StSmod\DoubleSS")
SOURCE = ROOT / "art_candidates" / "video_source" / "idle" / "short_blink_final_v8_bleed_color_normalized" / "alpha_frames"
OUT = ROOT / "art_candidates" / "video_source" / "idle" / "short_blink_final_v12_loop_color_stable"
FRAMES = OUT / "alpha_frames"
COUNT = 180
START_END = 15
END_START = 166


def metrics(rgba):
    solid = rgba[:, :, 3] >= 245
    inward = cv2.distanceTransform(solid.astype(np.uint8), cv2.DIST_L2, 5)
    mask = inward >= 6.0
    lab = cv2.cvtColor(rgba[:, :, :3], cv2.COLOR_RGB2LAB).astype(np.float32)
    aa = lab[:, :, 1] - 128.0
    bb = lab[:, :, 2] - 128.0
    return float(lab[:, :, 0][mask].mean()), float(np.sqrt(aa[mask] ** 2 + bb[mask] ** 2).mean())


def normalize(rgba, target_l, target_c):
    current_l, current_c = metrics(rgba)
    subject = rgba[:, :, 3] > 0
    # Float Lab avoids the ~1 L* quantization loss caused by an otherwise
    # no-op uint8 RGB -> Lab -> RGB round trip.
    rgb_float = rgba[:, :, :3].astype(np.float32) / 255.0
    lab = cv2.cvtColor(rgb_float, cv2.COLOR_RGB2LAB)
    l_delta = (target_l - current_l) * (100.0 / 255.0)
    lab[:, :, 0][subject] = np.clip(lab[:, :, 0][subject] + l_delta, 0.0, 100.0)
    scale = target_c / max(current_c, 1e-6)
    for ch in (1, 2):
        lab[:, :, ch][subject] = lab[:, :, ch][subject] * scale
    converted = cv2.cvtColor(lab, cv2.COLOR_LAB2RGB)
    rgba[:, :, :3] = np.clip(np.rint(converted * 255.0), 0, 255).astype(np.uint8)


def smoothstep(t):
    return t * t * (3.0 - 2.0 * t)


def main():
    if OUT.exists():
        shutil.rmtree(OUT)
    FRAMES.mkdir(parents=True)
    paths = sorted(SOURCE.glob("idle_*.png"))
    arrays = [np.asarray(Image.open(p).convert("RGBA"), dtype=np.uint8).copy() for p in paths]
    before = [metrics(a) for a in arrays]
    # Use the stable colors immediately outside the correction zones. Both loop
    # endpoints converge to their average, so frame 180 -> frame 1 has no flash.
    stable_left = before[START_END]          # frame 16
    stable_right = before[END_START - 2]     # frame 165
    loop_target = ((stable_left[0] + stable_right[0]) / 2.0,
                   (stable_left[1] + stable_right[1]) / 2.0)

    rows, images = [], []
    for index, rgba in enumerate(arrays, 1):
        target_l, target_c = before[index - 1]
        if index <= START_END:
            t = smoothstep((index - 1) / START_END)
            target_l = loop_target[0] * (1.0 - t) + stable_left[0] * t
            target_c = loop_target[1] * (1.0 - t) + stable_left[1] * t
            normalize(rgba, target_l, target_c)
        elif index >= END_START:
            t = smoothstep((index - (END_START - 1)) / (COUNT - (END_START - 1)))
            target_l = stable_right[0] * (1.0 - t) + loop_target[0] * t
            target_c = stable_right[1] * (1.0 - t) + loop_target[1] * t
            normalize(rgba, target_l, target_c)
        after_l, after_c = metrics(rgba)
        rows.append({"frame": index, "l_before": before[index-1][0], "l_target": target_l,
                     "l_after": after_l, "chroma_before": before[index-1][1],
                     "chroma_target": target_c, "chroma_after": after_c})
        im = Image.fromarray(rgba, "RGBA")
        im.save(FRAMES / f"idle_{index:03d}.png", optimize=True)
        images.append(im)

    for name, color in (("game_dark", (36,25,25,255)), ("black", (0,0,0,255))):
        views = [Image.alpha_composite(Image.new("RGBA", im.size, color), im)
                 .resize((533,400), Image.Resampling.LANCZOS).convert("RGB") for im in images]
        views[0].save(OUT / f"idle_{name}_3s_60fps.gif", save_all=True,
                      append_images=views[1:], duration=17, loop=0, disposal=2, optimize=False)
    report = {"frames": COUNT, "corrected_ranges": [[1, START_END], [END_START, COUNT]],
              "loop_target": {"l": loop_target[0], "chroma": loop_target[1]}, "rows": rows}
    (OUT / "loop_color_report.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"loop_target": loop_target,
                      "before": {"f1": before[0], "f180": before[-1]},
                      "after": {"f1": metrics(arrays[0]), "f180": metrics(arrays[-1])}}, indent=2))


if __name__ == "__main__":
    main()
