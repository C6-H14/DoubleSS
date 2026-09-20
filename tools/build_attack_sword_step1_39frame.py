from pathlib import Path

import cv2
import numpy as np
from PIL import Image, ImageDraw


ROOT = Path(r"D:\StSmod\DoubleSS\art_candidates\video_source")
SOURCE = ROOT / "attack_sword_v2_43frame_v1" / "frames"
WORK = ROOT / "attack_sword_v2_step1_39frame_v1"
RAW = WORK / "regenerated_raw"
FRAMES = WORK / "frames"
CANVAS = (1400, 1320)
REPLACED = {5: (4, 6), 7: (6, 8), 9: (8, 10), 11: (10, 12), 19: (18, 20),
            22: (21, 24), 23: (21, 24), 35: (34, 36), 37: (36, 38), 38: (37, 39)}
DELETED = {15, 16, 17, 18}


def visible_bbox(image: Image.Image):
    return image.getchannel("A").point(lambda v: 255 if v >= 8 else 0).getbbox()


def body_bbox(image: Image.Image):
    alpha = np.asarray(image.getchannel("A"))
    mask = np.uint8(alpha >= 32) * 255
    opened = cv2.morphologyEx(mask, cv2.MORPH_OPEN,
                              cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (31, 31)))
    count, _, stats, _ = cv2.connectedComponentsWithStats(opened)
    if count <= 1:
        return visible_bbox(image)
    x, y, w, h, _ = stats[1 + np.argmax(stats[1:, cv2.CC_STAT_AREA])]
    return int(x), int(y), int(x + w), int(y + h)


def normalize(raw: Image.Image, left: Image.Image, right: Image.Image) -> Image.Image:
    source_box = visible_bbox(raw)
    generated_body = body_bbox(raw)
    left_body, right_body = body_bbox(left), body_bbox(right)
    crop = raw.crop(source_box)
    target_h = round(((left_body[3] - left_body[1]) + (right_body[3] - right_body[1])) / 2)
    scale = target_h / (generated_body[3] - generated_body[1])
    resized = crop.resize((round(crop.width * scale), round(crop.height * scale)), Image.Resampling.LANCZOS)
    target_cx = ((left_body[0] + left_body[2]) / 2 + (right_body[0] + right_body[2]) / 2) / 2
    source_cx = ((generated_body[0] + generated_body[2]) / 2 - source_box[0]) * scale
    bottom = round((visible_bbox(left)[3] + visible_bbox(right)[3]) / 2)
    canvas = Image.new("RGBA", CANVAS)
    canvas.alpha_composite(resized, (round(target_cx - source_cx), bottom - resized.height))
    return canvas


def checker(size, cell=18):
    w, h = size
    yy, xx = np.indices((h, w))
    colors = np.array([[42, 45, 51], [62, 66, 74]], dtype=np.uint8)
    return Image.fromarray(colors[((xx // cell + yy // cell) % 2).astype(np.uint8)], "RGB")


def preview(frame):
    bg = checker(CANVAS)
    bg.paste(frame, mask=frame.getchannel("A"))
    return bg.resize((700, 660), Image.Resampling.LANCZOS)


def main():
    FRAMES.mkdir(parents=True, exist_ok=True)
    current = {i: Image.open(SOURCE / f"attack_sword_{i:02d}.png").convert("RGBA") for i in range(1, 44)}
    for target, (left, right) in REPLACED.items():
        raw = Image.open(RAW / f"current_{target:02d}.png").convert("RGBA")
        current[target] = normalize(raw, current[left], current[right])

    kept = [(old, current[old]) for old in range(1, 44) if old not in DELETED]
    for new, (_, image) in enumerate(kept, 1):
        image.save(FRAMES / f"attack_sword_{new:02d}.png", optimize=True)

    previews = [preview(image) for _, image in kept]
    durations = [30 if i % 3 != 2 else 40 for i in range(len(previews))]
    previews[0].save(WORK / "attack_sword_step1_39frame_30fps.gif", save_all=True,
                     append_images=previews[1:], duration=durations, loop=0, disposal=2, optimize=False)

    tw, th, cols = 280, 264, 8
    rows = (len(previews) + cols - 1) // cols
    sheet = Image.new("RGB", (tw * cols, th * rows), (28, 30, 35))
    draw = ImageDraw.Draw(sheet)
    for new, ((old, _), image) in enumerate(zip(kept, previews), 1):
        x, y = ((new - 1) % cols) * tw, ((new - 1) // cols) * th
        sheet.paste(image.resize((tw, th), Image.Resampling.LANCZOS), (x, y))
        draw.rectangle((x + 6, y + 6, x + 92, y + 31), fill=(12, 14, 18))
        draw.text((x + 11, y + 10), f"{new:02d} (old {old:02d})", fill=(255, 222, 105))
    sheet.save(WORK / "attack_sword_step1_39frame_contact_sheet.png", optimize=True)

    lines = [f"new {new:02d} <- old {old:02d}" for new, (old, _) in enumerate(kept, 1)]
    (WORK / "frame_mapping.txt").write_text("\n".join(lines) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
