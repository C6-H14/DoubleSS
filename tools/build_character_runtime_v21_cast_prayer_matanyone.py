from pathlib import Path

import build_character_runtime_v16_cast_prayer as builder


ROOT = Path(r"D:\StSmod\DoubleSS")
builder.BASE = ROOT / "art_candidates" / "character_runtime_v20_cast_prayer_1200ms"
builder.BASE_IMPORT_NAME = "DoubleSS_character_v20_import_3.8.json"
builder.SOURCE = ROOT / "art_candidates" / "video_source" / "cast_prayer" / "preview_v9_matanyone_grounded" / "alpha_frames"
builder.OUTPUT = ROOT / "art_candidates" / "character_runtime_v21_cast_prayer_matanyone"
builder.OUTPUT_IMPORT_NAME = "DoubleSS_character_v21_import_3.8.json"
builder.IMAGES = builder.OUTPUT / "images"
builder.EXPORT = builder.OUTPUT / "export"
builder.EXPECTED_FRAMES = 41
builder.SCALE = 0.5
builder.PASTE = (285, 310)
builder.FRAME_OFFSETS = {}
builder.TARGET_DURATION = 1.2


if __name__ == "__main__":
    builder.main()
