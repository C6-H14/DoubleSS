from pathlib import Path
import json
import math

import cv2
import numpy as np
from PIL import Image, ImageDraw


ROOT = Path(r"D:\StSmod\DoubleSS\art_candidates\video_source")
SOURCE = ROOT / "attack_sword_v2_final39_edgeclean_v5" / "frames"
OUT = ROOT / "attack_sword_v2_final39_swordfixed_v6"
FRAMES = OUT / "frames"
CANVAS = (1400, 1320)

# Hand-checked guard centers and blade tips on the 1400x1320 master frames.
ANCHORS = {
    5: ((474.7, 634.2), (158.1, 647.8)),
    6: ((481.8, 643.1), (211.1, 587.4)),
    7: ((599.6, 617.7), (344.2, 197.3)),
    10: ((574.6, 451.8), (859.9, 173.0)),
    11: ((588.0, 470.0), (899.0, 305.0)),
    15: ((609.2, 586.1), (327.0, 207.4)),
    16: ((605.9, 670.5), (237.2, 257.7)),
    19: ((948.0, 582.2), (1237.7, 467.4)),
    20: ((962.7, 595.8), (1249.3, 526.2)),
    21: ((944.4, 576.6), (1279.0, 437.6)),
    23: ((946.8, 578.7), (1353.7, 449.0)),
    24: ((946.8, 578.7), (1353.7, 449.0)),
    25: ((946.8, 578.7), (1353.7, 449.0)),
    26: ((942.5, 591.0), (1338.2, 453.7)),
    27: ((948.2, 590.5), (1389.4, 439.3)),
    28: ((948.2, 590.5), (1389.4, 439.3)),
    29: ((948.2, 590.5), (1389.4, 439.3)),
    30: ((900.7, 713.8), (1247.6, 608.6)),
}

REFERENCE = {
    10: 11,
    # Frame 7 has the desired apparent length, but its hands overlap far down
    # the blade axis and cannot be isolated cleanly.  Frame 15 is the nearest
    # clean rigid blade (472 px versus 492 px) and avoids duplicating anatomy.
    5: 7, 6: 7,
    16: 15,
    19: 21, 20: 21,
    23: 30, 24: 30, 25: 30, 26: 30, 27: 30, 28: 30, 29: 30,
}


def axis(frame):
    handle = np.array(ANCHORS[frame][0], dtype=np.float32)
    tip = np.array(ANCHORS[frame][1], dtype=np.float32)
    vector = tip - handle
    length = float(np.linalg.norm(vector))
    return handle, tip, vector / length, length


def blade_mask(image, frame, expand=False, geometric=False):
    rgba = np.asarray(image.convert("RGBA"))
    rgb = rgba[..., :3].astype(np.int16)
    alpha = rgba[..., 3]
    handle, _, direction, length = axis(frame)
    yy, xx = np.indices(alpha.shape, dtype=np.float32)
    rx = xx - handle[0]
    ry = yy - handle[1]
    projection = rx * direction[0] + ry * direction[1]
    perpendicular = np.abs(rx * direction[1] - ry * direction[0])
    maximum = rgb.max(axis=2)
    hsv = cv2.cvtColor(rgba[..., :3], cv2.COLOR_RGB2HSV)
    # Strictly neutral metal. The previous broad RGB spread test could include
    # skin highlights close to the guard and create a ghost hand.
    silver = (alpha > 0) & (maximum > 55) & (hsv[..., 1] < 52)
    # Keep the original hand, grip and guard. Replacement begins only after
    # the ricasso/base of the blade.
    corridor = (projection >= 32.0) & (projection <= length + 10.0) & (perpendicular <= 29.0)
    if geometric:
        # Past the guard the sword is isolated against transparency in all
        # selected frames.  Keep the complete blade (including its dark
        # bevel/shadow) so the transformed copy cannot break into fragments.
        # The anchor is at the gripping hand, while the guard is roughly
        # 35-55 px farther along the axis.  Generated reference frames can
        # still contain hand/guard pixels farther down this narrow corridor,
        # so start at 150 px; the target's existing rigid blade covers the
        # entire near half and the transplant changes only its distal length.
        # sleeve or guard can enter the transplanted layer; the target's own
        # ricasso/base remains and hides the join.
        corridor = (projection >= 150.0) & (projection <= length + 10.0) & (perpendicular <= 31.0)
        mask = (alpha > 0) & corridor
    else:
        mask = silver & corridor
    if expand:
        mask = cv2.dilate(mask.astype(np.uint8), np.ones((3, 3), np.uint8), iterations=1) > 0
        mask &= corridor
    return mask


def make_blade_layer(image, frame):
    rgba = np.asarray(image.convert("RGBA")).copy()
    mask = blade_mask(image, frame, geometric=True)
    rgba[~mask] = 0
    return rgba


