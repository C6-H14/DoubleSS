from pathlib import Path
import build_character_runtime_v25_idle as builder


ROOT = Path(r"D:\StSmod\DoubleSS")
builder.SOURCE = (
    ROOT
    / "art_candidates"
    / "video_source"
    / "idle"
    / "short_blink_final_v41_gap_repaired_pixel_evidence"
    / "alpha_frames"
)
builder.OUT = ROOT / "art_candidates" / "character_runtime_v26_idle_v41"
builder.IMAGES = builder.OUT / "images"
builder.EXPORT = builder.OUT / "export"


if __name__ == "__main__":
    builder.main()
