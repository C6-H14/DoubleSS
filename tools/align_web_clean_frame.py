from pathlib import Path

import numpy as np
from PIL import Image


BASE = Path(
    r"D:\StSmod\DoubleSS\art_candidates\video_source\attack_sword_v2_final16_15fps"
    r"\alpha_cleanup_v3\cleaned_master_frames"
)
PAIRS = [
    (
        "attack_sword_01.png",
        "ChatGPT Image 2026年9月17日 20_35_31.png",
        "attack_sword_01_web_clean_aligned_preview.png",
    ),
    (
        "attack_sword_02.png",
        "ChatGPT Image 2026年9月17日 20_49_11 (1).png",
        "attack_sword_02_web_clean_aligned_preview.png",
    ),
    (
        "attack_sword_03.png",
        "ChatGPT Image 2026年9月17日 20_49_11 (2).png",
        "attack_sword_03_web_clean_aligned_preview.png",
    ),
    (
        "attack_sword_04.png",
        "ChatGPT Image 2026年9月17日 20_49_12 (3).png",
        "attack_sword_04_web_clean_aligned_preview.png",
    ),
    (
        "attack_sword_05.png",
        "ChatGPT Image 2026年9月17日 20_53_57 (1).png",
        "attack_sword_05_web_clean_aligned_preview.png",
    ),
    (
        "attack_sword_06.png",
        "ChatGPT Image 2026年9月17日 20_53_57 (2).png",
        "attack_sword_06_web_clean_aligned_preview.png",
    ),
    (
        "attack_sword_07.png",
        "ChatGPT Image 2026年9月17日 20_53_58 (3).png",
        "attack_sword_07_web_clean_aligned_preview.png",
    ),
    (
        "attack_sword_08.png",
        "ChatGPT Image 2026年9月17日 20_53_58 (4).png",
        "attack_sword_08_web_clean_aligned_preview.png",
    ),
    (
        "attack_sword_09.png",
        "ChatGPT Image 2026年9月17日 21_00_36 (1).png",
        "attack_sword_09_web_clean_aligned_preview.png",
    ),
    (
        "attack_sword_10.png",
        "ChatGPT Image 2026年9月17日 21_00_37 (2).png",
        "attack_sword_10_web_clean_aligned_preview.png",
    ),
    (
        "attack_sword_11.png",
        "ChatGPT Image 2026年9月17日 21_00_37 (3).png",
        "attack_sword_11_web_clean_aligned_preview.png",
    ),
    (
        "attack_sword_12.png",
        "ChatGPT Image 2026年9月17日 21_00_38 (4).png",
        "attack_sword_12_web_clean_aligned_preview.png",
    ),
    (
        "attack_sword_13.png",
        "ChatGPT Image 2026年9月17日 21_06_57 (1).png",
        "attack_sword_13_web_clean_aligned_preview.png",
    ),
    (
        "attack_sword_14.png",
        "ChatGPT Image 2026年9月17日 21_06_57 (2).png",
        "attack_sword_14_web_clean_aligned_preview.png",
    ),
    (
        "attack_sword_15.png",
        "ChatGPT Image 2026年9月17日 21_06_59 (3).png",
        "attack_sword_15_web_clean_aligned_preview.png",
    ),
    (
        "attack_sword_16.png",
        "ChatGPT Image 2026年9月17日 21_06_59 (4).png",
        "attack_sword_16_web_clean_aligned_preview.png",
    ),
]


def alpha_bbox(image: Image.Image, threshold: int = 8) -> tuple[int, int, int, int]:
    alpha = np.asarray(image.convert("RGBA"), dtype=np.uint8)[..., 3]
    ys, xs = np.where(alpha > threshold)
    return int(xs.min()), int(ys.min()), int(xs.max() + 1), int(ys.max() + 1)


for reference_name, edited_name, output_name in PAIRS:
    reference_path = BASE / reference_name
    edited_path = BASE / edited_name
    output_path = BASE / output_name

    reference = Image.open(reference_path).convert("RGBA")
    edited = Image.open(edited_path).convert("RGBA")
    rx0, ry0, rx1, ry1 = alpha_bbox(reference)
    ex0, ey0, ex1, ey1 = alpha_bbox(edited)

    # Crop to visible content, then match the original silhouette bounds exactly.
    # This deliberately performs geometry only: no generative redraw or filtering.
    subject = edited.crop((ex0, ey0, ex1, ey1))
    subject = subject.resize((rx1 - rx0, ry1 - ry0), Image.Resampling.LANCZOS)

    canvas = Image.new("RGBA", reference.size, (0, 0, 0, 0))
    canvas.alpha_composite(subject, (rx0, ry0))
    canvas.save(output_path)

    print(f"frame={reference_path.stem}")
    print(f"reference_bbox={(rx0, ry0, rx1, ry1)}")
    print(f"edited_bbox={(ex0, ey0, ex1, ey1)}")
    print(f"saved={output_path}")

print("edge_audit:")
for reference_name, _edited_name, output_name in PAIRS:
    output_path = BASE / output_name
    image = Image.open(output_path).convert("RGBA")
    x0, y0, x1, y1 = alpha_bbox(image)
    width, height = image.size
    touched = "".join(
        side
        for side, condition in (
            ("L", x0 == 0),
            ("T", y0 == 0),
            ("R", x1 == width),
            ("B", y1 == height),
        )
        if condition
    )
    print(f"{Path(reference_name).stem}: bbox={(x0, y0, x1, y1)} edge={touched or 'none'}")
