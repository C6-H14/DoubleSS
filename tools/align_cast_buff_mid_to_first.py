from pathlib import Path

import numpy as np
from PIL import Image


ROOT = Path(r"D:\StSmod\DoubleSS\art_candidates\video_source\cast_buff_v1\refs")
REFERENCE = Path(r"D:\StSmod\DoubleSS\art_candidates\video_source\cast_v1\refs\cast_first_transparent.png")
SOURCE = ROOT / "cast_buff_mid_transparent_v2.png"
OUT_ALPHA = ROOT / "cast_buff_mid_transparent_v3_aligned.png"
OUT_PINK = ROOT / "cast_buff_mid_pink_v3_aligned.png"
REPORT = ROOT / "cast_buff_mid_v3_alignment.txt"
PINK = (244, 72, 148, 255)


def premultiplied_resize(image: Image.Image, scale: float) -> Image.Image:
    rgba = np.asarray(image.convert("RGBA"), dtype=np.float32) / 255.0
    rgba[..., :3] *= rgba[..., 3:4]
    size = (round(image.width * scale), round(image.height * scale))
    layers = []
    for channel in range(4):
        layer = Image.fromarray(np.clip(rgba[..., channel] * 255, 0, 255).astype(np.uint8), "L")
        layers.append(np.asarray(layer.resize(size, Image.Resampling.LANCZOS), dtype=np.float32) / 255.0)
    resized = np.stack(layers, axis=2)
    alpha = resized[..., 3:4]
    rgb = np.divide(resized[..., :3], alpha, out=np.zeros_like(resized[..., :3]), where=alpha > 1e-5)
    output = np.concatenate((rgb, alpha), axis=2)
    return Image.fromarray(np.clip(output * 255, 0, 255).astype(np.uint8), "RGBA")


def shoe_mask(image: Image.Image) -> np.ndarray:
    rgba = np.asarray(image.convert("RGBA"))
    r, g, b, a = np.moveaxis(rgba, 2, 0)
    yy = np.indices(a.shape)[0]
    return ((a >= 64) & (r > 35) & (r < 155) & (g > 15) & (g < 105)
            & (b < 100) & (yy > image.height * 0.78))


def centroid(mask: np.ndarray) -> tuple[float, float]:
    yy, xx = np.where(mask)
    return float(xx.mean()), float(yy.mean())


def main() -> None:
    reference = Image.open(REFERENCE).convert("RGBA")
    source = Image.open(SOURCE).convert("RGBA")

    # Character scale is based on hair-top to shoe-bottom, deliberately excluding
    # the raised sword tip in the middle pose.
    ref_hair_top, ref_shoe_bottom = 109.0, 705.0
    src_hair_top, src_shoe_bottom = 129.0, 1048.0
    scale = (ref_shoe_bottom - ref_hair_top) / (src_shoe_bottom - src_hair_top)

    ref_mask = shoe_mask(reference)
    src_mask = shoe_mask(source)
    # The requested right foot is the shoe on screen-left. It is separated in the
    # first frame but overlaps the other shoe in the closed-stance middle frame.
    ref_mask[:, 430:] = False
    src_mask[:, 738:] = False
    ref_anchor = centroid(ref_mask)
    src_anchor = centroid(src_mask)

    resized = premultiplied_resize(source, scale)
    paste_x = round(ref_anchor[0] - src_anchor[0] * scale)
    paste_y = round(ref_anchor[1] - src_anchor[1] * scale)

    canvas = Image.new("RGBA", reference.size, (0, 0, 0, 0))
    canvas.alpha_composite(resized, (paste_x, paste_y))
    canvas.save(OUT_ALPHA)

    pink = Image.new("RGBA", reference.size, PINK)
    pink.alpha_composite(canvas)
    pink.convert("RGB").save(OUT_PINK, quality=100)

    REPORT.write_text(
        f"scale={scale:.8f}\n"
        f"reference_right_foot_anchor={ref_anchor}\n"
        f"source_right_foot_anchor={src_anchor}\n"
        f"paste=({paste_x}, {paste_y})\n"
        f"output_size={canvas.size}\n"
        f"output_alpha_bbox={canvas.getchannel('A').point(lambda v: 255 if v >= 8 else 0).getbbox()}\n",
        encoding="utf-8",
    )


if __name__ == "__main__":
    main()
