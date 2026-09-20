from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw


SOURCE = Path(
    r"D:\StSmod\DoubleSS\art_candidates\video_source\attack_sword_v2_final16_15fps"
    r"\alpha_cleanup_v3\cleaned_master_frames"
)
OUTPUT = SOURCE.parent / "final_web_clean_frames"
PREVIEW = SOURCE.parent / "gif_preview_frames"
GIF_OUTPUT = SOURCE.parent / "attack_sword_web_clean_preview_15fps.gif"
CANVAS_SIZE = (1400, 1320)
OFFSET = (144, 310)

# Only these frames have a blade cross-section (rather than a finished point)
# cut by the old canvas. Coordinates are in the expanded canvas.
TIP_PATCHES = {
    4: {
        "base_a": (399, 160),
        "base_b": (421, 160),
        "tip": (351, 42),
    },
    5: {
        "base_a": (778, 160),
        "base_b": (801, 160),
        "tip": (884, 52),
    },
    12: {
        "base_a": (1256, 320),
        "base_b": (1256, 336),
        "tip": (1377, 287),
    },
    13: {
        "base_a": (1256, 312),
        "base_b": (1256, 331),
        "tip": (1378, 272),
    },
}

# Locally redrawn replacements produced after visual review of the geometric
# extensions. When present, these are used verbatim instead of constructing a
# synthetic point.
REDRAWN = {
    4: SOURCE.parent / "local_redraw" / "attack_sword_04.png",
    5: SOURCE.parent / "local_redraw" / "attack_sword_05.png",
    12: SOURCE.parent / "local_redraw" / "attack_sword_12.png",
    13: SOURCE.parent / "local_redraw" / "attack_sword_13.png",
}
REDRAW_OFFSETS = {
    # Align every redrawn frame's shoe baseline to y=1009 while preserving the
    # completed sword tip inside the enlarged common canvas.
    4: (14, 95),
    5: (17, 10),
    12: (9, 143),
    13: (10, 144),
}


def draw_blade_extension(image: Image.Image, patch: dict[str, tuple[int, int]]) -> None:
    """Continue a clipped straight blade into a tapered anime-style point."""
    a = patch["base_a"]
    b = patch["base_b"]
    tip = patch["tip"]
    draw = ImageDraw.Draw(image, "RGBA")

    # Dark outside contour, silver body, then two subtle facets. The overlap at
    # the former canvas edge hides the old hard cut without touching the actor.
    draw.polygon([a, b, tip], fill=(42, 43, 51, 255))

    center = ((a[0] + b[0]) // 2, (a[1] + b[1]) // 2)
    inset_a = (
        round(a[0] * 0.82 + center[0] * 0.18),
        round(a[1] * 0.82 + center[1] * 0.18),
    )
    inset_b = (
        round(b[0] * 0.82 + center[0] * 0.18),
        round(b[1] * 0.82 + center[1] * 0.18),
    )
    inner_tip = (
        round(tip[0] * 0.985 + center[0] * 0.015),
        round(tip[1] * 0.985 + center[1] * 0.015),
    )
    draw.polygon([inset_a, inset_b, inner_tip], fill=(213, 216, 226, 255))
    draw.polygon([inset_a, center, inner_tip], fill=(244, 246, 250, 235))
    draw.polygon([center, inset_b, inner_tip], fill=(151, 158, 175, 220))


def alpha_bbox(image: Image.Image, threshold: int = 8) -> tuple[int, int, int, int]:
    alpha = np.asarray(image, dtype=np.uint8)[..., 3]
    ys, xs = np.where(alpha > threshold)
    return int(xs.min()), int(ys.min()), int(xs.max() + 1), int(ys.max() + 1)


OUTPUT.mkdir(parents=True, exist_ok=True)
PREVIEW.mkdir(parents=True, exist_ok=True)

for frame_number in range(1, 17):
    source = SOURCE / f"attack_sword_{frame_number:02d}_web_clean_aligned_preview.png"
    frame = Image.open(source).convert("RGBA")
    canvas = Image.new("RGBA", CANVAS_SIZE, (0, 0, 0, 0))
    canvas.alpha_composite(frame, OFFSET)

    redrawn = REDRAWN.get(frame_number)
    if redrawn is not None and redrawn.exists():
        redraw_image = Image.open(redrawn).convert("RGBA")
        redraw_canvas = Image.new("RGBA", CANVAS_SIZE, (0, 0, 0, 0))
        redraw_canvas.alpha_composite(redraw_image, REDRAW_OFFSETS[frame_number])
        canvas = redraw_canvas
    elif frame_number in TIP_PATCHES:
        draw_blade_extension(canvas, TIP_PATCHES[frame_number])

    target = OUTPUT / f"attack_sword_{frame_number:02d}.png"
    canvas.save(target)

    preview = Image.new("RGB", CANVAS_SIZE, (31, 34, 40))
    preview.paste(canvas, mask=canvas.getchannel("A"))
    preview = preview.resize((700, 660), Image.Resampling.LANCZOS)
    preview.save(PREVIEW / f"attack_sword_{frame_number:02d}.png")
    print(f"{target.name}: bbox={alpha_bbox(canvas)}")

gif_frames = [
    Image.open(PREVIEW / f"attack_sword_{frame_number:02d}.png").convert(
        "P", palette=Image.Palette.ADAPTIVE, colors=256
    )
    for frame_number in range(1, 17)
]
gif_frames[0].save(
    GIF_OUTPUT,
    save_all=True,
    append_images=gif_frames[1:],
    duration=67,
    loop=0,
    disposal=2,
    optimize=False,
)

print(f"saved_dir={OUTPUT}")
print(f"preview_dir={PREVIEW}")
print(f"gif={GIF_OUTPUT}")
