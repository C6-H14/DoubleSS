from pathlib import Path
import json
import shutil

import cv2
import numpy as np
from PIL import Image, ImageDraw
from scipy.ndimage import distance_transform_edt


ROOT = Path(r"D:\StSmod\DoubleSS")
BASE = ROOT / "art_candidates" / "video_source" / "cast_prayer"
SOURCE = BASE / "preview_v6" / "alpha_frames"
OUTPUT = BASE / "preview_v7_stable_clean"
FRAMES = OUTPUT / "alpha_frames"
QA = OUTPUT / "alpha_qa"


def shoe_anchor(rgba: np.ndarray) -> tuple[float, float]:
    bgr = rgba[..., :3]
    alpha = rgba[..., 3]
    hsv = cv2.cvtColor(bgr, cv2.COLOR_BGR2HSV)
    mask = ((hsv[..., 0] <= 20) & (hsv[..., 1] >= 45) &
            (hsv[..., 2] >= 25) & (hsv[..., 2] <= 190) & (alpha > 100)).astype(np.uint8)
    mask[:620] = 0
    count, labels, stats, centers = cv2.connectedComponentsWithStats(mask, 8)
    candidates = []
    for component in range(1, count):
        x, y, w, h, area = stats[component]
        cx, cy = centers[component]
        if cx > 500 and cy > 650 and 18 <= area and w <= 110 and h <= 90:
            # Prefer the substantial rightmost shoe; reject tiny effect specks.
            candidates.append((area, cx, cy))
    if not candidates:
        raise RuntimeError("Unable to locate planted right shoe")
    area, cx, cy = max(candidates)
    return float(cx), float(cy)


def translate(rgba: np.ndarray, dx: int, dy: int) -> np.ndarray:
    matrix = np.float32([[1, 0, dx], [0, 1, dy]])
    return cv2.warpAffine(rgba, matrix, (rgba.shape[1], rgba.shape[0]), flags=cv2.INTER_LANCZOS4,
                          borderMode=cv2.BORDER_CONSTANT, borderValue=(0, 0, 0, 0))


def clean_edges(rgba: np.ndarray) -> np.ndarray:
    rgb = rgba[..., :3].copy()
    alpha = rgba[..., 3].copy()

    # Suppress low-alpha video/keying debris while retaining antialiasing.
    af = alpha.astype(np.float32)
    af = np.clip((af - 24.0) / (232.0 - 24.0), 0.0, 1.0)
    af = af * af * (3.0 - 2.0 * af)
    alpha = np.uint8(np.round(af * 255.0))
    alpha[alpha < 8] = 0
    alpha[alpha > 247] = 255

    solid = alpha >= 248
    core = cv2.erode(solid.astype(np.uint8), np.ones((3, 3), np.uint8), iterations=2).astype(bool)
    if not core.any():
        raise RuntimeError("No clean foreground core")
    _, nearest = distance_transform_edt(~core, return_indices=True)
    nearest_rgb = rgb[nearest[0], nearest[1]]

    # Replace the outer two pixels' RGB with uncontaminated interior colour.
    inside_distance = cv2.distanceTransform(solid.astype(np.uint8), cv2.DIST_L2, 3)
    boundary = (alpha > 0) & ((alpha < 255) | (inside_distance <= 2.2))
    rgb[boundary] = nearest_rgb[boundary]
    rgb[alpha == 0] = 0
    return np.dstack((rgb, alpha))


def composite(rgba: np.ndarray, background: tuple[int, int, int]) -> Image.Image:
    image = Image.fromarray(cv2.cvtColor(rgba, cv2.COLOR_BGRA2RGBA), "RGBA")
    return Image.alpha_composite(Image.new("RGBA", image.size, (*background, 255)), image).convert("RGB")


def main() -> None:
    if OUTPUT.exists():
        shutil.rmtree(OUTPUT)
    FRAMES.mkdir(parents=True)
    QA.mkdir(parents=True)

    paths = sorted(SOURCE.glob("cast_prayer_*.png"))
    raw = [cv2.imread(str(path), cv2.IMREAD_UNCHANGED) for path in paths]
    target_x, target_y = shoe_anchor(raw[0])
    frames = []
    manifest = []
    for index, image in enumerate(raw, 1):
        x, y = shoe_anchor(image)
        dx, dy = round(target_x - x), round(target_y - y)
        stable = translate(image, dx, dy)
        clean = clean_edges(stable)
        cv2.imwrite(str(FRAMES / f"cast_prayer_{index:03d}.png"), clean)
        frames.append(clean)
        manifest.append({"frame": index, "shoe_x": x, "shoe_y": y, "dx": dx, "dy": dy})

    # Dynamic black/white checks expose both bright and dark fringes.
    for name, bg in (("black", (0, 0, 0)), ("white", (255, 255, 255))):
        preview = [composite(frame, bg).resize((512, 512), Image.Resampling.LANCZOS) for frame in frames]
        preview[0].save(QA / f"preview_{name}_60fps.gif", save_all=True, append_images=preview[1:],
                        duration=17, loop=0, disposal=2)

    picks = np.linspace(0, len(frames) - 1, 20).round().astype(int)
    thumb, cols = 256, 5
    rows = (len(picks) + cols - 1) // cols
    sheet = Image.new("RGB", (cols * thumb, rows * (thumb * 2 + 24)), "#222")
    draw = ImageDraw.Draw(sheet)
    for slot, picked in enumerate(picks):
        x, y = slot % cols * thumb, slot // cols * (thumb * 2 + 24)
        sheet.paste(composite(frames[int(picked)], (0, 0, 0)).resize((thumb, thumb), Image.Resampling.LANCZOS), (x, y))
        sheet.paste(composite(frames[int(picked)], (255, 255, 255)).resize((thumb, thumb), Image.Resampling.LANCZOS), (x, y + thumb))
        draw.text((x + 4, y + thumb * 2 + 4), f"frame {int(picked)+1:03d}", fill="white")
    sheet.save(QA / "alpha_contact_black_white.jpg", quality=95)
    (OUTPUT / "stabilization.json").write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
    print(f"frames={len(frames)} target_shoe=({target_x:.2f},{target_y:.2f})")
    print(f"shift_x={min(x['dx'] for x in manifest)}..{max(x['dx'] for x in manifest)}")
    print(f"shift_y={min(x['dy'] for x in manifest)}..{max(x['dy'] for x in manifest)}")


if __name__ == "__main__":
    main()
