from pathlib import Path
import json

import numpy as np
from PIL import Image, ImageDraw


ROOT = Path(r"D:\StSmod\DoubleSS")
OUT = ROOT / "art_candidates" / "video_source" / "idle" / "diagnostics_pma_v2"
WRONG = OUT / "idle_001_wrong_straight_bleed.png"
FIXED = ROOT / "art_candidates" / "character_runtime_v25_idle" / "images" / "idle_001.png"


def pma_render(path, bg=(56, 23, 11)):
    rgba = np.asarray(Image.open(path).convert("RGBA"), dtype=np.float32)
    alpha = rgba[:, :, 3:4] / 255.0
    rgb = rgba[:, :, :3]
    out = np.clip(rgb + np.asarray(bg, dtype=np.float32) * (1.0 - alpha), 0, 255)
    return Image.fromarray(out.astype(np.uint8), "RGB")


def stats(path):
    rgba = np.asarray(Image.open(path).convert("RGBA"), dtype=np.uint8)
    alpha = rgba[:, :, 3]
    rgb = rgba[:, :, :3]
    # In a valid 8-bit PMA texture every RGB channel is <= alpha (rounding +1).
    violations = np.any(rgb.astype(np.int16) > alpha[:, :, None].astype(np.int16) + 1, axis=2)
    transparent_rgb = (alpha == 0) & np.any(rgb != 0, axis=2)
    low = (alpha > 0) & (alpha < 32)
    return {
        "pma_contract_violations": int(violations.sum()),
        "transparent_nonzero_rgb": int(transparent_rgb.sum()),
        "low_alpha_pixels": int(low.sum()),
        "low_alpha_max_rgb": int(rgb[low].max()) if low.any() else 0,
    }


def main():
    before = pma_render(WRONG).resize((512, 512), Image.Resampling.LANCZOS)
    after = pma_render(FIXED).resize((512, 512), Image.Resampling.LANCZOS)
    sheet = Image.new("RGB", (1024, 552), (25, 25, 27))
    sheet.paste(before, (0, 40))
    sheet.paste(after, (512, 40))
    draw = ImageDraw.Draw(sheet)
    draw.text((12, 12), "current wrong: straight/bleed texture rendered by PMA", fill="white")
    draw.text((524, 12), "candidate: RGB premultiplied by alpha", fill="white")
    sheet.save(OUT / "pma_runtime_formula_comparison.png", optimize=True)
    report = {"renderer_evidence": "AbstractCreature calls SkeletonMeshRenderer.setPremultipliedAlpha(true)",
              "current_wrong": stats(WRONG), "candidate_fixed": stats(FIXED)}
    (OUT / "pma_test_report.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    main()
