from pathlib import Path

import numpy as np
from PIL import Image, ImageFilter


SOURCE = Path(
    r"D:\StSmod\DoubleSS\art_candidates\video_source\attack_sword_v2_final16_15fps"
    r"\alpha_cleanup_v3\cleaned_master_frames"
)
DESTINATION = Path(
    r"D:\StSmod\DoubleSS\art_candidates\attack_sword_frame_animation_v4_clean\images_clean2"
)
TARGET_SIZE = (556, 417)


def resize_premultiplied(image: Image.Image) -> Image.Image:
    rgba = np.asarray(image.convert("RGBA"), dtype=np.float32)
    alpha = rgba[..., 3:4] / 255.0
    premultiplied = np.concatenate((rgba[..., :3] * alpha, rgba[..., 3:4]), axis=2)
    resized = np.asarray(
        Image.fromarray(np.clip(premultiplied, 0, 255).astype(np.uint8), "RGBA").resize(
            TARGET_SIZE, Image.Resampling.LANCZOS
        ),
        dtype=np.float32,
    )
    resized_alpha_image = Image.fromarray(resized[..., 3].astype(np.uint8), "L")
    resized_alpha = np.asarray(
        resized_alpha_image.filter(ImageFilter.MinFilter(3)), dtype=np.float32
    )[..., None]
    rgb = np.zeros_like(resized[..., :3])
    np.divide(
        resized[..., :3] * 255.0,
        resized_alpha,
        out=rgb,
        where=resized_alpha > 0,
    )
    return Image.fromarray(
        np.concatenate((np.clip(rgb, 0, 255), resized_alpha), axis=2).astype(np.uint8),
        "RGBA",
    )


DESTINATION.mkdir(parents=True, exist_ok=True)
frames = sorted(SOURCE.glob("attack_sword_*.png"))
for index, frame in enumerate(frames, 1):
    resized = resize_premultiplied(Image.open(frame))
    resized.save(DESTINATION / f"attack_sword_clean2_{index:02d}.png")

print(f"Wrote {len(frames)} frames to {DESTINATION}")
