from pathlib import Path
import importlib.util
import json
import shutil

from PIL import Image


ROOT = Path(r"D:\StSmod\DoubleSS")
BASE = ROOT / "art_candidates" / "character_runtime_v15_cast_attack_2"
BASE_IMPORT_NAME = "DoubleSS_character_v15_import_3.8.json"
SOURCE = ROOT / "art_candidates" / "video_source" / "cast_prayer" / "preview_v5" / "alpha_frames"
OUTPUT = ROOT / "art_candidates" / "character_runtime_v16_cast_prayer"
OUTPUT_IMPORT_NAME = "DoubleSS_character_v16_import_3.8.json"
IMAGES = OUTPUT / "images"
EXPORT = OUTPUT / "export"
FPS = 60.0
CANVAS = 1024
SCALE = 0.5
PASTE = (39, 135)
ATTACHMENT_Y = 210.0
EXPECTED_FRAMES = 31
FRAME_OFFSETS = {}
TARGET_DURATION = None


def attachment(name: str) -> dict:
    return {"path": name, "x": 0, "y": ATTACHMENT_Y, "width": CANVAS, "height": CANVAS}


def timeline(prefix: str, count: int) -> list[dict]:
    keys = []
    frame_step = (TARGET_DURATION / count) if TARGET_DURATION is not None else (1.0 / FPS)
    for index in range(1, count + 1):
        key = {"name": f"{prefix}_{index:03d}"}
        if index > 1:
            key["time"] = round((index - 1) * frame_step, 6)
        keys.append(key)
    return keys


def premultiplied_resize(source: Image.Image, size: tuple[int, int]) -> Image.Image:
    rgba = source.convert("RGBA")
    # Pillow's RGBA resize is premultiplied internally in current versions;
    # retaining the explicit helper boundary documents the alpha-safe intent.
    return rgba.resize(size, Image.Resampling.LANCZOS)


def runtime_frame(path: Path) -> Image.Image:
    source = Image.open(path).convert("RGBA")
    resized = premultiplied_resize(source, (round(source.width * SCALE), round(source.height * SCALE)))
    canvas = Image.new("RGBA", (CANVAS, CANVAS), (0, 0, 0, 0))
    index = int(path.stem.rsplit("_", 1)[-1])
    dx, dy = FRAME_OFFSETS.get(index, (0, 0))
    canvas.alpha_composite(resized, (PASTE[0] + dx, PASTE[1] + dy))
    return canvas


def main() -> None:
    if OUTPUT.exists():
        shutil.rmtree(OUTPUT)
    IMAGES.mkdir(parents=True)
    EXPORT.mkdir(parents=True)

    # Preserve every previously game-tested attachment and animation.
    for path in sorted((BASE / "images").glob("*.png")):
        shutil.copy2(path, IMAGES / path.name)

    data = json.loads((BASE / BASE_IMPORT_NAME).read_text(encoding="utf-8"))
    data["skeleton"]["images"] = str(IMAGES.resolve()).replace("\\", "/") + "/"
    attachments = data["skins"][0]["attachments"]["character_frame"]

    paths = sorted(SOURCE.glob("cast_prayer_*.png"))
    if len(paths) != EXPECTED_FRAMES:
        raise RuntimeError(f"Expected {EXPECTED_FRAMES} cast_prayer frames, found {len(paths)}")
    for index, path in enumerate(paths, 1):
        name = f"cast_prayer_{index:03d}"
        runtime_frame(path).save(IMAGES / f"{name}.png", optimize=True)
        attachments[name] = attachment(name)

    duration = TARGET_DURATION if TARGET_DURATION is not None else len(paths) / FPS
    data["animations"]["cast_prayer"] = {
        "slots": {"character_frame": {"attachment": timeline("cast_prayer", len(paths))}},
        "events": [{"time": round(duration, 6), "name": "attack_end"}],
    }

    import_json = OUTPUT / OUTPUT_IMPORT_NAME
    runtime_json = EXPORT / "character.json"
    import_json.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    converter_path = ROOT / "tools" / "spine-compat-probe" / "convert_spine_38_to_34.py"
    spec = importlib.util.spec_from_file_location("converter", converter_path)
    converter = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(converter)
    converter.convert(import_json, runtime_json)

    metadata = {
        "cast_prayer": {
            "frames": len(paths),
            "fps": FPS,
            "duration": duration,
            "canvas": CANVAS,
            "scale": SCALE,
            "paste": list(PASTE),
        }
    }
    (OUTPUT / "metadata.json").write_text(json.dumps(metadata, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(metadata, indent=2))


if __name__ == "__main__":
    main()
