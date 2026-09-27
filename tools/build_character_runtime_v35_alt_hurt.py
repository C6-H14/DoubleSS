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


SOURCE = ROOT / "art_candidates" / "alt_fencer_hurt_v1" / "alpha_frames"
CURRENT = ROOT / "src" / "main" / "resources" / "img" / "char" / "spine"
OUT = ROOT / "art_candidates" / "character_runtime_v35_alt_hurt"
IMAGES = OUT / "images"
EXPORT = OUT / "export"
EXPECTED = 40
FPS = 60.0
TARGET_SUBJECT_HEIGHT = 342
TARGET_FOOT_Y = 728
TARGET_CENTER_X = 512
BLEED_MARGIN = 4
FIRST_HURT_PAGE = 25


def prune_unused_legacy_pages(export: Path) -> None:
    """Remove stale frame attachments and atlas pages left by older previews."""
    json_path = export / "character.json"
    data = json.loads(json_path.read_text(encoding="utf-8"))

    used = {
        slot["attachment"] for slot in data.get("slots", [])
        if slot.get("attachment")
    }
    for animation in data.get("animations", {}).values():
        for slot in animation.get("slots", {}).values():
            for key in slot.get("attachment", []):
                if key.get("name"):
                    used.add(key["name"])

    default_skin = data.get("skins", {}).get("default", {})
    for slot_name, attachments in default_skin.items():
        default_skin[slot_name] = {
            name: value for name, value in attachments.items() if name in used
        }
    json_path.write_text(
        json.dumps(data, ensure_ascii=False, separators=(",", ":")), encoding="utf-8"
    )

    atlas_path = export / "character.atlas"
    atlas_text = atlas_path.read_text(encoding="utf-8")
    page_re = re.compile(r"(?m)^(character\d*\.png)\n")
    matches = list(page_re.finditer(atlas_text))
    kept = []
    for index, match in enumerate(matches):
        end = matches[index + 1].start() if index + 1 < len(matches) else len(atlas_text)
        section = atlas_text[match.start():end]
        region_names = {
            line for line in section.splitlines()[5:]
            if line and not line.startswith(" ")
        }
        if region_names & used:
            kept.append(section.rstrip() + "\n\n")
        else:
            page_path = export / match.group(1)
            if page_path.exists():
                page_path.unlink()
    atlas_path.write_text("".join(kept).rstrip() + "\n", encoding="utf-8")


def alpha_union(paths: list[Path]) -> tuple[int, int, int, int]:
    boxes = []
    for path in paths:
        box = Image.open(path).convert("RGBA").getchannel("A").getbbox()
        if box is None:
            raise RuntimeError(f"Empty alpha frame: {path}")
        boxes.append(box)
    return (
        min(b[0] for b in boxes), min(b[1] for b in boxes),
        max(b[2] for b in boxes), max(b[3] for b in boxes),
    )


def main() -> None:
    if OUT.exists():
        shutil.rmtree(OUT)
    IMAGES.mkdir(parents=True)
    EXPORT.mkdir(parents=True)

    for path in CURRENT.glob("character*.png"):
        shutil.copy2(path, EXPORT / path.name)
    shutil.copy2(CURRENT / "character.atlas", EXPORT / "character.atlas")
    shutil.copy2(CURRENT / "character.json", EXPORT / "character.json")

    # Replace the previously generated alternate-hurt atlas pages instead of
    # appending duplicate alt_hurt region names on every rebuild.
    for path in EXPORT.glob("character*.png"):
        suffix = path.stem.removeprefix("character")
        if suffix.isdigit() and int(suffix) >= FIRST_HURT_PAGE:
            path.unlink()
    atlas_path = EXPORT / "character.atlas"
    atlas_text = atlas_path.read_text(encoding="utf-8")
    marker = f"\ncharacter{FIRST_HURT_PAGE}.png\n"
    if marker in atlas_text:
        atlas_path.write_text(
            atlas_text.split(marker, 1)[0].rstrip() + "\n", encoding="utf-8"
        )
    prune_unused_legacy_pages(EXPORT)

    paths = sorted(SOURCE.glob("alt_hurt_*.png"))
    if len(paths) != EXPECTED:
        raise RuntimeError(f"Expected {EXPECTED} frames, found {len(paths)}")
    union = alpha_union(paths)
    ux0, uy0, ux1, uy1 = union
    scale = TARGET_SUBJECT_HEIGHT / (uy1 - uy0)
    source_width, source_height = Image.open(paths[0]).size
    scaled_width = round(source_width * scale)
    scaled_height = round(source_height * scale)
    # Align hurt by its neutral endpoint, not by the union of the whole motion.
    # The sword/body extend farther right during the hurt pose; union-centering
    # shifted the neutral first frame about 22 runtime pixels to the left.
    first_bbox = Image.open(paths[0]).convert("RGBA").getchannel("A").getbbox()
    last_bbox = Image.open(paths[-1]).convert("RGBA").getchannel("A").getbbox()
    endpoint_center_x = (
        first_bbox[0] + first_bbox[2] + last_bbox[0] + last_bbox[2]
    ) / 4.0
    paste_x = round(TARGET_CENTER_X - endpoint_center_x * scale)
    paste_y = round(TARGET_FOOT_Y - uy1 * scale)

    data = json.loads((EXPORT / "character.json").read_text(encoding="utf-8"))
    attachments = data["skins"]["default"]["character_frame"]
    # Drop frames from a previously longer hurt build before inserting the
    # current sequence, otherwise SkeletonJson still requires their removed
    # atlas regions even though the animation timeline no longer uses them.
    for old_name in [name for name in attachments if name.startswith("alt_hurt_")]:
        del attachments[old_name]
    names = []
    staged = []
    for index, path in enumerate(paths, 1):
        name = f"alt_hurt_{index:03d}"
        source = Image.open(path).convert("RGBA")
        resized = source.resize((scaled_width, scaled_height), Image.Resampling.LANCZOS)
        canvas = Image.new("RGBA", (1024, 1024), (0, 0, 0, 0))
        canvas.paste(resized, (paste_x, paste_y))
        canvas = builder.add_runtime_bleed(canvas, radius=4.0)
        canvas = builder.premultiply_alpha(canvas)
        canvas.save(IMAGES / f"{name}.png", optimize=True)
        attachments[name] = {
            "path": name, "x": 0, "y": 210.0,
            "width": 1024, "height": 1024,
        }
        names.append(name)
        staged.append(canvas)

    duration = EXPECTED / FPS
    keys = [{"time": round(i / FPS, 6), "name": name}
            for i, name in enumerate(names)]
    keys.append({"time": duration, "name": names[-1]})
    data["animations"]["hurt"] = {
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

    page_number = FIRST_HURT_PAGE
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
        "source": str(SOURCE), "frames": EXPECTED, "fps": FPS,
        "duration": duration, "source_union_bbox": list(union),
        "scale": scale, "scaled_canvas": [scaled_width, scaled_height],
        "paste": [paste_x, paste_y], "atlas_pages_added": pages,
    }
    (OUT / "metadata.json").write_text(
        json.dumps(metadata, indent=2) + "\n", encoding="utf-8"
    )
    print(json.dumps(metadata, indent=2))


if __name__ == "__main__":
    main()
