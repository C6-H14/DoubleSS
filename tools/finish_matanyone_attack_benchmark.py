from pathlib import Path

import cv2
import numpy as np
from PIL import Image, ImageDraw
from scipy.ndimage import distance_transform_edt


ROOT = Path(r"D:\StSmod\DoubleSS")
VIDEO = ROOT / "art_candidates" / "video_source" / "attack_sword_v2.mp4"
TEST = ROOT / "art_candidates" / "matting_tests" / "matanyone_attack_v1"
PHA = TEST / "output_768" / "attack_sword_v2_matanyone768" / "pha"
RGBA = TEST / "rgba_frames_768"
PREVIEW = TEST / "previews"


def decontaminate_white(rgb: np.ndarray, alpha: np.ndarray, background: np.ndarray) -> np.ndarray:
    a = alpha[..., None].astype(np.float32) / 255.0
    src = rgb.astype(np.float32)
    # Source was generated over a nearly white background. Recover an
    # approximate unassociated foreground color before storing straight alpha.
    safe = np.maximum(a, 0.08)
    foreground = (src - (1.0 - a) * background.reshape(1, 1, 3)) / safe
    foreground = np.clip(foreground, 0, 255)

    # White-background footage stores white RGB in translucent edge pixels.
    # Alpha correction alone cannot remove that halo on a black composite.
    # Propagate colors outward from the nearest reliable opaque foreground
    # pixel while retaining the model's soft alpha coverage.
    opaque = alpha >= 235
    if np.any(opaque):
        _, nearest = distance_transform_edt(~opaque, return_indices=True)
        nearest_rgb = src[nearest[0], nearest[1]]
        fringe = (alpha > 2) & (alpha < 235)
        foreground[fringe] = nearest_rgb[fringe]

    # Fully opaque pixels must remain pixel-identical to the source.
    foreground = np.where(a >= 0.995, src, foreground)
    foreground[a[..., 0] <= 0.01] = 0
    return foreground.astype(np.uint8)


