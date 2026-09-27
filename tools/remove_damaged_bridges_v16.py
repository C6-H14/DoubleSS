from pathlib import Path
import csv,cv2,numpy as np
from PIL import Image

ROOT=Path(r'D:\StSmod\DoubleSS\art_candidates\video_source\death_splice_analysis')
SRC=ROOT/'death_splice_frames_review_v14_last_hold_transition'
DST=ROOT/'death_splice_frames_review_v15_real_frames_no_bridges';DST.mkdir(exist_ok=True)
frames=[cv2.imread(str(p)) for p in sorted(SRC.glob('frame_*.png'))]
# f63 is the first genuine, complete-hand retract frame. Replace both damaged
# synthetic bridge frames with this real frame; keep timing/frame count intact.
frames[61]=frames[63].copy();frames[62]=frames[63].copy()
for p in DST.glob('frame_*.png'):p.unlink()
for i,f in enumerate(frames):cv2.imwrite(str(DST/f'frame_{i:04d}.png'),f)
with (DST/'frame_manifest.csv').open('w',newline='',encoding='utf-8-sig') as fp:
    wr=csv.writer(fp);wr.writerow(['output_frame','source'])
    for i in range(len(frames)):wr.writerow([i,'real_frame:63' if i in (61,62) else f'previous:{i}'])
h,w=frames[0].shape[:2];thumbs=[Image.fromarray(cv2.cvtColor(cv2.resize(f,(560,416),interpolation=cv2.INTER_AREA),cv2.COLOR_BGR2RGB)) for f in frames]
gif=ROOT/'death_splice_v16_real_frames_no_bridges_preview.gif';thumbs[0].save(gif,save_all=True,append_images=thumbs[1:],duration=42,loop=0,disposal=2,optimize=False)
mp4=ROOT/'death_splice_v16_real_frames_no_bridges_24fps.mp4';vw=cv2.VideoWriter(str(mp4),cv2.VideoWriter_fourcc(*'mp4v'),24,(w,h))
for f in frames:vw.write(f)
vw.release()
ids=list(range(57,68));tw,th,cols=500,372,6;rows=2;sheet=np.full((rows*(th+30),cols*tw,3),32,np.uint8)
for n,i in enumerate(ids):
    im=cv2.resize(frames[i],(tw,th),interpolation=cv2.INTER_AREA);y=(n//cols)*(th+30);x=(n%cols)*tw;sheet[y:y+th,x:x+tw]=im
    cv2.putText(sheet,f'f{i}',(x+5,y+th+21),cv2.FONT_HERSHEY_SIMPLEX,.55,(245,245,245),1,cv2.LINE_AA)
cv2.imwrite(str(ROOT/'death_v16_hand_integrity_review.png'),sheet)
print(gif);print(mp4);print(DST)
