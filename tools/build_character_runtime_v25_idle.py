from pathlib import Path
import importlib.util
import json
import shutil

from PIL import Image
import cv2
import numpy as np
from scipy.ndimage import distance_transform_edt


ROOT = Path(r"D:\StSmod\DoubleSS")
BASE = ROOT / "art_candidates" / "character_runtime_v24_hurt"
SOURCE = ROOT / "art_candidates" / "video_source" / "idle" / "short_blink_final_v15_garment_color_stable" / "alpha_frames"
OUT = ROOT / "art_candidates" / "character_runtime_v25_idle"
IMAGES = OUT / "images"
EXPORT = OUT / "export"
EXPECTED = 180
FPS = 60
PAGE_SIZE = 2048
PADDING = 4
BLEED_MARGIN = 0


def add_runtime_bleed(image, radius=4.0):
    """Extrude RGB after final resize, where the runtime filter footprint lives."""
    rgba = np.asarray(image.convert("RGBA"), dtype=np.uint8).copy()
    alpha = rgba[:, :, 3]
    subject = alpha > 0
    rgb = rgba[:, :, :3]
    hsv = cv2.cvtColor(rgb, cv2.COLOR_RGB2HSV)
    saturated = hsv[:, :, 1] > 180
    yellow = (rgb[:, :, 0] > 225) & (rgb[:, :, 1] > 185) & (rgb[:, :, 2] < 90)
    red_or_pink = (rgb[:, :, 0] > 205) & (rgb[:, :, 1] < 125) & (rgb[:, :, 2] < 155)
    suspicious = (alpha > 0) & (alpha < 160) & saturated & (yellow | red_or_pink)
    clean_visible = subject & (~suspicious) & (alpha >= 24)
    _, clean_nearest = distance_transform_edt(~clean_visible, return_indices=True)
    clean_rgb = rgba[clean_nearest[0], clean_nearest[1], :3]
    rgba[suspicious, :3] = clean_rgb[suspicious]

    _, nearest = distance_transform_edt(~subject, return_indices=True)
    distance_to_subject = distance_transform_edt(~subject)
    bleed = (~subject) & (distance_to_subject <= radius)
    donors = rgba[nearest[0], nearest[1], :3]
    rgba[bleed, :3] = donors[bleed]
    return Image.fromarray(rgba, "RGBA")


