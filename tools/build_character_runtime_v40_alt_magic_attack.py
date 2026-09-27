from pathlib import Path

import build_character_runtime_v36_alt_attack_basic as builder


ROOT = Path(r"D:\StSmod\DoubleSS")
builder.SOURCE = (
    ROOT / "art_candidates" / "alt_fencer_magic_attack_v1" / "alpha_frames_57f_60fps"
)
builder.CURRENT = ROOT / "src" / "main" / "resources" / "img" / "char" / "spine_inner"
builder.OUT = ROOT / "art_candidates" / "character_runtime_v40_alt_magic_attack"
builder.IMAGES = builder.OUT / "images"
builder.EXPORT = builder.OUT / "export"
builder.EXPECTED = 57
builder.FPS = 60.0
builder.FIRST_PAGE = 35
builder.PREFIX = "alt_attack_magic_"
builder.ANIMATION = "attack_magic"
builder.TARGET_SUBJECT_HEIGHT = 372


if __name__ == "__main__":
    builder.main()
