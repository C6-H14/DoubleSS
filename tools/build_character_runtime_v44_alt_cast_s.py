from pathlib import Path

import build_character_runtime_v36_alt_attack_basic as builder


ROOT = Path(r"D:\StSmod\DoubleSS")
builder.SOURCE = Path(r"D:\StSmod\art_preview\alternate_fencer_cast_s\cast_s_v5_alpha_clean_45f_60fps\alpha_frames_45f_60fps")
builder.CURRENT = ROOT / "src" / "main" / "resources" / "img" / "char" / "spine_inner"
builder.OUT = ROOT / "art_candidates" / "character_runtime_v44_alt_cast_s"
builder.IMAGES = builder.OUT / "images"
builder.EXPORT = builder.OUT / "export"
builder.EXPECTED = 45
builder.FPS = 60.0
builder.FIRST_PAGE = 37
builder.PREFIX = "cast_s_alpha_"
builder.ANIMATION = "cast_s"
builder.TARGET_SUBJECT_HEIGHT = 500


if __name__ == "__main__":
    builder.main()
