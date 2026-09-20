from pathlib import Path
import json
import shutil
import sys

import numpy as np
from PIL import Image, ImageDraw


TARGET_FOOT_CENTER_X = 559
TARGET_GROUND_Y = 768
RUNTIME_SCALE = 0.5


def shoe_groups(rgba: np.ndarray) -> list[tuple[int, int]]:
    rgb = rgba[..., :3]
    alpha = rgba[..., 3]
    dark = (alpha > 96) & (rgb.mean(axis=2) < 105)
    dark[: int(rgba.shape[0] * 0.82), :] = False
    counts = dark.sum(axis=0)
    active = np.where(counts >= 3)[0]
    groups: list[tuple[int, int]] = []
    if not len(active):
        return groups
    start = prev = int(active[0])
    for raw in active[1:]:
        value = int(raw)
        if value > prev + 2:
            if prev - start >= 12:
                groups.append((start, prev))
            start = value
        prev = value
    if prev - start >= 12:
        groups.append((start, prev))
    return groups


def infer_anchor(rgba: np.ndarray) -> tuple[float, int]:
    groups = shoe_groups(rgba)
    if len(groups) < 2:
        raise ValueError("Could not identify both shoe groups")
    groups = sorted(groups, key=lambda pair: pair[1] - pair[0], reverse=True)[:2]
    groups.sort()
    centers = [(a + b) / 2 for a, b in groups]
    alpha = rgba[..., 3]
    ys, _ = np.where(alpha > 16)
    return sum(centers) / 2, int(ys.max())


def translate(image: Image.Image, dx: int, dy: int) -> Image.Image:
    out = Image.new("RGBA", image.size, (0, 0, 0, 0))
    out.alpha_composite(image, (dx, dy))
    return out


def checker_contact_sheet(images: list[Image.Image], out_path: Path) -> None:
    thumb_w, thumb_h = 278, 209
    cols, rows = 4, 4
    cell_w, cell_h = thumb_w + 16, thumb_h + 36
    sheet = Image.new("RGB", (cols * cell_w, rows * cell_h), "#303238")
    draw = ImageDraw.Draw(sheet)
    for index, source in enumerate(images):
        thumb = source.resize((thumb_w, thumb_h), Image.Resampling.LANCZOS)
        checker = Image.new("RGB", thumb.size, "#d9d9d9")
        cdraw = ImageDraw.Draw(checker)
        tile = 10
        for y in range(0, thumb_h, tile):
            for x in range(0, thumb_w, tile):
                if (x // tile + y // tile) % 2:
                    cdraw.rectangle((x, y, x + tile - 1, y + tile - 1), fill="#bcbcbc")
        checker.paste(thumb, mask=thumb.getchannel("A"))
        col, row = index % cols, index // cols
        px, py = col * cell_w + 8, row * cell_h + 8
        sheet.paste(checker, (px, py))
        draw.text((px + 4, py + thumb_h + 5), f"{index + 1:02d}", fill="white")
    sheet.save(out_path, quality=94)


def main() -> None:
    source_dir = Path(sys.argv[1])
    output_root = Path(sys.argv[2])
    master_dir = output_root / "aligned_master_frames"
    runtime_dir = output_root / "runtime_frames_50pct"
    for directory in (master_dir, runtime_dir):
        if directory.exists():
            shutil.rmtree(directory)
        directory.mkdir(parents=True)

    records = []
    aligned_images: list[Image.Image] = []
    for path in sorted(source_dir.glob("attack_sword_*.png")):
        image = Image.open(path).convert("RGBA")
        rgba = np.asarray(image)
        foot_x, ground_y = infer_anchor(rgba)
        dx = round(TARGET_FOOT_CENTER_X - foot_x)
        dy = TARGET_GROUND_Y - ground_y
        aligned = translate(image, dx, dy)
        aligned.save(master_dir / path.name, optimize=True)
        runtime = aligned.resize(
            (round(aligned.width * RUNTIME_SCALE), round(aligned.height * RUNTIME_SCALE)),
            Image.Resampling.LANCZOS,
        )
        runtime.save(runtime_dir / path.name, optimize=True)
        aligned_images.append(aligned)
        records.append(
            {
                "frame": path.name,
                "source_foot_center_x": foot_x,
                "source_ground_y": ground_y,
                "shift_x": dx,
                "shift_y": dy,
            }
        )

    checker_contact_sheet(aligned_images, output_root / "aligned_contact_sheet.png")
    gif_frames = [
        image.resize((556, 417), Image.Resampling.LANCZOS) for image in aligned_images
    ]
    gif_frames[0].save(
        output_root / "attack_sword_aligned_15fps.gif",
        save_all=True,
        append_images=gif_frames[1:],
        duration=67,
        loop=0,
        disposal=2,
        transparency=0,
    )
    manifest = {
        "fps": 15,
        "frame_count": len(records),
        "duration_seconds": round(len(records) / 15, 6),
        "master_size": [1112, 834],
        "runtime_size": [556, 417],
        "runtime_scale": RUNTIME_SCALE,
        "anchor_master": {"foot_center_x": TARGET_FOOT_CENTER_X, "ground_y": TARGET_GROUND_Y},
        "anchor_runtime": {
            "foot_center_x": round(TARGET_FOOT_CENTER_X * RUNTIME_SCALE, 1),
            "ground_y": round(TARGET_GROUND_Y * RUNTIME_SCALE, 1),
        },
        "frames": records,
    }
    (output_root / "alignment_manifest.json").write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8"
    )


if __name__ == "__main__":
    main()
