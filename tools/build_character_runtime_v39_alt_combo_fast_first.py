from pathlib import Path

import build_character_runtime_v36_alt_attack_basic as builder


ROOT = Path(r"D:\StSmod\DoubleSS")
builder.SOURCE = ROOT / "art_candidates" / "alt_fencer_combo_attack_v3_fast_first" / "alpha_frames_103f_60fps"
builder.OUT = ROOT / "art_candidates" / "character_runtime_v39_alt_combo_fast_first"
builder.IMAGES = builder.OUT / "images"
builder.EXPORT = builder.OUT / "export"
builder.EXPECTED = 103
builder.FPS = 60.0
builder.FIRST_PAGE = 29
builder.PREFIX = "alt_combo_attack_"
builder.ANIMATION = "attack_combo"

# The generic builder normalizes against the tallest lunging frame.  Here the
# endpoints must instead match the measured runtime idle endpoint (337 px).
# 372/613 gives ~0.607 scale and turns the combo endpoint into the same height.
builder.TARGET_SUBJECT_HEIGHT = 372


if __name__ == "__main__":
    builder.main()
