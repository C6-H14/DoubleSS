from pathlib import Path
import json
import shutil

import cv2
import numpy as np
from PIL import Image, ImageDraw
from scipy.ndimage import distance_transform_edt


ROOT = Path(r"D:\StSmod\DoubleSS")
SOURCE = ROOT / "art_candidates" / "video_source" / "cast_special" / "final_v1_alpha" / "alpha_frames"
OUTPUT = ROOT / "art_candidates" / "video_source" / "cast_special" / "final_v3_repaired"
FRAMES = OUTPUT / "alpha_frames"
SMALL_AREA = 64
MAX_NEIGHBOUR_DISTANCE = 16.0


def repair(path: Path) -> tuple[Image.Image, int, int]:
    rgba = np.asarray(Image.open(path).convert("RGBA"), dtype=np.uint8).copy()
    alpha = rgba[..., 3]
    count, labels, stats, _ = cv2.connectedComponentsWithStats((alpha > 8).astype(np.uint8), 8)

    large = np.zeros(alpha.shape, dtype=np.uint8)
    for label in range(1, count):
        if stats[label, cv2.CC_STAT_AREA] >= SMALL_AREA:
            large[labels == label] = 1
    distance = cv2.distanceTransform((1 - large).astype(np.uint8), cv2.DIST_L2, 5)

    keep = large.astype(bool)
    restored_components = 0
    removed_components = 0
    for label in range(1, count):
        area = int(stats[label, cv2.CC_STAT_AREA])
        if area >= SMALL_AREA:
            continue
        component = labels == label
        if float(distance[component].min()) <= MAX_NEIGHBOUR_DISTANCE:
            keep |= component
            restored_components += 1
        else:
            removed_components += 1
    rgba[~keep] = 0

    alpha = rgba[..., 3]
    opaque = alpha >= 250
    if opaque.any():
        _, nearest = distance_transform_edt(~opaque, return_indices=True)
        edge = (alpha > 0) & (alpha < 250)
        rgba[..., :3][edge] = rgba[..., :3][nearest[0], nearest[1]][edge]
    rgba[alpha == 0, :3] = 0
    return Image.fromarray(rgba, "RGBA"), restored_components, removed_components


def main() -> None:
    if OUTPUT.exists():
        shutil.rmtree(OUTPUT)
    FRAMES.mkdir(parents=True)
    paths = sorted(SOURCE.glob("cast_special_*.png"))
    frames = []
    restored = removed = 0
    for index, path in enumerate(paths, 1):
        frame, frame_restored, frame_removed = repair(path)
        frame.save(FRAMES / f"cast_special_{index:03d}.png", optimize=True)
        frames.append(frame)
        restored += frame_restored
        removed += frame_removed

    for name, color in (("black", (0, 0, 0, 255)), ("white", (255, 255, 255, 255))):
        views = [Image.alpha_composite(Image.new("RGBA", frame.size, color), frame)
                 .resize((480, 480), Image.Resampling.LANCZOS).convert("RGB") for frame in frames]
        views[0].save(OUTPUT / f"cast_special_{name}_repaired_75f_50fps.gif",
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
        sheet.save(OUTPUT / f"cast_special_{name}_repaired_contact.jpg", quality=96)

    report = {"frames": len(frames), "restored_near_components": restored,
              "removed_far_components": removed, "max_neighbour_distance": MAX_NEIGHBOUR_DISTANCE}
    (OUTPUT / "repair_report.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    main()