def transform_layer(layer, source_frame, target_frame):
    source_handle, _, source_axis, _ = axis(source_frame)
    target_handle, _, target_axis, _ = axis(target_frame)
    source_angle = math.atan2(float(source_axis[1]), float(source_axis[0]))
    target_angle = math.atan2(float(target_axis[1]), float(target_axis[0]))
    angle = target_angle - source_angle
    c, s = math.cos(angle), math.sin(angle)
    rotation = np.array([[c, -s], [s, c]], dtype=np.float32)
    translation = target_handle - rotation @ source_handle
    matrix = np.c_[rotation, translation]

    source = layer.astype(np.float32) / 255.0
    alpha = source[..., 3:4]
    premultiplied = np.concatenate((source[..., :3] * alpha, alpha), axis=2)
    warped = cv2.warpAffine(
        premultiplied, matrix, CANVAS, flags=cv2.INTER_LANCZOS4,
        borderMode=cv2.BORDER_CONSTANT, borderValue=(0, 0, 0, 0),
    )
    out_alpha = warped[..., 3:4]
    rgb = np.divide(warped[..., :3], out_alpha, out=np.zeros_like(warped[..., :3]), where=out_alpha > 1e-5)
    return np.uint8(np.round(np.concatenate((np.clip(rgb, 0, 1), np.clip(out_alpha, 0, 1)), axis=2) * 255))


def alpha_over(base, overlay):
    b = base.astype(np.float32) / 255.0
    o = overlay.astype(np.float32) / 255.0
    oa = o[..., 3:4]
    ba = b[..., 3:4]
    out_a = oa + ba * (1.0 - oa)
    premul = o[..., :3] * oa + b[..., :3] * ba * (1.0 - oa)
    rgb = np.divide(premul, out_a, out=np.zeros_like(premul), where=out_a > 1e-5)
    return np.uint8(np.round(np.concatenate((np.clip(rgb, 0, 1), np.clip(out_a, 0, 1)), axis=2) * 255))


def vector_blade(target_frame, reference_frame):
    """Draw one rigid, clean anime-style blade at the target angle."""
    handle, _, direction, _ = axis(target_frame)
    _, _, _, length = axis(reference_frame)
    # Keep a small transparent margin so the atlas never crops the sword tip.
    limits = []
    if direction[0] > 1e-6:
        limits.append((CANVAS[0] - 9.0 - handle[0]) / direction[0])
    elif direction[0] < -1e-6:
        limits.append((8.0 - handle[0]) / direction[0])
    if direction[1] > 1e-6:
        limits.append((CANVAS[1] - 9.0 - handle[1]) / direction[1])
    elif direction[1] < -1e-6:
        limits.append((8.0 - handle[1]) / direction[1])
    length = min(length, *(value for value in limits if value > 0))
    normal = np.array([-direction[1], direction[0]], dtype=np.float32)
    start = handle + direction * 28.0
    shoulder = handle + direction * (length - 23.0)
    tip = handle + direction * length

    scale = 3
    layer = np.zeros((CANVAS[1] * scale, CANVAS[0] * scale, 4), dtype=np.uint8)

    def pts(values):
        return np.round(np.stack(values) * scale).astype(np.int32)

    outer = pts([start + normal * 15.0, shoulder + normal * 5.5, tip,
                 shoulder - normal * 5.5, start - normal * 15.0])
    main = pts([start + normal * 12.0, shoulder + normal * 4.0, tip,
                shoulder - normal * 4.0, start - normal * 12.0])
    upper = pts([start + normal * 9.5, shoulder + normal * 3.2, tip,
                 shoulder + normal * 0.3, start + normal * 0.7])
    lower = pts([start + normal * 0.5, shoulder + normal * 0.2, tip,
                 shoulder - normal * 3.2, start - normal * 9.5])
    cv2.fillConvexPoly(layer, outer, (58, 61, 72, 255), lineType=cv2.LINE_AA)
    cv2.fillConvexPoly(layer, main, (201, 205, 216, 255), lineType=cv2.LINE_AA)
    cv2.fillConvexPoly(layer, lower, (132, 139, 154, 255), lineType=cv2.LINE_AA)
    cv2.fillConvexPoly(layer, upper, (242, 242, 247, 255), lineType=cv2.LINE_AA)
    # Narrow cold highlight along the upper bevel.
    a = tuple(np.round((start + normal * 8.0) * scale).astype(int))
    b = tuple(np.round((shoulder + normal * 2.7) * scale).astype(int))
    cv2.line(layer, a, b, (255, 255, 255, 220), 2 * scale, cv2.LINE_AA)
    return cv2.resize(layer, CANVAS, interpolation=cv2.INTER_AREA)


