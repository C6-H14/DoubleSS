from pathlib import Path
import json
import shutil

import cv2
import numpy as np
from PIL import Image, ImageDraw


ROOT = Path(r"D:\StSmod\DoubleSS")
SOURCE = ROOT / "art_candidates" / "video_source" / "idle" / "short_blink_final_v8_bleed_color_normalized" / "alpha_frames"
OUT = ROOT / "art_candidates" / "video_source" / "idle" / "short_blink_final_v11_pommel_unified"
FRAMES = OUT / "alpha_frames"
DONOR_FRAME = 1
POMMEL_BOX = (438, 392, 482, 438)
HAND_ROI = (420, 350, 590, 472)


def hand_anchor(rgba):
    x1, y1, x2, y2 = HAND_ROI
    rgb = rgba[y1:y2, x1:x2, :3]
    alpha = rgba[y1:y2, x1:x2, 3]
    r, g, b = [rgb[:, :, i].astype(np.int16) for i in range(3)]
    skin = (alpha > 180) & (r > 165) & (g > 95) & (b > 75) & (r > g + 8) & (g > b - 10)
    count, labels, stats, centers = cv2.connectedComponentsWithStats(skin.astype(np.uint8), 8)
    candidates = []
    for i in range(1, count):
        if stats[i, cv2.CC_STAT_AREA] >= 80:
            cx, cy = centers[i]
            candidates.append((cx + x1, cy + y1, stats[i, cv2.CC_STAT_AREA]))
    if not candidates:
        raise RuntimeError("hand anchor not found")
    # Viewer-left hand is the leftmost substantial skin component.
    cx, cy, _ = min(candidates, key=lambda item: item[0])
    return np.array([cx, cy], dtype=np.float32)


def has_pommel(rgba, anchor):
    rgb = rgba[:, :, :3]
    alpha = rgba[:, :, 3]
    hsv = cv2.cvtColor(rgb, cv2.COLOR_RGB2HSV)
    yy, xx = np.indices(alpha.shape)
    region = ((xx >= anchor[0] - 62) & (xx <= anchor[0] - 20) &
              (yy >= anchor[1] - 38) & (yy <= anchor[1] - 7))
    gold = (alpha > 32) & (hsv[:, :, 0] >= 5) & (hsv[:, :, 0] <= 35) & (hsv[:, :, 1] > 75)
    return int(np.count_nonzero(region & gold)) >= 45


def main():
    if OUT.exists():
        shutil.rmtree(OUT)
    FRAMES.mkdir(parents=True)
    paths = sorted(SOURCE.glob("idle_*.png"))
    donor = np.asarray(Image.open(paths[DONOR_FRAME - 1]).convert("RGBA"), dtype=np.uint8)
    donor_anchor = hand_anchor(donor)
    x1, y1, x2, y2 = POMMEL_BOX
    patch = donor[y1:y2, x1:x2].copy()
    # Limit the donor to the weapon tail: exclude skin/sleeve accidentally
    # present in the rectangular crop. Target subject pixels will occlude it.
    hsv = cv2.cvtColor(patch[:, :, :3], cv2.COLOR_RGB2HSV)
    weapon = (patch[:, :, 3] > 0) & (hsv[:, :, 0] >= 2) & (hsv[:, :, 0] <= 35) & (hsv[:, :, 1] > 45)
    weapon = cv2.dilate(weapon.astype(np.uint8), np.ones((3, 3), np.uint8)) > 0
    patch[:, :, 3] = np.where(weapon, patch[:, :, 3], 0).astype(np.uint8)

    modified, already = [], []
    anchors = []
    outputs = []
    for index, path in enumerate(paths, 1):
        rgba = np.asarray(Image.open(path).convert("RGBA"), dtype=np.uint8).copy()
        anchor = hand_anchor(rgba)
        anchors.append(anchor.tolist())
        present_before = has_pommel(rgba, anchor)
        if present_before:
            already.append(index)
        dx, dy = np.rint(anchor - donor_anchor).astype(int)
        px, py = x1 + dx, y1 + dy
        h, w = patch.shape[:2]
        target = rgba[py:py+h, px:px+w]
        if target.shape[:2] != (h, w):
            raise RuntimeError(f"pommel outside canvas at frame {index}")
        # Apply to every frame at one canonical hand-relative position. Existing
        # opaque pixels win, so the hand/sleeve occlude the tail and frames that
        # already contain it are not double-painted.
        visible = (target[:, :, 3] <= 8) & (patch[:, :, 3] > 0)
        if np.any(visible):
            target[visible] = patch[visible]
            modified.append(index)
        image = Image.fromarray(rgba, "RGBA")
        image.save(FRAMES / f"idle_{index:03d}.png", optimize=True)
        outputs.append(image)

    views = [Image.alpha_composite(Image.new("RGBA", im.size, (38, 27, 27, 255)), im)
             .resize((533, 400), Image.Resampling.LANCZOS).convert("RGB") for im in outputs]
    views[0].save(OUT / "idle_game_dark_3s_60fps.gif", save_all=True,
                  append_images=views[1:], duration=17, loop=0, disposal=2, optimize=False)
    picks = list(range(1, 181, 8)) + [16, 57, 120, 180]
    picks = sorted(set(picks))
    sheet = Image.new("RGB", (320 * 5, 250 * ((len(picks)+4)//5)), (35,35,35))
    draw = ImageDraw.Draw(sheet)
    for k, n in enumerate(picks):
        crop = outputs[n-1].crop((400, 355, 620, 525)).resize((320, 247), Image.Resampling.NEAREST)
        bg = Image.new("RGBA", crop.size, (40,40,40,255)); bg.alpha_composite(crop)
        x, y = (k%5)*320, (k//5)*250
        sheet.paste(bg.convert("RGB"), (x,y)); draw.text((x+4,y+4), f"{n:03d}", fill="white")
    sheet.save(OUT / "pommel_audit_contact.png", optimize=True)
    report = {"frames": len(paths), "donor_frame": DONOR_FRAME,
              "modified_frames": modified, "already_visible_frames": already,
              "anchors": anchors}
    (OUT / "pommel_report.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"modified_count": len(modified), "already_visible_count": len(already),
                      "modified_frames": modified}, indent=2))


if __name__ == "__main__":
    main()
