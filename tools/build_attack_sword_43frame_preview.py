from __future__ import annotations

import shutil
from pathlib import Path

import cv2
import numpy as np
from PIL import Image, ImageDraw


ROOT = Path(r"D:\StSmod\DoubleSS\art_candidates\video_source")
SOURCE = ROOT / "attack_sword_v2_22frame_v1" / "frames"
WORK = ROOT / "attack_sword_v2_43frame_v1"
FRAMES = WORK / "frames"
GENERATED = WORK / "generated_midpoints"
CANVAS = (1400, 1320)


def shift_rgba(image: Image.Image, dx: int, dy: int) -> Image.Image:
    result = Image.new("RGBA", image.size)
    result.alpha_composite(image.convert("RGBA"), (dx, dy))
    return result


def midpoint(left: Image.Image, right: Image.Image) -> Image.Image:
    a = np.asarray(left.convert("RGBA"), dtype=np.float32) / 255.0
    b = np.asarray(right.convert("RGBA"), dtype=np.float32) / 255.0

    # Optical flow sees the opaque sprite on neutral gray, avoiding transparent RGB noise.
    bg = 0.18
    a_rgb = a[..., :3] * a[..., 3:4] + bg * (1.0 - a[..., 3:4])
    b_rgb = b[..., :3] * b[..., 3:4] + bg * (1.0 - b[..., 3:4])
    a_gray = cv2.cvtColor(np.uint8(np.clip(a_rgb * 255.0, 0, 255)), cv2.COLOR_RGB2GRAY)
    b_gray = cv2.cvtColor(np.uint8(np.clip(b_rgb * 255.0, 0, 255)), cv2.COLOR_RGB2GRAY)

    flow_ab = cv2.calcOpticalFlowFarneback(a_gray, b_gray, None, 0.5, 5, 35, 5, 7, 1.5, 0)
    flow_ba = cv2.calcOpticalFlowFarneback(b_gray, a_gray, None, 0.5, 5, 35, 5, 7, 1.5, 0)
    yy, xx = np.mgrid[0 : a.shape[0], 0 : a.shape[1]].astype(np.float32)

    def remap(data: np.ndarray, flow: np.ndarray) -> np.ndarray:
        map_x = xx + 0.5 * flow[..., 0]
        map_y = yy + 0.5 * flow[..., 1]
        return cv2.remap(data, map_x, map_y, cv2.INTER_CUBIC, borderMode=cv2.BORDER_CONSTANT, borderValue=0)

    # Warp premultiplied colors and alpha independently, then alpha-normalize.
    a_pm = np.concatenate((a[..., :3] * a[..., 3:4], a[..., 3:4]), axis=2)
    b_pm = np.concatenate((b[..., :3] * b[..., 3:4], b[..., 3:4]), axis=2)
    wa = remap(a_pm, flow_ba)
    wb = remap(b_pm, flow_ab)
    mixed = 0.5 * wa + 0.5 * wb
    alpha = np.clip(mixed[..., 3:4], 0.0, 1.0)
    rgb = np.divide(mixed[..., :3], alpha, out=np.zeros_like(mixed[..., :3]), where=alpha > 1e-5)
    rgba = np.concatenate((np.clip(rgb, 0, 1), alpha), axis=2)
    return Image.fromarray(np.uint8(np.round(rgba * 255.0)), "RGBA")


def visible_bbox(image: Image.Image) -> tuple[int, int, int, int]:
    bbox = image.getchannel("A").point(lambda value: 255 if value >= 8 else 0).getbbox()
    if bbox is None:
        raise RuntimeError("Image has no visible pixels")
    return bbox


def body_bbox(image: Image.Image) -> tuple[int, int, int, int]:
    alpha = np.asarray(image.getchannel("A"))
    mask = np.uint8(alpha >= 32) * 255
    opened = cv2.morphologyEx(
        mask,
        cv2.MORPH_OPEN,
        cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (31, 31)),
    )
    count, _, stats, _ = cv2.connectedComponentsWithStats(opened)
    if count <= 1:
        return visible_bbox(image)
    x, y, width, height, _ = stats[1 + np.argmax(stats[1:, cv2.CC_STAT_AREA])]
    return int(x), int(y), int(x + width), int(y + height)


