from pathlib import Path
import importlib.util
import json

import cv2
import numpy as np
from PIL import Image


ROOT = Path(r"D:\StSmod\DoubleSS")
SOURCE = ROOT / "art_candidates" / "matting_tests" / "matanyone_attack_pink_v1" / "selected47_50fps" / "frames"
OUTPUT = ROOT / "art_candidates" / "attack_sword_frame_animation_v11_pink40_50fps"
IMAGES = OUTPUT / "images"

FRAME_COUNT = 40
FPS = 50
CANVAS = 700

# One global transform for every frame. It maps the approved transparent
# endpoint bbox (335,108)-(766,706) onto the already game-tested runtime bbox
# (239,233)-(484,560). Per-frame fitting is deliberately forbidden because it
# creates the visible breathing/size jitter seen in earlier revisions.
RESIZED_W = 582
RESIZED_H = 420
PASTE_X = 49
PASTE_Y = 174
ATTACHMENT_Y = 210.0
HIT_FRAME = 20


def premultiplied_resize(image: Image.Image, size: tuple[int, int]) -> Image.Image:
    rgba = np.asarray(image.convert("RGBA"), dtype=np.float32) / 255.0
    alpha = rgba[..., 3:4]
    premul = np.concatenate((rgba[..., :3] * alpha, alpha), axis=2)
    resized = np.empty((size[1], size[0], 4), dtype=np.float32)
    for channel in range(4):
        layer = Image.fromarray(
            np.uint8(np.round(np.clip(premul[..., channel], 0, 1) * 255)), "L"
        )
        resized[..., channel] = np.asarray(
            layer.resize(size, Image.Resampling.LANCZOS), dtype=np.float32
        ) / 255.0
    out_alpha = resized[..., 3:4]
    rgb = np.divide(
        resized[..., :3], out_alpha, out=np.zeros_like(resized[..., :3]), where=out_alpha > 1e-5
    )
    out = np.uint8(
        np.round(
            np.concatenate((np.clip(rgb, 0, 1), np.clip(out_alpha, 0, 1)), axis=2) * 255
        )
    )
    out[out[..., 3] < 12] = 0
    count, labels, stats, _ = cv2.connectedComponentsWithStats(
        (out[..., 3] > 0).astype(np.uint8), 8
    )
    for component in range(1, count):
        if stats[component, cv2.CC_STAT_AREA] <= 3:
            out[labels == component] = 0
    out[out[..., 3] == 0, :3] = 0
    return Image.fromarray(out, "RGBA")


def skeleton_json() -> dict:
    attachments = {}
    timeline = []
    for frame in range(1, FRAME_COUNT + 1):
        name = f"attack_sword_{frame:02d}"
        attachments[name] = {
            "path": name,
            "x": 0,
            "y": ATTACHMENT_Y,
            "width": CANVAS,
            "height": CANVAS,
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
            "width": CANVAS,
            "height": CANVAS,
            "images": str(IMAGES.resolve()).replace("\\", "/") + "/",
        },
        "bones": [{"name": "root"}],
        "slots": [
            {"name": "character_frame", "bone": "root", "attachment": "attack_sword_01"}
        ],
        "skins": [
            {
                "name": "default",
                "attachments": {"character_frame": attachments},
            }
        ],
        "events": {"attack_hit": {}, "attack_end": {}},
        "animations": {
            "idle": {
                "slots": {
                    "character_frame": {"attachment": [{"name": "attack_sword_01"}]}
                }
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
    inputs = sorted(SOURCE.glob("attack_sword_*.png"))
    if len(inputs) != FRAME_COUNT:
        raise RuntimeError(f"Expected {FRAME_COUNT} frames, found {len(inputs)}")

    IMAGES.mkdir(parents=True, exist_ok=True)
    for old in IMAGES.glob("attack_sword_*.png"):
        old.unlink()

    for index, source_path in enumerate(inputs, 1):
        source = Image.open(source_path).convert("RGBA")
        resized = premultiplied_resize(source, (RESIZED_W, RESIZED_H))
        canvas = Image.new("RGBA", (CANVAS, CANVAS), (0, 0, 0, 0))
        canvas.alpha_composite(resized, (PASTE_X, PASTE_Y))
        canvas.save(IMAGES / f"attack_sword_{index:02d}.png", optimize=True)

    import_json = OUTPUT / "DoubleSS_attack_sword_pink40_import.json"
    runtime_json = OUTPUT / "character.json"
    import_json.write_text(
        json.dumps(skeleton_json(), ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )

    converter_path = ROOT / "tools" / "spine-compat-probe" / "convert_spine_38_to_34.py"
    spec = importlib.util.spec_from_file_location("converter", converter_path)
    converter = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(converter)
    converter.convert(import_json, runtime_json)

    (OUTPUT / "README.md").write_text(
        "# DoubleSS attack_sword pink40 runtime\n\n"
        "- Source: matanyone_attack_pink_v1/selected47_50fps, reduced to 40 frames\n"
        "- Spine editor import: 3.8.75\n"
        "- Game runtime JSON: 3.4.02\n"
        "- Timeline: 40 frames at 50 FPS (0.80 seconds)\n"
        "- Global transform only: 1024x768 -> 582x420 at canvas offset (49,174)\n"
        "- Runtime canvas: 700x700\n"
        "- First-frame visible bbox target: approximately (239,233)-(484,560)\n"
        "- attack_hit: frame 20 / 0.38 seconds\n"
        "- Alpha cleanup after resize: threshold 12; detached components <= 3 px removed\n",
        encoding="utf-8",
    )
    print(import_json)
    print(runtime_json)


if __name__ == "__main__":
    main()
