from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw


ROOT = Path(r"D:\StSmod\art_preview\alternate_fencer_cast_s")
SOURCE = ROOT / "cast_s_v5_alpha_clean_45f_60fps" / "alpha_frames_45f_60fps"
OUT = ROOT / "cast_s_v7_gif_encoding_test"
BACKGROUND = (32, 32, 32, 255)


def main() -> None:
    paths = sorted(SOURCE.glob("cast_s_alpha_*.png"))
    if len(paths) != 45:
        raise RuntimeError(f"Expected 45 frames; found {len(paths)}")
    OUT.mkdir(parents=True, exist_ok=True)
    rgb_frames = []
    for path in paths:
        rgba = Image.open(path).convert("RGBA").resize((640, 480), Image.Resampling.LANCZOS)
        rgb_frames.append(Image.alpha_composite(Image.new("RGBA", rgba.size, BACKGROUND), rgba).convert("RGB"))

    # GIF has a single 256-color palette and only 1-bit transparency. For a
    # dark-background preview, pre-composite RGBA instead of quantizing its
    # partially transparent fringe into false white/purple palette colors.
    palette_sample = Image.new("RGB", (640 * 9, 480 * 5))
    for i, frame in enumerate(rgb_frames):
        palette_sample.paste(frame, ((i % 9) * 640, (i // 9) * 480))
    palette = palette_sample.quantize(colors=256, method=Image.Quantize.MEDIANCUT,
                                      dither=Image.Dither.NONE)
    indexed = [frame.quantize(palette=palette, dither=Image.Dither.NONE) for frame in rgb_frames]
    # GIF delays are stored in centiseconds. 20, 10, 20 ms gives exactly
    # 60 fps on average, unlike Pillow's rounding of 17 ms to 10 ms.
    durations = [20 if i % 3 != 1 else 10 for i in range(len(indexed))]
    out_gif = OUT / "cast_s_45f_dark_preview_60fps.gif"
    indexed[0].save(out_gif, save_all=True, append_images=indexed[1:],
                    duration=durations, loop=0, disposal=2, optimize=False)
    # PNG preview is the pixel-accurate comparator for the indexed GIF.
    rgb_frames[28].save(OUT / "frame_029_reference_on_dark.png")
    decoded = Image.open(out_gif)
    decoded.seek(28)
    decoded.convert("RGB").save(OUT / "frame_029_decoded_from_gif.png")
    old_gif = Image.open(ROOT / "cast_s_v5_alpha_clean_45f_60fps" / "cast_s_alpha_clean_45f_60fps.gif")
    old_gif.seek(28)
    old = old_gif.convert("RGBA")
    old_dark = Image.alpha_composite(Image.new("RGBA", old.size, BACKGROUND), old).convert("RGB")
    new = decoded.convert("RGB")
    sheet = Image.new("RGB", (900, 420), (32, 32, 32))
    draw = ImageDraw.Draw(sheet)
    for i, (label, frame) in enumerate((("OLD GIF", old_dark), ("NEW GIF", new))):
        crop = frame.crop((145, 90, 295, 220)).resize((450, 390), Image.Resampling.NEAREST)
        sheet.paste(crop, (i * 450, 30))
        draw.text((i * 450 + 6, 6), label, fill="white")
    sheet.save(OUT / "hair_gif_encoding_comparison_029.png")
    a = np.asarray(rgb_frames[28]).astype(np.int16)
    b = np.asarray(decoded.convert("RGB")).astype(np.int16)
    print({"frames": len(indexed), "duration_ms": sum(durations),
           "mean_abs_rgb_error_frame29": round(float(np.abs(a - b).mean()), 3),
           "gif_bytes": out_gif.stat().st_size})


if __name__ == "__main__":
    main()
