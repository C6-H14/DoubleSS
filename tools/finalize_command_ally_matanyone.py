from pathlib import Path
import shutil
import numpy as np
from PIL import Image

ROOT = Path(r"D:\StSmod\DoubleSS")
BASE = ROOT / "art_candidates" / "video_source" / "command_ally"
MAT = BASE / "matanyone_v1" / "output" / "command_ally_16_command1024"
OUT = BASE / "final_v1_matanyone"
FRAMES = OUT / "alpha_frames"

def main():
    if OUT.exists(): shutil.rmtree(OUT)
    FRAMES.mkdir(parents=True)
    built=[]
    for i,(f,p) in enumerate(zip(sorted((MAT/'fgr').glob('*.png')), sorted((MAT/'pha').glob('*.png'))),1):
        rgb=np.array(Image.open(f).convert('RGB')); alpha=np.array(Image.open(p).convert('L'))
        alpha[alpha<3]=0; alpha[alpha>252]=255; rgb[alpha==0]=0
        im=Image.fromarray(np.dstack((rgb,alpha)),'RGBA'); im.save(FRAMES/f'command_ally_{i:03d}.png',optimize=True); built.append(im)
    if len(built)!=16: raise RuntimeError(f'Expected 16 frames, got {len(built)}')
    durations=[40]*16; durations[-1]=100
    for name,color in [('black',(0,0,0,255)),('white',(255,255,255,255))]:
        bg=Image.new('RGBA',built[0].size,color)
        views=[Image.alpha_composite(bg.copy(),x).resize((556,417),Image.Resampling.LANCZOS).convert('RGB') for x in built]
        views[0].save(OUT/f'command_ally_{name}_700ms.gif',save_all=True,append_images=views[1:],duration=durations,loop=0,disposal=2)
    print(f'frames={len(built)} output={OUT}')

if __name__=='__main__': main()
