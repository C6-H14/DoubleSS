from pathlib import Path
import argparse

from PIL import Image


PAGE_SIZE = 2048
PADDING = 6
EXTRUDE = 2


def crop_region(path: Path):
    image = Image.open(path).convert("RGBA")
    bbox = image.getchannel("A").point(lambda value: 255 if value >= 2 else 0).getbbox()
    if bbox is None:
        raise ValueError(f"Empty image: {path}")
    x0, y0, x1, y1 = bbox
    return {
        "name": path.stem,
        "crop": image.crop(bbox),
        "orig": image.size,
        "offset": (x0, image.height - y1),
        "w": x1 - x0,
        "h": y1 - y0,
    }


def pack_shelves(regions):
    remaining = sorted(regions, key=lambda r: (-r["h"], -r["w"], r["name"]))
    pages = []
    while remaining:
        placed = []
        pending = []
        x = PADDING
        y = PADDING
        shelf_h = 0
        for region in remaining:
            rw = region["w"] + PADDING
            rh = region["h"] + PADDING
            if rw > PAGE_SIZE or rh > PAGE_SIZE:
                raise ValueError(f"Region too large for atlas: {region['name']}")
            if x + rw > PAGE_SIZE:
                x = PADDING
                y += shelf_h
                shelf_h = 0
            if y + rh > PAGE_SIZE:
                pending.append(region)
                continue
            item = dict(region)
            item["x"] = x
            item["y"] = y
            placed.append(item)
            x += rw
            shelf_h = max(shelf_h, rh)
        if not placed:
            raise RuntimeError("Packing made no progress")
        pages.append(placed)
        remaining = pending
    return pages


def write_page(path: Path, regions):
    used_w = max(r["x"] + r["w"] + PADDING for r in regions)
    used_h = max(r["y"] + r["h"] + PADDING for r in regions)
    width = min(PAGE_SIZE, max(64, used_w))
    height = min(PAGE_SIZE, max(64, used_h))
    page = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    for region in regions:
        crop = region["crop"]
        x, y = region["x"], region["y"]
        page.alpha_composite(crop, (x, y))
        # Duplicate the outer texels beyond the atlas region. Spine samples
        # with Linear filtering; this prevents transparent neighboring texels
        # from producing dark/colored fringes around the character.
        top = crop.crop((0, 0, crop.width, 1)).resize((crop.width, EXTRUDE))
        bottom = crop.crop((0, crop.height - 1, crop.width, crop.height)).resize((crop.width, EXTRUDE))
        left = crop.crop((0, 0, 1, crop.height)).resize((EXTRUDE, crop.height))
        right = crop.crop((crop.width - 1, 0, crop.width, crop.height)).resize((EXTRUDE, crop.height))
        page.alpha_composite(top, (x, y - EXTRUDE))
        page.alpha_composite(bottom, (x, y + crop.height))
        page.alpha_composite(left, (x - EXTRUDE, y))
        page.alpha_composite(right, (x + crop.width, y))
        for px, py, color in (
            (x - EXTRUDE, y - EXTRUDE, crop.getpixel((0, 0))),
            (x + crop.width, y - EXTRUDE, crop.getpixel((crop.width - 1, 0))),
            (x - EXTRUDE, y + crop.height, crop.getpixel((0, crop.height - 1))),
            (x + crop.width, y + crop.height, crop.getpixel((crop.width - 1, crop.height - 1))),
        ):
            corner = Image.new("RGBA", (EXTRUDE, EXTRUDE), color)
            page.alpha_composite(corner, (px, py))
    page.save(path, optimize=True)
    return width, height


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("input", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument("--name", default="character")
    parser.add_argument("--pattern", default="attack_sword_*.png")
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)

    regions = [crop_region(p) for p in sorted(args.input.glob(args.pattern))]
    pages = pack_shelves(regions)
    atlas_lines = []
    for page_index, packed in enumerate(pages, 1):
        page_name = f"{args.name}.png" if page_index == 1 else f"{args.name}{page_index}.png"
        width, height = write_page(args.output / page_name, packed)
        if atlas_lines:
            atlas_lines.append("")
        atlas_lines.extend([
            page_name,
            f"size: {width},{height}",
            "format: RGBA8888",
            "filter: Linear,Linear",
            "repeat: none",
        ])
        for region in sorted(packed, key=lambda r: r["name"]):
            atlas_lines.extend([
                region["name"],
                "  rotate: false",
                f"  xy: {region['x']}, {region['y']}",
                f"  size: {region['w']}, {region['h']}",
                f"  orig: {region['orig'][0]}, {region['orig'][1]}",
                f"  offset: {region['offset'][0]}, {region['offset'][1]}",
                "  index: -1",
            ])
    (args.output / f"{args.name}.atlas").write_text("\n".join(atlas_lines) + "\n", encoding="utf-8")
    print(f"packed {len(regions)} regions into {len(pages)} page(s)")
    for i in range(1, len(pages) + 1):
        print(f"{args.name if i == 1 else args.name + str(i)}.png")


if __name__ == "__main__":
    main()
