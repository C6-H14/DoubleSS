from pathlib import Path
import shutil,cv2,numpy as np
from PIL import Image,ImageDraw

ROOT=Path(r'D:\StSmod\DoubleSS\art_candidates\video_source\death_splice_analysis')
SRC=ROOT/'death_splice_v11_alpha_refined_v2'/'alpha_frames'
OUT=ROOT/'death_splice_v11_alpha_refined_v6_restored'
FRAMES=OUT/'alpha_frames'
if OUT.exists():shutil.rmtree(OUT)
FRAMES.mkdir(parents=True)
frames=[]
for i,p in enumerate(sorted(SRC.glob('*.png'))):
    a=np.asarray(Image.open(p).convert('RGBA'),np.uint8).copy();rgb=a[...,:3];alpha=a[...,3]
    removed=0
    if i==22:
        h,s,v=cv2.split(cv2.cvtColor(rgb,cv2.COLOR_RGB2HSV));local=np.zeros(alpha.shape,bool);local[295:345,610:670]=1
        bad=local&(h>=125)&(h<=172)&(s>=45)&(alpha>0);removed=int(bad.sum());alpha[bad]=0;rgb[bad]=0
    im=Image.fromarray(np.dstack((rgb,alpha)),'RGBA');im.save(FRAMES/f'death_{i:04d}.png',optimize=True);frames.append(im)
black=Image.new('RGBA',frames[0].size,(0,0,0,255))
views=[Image.alpha_composite(black.copy(),f).resize((560,416),Image.Resampling.LANCZOS).convert('RGB') for f in frames]
views[0].save(OUT/'death_black_preview.gif',save_all=True,append_images=views[1:],duration=42,loop=0,disposal=2,optimize=False)
(OUT/'notes.txt').write_text('Restored from refined_v2. Only frame 22 targeted purple residue removal retained. No S-S redraw.\n',encoding='utf-8')
print(OUT,'frames',len(frames),'frame22 removed',removed)
