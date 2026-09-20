from pathlib import Path
import json
import shutil
import sys


FPS = 15
FRAME_COUNT = 16
WIDTH = 556
HEIGHT = 417
ATTACHMENT_X = -1.5
ATTACHMENT_Y = 175.5
HIT_FRAME = 11


def main() -> None:
    runtime_frames = Path(sys.argv[1])
    output_dir = Path(sys.argv[2])
    images_dir = output_dir / "images"
    images_dir.mkdir(parents=True, exist_ok=True)

    for index in range(1, FRAME_COUNT + 1):
        name = f"attack_sword_{index:02d}"
        shutil.copy2(runtime_frames / f"{name}.png", images_dir / f"{name}.png")

    attachments = {}
    timeline = []
    for index in range(1, FRAME_COUNT + 1):
        name = f"attack_sword_{index:02d}"
        attachments[name] = {
            "path": name,
            "x": ATTACHMENT_X,
            "y": ATTACHMENT_Y,
            "width": WIDTH,
            "height": HEIGHT,
        }
        entry = {"name": name}
        if index > 1:
            entry["time"] = round((index - 1) / FPS, 6)
        timeline.append(entry)

    duration = round(FRAME_COUNT / FPS, 6)
    data = {
        "skeleton": {
            "hash": "",
            "spine": "3.8.75",
            "x": -279.5,
            "y": -33,
            "width": WIDTH,
            "height": HEIGHT,
            "images": str(images_dir.resolve()).replace("\\", "/") + "/",
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
            "attack_sword": {
                "slots": {"character_frame": {"attachment": timeline}},
                "events": [
                    {
                        "time": round((HIT_FRAME - 1) / FPS, 6),
                        "name": "attack_hit",
                    },
                    {"time": duration, "name": "attack_end"},
                ],
            },
        },
    }
    output_path = output_dir / "DoubleSS_attack_sword_15fps.json"
    output_path.write_text(
        json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8"
    )

    readme = f"""# attack_sword Spine 3.8.75 import package

- Canvas: {WIDTH} x {HEIGHT}
- Frames: {FRAME_COUNT}
- FPS: {FPS}
- Duration: {duration:.6f} seconds
- Ground/root anchor: runtime pixel (279.5, 384)
- `attack_hit`: frame {HIT_FRAME}, t={(HIT_FRAME - 1) / FPS:.6f}s
- `attack_end`: t={duration:.6f}s (keeps frame 16 visible for one full frame)

## Import in Spine 3.8.75

1. Data Import -> JSON.
2. Select `DoubleSS_attack_sword_15fps.json`.
3. The editor import JSON stores the absolute `images/` directory so Spine 3.8.75 resolves it reliably.
   The game runtime should use the packed atlas, so this source-image path is not used in game.
4. Verify `idle` and non-looping `attack_sword`.
5. Export JSON with version 3.8 and pack the images into an atlas.

Do not move individual attachments: every PNG has the same canvas and anchor.
"""
    (output_dir / "README.md").write_text(readme, encoding="utf-8")


if __name__ == "__main__":
    main()
