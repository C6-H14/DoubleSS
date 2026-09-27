from pathlib import Path

import matte_alt_combo_attack_v2 as matte_builder


ROOT = Path(r"D:\StSmod")
matte_builder.SOURCE = ROOT / "art_preview" / "alternate_fencer_combo_attack" / "combo_attack_v5_fast_first_thrust" / "frames_103f_60fps"
matte_builder.OUT = ROOT / "DoubleSS" / "art_candidates" / "alt_fencer_combo_attack_v3_fast_first"
matte_builder.FRAMES = matte_builder.OUT / "alpha_frames_103f_60fps"
matte_builder.GIF = matte_builder.OUT / "combo_attack_alpha_103f_60fps.gif"
matte_builder.EXPECTED = 103


if __name__ == "__main__":
    matte_builder.main()
