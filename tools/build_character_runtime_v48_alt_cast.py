"""Append the approved 44-frame cast to the inner-character runtime."""
import shutil
from PIL import Image
import build_character_runtime_v36_alt_attack_basic as runtime

ROOT=runtime.ROOT
runtime.SOURCE=ROOT.parent/'art_preview/alternate_fencer_cast/cast_v5_short_windup_44f_60fps/alpha_frames_60fps'
runtime.CURRENT=ROOT/'src/main/resources/img/char/spine_inner'
runtime.OUT=ROOT/'art_candidates/character_runtime_v48_alt_cast'
runtime.IMAGES=runtime.OUT/'images'
runtime.EXPORT=runtime.OUT/'export'
runtime.EXPECTED=44
runtime.FIRST_PAGE=42
runtime.PREFIX='cast_alpha_'
runtime.ANIMATION='cast_attack_1'

if __name__=='__main__':
    aligned=ROOT/'art_candidates/alt_cast_runtime_aligned_v48'
    aligned.mkdir(parents=True,exist_ok=True)
    # Fixed head/foot alignment; stray sparks must not determine body scale.
    scale=341.0/(685-122)
    paste=(round(379-143*scale),round(728-685*scale))
    for i,path in enumerate(sorted(runtime.SOURCE.glob('cast_alpha_*.png')),1):
        source=Image.open(path).convert('RGBA')
        resized=source.resize((round(source.width*scale),round(source.height*scale)),Image.Resampling.LANCZOS)
        canvas=Image.new('RGBA',(1024,1024))
        canvas.paste(resized,paste)
        target=aligned/f'cast_{i:03d}.png'
        canvas.save(target)
        runtime.FRAME_OVERRIDES[i]=target
    runtime.main()
    for path in runtime.EXPORT.iterdir():
        if path.is_file(): shutil.copy2(path,runtime.CURRENT/path.name)
