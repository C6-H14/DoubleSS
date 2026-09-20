from pathlib import Path
import json
import shutil

import cv2
import numpy as np
from PIL import Image, ImageDraw


ROOT = Path(r"D:\StSmod\DoubleSS")
SOURCE = ROOT / "art_candidates" / "video_source" / "cast_prayer" / "preview_v3" / "frames"
OUTPUT = ROOT / "art_candidates" / "video_source" / "cast_prayer" / "preview_v4"
FRAMES = OUTPUT / "frames"


def foreground_anchor(image: np.ndarray) -> tuple[float, float, np.ndarray]:
    border = np.concatenate(
        [
            image[:30].reshape(-1, 3),
            image[-30:].reshape(-1, 3),
            image[:, :30].reshape(-1, 3),
            image[:, -30:].reshape(-1, 3),
        ]
    )
    background = np.median(border, axis=0)
    distance = np.linalg.norm(image.astype(np.float32) - background, axis=2)
    mask = distance > 35
    ys, xs = np.where(mask)
    if not len(xs):
        raise RuntimeError("No foreground detected")
    # Median X is resistant to the sword tip; the high Y percentile tracks the
    # contact line without being controlled by isolated compression speckles.
    return float(np.median(xs)), float(np.percentile(ys, 99.8)), background


def shift_to_anchor(image: np.ndarray, target_x: float, target_y: float) -> tuple[np.ndarray, int, int]:
    x, y, background = foreground_anchor(image)
    dx = int(round(target_x - x))
    dy = int(round(target_y - y))
    matrix = np.float32([[1, 0, dx], [0, 1, dy]])
    fill = tuple(int(round(v)) for v in background)
    shifted = cv2.warpAffine(
        image,
        matrix,
        (image.shape[1], image.shape[0]),
        flags=cv2.INTER_LANCZOS4,
        borderMode=cv2.BORDER_CONSTANT,
        borderValue=fill,
    )
    return shifted, dx, dy


def main() -> None:
    if OUTPUT.exists():
        shutil.rmtree(OUTPUT)
    FRAMES.mkdir(parents=True)

    # All numbers refer to preview_v3's original 1-based frame numbers.
    remove = {10, 12, 14, 16, 18, 35, 37, 39, 41, 43, *range(28, 34)}
    reference = cv2.imread(str(SOURCE / "cast_prayer_027.png"))
    target_x, target_y, _ = foreground_anchor(reference)

    kept = []
    manifest = []
    for original in range(1, 46):
        if original in remove:
            continue
        image = cv2.imread(str(SOURCE / f"cast_prayer_{original:03d}.png"))
        dx = dy = 0
        if original >= 34:
            image, dx, dy = shift_to_anchor(image, target_x, target_y)
        new_index = len(kept) + 1
        out = FRAMES / f"cast_prayer_{new_index:03d}.png"
        cv2.imwrite(str(out), image)
        kept.append(Image.fromarray(cv2.cvtColor(image, cv2.COLOR_BGR2RGB)))
        manifest.append({"frame": new_index, "source_frame": original, "dx": dx, "dy": dy})

    kept[0].save(
        OUTPUT / "cast_prayer_preview_60fps.gif",
        save_all=True,
        append_images=kept[1:],
        duration=17,
        loop=0,
        optimize=False,
    )

    thumb, cols = 250, 6
    rows = (len(kept) + cols - 1) // cols
    sheet = Image.new("RGB", (cols * thumb, rows * (thumb + 30)), "#222")
    draw = ImageDraw.Draw(sheet)
    for i, (image, item) in enumerate(zip(kept, manifest)):
        x = i % cols * thumb
        y = i // cols * (thumb + 30)
        sheet.paste(image.resize((thumb, thumb), Image.Resampling.LANCZOS), (x, y))
        draw.text((x + 5, y + thumb + 6), f"{i+1:02d} (src {item['source_frame']:02d})", fill="white")
    sheet.save(OUTPUT / "cast_prayer_frames_contact.jpg", quality=95)

    metadata = {
        "fps": 60,
        "frames": len(kept),
        "duration_seconds": len(kept) / 60,
        "removed_source_frames": sorted(remove),
        "alignment_reference_source_frame": 27,
        "manifest": manifest,
    }
    (OUTPUT / "metadata.json").write_text(json.dumps(metadata, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({k: v for k, v in metadata.items() if k != "manifest"}, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
