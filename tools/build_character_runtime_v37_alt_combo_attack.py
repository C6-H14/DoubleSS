from pathlib import Path

import build_character_runtime_v36_alt_attack_basic as builder


ROOT = Path(r"D:\StSmod\DoubleSS")
builder.SOURCE = ROOT / "art_candidates" / "alt_fencer_combo_attack_v1" / "alpha_frames_37f_60fps"
builder.OUT = ROOT / "art_candidates" / "character_runtime_v37_alt_combo_attack"
builder.IMAGES = builder.OUT / "images"
builder.EXPORT = builder.OUT / "export"
builder.EXPECTED = 37
builder.FPS = 60.0
builder.FIRST_PAGE = 29
builder.PREFIX = "alt_combo_attack_"
builder.ANIMATION = "attack_combo"


if __name__ == "__main__":
    builder.main()
