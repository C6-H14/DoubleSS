from pathlib import Path
import json
import sys

import numpy as np
from PIL import Image


ROOT = Path(r"D:\StSmod\DoubleSS")
sys.path.insert(0, str(ROOT / "tools"))
import build_character_runtime_v25_idle as builder

SOURCE = ROOT / "art_candidates/video_source/idle/short_blink_final_v43_saturation_stable/alpha_frames"
EXPORT = ROOT / "art_candidates/character_runtime_v28_idle_v43/export"
PICKS = [1, 2, 111, 112, 139, 140, 179, 180]


def parse_atlas(path):
    lines = path.read_text(encoding="utf-8").splitlines()
    page = None
    regions = {}
    i = 0
    while i < len(lines):
        line = lines[i].strip()
        if line.startswith("character") and line.endswith(".png"):
            page = line
        elif line.startswith("idle_"):
            name = line
            props = {}
            i += 1
            while i < len(lines) and (lines[i].startswith("  ") or not lines[i].strip()):
                entry = lines[i].strip()
                if ":" in entry:
                    key, value = entry.split(":", 1)
                    props[key] = value.strip()
                i += 1
            xy = tuple(int(v.strip()) for v in props["xy"].split(","))
            size = tuple(int(v.strip()) for v in props["size"].split(","))
            regions[name] = (page, xy, size)
            continue
        i += 1
    return regions


def expected_crop(frame_no):
    source = Image.open(SOURCE / f"idle_{frame_no:03d}.png").convert("RGBA")
    resized = source.resize((556, 417), Image.Resampling.LANCZOS)
    canvas = Image.new("RGBA", (1024, 1024), (0, 0, 0, 0))
    canvas.paste(resized, (227, 336))
    canvas = builder.premultiply_alpha(canvas)
    bbox = canvas.getbbox()
    return np.asarray(canvas.crop(bbox), np.uint8)


def main():
    regions = parse_atlas(EXPORT / "character.atlas")
    report = []
    for frame_no in PICKS:
        name = f"idle_{frame_no:03d}"
        page, (x, y), (w, h) = regions[name]
        atlas = np.asarray(Image.open(EXPORT / page).convert("RGBA"), np.uint8)[y:y+h, x:x+w]
        expected = expected_crop(frame_no)
        same = atlas.shape == expected.shape and np.array_equal(atlas, expected)
        max_delta = int(np.abs(atlas.astype(np.int16) - expected.astype(np.int16)).max()) if atlas.shape == expected.shape else None
        report.append({"frame": frame_no, "page": page, "atlas_shape": list(atlas.shape), "expected_shape": list(expected.shape), "exact": bool(same), "max_channel_delta": max_delta})
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    main()
