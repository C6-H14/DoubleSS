from pathlib import Path
import importlib.util
import json
import shutil

from PIL import Image

import build_character_runtime_v25_idle as runtime


ROOT = Path(r"D:\StSmod\DoubleSS")
BASE = ROOT / "art_candidates" / "character_runtime_v30_idle_v46"
SOURCE = (ROOT / "art_candidates" / "video_source" / "death_splice_analysis" /
          "death_splice_v25_alpha_conservative" / "alpha_frames")
OUT = ROOT / "art_candidates" / "character_runtime_v31_death"
IMAGES = OUT / "images"
EXPORT = OUT / "export"
FPS = 24.0
EXPECTED = 132


def main():
    if OUT.exists():
        shutil.rmtree(OUT)
    IMAGES.mkdir(parents=True)
    EXPORT.mkdir(parents=True)
    # The shared packer writes through its module-level EXPORT path.
    runtime.EXPORT = EXPORT

    for path in (BASE / "images").glob("*.png"):
        shutil.copy2(path, IMAGES / path.name)
    for path in (BASE / "export").glob("character*.png"):
        shutil.copy2(path, EXPORT / path.name)
    shutil.copy2(BASE / "export" / "character.atlas", EXPORT / "character.atlas")

    source_json = BASE / "DoubleSS_character_v25_import_3.8.json"
    data = json.loads(source_json.read_text(encoding="utf-8"))
    data["skeleton"]["images"] = str(IMAGES.resolve()).replace("\\", "/") + "/"
    attachments = data["skins"][0]["attachments"]["character_frame"]
    paths = sorted(SOURCE.glob("death_*.png"))
    if len(paths) != EXPECTED:
        raise RuntimeError(f"Expected {EXPECTED} death frames, found {len(paths)}")

    names = []
    for index, path in enumerate(paths, 1):
        name = f"death_{index:03d}"
        source = Image.open(path).convert("RGBA")
        resized = source.resize((560, 416), Image.Resampling.LANCZOS)
        canvas = Image.new("RGBA", (1024, 1024), (0, 0, 0, 0))
        canvas.paste(resized, (232, 337))
        canvas = runtime.premultiply_alpha(canvas)
        canvas.save(IMAGES / f"{name}.png", optimize=True)
        attachments[name] = {"path": name, "x": 0, "y": 210.0,
                             "width": 1024, "height": 1024}
        names.append(name)

    keys = []
    for index, name in enumerate(names):
        key = {"name": name}
        if index:
            key["time"] = round(index / FPS, 6)
        keys.append(key)
    # Explicitly hold the final fallen pose; death must never return to idle.
    keys.append({"time": round(EXPECTED / FPS, 6), "name": names[-1]})
    data["animations"]["death"] = {
        "slots": {"character_frame": {"attachment": keys}}
    }

    import_json = OUT / "DoubleSS_character_v31_import_3.8.json"
    import_json.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n",
                           encoding="utf-8")
    converter_path = ROOT / "tools" / "spine-compat-probe" / "convert_spine_38_to_34.py"
    spec = importlib.util.spec_from_file_location("converter", converter_path)
    converter = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(converter)
    converter.convert(import_json, EXPORT / "character.json")

    items = []
    for name in names:
        image = Image.open(IMAGES / f"{name}.png").convert("RGBA")
        bbox = image.getchannel("A").getbbox()
        if bbox is None:
            raise RuntimeError(f"Empty death frame: {name}")
        items.append((name, image, bbox))
    page_number = runtime.next_page_number(EXPORT)
    pages = []
    sections = []
    while items:
        items, section, page_name, count, size = runtime.pack_page(items, page_number)
        sections.append(section)
        pages.append({"page": page_name, "regions": count, "size": list(size)})
        page_number += 1
    with (EXPORT / "character.atlas").open("a", encoding="utf-8") as stream:
        stream.write("".join(sections))

    runtime_json = json.loads((EXPORT / "character.json").read_text(encoding="utf-8"))
    timeline = runtime_json["animations"]["death"]["slots"]["character_frame"]["attachment"]
    atlas = (EXPORT / "character.atlas").read_text(encoding="utf-8")
    missing = [name for name in names if f"{name}\n" not in atlas]
    if len(timeline) != EXPECTED + 1 or missing:
        raise RuntimeError({"timeline": len(timeline), "missing": missing})

    metadata = {"death": {"unique_frames": EXPECTED,
                           "timeline_keys": len(timeline), "fps": FPS,
                           "duration": EXPECTED / FPS,
                           "source_canvas": [1120, 832],
                           "runtime_canvas": [1024, 1024],
                           "resized": [560, 416], "paste": [232, 337],
                           "attachment_y": 210.0,
                           "atlas_pages_added": pages},
                "base": str(BASE),
                "total_atlas_pages": len(list(EXPORT.glob("character*.png")))}
    (OUT / "metadata.json").write_text(json.dumps(metadata, indent=2) + "\n",
                                        encoding="utf-8")
    print(json.dumps(metadata, indent=2))


if __name__ == "__main__":
    main()
