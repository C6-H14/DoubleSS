"""Read-only numeric scan of the current cast_s runtime frames."""

from pathlib import Path

import numpy as np
from PIL import Image


ROOT = Path(r"D:\StSmod\DoubleSS\art_candidates\character_runtime_v45_alt_cast_s_aligned\images")
frames = sorted(ROOT.glob("cast_s_alpha_*.png"))
if len(frames) != 45:
    raise RuntimeError(f"Expected 45 frames, found {len(frames)}")

previous = None
rows = []
for index, path in enumerate(frames, 1):
    rgba = np.asarray(Image.open(path).convert("RGBA"), dtype=np.float32)
    alpha = rgba[:, :, 3]
    yy, xx = np.where(alpha > 128)
    bbox = (int(xx.min()), int(yy.min()), int(xx.max() + 1), int(yy.max() + 1))
    center = (float(xx.mean()), float(yy.mean()))
    area = int(len(xx))
    if previous is None:
        change = 0.0
    else:
        # Composite premultiplied colour over a constant background.
        old_rgb = previous[:, :, :3] + 32.0 * (1.0 - previous[:, :, 3:4] / 255.0)
        new_rgb = rgba[:, :, :3] + 32.0 * (1.0 - rgba[:, :, 3:4] / 255.0)
        change = float(np.abs(new_rgb - old_rgb).mean())
    rows.append((index, change, bbox, center, area))
    previous = rgba

print("Largest adjacent visual changes (change = mean absolute RGB difference):")
for index, change, bbox, center, area in sorted(rows[1:], key=lambda row: row[1], reverse=True)[:20]:
    print(f"{index-1:02d}->{index:02d}: change={change:.2f} bbox={bbox} center={center[0]:.1f},{center[1]:.1f} area={area}")
print("Endpoints:")
for row in (rows[0], rows[-1]):
    print(row)

idle_root = Path(r"D:\StSmod\DoubleSS\art_candidates\character_runtime_v34_idle_60fps\images")
def load_composite(path: Path) -> np.ndarray:
    rgba = np.asarray(Image.open(path).convert("RGBA"), dtype=np.float32)
    return rgba[:, :, :3] + 32.0 * (1.0 - rgba[:, :, 3:4] / 255.0)

roi = (slice(350, 740), slice(350, 730))
idle_first = load_composite(idle_root / "alt_idle_001.png")
idle_last = load_composite(idle_root / "alt_idle_241.png")
cast_first = load_composite(frames[0])
cast_last = load_composite(frames[-1])
print("Character ROI mean absolute RGB difference:")
for name, left, right in (
    ("idle 241 -> cast 001", idle_last, cast_first),
    ("cast 045 -> idle 001", cast_last, idle_first),
    ("cast 045 -> cast 001", cast_last, cast_first),
    ("idle 241 -> idle 001", idle_last, idle_first),
):
    print(f"{name}: {np.abs(left[roi] - right[roi]).mean():.2f}")
adjacent_roi = [
    float(np.abs(load_composite(a)[roi] - load_composite(b)[roi]).mean())
    for a, b in zip(frames[:-1], frames[1:])
]
print(f"cast_s adjacent median={np.median(adjacent_roi):.2f}, max={max(adjacent_roi):.2f}")