def checker(size, tile=24):
    w, h = size
    yy, xx = np.indices((h, w))
    pattern = ((xx // tile + yy // tile) % 2)[..., None]
    return np.where(pattern == 0, 218, 170).astype(np.uint8).repeat(3, axis=2)


def composite(rgba: np.ndarray, bg: np.ndarray) -> np.ndarray:
    a = rgba[..., 3:4].astype(np.float32) / 255.0
    return np.uint8(np.round(rgba[..., :3] * a + bg * (1.0 - a)))


def suppress_border_connected_background(rgb: np.ndarray, matte: np.ndarray) -> np.ndarray:
    """Remove white source background that the temporal matte absorbed.

    Only pixels color-compatible with the measured border and connected to a
    canvas edge are affected. Enclosed white costume/weapon regions are safe.
    A graded color confidence keeps antialiased edges instead of hard clipping.
    """
    h, w = matte.shape
    border = np.concatenate((rgb[0], rgb[-1], rgb[:, 0], rgb[:, -1]), axis=0)
    background = np.median(border, axis=0)
    distance = np.max(np.abs(rgb.astype(np.int16) - background.astype(np.int16)), axis=2)
    saturation = cv2.cvtColor(rgb, cv2.COLOR_RGB2HSV)[..., 1]
    candidate = ((distance <= 34) & (saturation <= 46)).astype(np.uint8)
    count, labels, stats, _ = cv2.connectedComponentsWithStats(candidate, 8)
    connected = np.zeros((h, w), dtype=bool)
    border_labels = set(labels[0]) | set(labels[-1]) | set(labels[:, 0]) | set(labels[:, -1])
    for label in border_labels:
        if label and stats[label, cv2.CC_STAT_AREA] >= 100:
            connected |= labels == label
    confidence = np.clip((distance.astype(np.float32) - 3.0) / 24.0, 0.0, 1.0)
    limited = np.uint8(np.round(confidence * 255.0))
    result = matte.copy()
    result[connected] = np.minimum(result[connected], limited[connected])

    # Generated white backgrounds can also become completely enclosed by hair,
    # arms, or motion, so connectivity alone is insufficient. In the upper
    # two-thirds, suppress near-exact background color wherever it occurs.
    # White socks are below this zone; the straight sword is restored later by
    # structural_interior_mask. A graded threshold avoids cutting pale skin.
    yy = np.indices((h, w))[0]
    enclosed_bg = (distance <= 30) & (saturation <= 46) & (yy < int(h * 0.68))
    enclosed_confidence = np.clip((distance.astype(np.float32) - 15.0) / 15.0, 0.0, 1.0)
    enclosed_limit = np.uint8(np.round(enclosed_confidence * 255.0))
    result[enclosed_bg] = np.minimum(result[enclosed_bg], enclosed_limit[enclosed_bg])
    return result


def structural_interior_mask(rgb: np.ndarray, matte: np.ndarray) -> np.ndarray:
    """Recover opaque interiors enclosed by dark outlines on the white source.

    MatAnyone correctly tracks the sword outline but can assign low alpha to its
    silver-white interior. Border-connected background analysis distinguishes
    that enclosed blade from the actually connected white background.
    """
    h, w = matte.shape
    border = np.concatenate((rgb[0], rgb[-1], rgb[:, 0], rgb[:, -1]), axis=0)
    background = np.median(border, axis=0)
    distance = np.max(np.abs(rgb.astype(np.int16) - background.astype(np.int16)), axis=2)
    hsv = cv2.cvtColor(rgb, cv2.COLOR_RGB2HSV)
    candidate_bg = ((distance <= 30) & (hsv[..., 1] <= 42)).astype(np.uint8)
    count, labels, stats, _ = cv2.connectedComponentsWithStats(candidate_bg, 8)
    border_labels = set(labels[0]) | set(labels[-1]) | set(labels[:, 0]) | set(labels[:, -1])
    connected_bg = np.zeros((h, w), np.uint8)
    for label in border_labels:
        if label and stats[label, cv2.CC_STAT_AREA] >= 100:
            connected_bg[labels == label] = 255
    silhouette = 255 - connected_bg

    # Reject unrelated video-compression islands: a structural component must
    # overlap the tracked MatAnyone subject after a small dilation.
    tracked = cv2.dilate((matte >= 24).astype(np.uint8), np.ones((9, 9), np.uint8))
    count, labels, stats, _ = cv2.connectedComponentsWithStats((silhouette > 0).astype(np.uint8), 8)
    kept = np.zeros_like(silhouette)
    for label in range(1, count):
        component = labels == label
        if stats[label, cv2.CC_STAT_AREA] >= 16 and np.any(tracked[component]):
            kept[component] = 255
    # Only repair long, narrow low-alpha gaps. This describes the rigid sword
    # blade, while rejecting small enclosed white holes between hair strands.
    safe_interior = cv2.erode(kept, np.ones((3, 3), np.uint8), iterations=1)
    missing = ((safe_interior > 0) & (matte < 245)).astype(np.uint8)
    count, labels, stats, _ = cv2.connectedComponentsWithStats(missing, 8)
    sword_repair = np.zeros_like(safe_interior)
    for label in range(1, count):
        ys, xs = np.where(labels == label)
        area = len(xs)
        if area < 40:
            continue
        points = np.column_stack((xs, ys)).astype(np.float32)
        centered = points - points.mean(axis=0, keepdims=True)
        covariance = centered.T @ centered / max(1, area - 1)
        eigenvalues = np.linalg.eigvalsh(covariance)
        minor = max(float(eigenvalues[0]), 1.0)
        major = float(eigenvalues[1])
        major_span = 4.0 * np.sqrt(major)
        elongation = major / minor
        # A sword interior is extremely linear. Curved gaps between long hair
        # strands may be elongated, but their PCA ratio is substantially lower.
        if major_span >= 100 and elongation >= 30:
            sword_repair[labels == label] = 255
    # Recover a narrow antialiased band around the accepted blade interior,
    # but never extend beyond the structural silhouette.
    sword_repair = cv2.dilate(sword_repair, np.ones((3, 3), np.uint8), iterations=1)
    return cv2.bitwise_and(sword_repair, safe_interior)


def main():
    RGBA.mkdir(parents=True, exist_ok=True)
    PREVIEW.mkdir(parents=True, exist_ok=True)
    alpha_paths = sorted(PHA.glob("*.png"))
    if not alpha_paths:
        raise RuntimeError(f"No alpha frames in {PHA}")

    capture = cv2.VideoCapture(str(VIDEO))
    fps = capture.get(cv2.CAP_PROP_FPS)
    frames = []
    for index, alpha_path in enumerate(alpha_paths):
        ok, bgr = capture.read()
        if not ok:
            raise RuntimeError(f"Source video ended at frame {index}")
        alpha = cv2.imread(str(alpha_path), cv2.IMREAD_GRAYSCALE)
        h, w = alpha.shape
        rgb = cv2.cvtColor(cv2.resize(bgr, (w, h), interpolation=cv2.INTER_AREA), cv2.COLOR_BGR2RGB)
        alpha = suppress_border_connected_background(rgb, alpha)
        interior = structural_interior_mask(rgb, alpha)
        alpha = np.maximum(alpha, interior)
        clean_rgb = decontaminate_white(rgb, alpha, np.array([253, 253, 253], np.float32))
        rgba = np.dstack((clean_rgb, alpha))
        Image.fromarray(rgba, "RGBA").save(RGBA / f"frame_{index:03d}.png", optimize=True)
        frames.append(rgba)

    # Animated previews on backgrounds that expose both white and dark fringes.
    preview_frames = []
    for rgba in frames:
        h, w = rgba.shape[:2]
        scale = 512 / w
        small = np.asarray(Image.fromarray(rgba, "RGBA").resize((512, round(h * scale)), Image.Resampling.LANCZOS))
        sh, sw = small.shape[:2]
        panels = [
            composite(small, np.zeros((sh, sw, 3), np.uint8)),
            composite(small, np.full((sh, sw, 3), 255, np.uint8)),
            composite(small, checker((sw, sh))),
        ]
        preview_frames.append(Image.fromarray(np.concatenate(panels, axis=1), "RGB"))
    duration = max(1, round(1000 / fps))
    preview_frames[0].save(
        PREVIEW / "matanyone_black_white_checker.gif",
        save_all=True,
        append_images=preview_frames[1:],
        duration=duration,
        loop=0,
        optimize=False,
    )

    # Five-point contact sheet: source alpha plus checker composite.
    picks = np.linspace(0, len(frames) - 1, 5).round().astype(int)
    thumb_w = 320
    rows = []
    for index in picks:
        rgba = frames[index]
        h, w = rgba.shape[:2]
        th = round(h * thumb_w / w)
        small = np.asarray(Image.fromarray(rgba, "RGBA").resize((thumb_w, th), Image.Resampling.LANCZOS))
        matte = np.repeat(small[..., 3:4], 3, axis=2)
        comp = composite(small, checker((thumb_w, th), 16))
        row = np.concatenate((matte, comp), axis=1)
        image = Image.fromarray(row, "RGB")
        draw = ImageDraw.Draw(image)
        draw.rectangle((0, 0, 110, 24), fill=(0, 0, 0))
        draw.text((6, 5), f"frame {index:03d}", fill=(255, 255, 255))
        rows.append(image)
    sheet = Image.new("RGB", (thumb_w * 2, sum(r.height for r in rows)), (32, 32, 32))
    y = 0
    for row in rows:
        sheet.paste(row, (0, y))
        y += row.height
    sheet.save(PREVIEW / "matanyone_contact_sheet.png")

    # Black is the strict acceptance background for white-source footage.
    black_panels = []
    for index in picks:
        rgba = frames[index]
        h, w = rgba.shape[:2]
        rendered = composite(rgba, np.zeros((h, w, 3), np.uint8))
        crop = Image.fromarray(rendered, "RGB").crop((200, 20, 840, 740))
        draw = ImageDraw.Draw(crop)
        draw.rectangle((0, 0, 120, 28), fill=(0, 0, 0))
        draw.text((7, 7), f"frame {index:03d}", fill=(255, 255, 255))
        black_panels.append(crop)
    black_sheet = Image.new("RGB", (sum(p.width for p in black_panels), black_panels[0].height), (0, 0, 0))
    x = 0
    for panel in black_panels:
        black_sheet.paste(panel, (x, 0))
        x += panel.width
    black_sheet.save(PREVIEW / "black_background_contact_sheet.png")
    print(f"frames={len(frames)} fps={fps:.6f} size={frames[0].shape[1]}x{frames[0].shape[0]}")


if __name__ == "__main__":
    main()
