from __future__ import annotations

import shutil
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFont


ROOT = Path(r"D:\StSmod\DoubleSS\art_candidates\video_source")
SOURCE = ROOT / "attack_sword_v2_final16_15fps" / "alpha_cleanup_v3" / "final_web_clean_frames"
WORK = ROOT / "attack_sword_v2_22frame_v1"
GENERATED = WORK / "generated_transitions"
FRAMES = WORK / "frames"
CANVAS = (1400, 1320)
BASELINE_Y = 1079


def premultiplied_resize(image: Image.Image, size: tuple[int, int]) -> Image.Image:
    rgba = np.asarray(image.convert("RGBA"), dtype=np.float32) / 255.0
    alpha = rgba[..., 3:4]
    premul = np.concatenate((rgba[..., :3] * alpha, alpha), axis=2)
    channels = []
    for index in range(4):
        channel = Image.fromarray(np.clip(premul[..., index] * 255.0, 0, 255).astype(np.uint8), "L")
        channels.append(np.asarray(channel.resize(size, Image.Resampling.LANCZOS), dtype=np.float32) / 255.0)
    resized = np.stack(channels, axis=2)
    out_alpha = resized[..., 3:4]
    rgb = np.divide(resized[..., :3], out_alpha, out=np.zeros_like(resized[..., :3]), where=out_alpha > 1e-5)
    out = np.concatenate((np.clip(rgb, 0, 1), np.clip(out_alpha, 0, 1)), axis=2)
    return Image.fromarray(np.round(out * 255.0).astype(np.uint8), "RGBA")


def scale_canvas_about_anchor(image: Image.Image, scale: float, anchor: tuple[float, float]) -> Image.Image:
    content = image.convert("RGBA")
    new_size = (round(content.width * scale), round(content.height * scale))
    resized = premultiplied_resize(content, new_size)
    x = round(anchor[0] - anchor[0] * scale)
    y = round(anchor[1] - anchor[1] * scale)
    canvas = Image.new("RGBA", CANVAS)
    canvas.alpha_composite(resized, (x, y))
    return canvas


def normalize_generated(filename: str, target_center_x: float, target_height: int = 650) -> Image.Image:
    image = Image.open(GENERATED / filename).convert("RGBA")
    # Image generation can leave nearly invisible alpha noise far from the sprite.
    # Ignore it when determining the crop or it makes the actual character tiny.
    solid_alpha = image.getchannel("A").point(lambda value: 255 if value >= 8 else 0)
    bbox = solid_alpha.getbbox()
    if bbox is None:
        raise RuntimeError(f"Generated image has no visible pixels: {filename}")
    crop = image.crop(bbox)
    scale = target_height / crop.height
    resized = premultiplied_resize(crop, (round(crop.width * scale), target_height))
    x = round(target_center_x - resized.width / 2)
    y = BASELINE_Y - resized.height
    canvas = Image.new("RGBA", CANVAS)
    canvas.alpha_composite(resized, (x, y))
    return canvas


def checkerboard(size: tuple[int, int], cell: int = 18) -> Image.Image:
    width, height = size
    yy, xx = np.indices((height, width))
    mask = ((xx // cell + yy // cell) % 2).astype(np.uint8)
    colors = np.array([[42, 45, 51], [62, 66, 74]], dtype=np.uint8)
    return Image.fromarray(colors[mask], "RGB")


def preview_frame(frame: Image.Image, size: tuple[int, int] = (700, 660)) -> Image.Image:
    background = checkerboard(CANVAS)
    background.paste(frame, mask=frame.getchannel("A"))
    return background.resize(size, Image.Resampling.LANCZOS)


def main() -> None:
    FRAMES.mkdir(parents=True, exist_ok=True)
    originals = {i: Image.open(SOURCE / f"attack_sword_{i:02d}.png").convert("RGBA") for i in range(1, 17)}

    corrected_04 = scale_canvas_about_anchor(originals[4], 0.90, (735, BASELINE_Y))
    corrected_05 = scale_canvas_about_anchor(originals[5], 0.90, (735, BASELINE_Y))
    transition_03_04 = normalize_generated("transition_03_04.png", 608, 650)
    transition_13_14 = normalize_generated("transition_13_14.png", 906, 650)
    transition_14_15 = normalize_generated("transition_14_15.png", 804, 650)

    sequence = [
        originals[1], originals[2], originals[3], transition_03_04,
        corrected_04, corrected_05, originals[6], originals[7], originals[7].copy(),
        originals[8], originals[9], originals[10], originals[11], originals[12], originals[12].copy(),
        originals[13], originals[13].copy(), transition_13_14, originals[14],
        transition_14_15, originals[15], originals[16],
    ]

    for index, frame in enumerate(sequence, 1):
        frame.save(FRAMES / f"attack_sword_{index:02d}.png", optimize=True)

    previews = [preview_frame(frame) for frame in sequence]
    previews[0].save(
        WORK / "attack_sword_22frame_preview_15fps.gif",
        save_all=True,
        append_images=previews[1:],
        duration=67,
        loop=0,
        disposal=2,
        optimize=False,
    )

    thumb_size = (350, 330)
    sheet = Image.new("RGB", (thumb_size[0] * 6, thumb_size[1] * 4), (28, 30, 35))
    draw = ImageDraw.Draw(sheet)
    for index, preview in enumerate(previews, 1):
        thumb = preview.resize(thumb_size, Image.Resampling.LANCZOS)
        x = ((index - 1) % 6) * thumb_size[0]
        y = ((index - 1) // 6) * thumb_size[1]
        sheet.paste(thumb, (x, y))
        draw.rectangle((x + 8, y + 8, x + 54, y + 38), fill=(12, 14, 18))
        draw.text((x + 17, y + 13), f"{index:02d}", fill=(255, 222, 105))
    sheet.save(WORK / "attack_sword_22frame_contact_sheet.png", optimize=True)

    # Preserve an exact copy of the adopted source sequence alongside the result.
    backup = WORK / "adopted_source_16"
    backup.mkdir(exist_ok=True)
    for path in SOURCE.glob("attack_sword_*.png"):
        destination = backup / path.name
        if not destination.exists():
            shutil.copy2(path, destination)


if __name__ == "__main__":
    main()
