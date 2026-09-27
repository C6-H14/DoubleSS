from pathlib import Path
import importlib.util
import json
import shutil

from PIL import Image


ROOT = Path(r"D:\StSmod\DoubleSS")
BASE = ROOT / "art_candidates" / "character_runtime_v22_command_ally"
BASE_JSON = BASE / "DoubleSS_character_v22_import_3.8.json"
SOURCE = ROOT / "art_candidates" / "video_source" / "cast_special" / "final_v4_pixelwise" / "alpha_frames"
OUT = ROOT / "art_candidates" / "character_runtime_v23_cast_special"
IMAGES = OUT / "images"
EXPORT = OUT / "export"

EXPECTED = 75
FPS = 50.0
DURATION = EXPECTED / FPS
CANVAS = 1024
SCALE = 0.58
PASTE = (243, 191)
ATTACHMENT_Y = 210.0


def main() -> None:
    if OUT.exists():
        shutil.rmtree(OUT)
    IMAGES.mkdir(parents=True)
    EXPORT.mkdir(parents=True)

    for path in sorted((BASE / "images").glob("*.png")):
        shutil.copy2(path, IMAGES / path.name)

    data = json.loads(BASE_JSON.read_text(encoding="utf-8"))
    data["skeleton"]["images"] = str(IMAGES.resolve()).replace("\\", "/") + "/"
    attachments = data["skins"][0]["attachments"]["character_frame"]

    paths = sorted(SOURCE.glob("cast_special_*.png"))
    if len(paths) != EXPECTED:
        raise RuntimeError(f"Expected {EXPECTED} cast_special frames, found {len(paths)}")

    for index, path in enumerate(paths, 1):
        name = f"cast_special_{index:03d}"
        source = Image.open(path).convert("RGBA")
        resized = source.resize(
            (round(source.width * SCALE), round(source.height * SCALE)),
            Image.Resampling.LANCZOS,
        )
        canvas = Image.new("RGBA", (CANVAS, CANVAS), (0, 0, 0, 0))
        canvas.alpha_composite(resized, PASTE)
        canvas.save(IMAGES / f"{name}.png", optimize=True)
        attachments[name] = {
            "path": name,
            "x": 0,
            "y": ATTACHMENT_Y,
            "width": CANVAS,
            "height": CANVAS,
        }

    keys = []
    for index in range(1, EXPECTED + 1):
        key = {"name": f"cast_special_{index:03d}"}
        if index > 1:
            key["time"] = round((index - 1) / FPS, 6)
        keys.append(key)
    data["animations"]["cast_special"] = {
        "slots": {"character_frame": {"attachment": keys}},
        "events": [{"time": DURATION, "name": "attack_end"}],
    }

    import_json = OUT / "DoubleSS_character_v23_import_3.8.json"
    runtime_json = EXPORT / "character.json"
    import_json.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    converter_path = ROOT / "tools" / "spine-compat-probe" / "convert_spine_38_to_34.py"
    spec = importlib.util.spec_from_file_location("converter", converter_path)
    converter = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(converter)
    converter.convert(import_json, runtime_json)

    metadata = {
        "cast_special": {
            "frames": EXPECTED,
            "fps": FPS,
            "duration": DURATION,
            "source_canvas": [960, 960],
            "runtime_canvas": [CANVAS, CANVAS],
            "scale": SCALE,
            "paste": list(PASTE),
            "attachment_y": ATTACHMENT_Y,
        }
    }
    (OUT / "metadata.json").write_text(json.dumps(metadata, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(metadata, indent=2))


if __name__ == "__main__":
    main()
