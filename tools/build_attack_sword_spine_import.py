import json
import shutil
from pathlib import Path

from PIL import Image


ROOT = Path(r"D:\StSmod\DoubleSS")
SOURCE = (
    ROOT
    / "art_candidates"
    / "video_source"
    / "attack_sword_v2_final16_15fps"
    / "alpha_cleanup_v3"
    / "final_web_clean_frames"
)
PACKAGE = ROOT / "art_candidates" / "attack_sword_frame_animation_v5_webclean"
IMAGES = PACKAGE / "images"
IMPORT_JSON = PACKAGE / "DoubleSS_attack_sword_webclean_import.json"

WIDTH = 700
HEIGHT = 660
GROUND_X = 350.0
GROUND_Y_FROM_TOP = 539.5
ROOT_Y_FROM_BOTTOM = HEIGHT - GROUND_Y_FROM_TOP
ATTACHMENT_Y = HEIGHT / 2 - ROOT_Y_FROM_BOTTOM


def make_animation():
    attachment_keys = []
    for index in range(16):
        key = {"name": f"attack_sword_{index + 1:02d}"}
        if index:
            key["time"] = round(index / 15, 6)
        attachment_keys.append(key)
    return {
        "slots": {"character_frame": {"attachment": attachment_keys}},
        "events": [
            {"time": round(10 / 15, 6), "name": "attack_hit"},
            {"time": round(16 / 15, 6), "name": "attack_end"},
        ],
    }


IMAGES.mkdir(parents=True, exist_ok=True)
for index in range(1, 17):
    source = SOURCE / f"attack_sword_{index:02d}.png"
    target = IMAGES / source.name
    image = Image.open(source).convert("RGBA")
    image = image.resize((WIDTH, HEIGHT), Image.Resampling.LANCZOS)
    image.save(target)

attachments = {
    f"attack_sword_{index:02d}": {
        "path": f"attack_sword_{index:02d}",
        "x": 0,
        "y": ATTACHMENT_Y,
        "width": WIDTH,
        "height": HEIGHT,
    }
    for index in range(1, 17)
}

data = {
    "skeleton": {
        "hash": "",
        "spine": "3.8.75",
        "x": -GROUND_X,
        "y": -ROOT_Y_FROM_BOTTOM,
        "width": WIDTH,
        "height": HEIGHT,
        "images": IMAGES.as_posix() + "/",
    },
    "bones": [{"name": "root"}],
    "slots": [
        {
            "name": "character_frame",
            "bone": "root",
            "attachment": "attack_sword_01",
        }
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
                "character_frame": {
                    "attachment": [{"name": "attack_sword_01"}]
                }
            }
        },
        "attack_sword": make_animation(),
    },
}

IMPORT_JSON.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")

readme = f"""# attack_sword Spine 3.8.75 final import package

- Source canvas: 1400 x 1320
- Runtime/import canvas: {WIDTH} x {HEIGHT}
- Frames: 16
- FPS: 15
- Duration: 1.066667 seconds
- Ground/root anchor: ({GROUND_X}, {GROUND_Y_FROM_TOP}) from image top-left
- attack_hit: frame 11, t=0.666667s
- attack_end: t=1.066667s

The source frames are uniformly aligned. Do not reposition individual attachments.
"""
(PACKAGE / "README.md").write_text(readme, encoding="utf-8")

print(f"package={PACKAGE}")
print(f"json={IMPORT_JSON}")
print(f"images={IMAGES}")
