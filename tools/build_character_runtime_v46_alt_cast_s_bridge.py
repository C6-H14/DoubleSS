"""Bridge cast_s to the actual idle attachment at both ends (60 fps)."""

from __future__ import annotations

import json
import shutil
from pathlib import Path

import cv2
import numpy as np
from PIL import Image, ImageDraw

import build_character_runtime_v36_alt_attack_basic as runtime


ROOT = Path(r"D:\StSmod\DoubleSS")
SOURCE = ROOT / "art_candidates" / "character_runtime_v45_alt_cast_s_aligned" / "images"
IDLE = ROOT / "art_candidates" / "character_runtime_v34_idle_60fps" / "images" / "alt_idle_001.png"
CURRENT = ROOT / "src" / "main" / "resources" / "img" / "char" / "spine_inner"
OUT = ROOT / "art_candidates" / "character_runtime_v46_alt_cast_s_bridge"
IMAGES = OUT / "images"
EXPORT = OUT / "export"
FIRST_PAGE = 37
FPS = 60
BRIDGE_STEPS = 3


def read_premultiplied(path: Path) -> np.ndarray:
    return np.asarray(Image.open(path).convert("RGBA"), dtype=np.float32)


def flow_between(a: np.ndarray, b: np.ndarray) -> np.ndarray:
    def grayscale(rgba: np.ndarray) -> np.ndarray:
        alpha = rgba[:, :, 3:4] / 255.0
        composite = np.clip(rgba[:, :, :3] + 32.0 * (1.0 - alpha), 0, 255).astype(np.uint8)
        return cv2.cvtColor(composite, cv2.COLOR_RGB2GRAY)

    # The character occupies only the central 300x400 px of the runtime canvas.
    # Estimate motion there so empty margins do not dominate the flow.
    ys, xs = slice(365, 735), slice(365, 665)
    crop_a = np.ascontiguousarray(grayscale(a)[ys, xs])
    crop_b = np.ascontiguousarray(grayscale(b)[ys, xs])
    estimator = cv2.DISOpticalFlow.create(cv2.DISOPTICAL_FLOW_PRESET_MEDIUM)
    estimator.setFinestScale(1)
    crop_flow = estimator.calc(crop_a, crop_b, None)
    full_flow = np.zeros((1024, 1024, 2), dtype=np.float32)
    full_flow[ys, xs] = crop_flow
    return full_flow


def warp(image: np.ndarray, flow: np.ndarray, amount: float) -> np.ndarray:
    height, width = image.shape[:2]
    yy, xx = np.mgrid[:height, :width].astype(np.float32)
    return cv2.remap(image, xx - amount * flow[:, :, 0], yy - amount * flow[:, :, 1],
                     cv2.INTER_LINEAR, borderMode=cv2.BORDER_CONSTANT, borderValue=(0, 0, 0, 0))


def morph(a: np.ndarray, b: np.ndarray, forward: np.ndarray,
          backward: np.ndarray, fraction: float) -> np.ndarray:
    left = warp(a, forward, fraction)
    right = warp(b, backward, 1.0 - fraction)
    result = left * (1.0 - fraction) + right * fraction
    return np.clip(np.rint(result), 0, 255).astype(np.uint8)


