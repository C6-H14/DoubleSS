from pathlib import Path

import build_character_runtime_v16_cast_prayer as builder


ROOT = Path(r"D:\StSmod\DoubleSS")
builder.BASE = ROOT / "art_candidates" / "character_runtime_v18_cast_prayer_centered"
builder.BASE_IMPORT_NAME = "DoubleSS_character_v18_import_3.8.json"
builder.SOURCE = ROOT / "art_candidates" / "video_source" / "cast_prayer" / "preview_v7_stable_clean" / "alpha_frames"
builder.OUTPUT = ROOT / "art_candidates" / "character_runtime_v19_cast_prayer_stable_clean"
builder.OUTPUT_IMPORT_NAME = "DoubleSS_character_v19_import_3.8.json"
builder.IMAGES = builder.OUTPUT / "images"
builder.EXPORT = builder.OUTPUT / "export"
builder.EXPECTED_FRAMES = 41

# Restore the originally accepted character size. Position is calibrated in
# local attachment coordinates, accounting for the 1024 vs 700 canvas centers.
builder.SCALE = 0.5
builder.PASTE = (285, 310)
builder.FRAME_OFFSETS = {}


if __name__ == "__main__":
    builder.main()
