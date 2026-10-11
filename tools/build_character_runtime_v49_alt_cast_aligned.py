"""Correct cast width/height against measured idle head and both shoes."""
import shutil,json
import cv2,numpy as np
from PIL import Image
import build_character_runtime_v48_alt_cast as previous
import bridge_alt_buff_v3 as media

r=previous.runtime
r.OUT=previous.ROOT/'art_candidates/character_runtime_v49_alt_cast_aligned'
r.IMAGES=r.OUT/'images'
r.EXPORT=r.OUT/'export'

if __name__=='__main__':
    if r.OUT.exists(): raise RuntimeError('Inspect existing output before rebuilding')
    aligned=previous.ROOT/'art_candidates/alt_cast_runtime_aligned_v49'
    aligned.mkdir(parents=True,exist_ok=True)
    sx,sy=.956,.974
    matrix=np.float32([[sx,0,381-378*sx],[0,sy,728-731*sy]])
    for i,path in enumerate(sorted((previous.ROOT/'art_candidates/alt_cast_runtime_aligned_v48').glob('cast_*.png')),1):
        source=Image.open(path).convert('RGBA')
        pixels=cv2.warpAffine(media.to_premultiplied(source),matrix,(1024,1024),flags=cv2.INTER_LINEAR,borderMode=cv2.BORDER_CONSTANT,borderValue=(0,0,0,0))
        target=aligned/f'cast_{i:03d}.png'
        media.to_straight(pixels).save(target)
        r.FRAME_OVERRIDES[i]=target
    r.main()
    for path in r.EXPORT.iterdir():
        if path.is_file(): shutil.copy2(path,r.CURRENT/path.name)
    (r.OUT/'alignment.json').write_text(json.dumps({'uniform_for_all_frames':True,'scale_x':sx,'scale_y':sy,'matrix':matrix.tolist(),'target_rear_foot':[381,728]},indent=2))
