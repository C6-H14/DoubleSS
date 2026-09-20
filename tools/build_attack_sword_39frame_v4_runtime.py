from pathlib import Path
import json
import importlib.util

import numpy as np
import cv2
from PIL import Image


ROOT = Path(r"D:\StSmod\DoubleSS")
SOURCE = ROOT / "art_candidates" / "video_source" / "attack_sword_v2_final39_swordfixed_v6" / "frames"
OUTPUT = ROOT / "art_candidates" / "attack_sword_frame_animation_v10_final30_hires40fps"
IMAGES = OUTPUT / "images"
# User-approved removals from the 39-frame master. Output files are renumbered
# sequentially so the Spine timeline stays compact and deterministic.
REMOVED_SOURCE_FRAMES = {2, 8, 10, 12, 15, 22, 23, 24, 25}
SOURCE_FRAMES = [frame for frame in range(1, 40) if frame not in REMOVED_SOURCE_FRAMES]
FRAME_COUNT = len(SOURCE_FRAMES)
FPS = 40
WIDTH = 700
HEIGHT = 700
CONTENT_HEIGHT = 660
ATTACHMENT_Y = 210.0
# Old frame 26 is the first retained frame after the removed contact cluster;
# it becomes output frame 17.
HIT_FRAME = SOURCE_FRAMES.index(26) + 1


def premultiplied_resize(image: Image.Image, size: tuple[int, int]) -> Image.Image:
    rgba = np.asarray(image.convert("RGBA"), dtype=np.float32) / 255.0
    alpha = rgba[..., 3:4]
    premul = np.concatenate((rgba[..., :3] * alpha, alpha), axis=2)
    resized = np.empty((size[1], size[0], 4), dtype=np.float32)
    for channel in range(4):
        layer = Image.fromarray(np.uint8(np.round(np.clip(premul[..., channel], 0, 1) * 255)), "L")
        resized[..., channel] = np.asarray(layer.resize(size, Image.Resampling.LANCZOS), dtype=np.float32) / 255.0
    out_alpha = resized[..., 3:4]
    rgb = np.divide(resized[..., :3], out_alpha, out=np.zeros_like(resized[..., :3]), where=out_alpha > 1e-5)
    out = np.uint8(np.round(np.concatenate((np.clip(rgb, 0, 1), np.clip(out_alpha, 0, 1)), axis=2) * 255))
    # Remove resampling dust that becomes bright speckles under Spine's linear
    # texture filtering. Keep all connected antialiased silhouette pixels.
    out[out[..., 3] < 12] = 0
    count, labels, stats, _ = cv2.connectedComponentsWithStats((out[..., 3] > 0).astype(np.uint8), 8)
    for component in range(1, count):
        if stats[component, cv2.CC_STAT_AREA] <= 3:
            out[labels == component] = 0
    out[out[..., 3] == 0, :3] = 0
    out[:1, :, :] = out[-1:, :, :] = 0
    out[:, :1, :] = out[:, -1:, :] = 0
    return Image.fromarray(out, "RGBA")


def build_json() -> dict:
    attachments = {}
    timeline = []
    for frame in range(1, FRAME_COUNT + 1):
        name = f"attack_sword_{frame:02d}"
        attachments[name] = {
            "path": name,
            "x": 0,
            "y": ATTACHMENT_Y,
            "width": WIDTH,
            "height": HEIGHT,
        }
        key = {"name": name}
        if frame > 1:
            key["time"] = round((frame - 1) / FPS, 6)
        timeline.append(key)
    return {
        "skeleton": {
            "hash": "",
            "spine": "3.8.75",
            "x": -175.0,
            "y": -70.0,
            "width": WIDTH,
            "height": HEIGHT,
            "images": str(IMAGES.resolve()).replace("\\", "/") + "/",
        },
        "bones": [{"name": "root"}],
        "slots": [{"name": "character_frame", "bone": "root", "attachment": "attack_sword_01"}],
        "skins": [{"name": "default", "attachments": {"character_frame": attachments}}],
        "events": {"attack_hit": {}, "attack_end": {}},
        "animations": {
            "idle": {
                "slots": {"character_frame": {"attachment": [{"name": "attack_sword_01"}]}}
            },
            "attack_sword": {
                "slots": {"character_frame": {"attachment": timeline}},
                "events": [
                    {"time": round((HIT_FRAME - 1) / FPS, 6), "name": "attack_hit"},
                    {"time": round(FRAME_COUNT / FPS, 6), "name": "attack_end"},
                ],
            },
        },
    }


def main():
    IMAGES.mkdir(parents=True, exist_ok=True)
    for old in IMAGES.glob("attack_sword_*.png"):
        old.unlink()
    for output_frame, source_frame in enumerate(SOURCE_FRAMES, 1):
        source = Image.open(SOURCE / f"attack_sword_{source_frame:02d}.png").convert("RGBA")
        resized = premultiplied_resize(source, (WIDTH, CONTENT_HEIGHT))
        canvas = Image.new("RGBA", (WIDTH, HEIGHT), (0, 0, 0, 0))
        canvas.alpha_composite(resized, (0, (HEIGHT - CONTENT_HEIGHT) // 2))
        canvas.save(IMAGES / f"attack_sword_{output_frame:02d}.png", optimize=True)

    source_json = OUTPUT / "DoubleSS_attack_sword_final30_import.json"
    runtime_json = OUTPUT / "character.json"
    source_json.write_text(json.dumps(build_json(), ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    converter_path = ROOT / "tools" / "spine-compat-probe" / "convert_spine_38_to_34.py"
    spec = importlib.util.spec_from_file_location("converter", converter_path)
    converter = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(converter)
    converter.convert(source_json, runtime_json)

    (OUTPUT / "README.md").write_text(
        "# DoubleSS attack_sword final30 high-resolution runtime\n\n"
        "- Approved source: attack_sword_v2_final39_swordfixed_v6\n"
        "- Removed source frames: 02, 08, 10, 12, 15, 22, 23, 24, 25\n"
        "- Spine editor: 3.8.75\n"
        "- Runtime JSON: 3.4.02\n"
        "- Frames: 30\n"
        "- FPS: 40\n"
        "- Duration: 0.75 seconds\n"
        "- Canvas per attachment: 700 x 700 (content 700 x 660)\n"
        "- Runtime scale: 0.25 (same displayed size as 350 x 350 at 0.5)\n"
        "- Post-resize alpha cleanup: threshold 12; detached components <= 3 px removed\n"
        "- attack_hit: frame 17 / 0.4 seconds\n",
        encoding="utf-8",
    )
    print(source_json)
    print(runtime_json)


if __name__ == "__main__":
    main()
