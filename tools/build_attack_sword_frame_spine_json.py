import json
from pathlib import Path


ROOT = Path(r"D:\StSmod\DoubleSS\art_candidates\attack_sword_frame_animation_v1")
OUTPUT = ROOT / "DoubleSS_attack_sword_frames_v1.json"
FRAME_COUNT = 15
DURATION = 0.8


def attachment_name(frame: int) -> str:
    return f"attack_sword_{frame:02d}"


def attachment_path(frame: int) -> str:
    return f"frames/{attachment_name(frame)}"


def main() -> None:
    attachments = {
        attachment_name(frame): {
            "path": attachment_path(frame),
            "width": 1024,
            "height": 768,
        }
        for frame in range(1, FRAME_COUNT + 1)
    }

    timeline = [
        {
            **({} if frame == 1 else {"time": round((frame - 1) * DURATION / (FRAME_COUNT - 1), 6)}),
            "name": attachment_name(frame),
        }
        for frame in range(1, FRAME_COUNT + 1)
    ]

    data = {
        "skeleton": {
            "hash": "",
            "spine": "3.8.75",
            "x": -512,
            "y": -384,
            "width": 1024,
            "height": 768,
            "images": "./",
        },
        "bones": [{"name": "root"}],
        "slots": [
            {
                "name": "character_frame",
                "bone": "root",
                "attachment": attachment_name(1),
            }
        ],
        "skins": [
            {
                "name": "default",
                "attachments": {"character_frame": attachments},
            }
        ],
        "events": {"attack_hit": {}},
        "animations": {
            "idle": {
                "slots": {
                    "character_frame": {
                        "attachment": [{"name": attachment_name(1)}]
                    }
                }
            },
            "attack_sword": {
                "slots": {"character_frame": {"attachment": timeline}},
                "events": [{"time": 0.4, "name": "attack_hit"}],
            },
        },
    }

    OUTPUT.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")


if __name__ == "__main__":
    main()