def normalize_generated(image: Image.Image, left: Image.Image, right: Image.Image) -> Image.Image:
    source_box = visible_bbox(image)
    generated_body = body_bbox(image)
    left_box = body_bbox(left)
    right_box = body_bbox(right)
    crop = image.crop(source_box)
    target_height = round(((left_box[3] - left_box[1]) + (right_box[3] - right_box[1])) / 2)
    scale = target_height / (generated_body[3] - generated_body[1])
    resized = crop.resize((round(crop.width * scale), round(crop.height * scale)), Image.Resampling.LANCZOS)
    center_x = ((left_box[0] + left_box[2]) / 2 + (right_box[0] + right_box[2]) / 2) / 2
    bottom_y = round((visible_bbox(left)[3] + visible_bbox(right)[3]) / 2)
    source_body_center = (generated_body[0] + generated_body[2]) / 2
    body_center_in_crop = (source_body_center - source_box[0]) * scale
    canvas = Image.new("RGBA", CANVAS)
    canvas.alpha_composite(resized, (round(center_x - body_center_in_crop), bottom_y - resized.height))
    return canvas


def checkerboard(size: tuple[int, int], cell: int = 18) -> Image.Image:
    width, height = size
    yy, xx = np.indices((height, width))
    mask = ((xx // cell + yy // cell) % 2).astype(np.uint8)
    colors = np.array([[42, 45, 51], [62, 66, 74]], dtype=np.uint8)
    return Image.fromarray(colors[mask], "RGB")


def preview(frame: Image.Image) -> Image.Image:
    background = checkerboard(CANVAS)
    background.paste(frame, mask=frame.getchannel("A"))
    return background.resize((700, 660), Image.Resampling.LANCZOS)


def main() -> None:
    FRAMES.mkdir(parents=True, exist_ok=True)
    source = [Image.open(SOURCE / f"attack_sword_{i:02d}.png").convert("RGBA") for i in range(1, 23)]

    # Frame 4's feet/body center was about 91 px to the right of the midpoint
    # between frames 3 and 5. Correct only that global placement.
    source[3] = shift_rgba(source[3], -91, 0)

    sequence: list[Image.Image] = []
    for index, frame in enumerate(source):
        sequence.append(frame)
        if index < len(source) - 1:
            if np.array_equal(np.asarray(frame), np.asarray(source[index + 1])):
                sequence.append(frame.copy())
            else:
                pair_name = f"mid_{index + 1:02d}_{index + 2:02d}.png"
                generated = Image.open(GENERATED / pair_name).convert("RGBA")
                sequence.append(normalize_generated(generated, frame, source[index + 1]))

    if len(sequence) != 43:
        raise RuntimeError(f"Expected 43 frames, got {len(sequence)}")

    for index, frame in enumerate(sequence, 1):
        frame.save(FRAMES / f"attack_sword_{index:02d}.png", optimize=True)

    previews = [preview(frame) for frame in sequence]
    previews[0].save(
        WORK / "attack_sword_43frame_preview_30fps.gif",
        save_all=True,
        append_images=previews[1:],
        # GIF timing is quantized to 10 ms. 30/30/40 averages exactly 30 FPS.
        duration=[30 if index % 3 != 2 else 40 for index in range(len(previews))],
        loop=0,
        disposal=2,
        optimize=False,
    )

    thumb = (280, 264)
    columns = 8
    rows = (len(previews) + columns - 1) // columns
    sheet = Image.new("RGB", (thumb[0] * columns, thumb[1] * rows), (28, 30, 35))
    draw = ImageDraw.Draw(sheet)
    for index, image in enumerate(previews, 1):
        tile = image.resize(thumb, Image.Resampling.LANCZOS)
        x = ((index - 1) % columns) * thumb[0]
        y = ((index - 1) // columns) * thumb[1]
        sheet.paste(tile, (x, y))
        draw.rectangle((x + 6, y + 6, x + 44, y + 30), fill=(12, 14, 18))
        draw.text((x + 12, y + 10), f"{index:02d}", fill=(255, 222, 105))
    sheet.save(WORK / "attack_sword_43frame_contact_sheet.png", optimize=True)

    adopted = WORK / "aligned_source_22"
    adopted.mkdir(exist_ok=True)
    for index, frame in enumerate(source, 1):
        frame.save(adopted / f"attack_sword_{index:02d}.png", optimize=True)


if __name__ == "__main__":
    main()