def pack() -> None:
    if OUT.exists():
        shutil.rmtree(OUT)
    IMAGES.mkdir(parents=True)
    EXPORT.mkdir(parents=True)

    for path in CURRENT.glob("character*.png"):
        suffix = path.stem.removeprefix("character")
        if not suffix.isdigit() or int(suffix) < FIRST_PAGE:
            shutil.copy2(path, EXPORT / path.name)
    atlas = (CURRENT / "character.atlas").read_text(encoding="utf-8")
    marker = f"\ncharacter{FIRST_PAGE}.png\n"
    (EXPORT / "character.atlas").write_text(atlas.split(marker, 1)[0].rstrip() + "\n", encoding="utf-8")
    data = json.loads((CURRENT / "character.json").read_text(encoding="utf-8"))
    attachments = data["skins"]["default"]["character_frame"]
    for name in tuple(attachments):
        if name.startswith("cast_s_alpha_") or name.startswith("cast_s_bridge_"):
            del attachments[name]

    paths = sorted(SOURCE.glob("cast_s_alpha_*.png"))
    if len(paths) != 45:
        raise RuntimeError(f"Expected 45 source frames, got {len(paths)}")
    idle = read_premultiplied(IDLE)
    cast_first = read_premultiplied(paths[0])
    cast_last = read_premultiplied(paths[-1])
    in_forward, in_backward = flow_between(idle, cast_first), flow_between(cast_first, idle)
    out_forward, out_backward = flow_between(cast_last, idle), flow_between(idle, cast_last)

    sequence: list[tuple[str, np.ndarray]] = []
    sequence.append(("cast_s_bridge_in_000", idle.astype(np.uint8)))
    for step in range(1, BRIDGE_STEPS):
        t = step / BRIDGE_STEPS
        sequence.append((f"cast_s_bridge_in_{step:03d}", morph(idle, cast_first, in_forward, in_backward, t)))
    for index, path in enumerate(paths, 1):
        sequence.append((f"cast_s_alpha_{index:03d}", np.asarray(Image.open(path).convert("RGBA"), dtype=np.uint8)))
    for step in range(1, BRIDGE_STEPS):
        t = step / BRIDGE_STEPS
        sequence.append((f"cast_s_bridge_out_{step:03d}", morph(cast_last, idle, out_forward, out_backward, t)))
    sequence.append(("cast_s_bridge_out_003", idle.astype(np.uint8)))

    items = []
    keys = []
    for index, (name, pixels) in enumerate(sequence):
        image = Image.fromarray(pixels, "RGBA")
        image.save(IMAGES / f"{index+1:03d}_{name}.png", optimize=True)
        attachments[name] = {"path": name, "x": 0, "y": 210.0, "width": 1024, "height": 1024}
        keys.append({"time": round(index / FPS, 6), "name": name})
        left, top, right, bottom = image.getchannel("A").getbbox()
        bbox = (max(0, left - 4), max(0, top - 4), min(1024, right + 4), min(1024, bottom + 4))
        items.append((name, image, bbox))
    duration = len(sequence) / FPS
    keys.append({"time": duration, "name": sequence[-1][0]})
    data["animations"]["cast_s"] = {
        "slots": {"character_frame": {"attachment": keys}},
        "events": [{"time": duration, "name": "attack_end"}],
    }
    (EXPORT / "character.json").write_text(
        json.dumps(data, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")

    runtime.builder.EXPORT = EXPORT
    page_number = FIRST_PAGE
    page_report = []
    while items:
        items, section, page_name, count, size = runtime.builder.pack_page(items, page_number)
        with (EXPORT / "character.atlas").open("a", encoding="utf-8") as stream:
            stream.write(section)
        page_report.append((page_name, count, size))
        page_number += 1

    # A compact view of both joins, without GIF palette/alpha artifacts.
    review_indices = list(range(0, 6)) + list(range(len(sequence) - 6, len(sequence)))
    sheet = Image.new("RGB", (6 * 300, 2 * 330), (32, 32, 32))
    draw = ImageDraw.Draw(sheet)
    for cell, index in enumerate(review_indices):
        image = np.asarray(Image.open(IMAGES / f"{index+1:03d}_{sequence[index][0]}.png"), dtype=np.float32)
        rgb = np.clip(image[:, :, :3] + 32.0 * (1.0 - image[:, :, 3:4] / 255.0), 0, 255).astype(np.uint8)
        thumbnail = Image.fromarray(rgb, "RGB").crop((330, 350, 710, 760)).resize((285, 305))
        col, row = cell % 6, cell // 6
        sheet.paste(thumbnail, (col * 300, row * 330))
        draw.text((col * 300 + 4, row * 330 + 308), f"{index+1:02d} {sequence[index][0]}", fill=(255, 255, 255))
    sheet.save(OUT / "bridge_review.png")
    (OUT / "metadata.json").write_text(json.dumps({
        "frames": len(sequence), "fps": FPS, "duration": duration,
        "bridge_frames_each_end": BRIDGE_STEPS, "atlas_pages": page_report,
    }, indent=2) + "\n", encoding="utf-8")
    print(f"Built {len(sequence)} frames, {duration:.3f}s, pages={page_report}")


if __name__ == "__main__":
    pack()
