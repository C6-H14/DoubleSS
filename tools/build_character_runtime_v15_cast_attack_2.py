from pathlib import Path
import importlib.util
import json
import shutil

from PIL import Image


ROOT = Path(r"D:\StSmod\DoubleSS")
BASE = ROOT / "art_candidates" / "character_runtime_v14_three_animations"
SOURCE = ROOT / "art_candidates" / "video_source" / "cast_attack_2_final_cut" / "alpha_frames_115s_30fps_sword_completed_v2"
OUTPUT = ROOT / "art_candidates" / "character_runtime_v15_cast_attack_2"
IMAGES = OUTPUT / "images"
EXPORT = OUTPUT / "export"
FPS = 30.0
CANVAS = 1024
SCALE = 0.5
PASTE = (39, 135)
ATTACHMENT_Y = 210.0


def attachment(name: str) -> dict:
    return {"path": name, "x": 0, "y": ATTACHMENT_Y, "width": CANVAS, "height": CANVAS}


def timeline(prefix: str, count: int) -> list[dict]:
    result = []
    for index in range(1, count + 1):
        key = {"name": f"{prefix}_{index:03d}"}
        if index > 1:
            key["time"] = round((index - 1) / FPS, 6)
        result.append(key)
    return result


def runtime_frame(path: Path) -> Image.Image:
    source = Image.open(path).convert("RGBA")
    resized = source.resize(
        (round(source.width * SCALE), round(source.height * SCALE)),
        Image.Resampling.LANCZOS,
    )
    canvas = Image.new("RGBA", (CANVAS, CANVAS), (0, 0, 0, 0))
    canvas.alpha_composite(resized, PASTE)
    return canvas


def main() -> None:
    if OUTPUT.exists():
        shutil.rmtree(OUTPUT)
    IMAGES.mkdir(parents=True)
    EXPORT.mkdir(parents=True)

    for path in sorted((BASE / "images").glob("*.png")):
        shutil.copy2(path, IMAGES / path.name)

    data = json.loads((BASE / "DoubleSS_character_v14_import_3.8.json").read_text(encoding="utf-8"))
    data["skeleton"]["images"] = str(IMAGES.resolve()).replace("\\", "/") + "/"
    attachments = data["skins"][0]["attachments"]["character_frame"]
    paths = sorted(SOURCE.glob("cast_attack_2_*.png"))
    if len(paths) != 35:
        raise RuntimeError(f"Expected 35 cast_attack_2 frames, found {len(paths)}")

    for index, path in enumerate(paths, 1):
        name = f"cast_attack_2_{index:03d}"
        runtime_frame(path).save(IMAGES / f"{name}.png", optimize=True)
        attachments[name] = attachment(name)

    duration = len(paths) / FPS
    data["animations"]["cast_attack_2"] = {
        "slots": {"character_frame": {"attachment": timeline("cast_attack_2", len(paths))}},
        "events": [{"time": round(duration, 6), "name": "attack_end"}],
    }

    import_json = OUTPUT / "DoubleSS_character_v15_import_3.8.json"
    runtime_json = EXPORT / "character.json"
    import_json.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    converter_path = ROOT / "tools" / "spine-compat-probe" / "convert_spine_38_to_34.py"
    spec = importlib.util.spec_from_file_location("converter", converter_path)
    converter = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(converter)
    converter.convert(import_json, runtime_json)

    metadata = {"cast_attack_2": {"frames": len(paths), "fps": FPS, "duration": duration,
                                   "canvas": CANVAS, "scale": SCALE, "paste": list(PASTE)}}
    (OUTPUT / "metadata.json").write_text(json.dumps(metadata, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(metadata, indent=2))


if __name__ == "__main__":
    main()
