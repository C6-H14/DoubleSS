from pathlib import Path
import json
import shutil

import cv2
import numpy as np
from PIL import Image


ROOT = Path(r"D:\StSmod")
SOURCE = ROOT / "art_preview" / "hurt_v8_measured_landmark_warp" / "hurt_v8_46f_frames_60fps"
OUT = ROOT / "art_preview" / "hurt_v11_abdomen_hand_even_reveal"
FRAMES = OUT / "hurt_v11_46f_frames_60fps"
GIF = OUT / "hurt_v11_46f_60fps.gif"
MP4 = OUT / "hurt_v11_46f_60fps.mp4"
FPS = 60
ROI = (495, 325, 600, 400)
WRIST = (570, 338)


def endpoint_hand_layer(endpoint_rgb: np.ndarray) -> tuple[np.ndarray, np.ndarray]:
    x1, y1, x2, y2 = ROI
    roi = endpoint_rgb[y1:y2, x1:x2]
    r, g, b = [roi[..., i].astype(np.int16) for i in range(3)]
    skin = ((r >= 155) & (g >= 95) & (g <= 215) & (b >= 80) & (b <= 195)
            & (r - g >= 10) & (g - b >= 0)).astype(np.uint8) * 255
    skin = cv2.morphologyEx(skin, cv2.MORPH_CLOSE, np.ones((5, 5), np.uint8))
    count, labels, stats, _ = cv2.connectedComponentsWithStats(skin, 8)
    keep = np.zeros_like(skin)
    if count > 1:
        largest = 1 + int(np.argmax(stats[1:, cv2.CC_STAT_AREA]))
        keep[labels == largest] = 255
    keep = cv2.GaussianBlur(keep, (5, 5), 0.8)
    layer = np.zeros_like(endpoint_rgb)
    alpha = np.zeros(endpoint_rgb.shape[:2], np.uint8)
    layer[y1:y2, x1:x2] = roi
    alpha[y1:y2, x1:x2] = keep
    return layer, alpha


def scaled_about_wrist(layer: np.ndarray, alpha: np.ndarray, scale: float,
                       target_wrist: tuple[float, float]) -> tuple[np.ndarray, np.ndarray]:
    wx, wy = WRIST
    tx, ty = target_wrist
    matrix = np.array([[scale, 0.0, tx - scale * wx],
                       [0.0, scale, ty - scale * wy]], dtype=np.float32)
    size = (layer.shape[1], layer.shape[0])
    warped_layer = cv2.warpAffine(layer, matrix, size, flags=cv2.INTER_LANCZOS4,
                                  borderMode=cv2.BORDER_CONSTANT)
    warped_alpha = cv2.warpAffine(alpha, matrix, size, flags=cv2.INTER_LANCZOS4,
                                  borderMode=cv2.BORDER_CONSTANT)
    return warped_layer, warped_alpha


def composite(base: np.ndarray, layer: np.ndarray, alpha: np.ndarray) -> np.ndarray:
    a = alpha.astype(np.float32)[..., None] / 255.0
    return np.clip(base * (1.0 - a) + layer * a, 0, 255).astype(np.uint8)


def main() -> None:
    paths = sorted(SOURCE.glob("frame_*.png"))
    images = [np.asarray(Image.open(path).convert("RGB")) for path in paths]
    if len(images) != 46:
        raise RuntimeError(f"Expected 46 frames, got {len(images)}")
    # In v8, output 32 is the unchanged endpoint with the complete palm.
    layer, alpha = endpoint_hand_layer(images[31])
    settings = {
        29: (0.54, (568.7, 340.7)),  # output frame 30
        30: (0.78, (569.3, 339.3)),  # output frame 31
    }
    for zero_index, (scale, wrist) in settings.items():
        hand, hand_alpha = scaled_about_wrist(layer, alpha, scale, wrist)
        images[zero_index] = composite(images[zero_index], hand, hand_alpha)

    if OUT.exists():
        shutil.rmtree(OUT)
    FRAMES.mkdir(parents=True)
    pil_images = []
    for i, rgb in enumerate(images, 1):
        image = Image.fromarray(rgb, "RGB")
        image.save(FRAMES / f"frame_{i:03d}.png", optimize=True)
        pil_images.append(image)
    pil_images[0].save(GIF, save_all=True, append_images=pil_images[1:],
                       duration=round(1000 / FPS), loop=0, disposal=2,
                       optimize=False)
    width, height = pil_images[0].size
    writer = cv2.VideoWriter(str(MP4), cv2.VideoWriter_fourcc(*"mp4v"),
                             FPS, (width, height))
    for rgb in images:
        writer.write(cv2.cvtColor(rgb, cv2.COLOR_RGB2BGR))
    writer.release()

    # Native-pixel close-up contact for checking visible hand area progression.
    crops = [Image.fromarray(images[i], "RGB").crop((460, 285, 625, 395))
             .resize((660, 440), Image.Resampling.NEAREST) for i in range(28, 32)]
    contact = Image.new("RGB", (1320, 880), (20, 20, 20))
    for i, crop in enumerate(crops):
        contact.paste(crop, ((i % 2) * 660, (i // 2) * 440))
    contact.save(OUT / "abdomen_hand_29_32.png")

    report = {
        "frames": 46, "fps": FPS,
        "repaired_output_frames": [30, 31],
        "method": "progressive endpoint-palm reveal about linearly moving wrist",
        "palm_scales": [0.54, 0.78, 1.0],
        "wrist_anchor_points": [[568, 342], [568.7, 340.7],
                                 [569.3, 339.3], [570, 338]],
        "wrist_points": [[528, 342], [529.33, 339.67],
                         [530.67, 337.33], [532, 335]],
    }
    (OUT / "repair_report.json").write_text(
        json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    print(json.dumps(report, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
