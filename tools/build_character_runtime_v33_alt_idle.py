from __future__ import annotations

import json
import shutil
import sys
from pathlib import Path

from PIL import Image


ROOT = Path(r"D:\StSmod\DoubleSS")
sys.path.insert(0, str(ROOT / "tools"))
import build_character_runtime_v25_idle as builder


SOURCE = ROOT / "art_candidates" / "alt_fencer_idle_v1" / "alpha_frames"
CURRENT = ROOT / "src" / "main" / "resources" / "img" / "char" / "spine"
OUT = ROOT / "art_candidates" / "character_runtime_v33_alt_idle"
IMAGES = OUT / "images"
EXPORT = OUT / "export"
FPS = 24.15
EXPECTED = 80
UNION_BBOX = (105, 128, 592, 784)
TARGET_SUBJECT_HEIGHT = 342
TARGET_FOOT_Y = 728
TARGET_CENTER_X = 512
BLEED_MARGIN = 4
FIRST_ALT_PAGE = 18
SOURCE_PATTERN = "alt_idle_*.png"
ATTACHMENT_PREFIX = "alt_idle"


def main() -> None:
    if OUT.exists():
        shutil.rmtree(OUT)
    IMAGES.mkdir(parents=True)
    EXPORT.mkdir(parents=True)

    # Start from the exact currently deployed runtime so every non-idle
    # animation and atlas page remains byte-for-byte intact.
    for path in CURRENT.glob("character*.png"):
        shutil.copy2(path, EXPORT / path.name)
    shutil.copy2(CURRENT / "character.atlas", EXPORT / "character.atlas")
    shutil.copy2(CURRENT / "character.json", EXPORT / "character.json")

    # This builder is intentionally idempotent.  A previous generated idle may
    # already occupy character18+ in CURRENT; remove those generated pages and
    # their atlas sections before packing the replacement again.
    for path in EXPORT.glob("character*.png"):
        suffix = path.stem.removeprefix("character")
        if suffix.isdigit() and int(suffix) >= FIRST_ALT_PAGE:
            path.unlink()
    atlas_path = EXPORT / "character.atlas"
    atlas_text = atlas_path.read_text(encoding="utf-8")
    marker = f"\ncharacter{FIRST_ALT_PAGE}.png\n"
    if marker in atlas_text:
        atlas_text = atlas_text.split(marker, 1)[0].rstrip() + "\n"
        atlas_path.write_text(atlas_text, encoding="utf-8")

    data = json.loads((EXPORT / "character.json").read_text(encoding="utf-8"))
    attachments = data["skins"]["default"]["character_frame"]
    paths = sorted(SOURCE.glob(SOURCE_PATTERN))
    if len(paths) != EXPECTED:
        raise RuntimeError(f"Expected {EXPECTED} frames, found {len(paths)}")

    ux0, uy0, ux1, uy1 = UNION_BBOX
    scale = TARGET_SUBJECT_HEIGHT / (uy1 - uy0)
    scaled_size = round(960 * scale)
    paste_x = round(TARGET_CENTER_X - ((ux0 + ux1) / 2.0) * scale)
    paste_y = round(TARGET_FOOT_Y - uy1 * scale)

    names: list[str] = []
    staged: list[Image.Image] = []
    for index, path in enumerate(paths, 1):
        name = f"{ATTACHMENT_PREFIX}_{index:03d}"
        source = Image.open(path).convert("RGBA")
        resized = source.resize((scaled_size, scaled_size), Image.Resampling.LANCZOS)
        canvas = Image.new("RGBA", (1024, 1024), (0, 0, 0, 0))
        canvas.paste(resized, (paste_x, paste_y))
        canvas = builder.add_runtime_bleed(canvas, radius=4.0)
        canvas = builder.premultiply_alpha(canvas)
        canvas.save(IMAGES / f"{name}.png", optimize=True)
        attachments[name] = {
            "path": name,
            "x": 0,
            "y": 210.0,
            "width": 1024,
            "height": 1024,
        }
        names.append(name)
        staged.append(canvas)

    keys = []
    for index, name in enumerate(names):
        # Slay the Spire ships Spine 3.4, whose JSON reader requires an
        # explicit time value even on the first attachment keyframe.
        key = {"time": round(index / FPS, 6), "name": name}
        keys.append(key)
    duration = round(EXPECTED / FPS, 6)
    keys.append({"time": duration, "name": names[-1]})
    data["animations"]["idle"] = {
        "slots": {"character_frame": {"attachment": keys}}
    }
    (EXPORT / "character.json").write_text(
        json.dumps(data, ensure_ascii=False, separators=(",", ":")),
        encoding="utf-8",
    )

    builder.EXPORT = EXPORT
    items = []
    for name, image in zip(names, staged):
        alpha_bbox = image.getchannel("A").getbbox()
        if alpha_bbox is None:
            raise RuntimeError(f"Empty staged frame: {name}")
        left, top, right, bottom = alpha_bbox
        bbox = (
            max(0, left - BLEED_MARGIN),
            max(0, top - BLEED_MARGIN),
            min(image.width, right + BLEED_MARGIN),
            min(image.height, bottom + BLEED_MARGIN),
        )
        items.append((name, image, bbox))

    page_number = FIRST_ALT_PAGE
    pages = []
    sections = []
    while items:
        items, section, page_name, count, size = builder.pack_page(items, page_number)
        sections.append(section)
        pages.append({"page": page_name, "regions": count, "size": list(size)})
        page_number += 1
    with (EXPORT / "character.atlas").open("a", encoding="utf-8") as stream:
        stream.write("".join(sections))

    # Runtime-scale preview against a neutral background.
    previews = []
    for image in staged:
        # Staged images are premultiplied for the game renderer; unpremultiply
        # is unnecessary for this small placement preview on dark gray.
        bg = Image.new("RGBA", image.size, (42, 42, 42, 255))
        bg.alpha_composite(image)
        previews.append(bg.resize((512, 512), Image.Resampling.LANCZOS).convert("RGB"))
    previews[0].save(
        OUT / "runtime_preview.gif",
        save_all=True,
        append_images=previews[1:],
        duration=round(1000 / FPS),
        loop=0,
        disposal=2,
        optimize=False,
    )

    atlas_text = (EXPORT / "character.atlas").read_text(encoding="utf-8")
    missing = [name for name in names if f"{name}\n" not in atlas_text]
    runtime = json.loads((EXPORT / "character.json").read_text(encoding="utf-8"))
    timeline = runtime["animations"]["idle"]["slots"]["character_frame"]["attachment"]
    if missing or len(timeline) != EXPECTED + 1 or abs(timeline[-1]["time"] - duration) > 1e-6:
        raise RuntimeError({"missing": missing, "timeline": len(timeline), "last": timeline[-1]})

    metadata = {
        "source": str(SOURCE),
        "frames": EXPECTED,
        "fps": FPS,
        "duration": duration,
        "source_union_bbox": list(UNION_BBOX),
        "scale": scale,
        "scaled_canvas": [scaled_size, scaled_size],
        "paste": [paste_x, paste_y],
        "runtime_subject_height": TARGET_SUBJECT_HEIGHT,
        "runtime_foot_y": TARGET_FOOT_Y,
        "attachment_y": 210.0,
        "atlas_pages_added": pages,
        "total_atlas_pages": len(list(EXPORT.glob("character*.png"))),
    }
    (OUT / "metadata.json").write_text(json.dumps(metadata, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(metadata, indent=2))


if __name__ == "__main__":
    main()
