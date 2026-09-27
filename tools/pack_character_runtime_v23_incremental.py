from pathlib import Path
import json
import shutil

from PIL import Image


ROOT = Path(r"D:\StSmod\DoubleSS")
BASE_EXPORT = ROOT / "art_candidates" / "character_runtime_v22_command_ally" / "export"
RUNTIME = ROOT / "art_candidates" / "character_runtime_v23_cast_special"
IMAGES = RUNTIME / "images"
EXPORT = RUNTIME / "export"
PAGE_SIZE = 2048
PADDING = 4


def next_page_number() -> int:
    numbers = []
    for path in BASE_EXPORT.glob("character*.png"):
        suffix = path.stem.removeprefix("character")
        numbers.append(1 if not suffix else int(suffix))
    return max(numbers) + 1


def pack_page(items: list[tuple[str, Image.Image, tuple[int, int, int, int]]], page_number: int):
    page = Image.new("RGBA", (PAGE_SIZE, PAGE_SIZE), (0, 0, 0, 0))
    placements = []
    remaining = []
    x = y = PADDING
    row_height = 0
    for name, image, bbox in items:
        left, top, right, bottom = bbox
        crop = image.crop(bbox)
        width, height = crop.size
        if x + width + PADDING > PAGE_SIZE:
            x = PADDING
            y += row_height + PADDING
            row_height = 0
        if y + height + PADDING > PAGE_SIZE:
            remaining.append((name, image, bbox))
            continue
        page.alpha_composite(crop, (x, y))
        placements.append((name, x, y, width, height, left, image.height - bottom, image.width, image.height))
        x += width + PADDING
        row_height = max(row_height, height)
    if not placements:
        raise RuntimeError("A frame cannot fit on an empty atlas page")

    used_height = max(p[2] + p[4] for p in placements) + PADDING
    used_width = max(p[1] + p[3] for p in placements) + PADDING
    page = page.crop((0, 0, used_width, used_height))
    page_name = "character.png" if page_number == 1 else f"character{page_number}.png"
    page.save(EXPORT / page_name, optimize=True)

    lines = [
        "",
        page_name,
        f"size: {page.width},{page.height}",
        "format: RGBA8888",
        "filter: Linear,Linear",
        "repeat: none",
    ]
    for name, px, py, width, height, off_x, off_y, orig_w, orig_h in placements:
        lines.extend([
            name,
            "  rotate: false",
            f"  xy: {px}, {py}",
            f"  size: {width}, {height}",
            f"  orig: {orig_w}, {orig_h}",
            f"  offset: {off_x}, {off_y}",
            "  index: -1",
        ])
    return remaining, "\n".join(lines) + "\n", page_name, len(placements)


def main() -> None:
    # Restore the proven v22 export, then replace only the JSON with v23 data.
    for path in EXPORT.glob("character*.png"):
        path.unlink()
    atlas_path = EXPORT / "character.atlas"
    if atlas_path.exists():
        atlas_path.unlink()
    for path in BASE_EXPORT.glob("character*.png"):
        shutil.copy2(path, EXPORT / path.name)
    shutil.copy2(BASE_EXPORT / "character.atlas", atlas_path)

    paths = sorted(IMAGES.glob("cast_special_*.png"))
    if len(paths) != 75:
        raise RuntimeError(f"Expected 75 runtime frames, found {len(paths)}")
    items = []
    for path in paths:
        image = Image.open(path).convert("RGBA")
        bbox = image.getchannel("A").getbbox()
        if bbox is None:
            raise RuntimeError(f"Empty frame: {path}")
        items.append((path.stem, image, bbox))

    page_number = next_page_number()
    atlas_append = []
    pages = []
    while items:
        items, text, page_name, count = pack_page(items, page_number)
        atlas_append.append(text)
        pages.append({"page": page_name, "regions": count})
        page_number += 1
    with atlas_path.open("a", encoding="utf-8") as stream:
        stream.write("".join(atlas_append))

    # Verify that every new attachment exists in both JSON and atlas.
    runtime_json = json.loads((EXPORT / "character.json").read_text(encoding="utf-8"))
    animation = runtime_json["animations"]["cast_special"]
    timeline = animation["slots"]["character_frame"]["attachment"]
    if len(timeline) != 75:
        raise RuntimeError(f"Runtime JSON has {len(timeline)} cast_special keys")
    atlas_text = atlas_path.read_text(encoding="utf-8")
    missing = [f"cast_special_{i:03d}" for i in range(1, 76)
               if f"cast_special_{i:03d}\n" not in atlas_text]
    if missing:
        raise RuntimeError(f"Missing atlas regions: {missing}")

    report = {"base_pages": next_page_number() - 1, "new_pages": pages,
              "total_pages": len(list(EXPORT.glob("character*.png")))}
    (RUNTIME / "atlas_pack_report.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    main()
