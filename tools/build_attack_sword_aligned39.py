from pathlib import Path

import cv2
import numpy as np
from PIL import Image, ImageDraw


ROOT = Path(r"D:\StSmod\DoubleSS\art_candidates\video_source")
SOURCE = ROOT / "attack_sword_v2_step1_39frame_v1" / "frames"
WORK = ROOT / "attack_sword_v2_aligned39_v1"
RAW_FIX = WORK / "sword_fixes_raw"
FRAMES = WORK / "frames"
CANVAS = (1400, 1320)
TARGET_BASELINE = 1079
TARGET_FOOT_CENTER = 715


def visible_bbox(image):
    return image.getchannel("A").point(lambda v: 255 if v >= 8 else 0).getbbox()


def body_stats(image):
    alpha = np.asarray(image.getchannel("A"))
    mask = np.uint8(alpha >= 32) * 255
    opened = cv2.morphologyEx(mask, cv2.MORPH_OPEN,
                              cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (31, 31)))
    count, _, stats, _ = cv2.connectedComponentsWithStats(opened)
    index = 1 + np.argmax(stats[1:, cv2.CC_STAT_AREA])
    x, y, w, h, area = stats[index]
    return (int(x), int(y), int(x + w), int(y + h)), int(area)


def premultiplied_resize(image, size):
    rgba = np.asarray(image.convert("RGBA"), dtype=np.float32) / 255.0
    a = rgba[..., 3:4]
    premul = np.concatenate((rgba[..., :3] * a, a), axis=2)
    channels = []
    for i in range(4):
        layer = Image.fromarray(np.uint8(np.clip(premul[..., i] * 255, 0, 255)), "L")
        channels.append(np.asarray(layer.resize(size, Image.Resampling.LANCZOS), dtype=np.float32) / 255.0)
    out = np.stack(channels, axis=2)
    oa = out[..., 3:4]
    rgb = np.divide(out[..., :3], oa, out=np.zeros_like(out[..., :3]), where=oa > 1e-5)
    return Image.fromarray(np.uint8(np.round(np.concatenate((np.clip(rgb, 0, 1), oa), axis=2) * 255)), "RGBA")


def normalize_fix(raw, left, right):
    sb = visible_bbox(raw)
    gb, _ = body_stats(raw)
    lb, _ = body_stats(left)
    rb, _ = body_stats(right)
    target_h = round(((lb[3] - lb[1]) + (rb[3] - rb[1])) / 2)
    scale = target_h / (gb[3] - gb[1])
    crop = raw.crop(sb)
    resized = premultiplied_resize(crop, (round(crop.width * scale), round(crop.height * scale)))
    target_cx = ((lb[0] + lb[2]) / 2 + (rb[0] + rb[2]) / 2) / 2
    source_cx = ((gb[0] + gb[2]) / 2 - sb[0]) * scale
    bottom = round((visible_bbox(left)[3] + visible_bbox(right)[3]) / 2)
    canvas = Image.new("RGBA", CANVAS)
    canvas.alpha_composite(resized, (round(target_cx - source_cx), bottom - resized.height))
    return canvas


def foot_center(image):
    alpha = np.asarray(image.getchannel("A"))
    bbox = visible_bbox(image)
    yy, xx = np.where((alpha >= 32) & (np.indices(alpha.shape)[0] >= bbox[3] - 55))
    if len(xx) == 0:
        return (bbox[0] + bbox[2]) / 2
    midpoint = np.median(xx)
    left = xx[xx <= midpoint]
    right = xx[xx > midpoint]
    if len(left) and len(right):
        return (float(np.mean(left)) + float(np.mean(right))) / 2
    return float(np.mean(xx))


def align(image, scale):
    bbox = visible_bbox(image)
    anchor_x = foot_center(image)
    anchor_y = bbox[3]
    resized = premultiplied_resize(image, (round(image.width * scale), round(image.height * scale)))
    x = round(TARGET_FOOT_CENTER - anchor_x * scale)
    y = round(TARGET_BASELINE - anchor_y * scale)
    canvas = Image.new("RGBA", CANVAS)
    canvas.alpha_composite(resized, (x, y))
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
    frames = [Image.open(SOURCE / f"attack_sword_{i:02d}.png").convert("RGBA") for i in range(1, 40)]
    frames[4], frames[5] = frames[5], frames[4]
    frames[20] = normalize_fix(Image.open(RAW_FIX / "frame_21.png").convert("RGBA"), frames[19], frames[22])
    frames[21] = normalize_fix(Image.open(RAW_FIX / "frame_22.png").convert("RGBA"), frames[19], frames[22])

    body_heights = [body_stats(frame)[0][3] - body_stats(frame)[0][1] for frame in frames]
    target_height = float(np.median(body_heights))
    scales = [min(1.35, max(0.94, target_height / height)) for height in body_heights]
    aligned = [align(frame, scale) for frame, scale in zip(frames, scales)]

    for i, frame in enumerate(aligned, 1):
        frame.save(FRAMES / f"attack_sword_{i:02d}.png", optimize=True)

    previews = [preview(frame) for frame in aligned]
    durations = [30 if i % 3 != 2 else 40 for i in range(len(previews))]
    previews[0].save(WORK / "attack_sword_aligned39_30fps.gif", save_all=True,
                     append_images=previews[1:], duration=durations, loop=0, disposal=2, optimize=False)

    tw, th, cols = 280, 264, 8
    rows = (len(previews) + cols - 1) // cols
    sheet = Image.new("RGB", (tw * cols, th * rows), (28, 30, 35))
    draw = ImageDraw.Draw(sheet)
    for i, image in enumerate(previews, 1):
        x, y = ((i - 1) % cols) * tw, ((i - 1) // cols) * th
        sheet.paste(image.resize((tw, th), Image.Resampling.LANCZOS), (x, y))
        draw.rectangle((x + 6, y + 6, x + 45, y + 31), fill=(12, 14, 18))
        draw.text((x + 12, y + 10), f"{i:02d}", fill=(255, 222, 105))
    sheet.save(WORK / "attack_sword_aligned39_contact_sheet.png", optimize=True)
    (WORK / "alignment_scales.txt").write_text(
        "\n".join(f"{i:02d}: {scale:.5f}" for i, scale in enumerate(scales, 1)) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
