from pathlib import Path
import json

import numpy as np
from PIL import Image, ImageDraw
from scipy.ndimage import distance_transform_edt


ROOT = Path(r"D:\StSmod\DoubleSS")
SOURCE = ROOT / "art_candidates" / "video_source" / "cast_v1" / "preview_v3_37f_50fps" / "frames"
OUT = ROOT / "art_candidates" / "video_source" / "cast_v1" / "final_v4_edgeclean_37f_50fps"
FRAMES = OUT / "frames"
FPS = 50


def clean_edge_rgb(image):
    rgba = np.array(image.convert("RGBA"), copy=True)
    alpha = rgba[..., 3]
    solid = alpha >= 248
    if not solid.any():
        raise RuntimeError("Frame has no solid foreground")
    _, nearest_index = distance_transform_edt(~solid, return_indices=True)
    nearest_rgb = rgba[nearest_index[0], nearest_index[1], :3]
    edge = (alpha > 0) & (alpha < 248)
    rgba[edge, :3] = nearest_rgb[edge]
    rgba[alpha == 0, :3] = 0
    return Image.fromarray(rgba, "RGBA")


def on_black(image):
    background = Image.new("RGBA", image.size, (0, 0, 0, 255))
    return Image.alpha_composite(background, image).convert("RGB")


def main():
    paths = sorted(SOURCE.glob("cast_attack_1_*.png"))
    if len(paths) != 37:
        raise RuntimeError(f"Expected 37 frames, found {len(paths)}")
    FRAMES.mkdir(parents=True, exist_ok=True)
    for old in FRAMES.glob("cast_attack_1_*.png"):
        old.unlink()

    cleaned = []
    for path in paths:
        image = clean_edge_rgb(Image.open(path))
        image.save(FRAMES / path.name, optimize=True)
        cleaned.append(image)

    previews = [on_black(image) for image in cleaned]
    previews[0].save(
        OUT / "cast_attack_1_v4_edgeclean_37f_50fps_black.gif",
        save_all=True,
        append_images=previews[1:],
        duration=20,
        loop=0,
        disposal=2,
    )

    thumb_w, thumb_h, cols = 256, 192, 7
    rows = (len(cleaned) + cols - 1) // cols
    sheet = Image.new("RGB", (cols * thumb_w, rows * (thumb_h + 22)), "black")
    draw = ImageDraw.Draw(sheet)
    for i, image in enumerate(cleaned):
        thumb = on_black(image).resize((thumb_w, thumb_h), Image.Resampling.LANCZOS)
        x = (i % cols) * thumb_w
        y = (i // cols) * (thumb_h + 22)
        sheet.paste(thumb, (x, y))
        draw.text((x + 5, y + thumb_h + 3), f"{i + 1:02d}", fill="white")
    sheet.save(OUT / "black_contact_sheet.png", optimize=True)
    (OUT / "metadata.json").write_text(
        json.dumps(
            {
                "source": str(SOURCE),
                "frames": len(cleaned),
                "fps": FPS,
                "duration_seconds": len(cleaned) / FPS,
                "edge_rgb_cleanup": "all 0<alpha<248 RGB replaced by nearest alpha>=248 RGB",
            },
            ensure_ascii=False,
            indent=2,
        )
        + "\n",
        encoding="utf-8",
    )
    print(OUT)


if __name__ == "__main__":
    main()
