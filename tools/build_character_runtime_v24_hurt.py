from pathlib import Path
import importlib.util
import json
import shutil

from PIL import Image


ROOT = Path(r"D:\StSmod\DoubleSS")
BASE = ROOT / "art_candidates" / "character_runtime_v23_cast_special"
SOURCE = ROOT / "art_candidates" / "video_source" / "hurt" / "final_v5_42f_defringed" / "alpha_frames"
OUT = ROOT / "art_candidates" / "character_runtime_v24_hurt"
IMAGES = OUT / "images"
EXPORT = OUT / "export"

EXPECTED = 42
DURATION = 0.45
EFFECTIVE_FPS = EXPECTED / DURATION


def main() -> None:
    if OUT.exists():
        shutil.rmtree(OUT)
    IMAGES.mkdir(parents=True)
    EXPORT.mkdir(parents=True)

    for path in (BASE / "images").glob("*.png"):
        shutil.copy2(path, IMAGES / path.name)

    source_json = BASE / "DoubleSS_character_v23_import_3.8.json"
    data = json.loads(source_json.read_text(encoding="utf-8"))
    data["skeleton"]["images"] = str(IMAGES.resolve()).replace("\\", "/") + "/"
    attachments = data["skins"][0]["attachments"]["character_frame"]

    paths = sorted(SOURCE.glob("hurt_*.png"))
    if len(paths) != EXPECTED:
        raise RuntimeError(f"Expected {EXPECTED} hurt frames, found {len(paths)}")

    for index, path in enumerate(paths, 1):
        name = f"hurt_{index:03d}"
        source = Image.open(path).convert("RGBA")
        resized = source.resize((556, 417), Image.Resampling.LANCZOS)
        canvas = Image.new("RGBA", (1024, 1024), (0, 0, 0, 0))
        canvas.alpha_composite(resized, (227, 336))
        canvas.save(IMAGES / f"{name}.png", optimize=True)
        attachments[name] = {
            "path": name,
            "x": 0,
            "y": 210.0,
            "width": 1024,
            "height": 1024,
        }

    keys = []
    for index in range(1, EXPECTED + 1):
        key = {"name": f"hurt_{index:03d}"}
        if index > 1:
            key["time"] = round((index - 1) * DURATION / EXPECTED, 6)
        keys.append(key)
    data["animations"]["hurt"] = {
        "slots": {"character_frame": {"attachment": keys}},
        "events": [{"time": DURATION, "name": "attack_end"}],
    }

    import_json = OUT / "DoubleSS_character_v24_import_3.8.json"
    import_json.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    converter_path = ROOT / "tools" / "spine-compat-probe" / "convert_spine_38_to_34.py"
    spec = importlib.util.spec_from_file_location("converter", converter_path)
    converter = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(converter)
    converter.convert(import_json, EXPORT / "character.json")

    metadata = {
        "hurt": {
            "frames": EXPECTED,
            "fps": EFFECTIVE_FPS,
            "duration": DURATION,
            "source_canvas": [1112, 834],
            "runtime_canvas": [1024, 1024],
            "resized": [556, 417],
            "paste": [227, 336],
            "attachment_y": 210.0,
        }
    }
    (OUT / "metadata.json").write_text(json.dumps(metadata, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(metadata, indent=2))


if __name__ == "__main__":
    main()
