from __future__ import annotations

import json
import re
import shutil
import sys
from pathlib import Path

from PIL import Image


ROOT = Path(r"D:\StSmod\DoubleSS")
sys.path.insert(0, str(ROOT / "tools"))
import build_character_runtime_v25_idle as builder
from build_character_runtime_v35_alt_hurt import alpha_union, prune_unused_legacy_pages


SOURCE = ROOT / "art_candidates" / "alt_fencer_attack_basic_v1" / "alpha_frames"
CURRENT = ROOT / "src" / "main" / "resources" / "img" / "char" / "spine"
OUT = ROOT / "art_candidates" / "character_runtime_v36_alt_attack_basic"
IMAGES = OUT / "images"
EXPORT = OUT / "export"
EXPECTED = 30
FPS = 60.0
TARGET_SUBJECT_HEIGHT = 342
TARGET_FOOT_Y = 728
TARGET_CENTER_X = 512
BLEED_MARGIN = 4
FIRST_PAGE = 27
PREFIX = "alt_attack_basic_"
ANIMATION = "attack_basic"
FRAME_OVERRIDES = {}


def main() -> None:
    if OUT.exists():
        shutil.rmtree(OUT)
    IMAGES.mkdir(parents=True)
    EXPORT.mkdir(parents=True)

    for path in CURRENT.glob("character*.png"):
        shutil.copy2(path, EXPORT / path.name)
    shutil.copy2(CURRENT / "character.atlas", EXPORT / "character.atlas")
    shutil.copy2(CURRENT / "character.json", EXPORT / "character.json")

    # Rebuild only pages owned by this animation; idle and hurt remain intact.
    for path in EXPORT.glob("character*.png"):
        suffix = path.stem.removeprefix("character")
        if suffix.isdigit() and int(suffix) >= FIRST_PAGE:
            path.unlink()
    atlas_path = EXPORT / "character.atlas"
    atlas_text = atlas_path.read_text(encoding="utf-8")
    marker = f"\ncharacter{FIRST_PAGE}.png\n"
    if marker in atlas_text:
        atlas_path.write_text(atlas_text.split(marker, 1)[0].rstrip() + "\n", encoding="utf-8")
    prune_unused_legacy_pages(EXPORT)

    paths = sorted(SOURCE.glob(f"{PREFIX}*.png"))
    if len(paths) != EXPECTED:
        raise RuntimeError(f"Expected {EXPECTED} frames, found {len(paths)}")
    union = alpha_union(paths)
    ux0, uy0, ux1, uy1 = union
    scale = TARGET_SUBJECT_HEIGHT / (uy1 - uy0)
    source_width, source_height = Image.open(paths[0]).size
    scaled_width = round(source_width * scale)
    scaled_height = round(source_height * scale)

    first_bbox = Image.open(paths[0]).convert("RGBA").getchannel("A").getbbox()
    last_bbox = Image.open(paths[-1]).convert("RGBA").getchannel("A").getbbox()
    endpoint_center_x = (first_bbox[0] + first_bbox[2] + last_bbox[0] + last_bbox[2]) / 4.0
    endpoint_foot_y = (first_bbox[3] + last_bbox[3]) / 2.0
    paste_x = round(TARGET_CENTER_X - endpoint_center_x * scale)
    paste_y = round(TARGET_FOOT_Y - endpoint_foot_y * scale)

    data = json.loads((EXPORT / "character.json").read_text(encoding="utf-8"))
    attachments = data["skins"]["default"]["character_frame"]
    for old_name in [name for name in attachments if name.startswith(PREFIX)]:
        del attachments[old_name]

    names = []
    staged = []
    for index, path in enumerate(paths, 1):
        name = f"{PREFIX}{index:03d}"
        if index in FRAME_OVERRIDES:
            canvas = Image.open(FRAME_OVERRIDES[index]).convert("RGBA")
            if canvas.size != (1024, 1024):
                raise RuntimeError(f"Override frame {index} must be a 1024x1024 runtime canvas")
        else:
            source = Image.open(path).convert("RGBA")
            resized = source.resize((scaled_width, scaled_height), Image.Resampling.LANCZOS)
            canvas = Image.new("RGBA", (1024, 1024), (0, 0, 0, 0))
            canvas.paste(resized, (paste_x, paste_y))
        canvas = builder.add_runtime_bleed(canvas, radius=4.0)
        canvas = builder.premultiply_alpha(canvas)
        canvas.save(IMAGES / f"{name}.png", optimize=True)
        attachments[name] = {"path": name, "x": 0, "y": 210.0, "width": 1024, "height": 1024}
        names.append(name)
        staged.append(canvas)

    duration = EXPECTED / FPS
    keys = [{"time": round(i / FPS, 6), "name": name} for i, name in enumerate(names)]
    keys.append({"time": duration, "name": names[-1]})
    data["animations"][ANIMATION] = {
        "slots": {"character_frame": {"attachment": keys}},
        "events": [{"time": duration, "name": "attack_end"}],
    }
    (EXPORT / "character.json").write_text(
        json.dumps(data, ensure_ascii=False, separators=(",", ":")), encoding="utf-8"
    )

    builder.EXPORT = EXPORT
    items = []
    for name, image in zip(names, staged):
        left, top, right, bottom = image.getchannel("A").getbbox()
        bbox = (
            max(0, left - BLEED_MARGIN), max(0, top - BLEED_MARGIN),
            min(image.width, right + BLEED_MARGIN), min(image.height, bottom + BLEED_MARGIN),
        )
        items.append((name, image, bbox))

    page_number = FIRST_PAGE
    pages = []
    sections = []
    while items:
        items, section, page_name, count, size = builder.pack_page(items, page_number)
        sections.append(section)
        pages.append({"page": page_name, "regions": count, "size": list(size)})
        page_number += 1
    with (EXPORT / "character.atlas").open("a", encoding="utf-8") as stream:
        stream.write("".join(sections))

    previews = []
    for image in staged:
        bg = Image.new("RGBA", image.size, (42, 42, 42, 255))
        bg.alpha_composite(image)
        previews.append(bg.resize((512, 512), Image.Resampling.LANCZOS).convert("RGB"))
    previews[0].save(
        OUT / "runtime_preview.gif", save_all=True, append_images=previews[1:],
        duration=round(1000 / FPS), loop=0, disposal=2, optimize=False,
    )

    metadata = {
        "source": str(SOURCE), "frames": EXPECTED, "fps": FPS, "duration": duration,
        "source_union_bbox": list(union), "scale": scale,
        "scaled_canvas": [scaled_width, scaled_height], "paste": [paste_x, paste_y],
        "atlas_pages_added": pages,
    }
    (OUT / "metadata.json").write_text(json.dumps(metadata, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(metadata, indent=2))


if __name__ == "__main__":
    main()
