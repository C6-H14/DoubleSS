from pathlib import Path
import json
import shutil

import cv2
import numpy as np
from PIL import Image, ImageDraw
from scipy.ndimage import distance_transform_edt


ROOT = Path(r"D:\StSmod\DoubleSS")
SOURCE = ROOT / "art_candidates" / "video_source" / "cast_special" / "final_v1_alpha" / "alpha_frames"
OUTPUT = ROOT / "art_candidates" / "video_source" / "cast_special" / "final_v2_denoised"
FRAMES = OUTPUT / "alpha_frames"
MIN_COMPONENT_AREA = 64


def clean(path: Path) -> tuple[Image.Image, int]:
    rgba = np.asarray(Image.open(path).convert("RGBA"), dtype=np.uint8).copy()
    alpha = rgba[..., 3]
    count, labels, stats, _ = cv2.connectedComponentsWithStats((alpha > 8).astype(np.uint8), 8)
    keep = np.zeros(alpha.shape, dtype=bool)
    removed = 0
    for label in range(1, count):
        area = int(stats[label, cv2.CC_STAT_AREA])
        if area >= MIN_COMPONENT_AREA:
            keep |= labels == label
        else:
            removed += area
    rgba[~keep] = 0
    alpha = rgba[..., 3]

    # Replace every translucent edge pixel's RGB with its nearest fully opaque
    # interior colour. Alpha remains unchanged, so the silhouette is preserved
    # while purple/white chroma contamination cannot bleed under linear filtering.
    opaque = alpha >= 250
    if opaque.any():
        _, nearest = distance_transform_edt(~opaque, return_indices=True)
        edge = (alpha > 0) & (alpha < 250)
        rgba[..., :3][edge] = rgba[..., :3][nearest[0], nearest[1]][edge]
    rgba[alpha == 0, :3] = 0
    return Image.fromarray(rgba, "RGBA"), removed


def main() -> None:
    if OUTPUT.exists():
        shutil.rmtree(OUTPUT)
    FRAMES.mkdir(parents=True)
    paths = sorted(SOURCE.glob("cast_special_*.png"))
    if len(paths) != 75:
        raise RuntimeError(f"Expected 75 frames, found {len(paths)}")

    frames = []
    removed_counts = []
    for index, path in enumerate(paths, 1):
        frame, removed = clean(path)
        frame.save(FRAMES / f"cast_special_{index:03d}.png", optimize=True)
        frames.append(frame)
        removed_counts.append(removed)

    for name, color in (("black", (0, 0, 0, 255)), ("white", (255, 255, 255, 255))):
        views = [Image.alpha_composite(Image.new("RGBA", frame.size, color), frame)
                 .resize((480, 480), Image.Resampling.LANCZOS).convert("RGB") for frame in frames]
        views[0].save(OUTPUT / f"cast_special_{name}_denoised_75f_50fps.gif",
                      save_all=True, append_images=views[1:], duration=20, loop=0,
                      disposal=2, optimize=False)

        columns, thumb = 10, 160
        rows = (len(frames) + columns - 1) // columns
        sheet = Image.new("RGB", (columns * thumb, rows * (thumb + 20)), (24, 24, 24))
        draw = ImageDraw.Draw(sheet)
        for i, view in enumerate(views):
            small = view.resize((thumb, thumb), Image.Resampling.LANCZOS)
            x, y = (i % columns) * thumb, (i // columns) * (thumb + 20)
            sheet.paste(small, (x, y))
            draw.text((x + 4, y + thumb + 2), f"{i + 1:03d}", fill="white")
        sheet.save(OUTPUT / f"cast_special_{name}_denoised_contact.jpg", quality=96)

    report = {
        "frames": 75,
        "minimum_component_area": MIN_COMPONENT_AREA,
        "removed_pixels_total": sum(removed_counts),
        "removed_pixels_per_frame": removed_counts,
        "edge_rgb": "nearest opaque interior; alpha silhouette unchanged",
    }
    (OUTPUT / "cleanup_report.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({k: v for k, v in report.items() if k != "removed_pixels_per_frame"}, indent=2))


if __name__ == "__main__":
    main()
