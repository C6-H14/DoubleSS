from pathlib import Path
import sys

import numpy as np
from PIL import Image


def main() -> None:
    frame_dir = Path(sys.argv[1])
    files = sorted(frame_dir.glob("attack_sword_*.png"))
    if not files:
        raise SystemExit(f"No frames found in {frame_dir}")

    print("frame,size,alpha_bbox,opaque_bottom,dark_feet_bbox,dark_feet_center")
    for path in files:
        rgba = np.asarray(Image.open(path).convert("RGBA"))
        rgb = rgba[..., :3]
        alpha = rgba[..., 3]
        ys, xs = np.where(alpha > 16)
        bbox = (int(xs.min()), int(ys.min()), int(xs.max()), int(ys.max()))

        # Shoes and their outlines are the darkest opaque pixels in the lower
        # quarter of the character. Restricting the search to this band avoids
        # the sword and dark skirt influencing the inferred ground anchor.
        lum = rgb.mean(axis=2)
        dark = (alpha > 96) & (lum < 105)
        dark[: int(rgba.shape[0] * 0.82), :] = False
        dys, dxs = np.where(dark)
        if len(dxs):
            feet_bbox = (int(dxs.min()), int(dys.min()), int(dxs.max()), int(dys.max()))
            feet_center = (round(float(np.median(dxs)), 2), int(dys.max()))
        else:
            feet_bbox = None
            feet_center = None

        col_counts = dark.sum(axis=0)
        active = np.where(col_counts >= 3)[0]
        groups = []
        if len(active):
            start = prev = int(active[0])
            for value in active[1:]:
                value = int(value)
                if value > prev + 2:
                    if prev - start >= 12:
                        groups.append((start, prev, int(col_counts[start : prev + 1].sum())))
                    start = value
                prev = value
            if prev - start >= 12:
                groups.append((start, prev, int(col_counts[start : prev + 1].sum())))

        print(
            f"{path.name},{rgba.shape[1]}x{rgba.shape[0]},{bbox},"
            f"{int(ys.max())},{feet_bbox},{feet_center},groups={groups}"
        )


if __name__ == "__main__":
    main()
