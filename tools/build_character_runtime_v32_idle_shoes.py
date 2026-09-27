from pathlib import Path
import build_character_runtime_v25_idle as builder


ROOT = Path(r"D:\StSmod\DoubleSS")
builder.SOURCE = (ROOT / "art_candidates/video_source/idle" /
                  "short_blink_final_v47_shoes_repaired/alpha_frames")
builder.OUT = ROOT / "art_candidates/character_runtime_v32_idle_shoes"
builder.IMAGES = builder.OUT / "images"
builder.EXPORT = builder.OUT / "export"
# Keep a transparent safety gutter around every tightly cropped idle frame so
# Spine's Linear filtering cannot sample a neighbouring atlas region.
builder.BLEED_MARGIN = 4


if __name__ == "__main__":
    builder.main()
