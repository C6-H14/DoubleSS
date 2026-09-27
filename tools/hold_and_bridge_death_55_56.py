from pathlib import Path
import csv,cv2,numpy as np
from PIL import Image
from retime_death_v7 import tween

ROOT=Path(r'D:\StSmod\DoubleSS\art_candidates\video_source\death_splice_analysis')
SRC=ROOT/'death_splice_frames_review_v11_2087_start'
DST=ROOT/'death_splice_frames_review_v12_hold55_bridge'
DST.mkdir(exist_ok=True)

frames=[cv2.imread(str(p)) for p in sorted(SRC.glob('frame_*.png'))]
bridge=tween(frames[55],frames[56],0.5)
# Original f55 + three additional holds + one bridge, then original f56 onward.
out=frames[:56]+[frames[55].copy() for _ in range(3)]+[bridge]+frames[56:]
for p in DST.glob('frame_*.png'):p.unlink()
for i,f in enumerate(out):cv2.imwrite(str(DST/f'frame_{i:04d}.png'),f)
with (DST/'frame_manifest.csv').open('w',newline='',encoding='utf-8-sig') as fp:
    wr=csv.writer(fp);wr.writerow(['output_frame','source'])
    for i in range(56):wr.writerow([i,f'previous:{i}'])
    for i in range(56,59):wr.writerow([i,'hold:55'])
    wr.writerow([59,'optical_bridge:55-56@0.5'])
    for i in range(60,len(out)):wr.writerow([i,f'previous:{i-4}'])
h,w=out[0].shape[:2]
thumbs=[Image.fromarray(cv2.cvtColor(cv2.resize(f,(560,416),interpolation=cv2.INTER_AREA),cv2.COLOR_BGR2RGB)) for f in out]
gif=ROOT/'death_splice_v13_hold55_bridge_preview.gif';thumbs[0].save(gif,save_all=True,append_images=thumbs[1:],duration=42,loop=0,disposal=2,optimize=False)
mp4=ROOT/'death_splice_v13_hold55_bridge_24fps.mp4';vw=cv2.VideoWriter(str(mp4),cv2.VideoWriter_fourcc(*'mp4v'),24,(w,h))
for f in out:vw.write(f)
vw.release()
ids=list(range(51,65));tw,th=420,312;cols=7;sheet=np.full((2*(th+30),cols*tw,3),32,np.uint8)
for n,i in enumerate(ids):
    im=cv2.resize(out[i],(tw,th),interpolation=cv2.INTER_AREA);y=(n//cols)*(th+30);x=(n%cols)*tw;sheet[y:y+th,x:x+tw]=im
    cv2.putText(sheet,f'f{i}',(x+5,y+th+21),cv2.FONT_HERSHEY_SIMPLEX,.52,(245,245,245),1,cv2.LINE_AA)
cv2.imwrite(str(ROOT/'death_v13_hand_bridge_review.png'),sheet)
print('frames',len(out),'seconds',len(out)/24);print(gif);print(mp4);print(DST)
