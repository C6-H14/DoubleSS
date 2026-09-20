from pathlib import Path
import importlib.util
import json
import shutil

from PIL import Image

from build_attack_sword_pink40_runtime import premultiplied_resize


ROOT = Path(r"D:\StSmod\DoubleSS")
ATTACK = ROOT / "art_candidates" / "attack_sword_frame_animation_v11_pink40_50fps" / "images"
CAST = ROOT / "art_candidates" / "video_source" / "cast_v1" / "final_v4_edgeclean_37f_50fps" / "frames"
OUTPUT = ROOT / "art_candidates" / "character_runtime_v13_attack40_cast37_edgeclean"
IMAGES = OUTPUT / "images"

CANVAS = 700
RESIZED_W = 582
RESIZED_H = 420
PASTE_X = 49
PASTE_Y = 174
ATTACHMENT_Y = 210.0
FPS = 50
ATTACK_COUNT = 40
CAST_COUNT = 37


def attachment(name):
    return {
        "path": name,
        "x": 0,
        "y": ATTACHMENT_Y,
        "width": CANVAS,
        "height": CANVAS,
    }


def attachment_timeline(prefix, count):
    result = []
    for frame in range(1, count + 1):
        key = {"name": f"{prefix}_{frame:02d}"}
        if frame > 1:
            key["time"] = round((frame - 1) / FPS, 6)
        result.append(key)
    return result


def build_json():
    attachments = {}
    for prefix, count in (("attack_sword", ATTACK_COUNT), ("cast_attack_1", CAST_COUNT)):
        for frame in range(1, count + 1):
            name = f"{prefix}_{frame:02d}"
            attachments[name] = attachment(name)
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
        "slots": [{"name": "character_frame", "bone": "root", "attachment": "attack_sword_01"}],
        "skins": [{"name": "default", "attachments": {"character_frame": attachments}}],
        "events": {"attack_hit": {}, "attack_end": {}},
        "animations": {
            "idle": {
                "slots": {"character_frame": {"attachment": [{"name": "attack_sword_01"}]}}
            },
            "attack_sword": {
                "slots": {"character_frame": {"attachment": attachment_timeline("attack_sword", ATTACK_COUNT)}},
                "events": [
                    {"time": 0.38, "name": "attack_hit"},
                    {"time": 0.8, "name": "attack_end"},
                ],
            },
            "cast_attack_1": {
                "slots": {"character_frame": {"attachment": attachment_timeline("cast_attack_1", CAST_COUNT)}},
                "events": [
                    {"time": 0.24, "name": "attack_hit"},
                    {"time": 0.74, "name": "attack_end"},
                ],
            },
        },
    }


def main():
    attack_files = sorted(ATTACK.glob("attack_sword_*.png"))
    cast_files = sorted(CAST.glob("cast_attack_1_*.png"))
    if len(attack_files) != ATTACK_COUNT or len(cast_files) != CAST_COUNT:
        raise RuntimeError(
            f"Expected attack={ATTACK_COUNT}, cast={CAST_COUNT}; got {len(attack_files)}, {len(cast_files)}"
        )

    if IMAGES.exists():
        shutil.rmtree(IMAGES)
    IMAGES.mkdir(parents=True)
    for source in attack_files:
        shutil.copy2(source, IMAGES / source.name)

    for index, source in enumerate(cast_files, 1):
        image = Image.open(source).convert("RGBA")
        resized = premultiplied_resize(image, (RESIZED_W, RESIZED_H))
        canvas = Image.new("RGBA", (CANVAS, CANVAS), (0, 0, 0, 0))
        canvas.alpha_composite(resized, (PASTE_X, PASTE_Y))
        canvas.save(IMAGES / f"cast_attack_1_{index:02d}.png", optimize=True)

    import_json = OUTPUT / "DoubleSS_character_attack40_cast37_import.json"
    runtime_json = OUTPUT / "character.json"
    import_json.write_text(json.dumps(build_json(), ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    converter_path = ROOT / "tools" / "spine-compat-probe" / "convert_spine_38_to_34.py"
    spec = importlib.util.spec_from_file_location("converter", converter_path)
    converter = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(converter)
    converter.convert(import_json, runtime_json)

    (OUTPUT / "README.md").write_text(
        "# DoubleSS combined character runtime v12\n\n"
        "- idle: attack_sword_01\n"
        "- attack_sword: 40 frames, 50 FPS, 0.80 s\n"
        "- cast_attack_1: 37 frames, 50 FPS, 0.74 s\n"
        "- Runtime canvas: 700 x 700\n"
        "- Both animations use the same global transform and game-tested display size\n"
        "- Spine import JSON: 3.8.75\n"
        "- Game runtime JSON: 3.4.02\n",
        encoding="utf-8",
    )
    print(import_json)
    print(runtime_json)


if __name__ == "__main__":
    main()
