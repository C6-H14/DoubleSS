"""Append the approved buff frames to the inner atlas, keeping other actions."""
import shutil
import build_character_runtime_v36_alt_attack_basic as runtime

ROOT = runtime.ROOT
runtime.SOURCE = ROOT.parent / 'art_preview/alternate_fencer_buff/buff_v5_sparks_clean_60f_60fps/alpha_frames_60fps'
runtime.CURRENT = ROOT / 'src/main/resources/img/char/spine_inner'
runtime.OUT = ROOT / 'art_candidates/character_runtime_v47_alt_buff'
runtime.IMAGES = runtime.OUT / 'images'
runtime.EXPORT = runtime.OUT / 'export'
runtime.EXPECTED = 60
runtime.FIRST_PAGE = 40
runtime.PREFIX = 'buff_alpha_'
runtime.ANIMATION = 'cast_buff'

if __name__ == '__main__':
    if runtime.OUT.exists():
        raise RuntimeError('Output already exists; inspect before rebuilding')
    runtime.main()
    for path in runtime.EXPORT.iterdir():
        if path.is_file():
            shutil.copy2(path, runtime.CURRENT / path.name)
