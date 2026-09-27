from __future__ import annotations

import json
import shutil
from pathlib import Path

import cv2
import numpy as np
from PIL import Image, ImageDraw
from scipy.ndimage import distance_transform_edt


ROOT = Path(r"D:\StSmod\DoubleSS")
SOURCE = Path(r"D:\StSmod\art_preview\video\idle_trimmed_f10_f89.mp4")
OUT = ROOT / "art_candidates" / "alt_fencer_idle_v1"
FRAMES = OUT / "alpha_frames"


def matte(frame_bgr: np.ndarray) -> Image.Image:
    rgb = cv2.cvtColor(frame_bgr, cv2.COLOR_BGR2RGB)
    work = rgb.astype(np.int16)
    r, g, b = work[..., 0], work[..., 1], work[..., 2]
    purple_strength = np.minimum(r - g, b - g).astype(np.float32)
    hsv = cv2.cvtColor(rgb, cv2.COLOR_RGB2HSV)
    h, s, v = cv2.split(hsv)

    # Strictly reject every purple-dominant pixel, including low-saturation
    # H.264 blends at the silhouette. Keeping those blends as partial alpha
    # creates the familiar violet saw-tooth halo after compositing.
    silver_candidate = (v >= 105) & (s <= 100)
    purple_family = (
        (r >= 22) & (b >= 28) &
        (purple_strength >= 6) &
        (((h >= 118) & (h <= 172) & (s >= 20)) | (purple_strength >= 18)) &
        ~silver_candidate
    )
    alpha = np.where(purple_family, 0, 255).astype(np.uint8)

    # Keep the connected character and sword, reject isolated codec flecks.
    visible = (alpha > 0).astype(np.uint8)
    count, labels, stats, _ = cv2.connectedComponentsWithStats(visible, 8)
    if count <= 1:
        raise RuntimeError("No foreground component")
    principal = 1 + int(np.argmax(stats[1:, cv2.CC_STAT_AREA]))
    # The épée is only one to three pixels wide and H.264 may split it into
    # several legitimate components. Preserve all meaningful non-purple
    # components; discard only tiny codec specks.
    keep_labels = [principal]
    for label in range(1, count):
        if stats[label, cv2.CC_STAT_AREA] >= 6:
            keep_labels.append(label)
    keep = np.isin(labels, keep_labels)
    alpha[~keep] = 0

    # Rebuild a controlled one-pixel antialias from inside the accepted solid
    # silhouette. No keyed purple RGB participates in this edge.
    inward = cv2.distanceTransform(keep.astype(np.uint8), cv2.DIST_L2, 5)
    alpha = np.uint8(np.clip(inward / 1.15, 0.0, 1.0) * 255)
    alpha[~keep] = 0
    alpha[alpha >= 248] = 255

    # Clean the semi-transparent edge RGB and any opaque purple spill in the
    # outer three pixels using the nearest verified subject pixel.
    clean = rgb.copy()
    opaque = (alpha >= 248) & ~purple_family
    if not np.any(opaque):
        raise RuntimeError("No opaque subject pixels")
    _, nearest = distance_transform_edt(~opaque, return_indices=True)
    donor = rgb[nearest[0], nearest[1]]
    contour = (alpha > 0) & (alpha < 248)
    clean[contour] = donor[contour]

    baked = keep & (inward <= 3.2) & (purple_strength >= 2) & (r > g) & (b > g)
    clean[baked] = donor[baked]
    # Neutralize the low-saturation lavender cast baked into bright steel and
    # white antialias pixels. This preserves value/detail while removing spill.
    lavender = (alpha > 0) & silver_candidate & (h >= 118) & (h <= 172) & (s >= 8)
    gray = np.clip(
        clean[..., 0].astype(np.float32) * 0.299
        + clean[..., 1].astype(np.float32) * 0.587
        + clean[..., 2].astype(np.float32) * 0.114,
        0,
        255,
    ).astype(np.uint8)
    clean[lavender] = np.repeat(gray[..., None], 3, axis=2)[lavender]
    clean[alpha == 0] = 0
    return Image.fromarray(np.dstack((clean, alpha)), "RGBA")


def make_contact(frames: list[Image.Image], path: Path, background: tuple[int, int, int, int]) -> None:
    picks = np.linspace(0, len(frames) - 1, 20).round().astype(int)
    tw, th, cols = 240, 240, 5
    sheet = Image.new("RGB", (cols * tw, 4 * (th + 24)), (28, 28, 28))
    draw = ImageDraw.Draw(sheet)
    for n, index in enumerate(picks):
        view = Image.alpha_composite(Image.new("RGBA", frames[index].size, background), frames[index])
        thumb = view.resize((tw, th), Image.Resampling.LANCZOS).convert("RGB")
        x, y = (n % cols) * tw, (n // cols) * (th + 24)
        sheet.paste(thumb, (x, y + 24))
        draw.text((x + 5, y + 5), f"frame {index + 1:02d}", fill="white")
    sheet.save(path, quality=96)


def main() -> None:
    if OUT.exists():
        shutil.rmtree(OUT)
    FRAMES.mkdir(parents=True)

    cap = cv2.VideoCapture(str(SOURCE))
    fps = float(cap.get(cv2.CAP_PROP_FPS))
    frames: list[Image.Image] = []
    while True:
        ok, frame = cap.read()
        if not ok:
            break
        frames.append(matte(frame))
    cap.release()
    if len(frames) != 80:
        raise RuntimeError(f"Expected 80 frames, found {len(frames)}")

    bboxes = []
    for i, frame in enumerate(frames, 1):
        frame.save(FRAMES / f"alt_idle_{i:03d}.png", optimize=True)
        bbox = frame.getchannel("A").point(lambda a: 255 if a >= 8 else 0).getbbox()
        if bbox is None:
            raise RuntimeError(f"Empty matte frame {i}")
        bboxes.append(bbox)

    for name, color in {
        "black": (0, 0, 0, 255),
        "white": (255, 255, 255, 255),
        "gray": (127, 127, 127, 255),
        "green": (0, 180, 60, 255),
    }.items():
        make_contact(frames, OUT / f"contact_{name}.jpg", color)

    union = (
        min(b[0] for b in bboxes), min(b[1] for b in bboxes),
        max(b[2] for b in bboxes), max(b[3] for b in bboxes),
    )
    report = {
        "source": str(SOURCE),
        "frames": len(frames),
        "fps": fps,
        "canvas": list(frames[0].size),
        "union_alpha_bbox": list(union),
        "frame_bbox_min": [min(b[0] for b in bboxes), min(b[1] for b in bboxes)],
        "frame_bbox_max": [max(b[2] for b in bboxes), max(b[3] for b in bboxes)],
    }
    (OUT / "matte_report.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    main()