def repair(target_image, target_frame, reference_image, reference_frame):
    base = np.asarray(target_image.convert("RGBA")).copy()
    # Remove the old distal blade only.  The original hilt, hands, guard and
    # first 28 px of ricasso remain untouched, while the new blade overlaps
    # them slightly to make a seamless joint.
    rgba = np.asarray(target_image.convert("RGBA"))
    alpha = rgba[..., 3]
    handle, _, direction, old_length = axis(target_frame)
    yy, xx = np.indices(alpha.shape, dtype=np.float32)
    rx, ry = xx - handle[0], yy - handle[1]
    projection = rx * direction[0] + ry * direction[1]
    perpendicular = np.abs(rx * direction[1] - ry * direction[0])
    remove = (alpha > 0) & (projection >= 32.0) & (projection <= old_length + 15.0) & (perpendicular <= 36.0)
    base[remove] = 0
    replacement = vector_blade(target_frame, reference_frame)
    return Image.fromarray(alpha_over(base, replacement), "RGBA")


def checker(size, cell=18):
    w, h = size
    yy, xx = np.indices((h, w))
    colors = np.array([[42, 45, 51], [62, 66, 74]], dtype=np.uint8)
    return Image.fromarray(colors[((xx // cell + yy // cell) % 2).astype(np.uint8)], "RGB")


def composite(image):
    background = checker(CANVAS)
    background.paste(image, mask=image.getchannel("A"))
    return background


def marker_sheet(images):
    selected = sorted(ANCHORS)
    tile_w, tile_h, cols = 350, 330, 6
    rows = math.ceil(len(selected) / cols)
    sheet = Image.new("RGB", (tile_w * cols, tile_h * rows), (28, 30, 35))
    draw = ImageDraw.Draw(sheet)
    records = []
    for index, frame in enumerate(selected):
        tile = composite(images[frame - 1]).resize((tile_w, tile_h), Image.Resampling.LANCZOS)
        x = index % cols * tile_w
        y = index // cols * tile_h
        sheet.paste(tile, (x, y))
        handle, tip, _, length = axis(frame)
        h = (x + handle[0] / 4, y + handle[1] / 4)
        t = (x + tip[0] / 4, y + tip[1] / 4)
        draw.line((h[0], h[1], t[0], t[1]), fill=(70, 210, 255), width=2)
        draw.ellipse((h[0]-5, h[1]-5, h[0]+5, h[1]+5), fill=(255, 70, 70), outline="black")
        draw.ellipse((t[0]-5, t[1]-5, t[0]+5, t[1]+5), fill=(80, 220, 255), outline="black")
        draw.rectangle((x+5, y+5, x+112, y+31), fill=(12, 14, 18))
        draw.text((x+10, y+10), f"{frame:02d}  {length:.0f}px", fill=(255, 222, 105))
        records.append({"frame": frame, "handle": handle.tolist(), "tip": tip.tolist(), "length": round(length, 2)})
    sheet.save(OUT / "sword_anchor_markers_before.png", optimize=True)
    (OUT / "sword_anchors.json").write_text(json.dumps(records, ensure_ascii=False, indent=2)+"\n", encoding="utf-8")


def full_contact(images, filename):
    tw, th, cols = 280, 264, 8
    sheet = Image.new("RGB", (tw * cols, th * 5), (28, 30, 35))
    draw = ImageDraw.Draw(sheet)
    for i, image in enumerate(images, 1):
        x, y = ((i - 1) % cols) * tw, ((i - 1) // cols) * th
        sheet.paste(composite(image).resize((tw, th), Image.Resampling.LANCZOS), (x, y))
        draw.rectangle((x+6, y+6, x+45, y+31), fill=(12, 14, 18))
        draw.text((x+12, y+10), f"{i:02d}", fill=(255, 222, 105))
    sheet.save(OUT / filename, optimize=True)


def main():
    FRAMES.mkdir(parents=True, exist_ok=True)
    source = [Image.open(SOURCE / f"attack_sword_{i:02d}.png").convert("RGBA") for i in range(1, 40)]
    marker_sheet(source)
    result = [image.copy() for image in source]
    for target, reference in REFERENCE.items():
        result[target - 1] = repair(source[target - 1], target, source[reference - 1], reference)
    for i, image in enumerate(result, 1):
        image.save(FRAMES / f"attack_sword_{i:02d}.png", optimize=True)
    full_contact(result, "swordfixed_contact_sheet.png")
    previews = [composite(image).resize((700, 660), Image.Resampling.LANCZOS) for image in result]
    for i, preview in enumerate(previews):
        ImageDraw.Draw(preview).rectangle((696,656,699,659), fill=((i*67)%256,(i*131)%256,(i*197)%256))
    previews[0].save(
        OUT / "attack_sword_swordfixed39_40fps.gif", save_all=True,
        append_images=previews[1:], duration=[25]*39, loop=0,
        disposal=2, optimize=False,
    )


if __name__ == "__main__":
    main()
