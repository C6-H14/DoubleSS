from collections import deque
from pathlib import Path
import json
import shutil
import sys

import numpy as np
from PIL import Image, ImageDraw


def connected_components(mask: np.ndarray):
    h, w = mask.shape
    seen = np.zeros_like(mask, dtype=bool)
    for y0, x0 in zip(*np.where(mask & ~seen)):
        if seen[y0, x0]:
            continue
        q = deque([(int(y0), int(x0))])
        seen[y0, x0] = True
        coords = []
        while q:
            y, x = q.popleft()
            coords.append((y, x))
            for ny, nx in ((y - 1, x), (y + 1, x), (y, x - 1), (y, x + 1)):
                if 0 <= ny < h and 0 <= nx < w and mask[ny, nx] and not seen[ny, nx]:
                    seen[ny, nx] = True
                    q.append((ny, nx))
        yield coords


def clean_one(image: Image.Image, debug=False):
    rgba = np.asarray(image.convert("RGBA")).copy()
    rgb = rgba[..., :3]
    alpha = rgba[..., 3]
    h, w = alpha.shape
    chroma = rgb.max(axis=2).astype(int) - rgb.min(axis=2).astype(int)

    # Background-white seeds. Grow slightly into antialiased gray-white pixels.
    seed = (alpha > 80) & (rgb.min(axis=2) >= 238) & (chroma <= 16)
    grow = (alpha > 48) & (rgb.min(axis=2) >= 208) & (chroma <= 32)

    # Golden/blond pixels used only to identify white holes enclosed by hair.
    r, g, b = (rgb[..., i].astype(int) for i in range(3))
    hair = (alpha > 100) & (r >= 145) & (g >= 85) & (b <= 155) & (r - b >= 35)
    hair_ys, hair_xs = np.where(hair & (np.indices(hair.shape)[0] < int(h * 0.45)))
    hair_center_x = float(np.median(hair_xs)) if len(hair_xs) else w / 2

    removal = np.zeros_like(alpha, dtype=bool)
    removed_components = []
    for component in connected_components(grow):
        if not any(seed[y, x] for y, x in component):
            continue
        ys = [p[0] for p in component]
        xs = [p[1] for p in component]
        # Hair holes live in the upper 48% of the frame. This excludes white
        # socks and the sword even when they contain near-white highlights.
        if min(ys) >= int(h * 0.48):
            continue

        comp_set = set(component)
        ring = set()
        for y, x in component:
            for ny, nx in ((y - 1, x), (y + 1, x), (y, x - 1), (y, x + 1)):
                if 0 <= ny < h and 0 <= nx < w and (ny, nx) not in comp_set:
                    ring.add((ny, nx))
        if not ring:
            continue
        hair_fraction = sum(bool(hair[y, x]) for y, x in ring) / len(ring)
        transparent_fraction = sum(alpha[y, x] < 32 for y, x in ring) / len(ring)
        area = len(component)
        # Require strong hair enclosure, or a small background chip touching
        # both hair and existing transparency. Eyes/collar are not hair-ringed.
        if debug and area >= 8:
            print("candidate", area, [min(xs), min(ys), max(xs), max(ys)],
                  "hair", round(hair_fraction, 3), "transparent", round(transparent_fraction, 3))
        component_center_x = (min(xs) + max(xs)) / 2
        outer_hair_gap = (
            area >= 25
            and hair_fraction >= 0.015
            and abs(component_center_x - hair_center_x) >= 50
        )
        if hair_fraction >= 0.075 or outer_hair_gap or (
            area <= 600 and hair_fraction >= 0.05 and transparent_fraction >= 0.08
        ):
            for y, x in component:
                removal[y, x] = True
            removed_components.append(
                {
                    "area": area,
                    "bbox": [min(xs), min(ys), max(xs), max(ys)],
                    "hair_fraction": round(hair_fraction, 3),
                }
            )

    alpha[removal] = 0

    # Remove white matte contamination from semitransparent silhouette pixels.
    edge = (alpha > 0) & (alpha < 250)
    a = alpha.astype(np.float32) / 255.0
    safe_a = np.maximum(a, 1 / 255.0)
    recovered = (rgb.astype(np.float32) - 255.0 * (1.0 - a[..., None])) / safe_a[..., None]
    recovered = np.clip(recovered, 0, 255).astype(np.uint8)
    rgb[edge] = recovered[edge]

    # Fully transparent pixels carry neutral black RGB to prevent atlas bleed.
    rgb[alpha == 0] = 0
    return Image.fromarray(rgba, "RGBA"), removed_components, int(removal.sum()), int(edge.sum())


def contact_sheet(images: list[Image.Image], path: Path):
    tw, th, cols = 278, 209, 4
    cell_w, cell_h = tw + 16, th + 34
    sheet = Image.new("RGB", (cell_w * cols, cell_h * 4), "#303238")
    draw = ImageDraw.Draw(sheet)
    for i, image in enumerate(images):
        thumb = image.resize((tw, th), Image.Resampling.LANCZOS)
        bg = Image.new("RGB", (tw, th), "#ddd")
        bd = ImageDraw.Draw(bg)
        for y in range(0, th, 10):
            for x in range(0, tw, 10):
                if (x // 10 + y // 10) % 2:
                    bd.rectangle((x, y, x + 9, y + 9), fill="#bbb")
        bg.paste(thumb, mask=thumb.getchannel("A"))
        px, py = (i % cols) * cell_w + 8, (i // cols) * cell_h + 8
        sheet.paste(bg, (px, py))
        draw.text((px + 3, py + th + 4), f"{i + 1:02d}", fill="white")
    sheet.save(path, quality=95)


def main():
    source = Path(sys.argv[1])
    output = Path(sys.argv[2])
    if output.exists():
        shutil.rmtree(output)
    output.mkdir(parents=True)
    records, images = [], []
    for path in sorted(source.glob("attack_sword_*.png")):
        cleaned, comps, removed, edges = clean_one(Image.open(path), debug=(path.name == "attack_sword_01.png"))
        cleaned.save(output / path.name, optimize=True)
        images.append(cleaned)
        records.append(
            {"frame": path.name, "removed_pixels": removed, "decontaminated_edge_pixels": edges, "removed_components": comps}
        )
    contact_sheet(images, output.parent / "cleaned_contact_sheet.png")
    (output.parent / "cleanup_report.json").write_text(
        json.dumps(records, ensure_ascii=False, indent=2), encoding="utf-8"
    )


if __name__ == "__main__":
    main()