def premultiply_alpha(image):
    """Slay the Spire forces SkeletonMeshRenderer PMA=true."""
    rgba = np.asarray(image.convert("RGBA"), dtype=np.uint8).copy()
    alpha = rgba[:, :, 3:4].astype(np.uint16)
    rgb = rgba[:, :, :3].astype(np.uint16)
    rgba[:, :, :3] = ((rgb * alpha + 127) // 255).astype(np.uint8)
    return Image.fromarray(rgba, "RGBA")


def next_page_number(folder: Path) -> int:
    numbers = []
    for path in folder.glob("character*.png"):
        suffix = path.stem.removeprefix("character")
        numbers.append(1 if not suffix else int(suffix))
    return max(numbers) + 1


def pack_page(items, page_number):
    page = Image.new("RGBA", (PAGE_SIZE, PAGE_SIZE), (0, 0, 0, 0))
    placements, remaining = [], []
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
        # Raw paste is intentional: alpha_composite discards RGB stored in
        # fully transparent bleed texels, recreating the dark linear-filter halo.
        page.paste(crop, (x, y))
        placements.append((name, x, y, width, height, left,
                           image.height - bottom, image.width, image.height))
        x += width + PADDING
        row_height = max(row_height, height)
    if not placements:
        raise RuntimeError("Idle frame cannot fit on an empty atlas page")

    used_w = max(p[1] + p[3] for p in placements) + PADDING
    used_h = max(p[2] + p[4] for p in placements) + PADDING
    page = page.crop((0, 0, used_w, used_h))
    page_name = f"character{page_number}.png"
    page.save(EXPORT / page_name, optimize=True)
    lines = ["", page_name, f"size: {page.width},{page.height}",
             "format: RGBA8888", "filter: Linear,Linear", "repeat: none"]
    for name, px, py, width, height, off_x, off_y, orig_w, orig_h in placements:
        lines.extend([
            name, "  rotate: false", f"  xy: {px}, {py}",
            f"  size: {width}, {height}", f"  orig: {orig_w}, {orig_h}",
            f"  offset: {off_x}, {off_y}", "  index: -1",
        ])
    return remaining, "\n".join(lines) + "\n", page_name, len(placements), page.size


def main():
    if OUT.exists():
        shutil.rmtree(OUT)
    IMAGES.mkdir(parents=True)
    EXPORT.mkdir(parents=True)

    for path in (BASE / "images").glob("*.png"):
        shutil.copy2(path, IMAGES / path.name)
    for path in (BASE / "export").glob("character*.png"):
        shutil.copy2(path, EXPORT / path.name)
    shutil.copy2(BASE / "export" / "character.atlas", EXPORT / "character.atlas")

    source_json = BASE / "DoubleSS_character_v24_import_3.8.json"
    data = json.loads(source_json.read_text(encoding="utf-8"))
    data["skeleton"]["images"] = str(IMAGES.resolve()).replace("\\", "/") + "/"
    attachments = data["skins"][0]["attachments"]["character_frame"]
    paths = sorted(SOURCE.glob("idle_*.png"))
    if len(paths) != EXPECTED:
        raise RuntimeError(f"Expected {EXPECTED} idle frames, found {len(paths)}")

    idle_names = []
    for index, path in enumerate(paths, 1):
        name = f"idle_{index:03d}"
        source = Image.open(path).convert("RGBA")
        resized = source.resize((556, 417), Image.Resampling.LANCZOS)
        canvas = Image.new("RGBA", (1024, 1024), (0, 0, 0, 0))
        # Preserve RGB in alpha-zero bleed texels through the staging canvas.
        canvas.paste(resized, (227, 336))
        canvas = premultiply_alpha(canvas)
        canvas.save(IMAGES / f"{name}.png", optimize=True)
        attachments[name] = {"path": name, "x": 0, "y": 210.0,
                             "width": 1024, "height": 1024}
        idle_names.append(name)

    keys = []
    for index, name in enumerate(idle_names):
        key = {"name": name}
        if index:
            key["time"] = round(index / FPS, 6)
        keys.append(key)
    # Hold frame 180 for its full 1/60 second and make the loop exactly 3.0s.
    keys.append({"time": 3.0, "name": idle_names[-1]})
    data["animations"]["idle"] = {
        "slots": {"character_frame": {"attachment": keys}}
    }

    import_json = OUT / "DoubleSS_character_v25_import_3.8.json"
    import_json.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    converter_path = ROOT / "tools" / "spine-compat-probe" / "convert_spine_38_to_34.py"
    spec = importlib.util.spec_from_file_location("converter", converter_path)
    converter = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(converter)
    converter.convert(import_json, EXPORT / "character.json")

    items = []
    for name in idle_names:
        image = Image.open(IMAGES / f"{name}.png").convert("RGBA")
        alpha_bbox = image.getchannel("A").getbbox()
        if alpha_bbox is None:
            raise RuntimeError(f"Empty idle frame: {name}")
        # Transparent texels carry extruded subject RGB for linear filtering.
        # Keep them in the packed region instead of cropping back to alpha.
        left, top, right, bottom = alpha_bbox
        bbox = (max(0, left - BLEED_MARGIN), max(0, top - BLEED_MARGIN),
                min(image.width, right + BLEED_MARGIN), min(image.height, bottom + BLEED_MARGIN))
        items.append((name, image, bbox))
    page_number = next_page_number(EXPORT)
    pages = []
    atlas_sections = []
    while items:
        items, section, page_name, count, size = pack_page(items, page_number)
        atlas_sections.append(section)
        pages.append({"page": page_name, "regions": count, "size": list(size)})
        page_number += 1
    with (EXPORT / "character.atlas").open("a", encoding="utf-8") as stream:
        stream.write("".join(atlas_sections))

    runtime = json.loads((EXPORT / "character.json").read_text(encoding="utf-8"))
    timeline = runtime["animations"]["idle"]["slots"]["character_frame"]["attachment"]
    atlas_text = (EXPORT / "character.atlas").read_text(encoding="utf-8")
    missing = [name for name in idle_names if f"{name}\n" not in atlas_text]
    if len(timeline) != 181 or abs(timeline[-1]["time"] - 3.0) > 1e-6 or missing:
        raise RuntimeError({"timeline_keys": len(timeline), "last": timeline[-1], "missing": missing})

    metadata = {
        "idle": {"unique_frames": EXPECTED, "timeline_keys": len(timeline),
                 "fps": FPS, "duration": 3.0,
                 "source_canvas": [1112, 834], "runtime_canvas": [1024, 1024],
                 "resized": [556, 417], "paste": [227, 336],
                 "attachment_y": 210.0, "atlas_pages_added": pages},
        "transparent_rgb_bleed_margin": BLEED_MARGIN,
        "total_atlas_pages": len(list(EXPORT.glob("character*.png"))),
    }
    (OUT / "metadata.json").write_text(json.dumps(metadata, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(metadata, indent=2))


if __name__ == "__main__":
    main()
