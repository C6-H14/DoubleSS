from pathlib import Path
import csv,cv2,numpy as np
from PIL import Image

ROOT=Path(r'D:\StSmod\DoubleSS\art_candidates\video_source\death_splice_analysis')
SRC=ROOT/'death_splice_frames_review_v12_hold55_bridge'
DST=ROOT/'death_splice_frames_review_v13_hold7_retract50pct'
DST.mkdir(exist_ok=True)
frames=[cv2.imread(str(p)) for p in sorted(SRC.glob('frame_*.png'))]

# Existing f55..58 are the four-frame hold. Add three more copies, keep f59 bridge.
out=frames[:59]+[frames[55].copy() for _ in range(3)]+[frames[59]]
manifest=[(i,f'previous:{i}') for i in range(59)]
manifest += [(59+i,'extra_hold:55') for i in range(3)] + [(62,'previous_bridge:59')]
# Existing f60..67 is hand leaving ground and returning to the torso.
# Duplicate every real frame once: speed becomes 50%, without synthetic pixels.
for src_i in range(60,68):
    out.extend([frames[src_i].copy(),frames[src_i].copy()])
    manifest.extend([(len(out)-2,f'previous:{src_i}'),(len(out)-1,f'repeat:{src_i}')])
for src_i in range(68,len(frames)):
    out.append(frames[src_i]);manifest.append((len(out)-1,f'previous:{src_i}'))

for p in DST.glob('frame_*.png'):p.unlink()
for i,f in enumerate(out):cv2.imwrite(str(DST/f'frame_{i:04d}.png'),f)
with (DST/'frame_manifest.csv').open('w',newline='',encoding='utf-8-sig') as fp:
    wr=csv.writer(fp);wr.writerow(['output_frame','source']);wr.writerows(manifest)
h,w=out[0].shape[:2];thumbs=[Image.fromarray(cv2.cvtColor(cv2.resize(f,(560,416),interpolation=cv2.INTER_AREA),cv2.COLOR_BGR2RGB)) for f in out]
gif=ROOT/'death_splice_v14_hold7_retract50pct_preview.gif';thumbs[0].save(gif,save_all=True,append_images=thumbs[1:],duration=42,loop=0,disposal=2,optimize=False)
mp4=ROOT/'death_splice_v14_hold7_retract50pct_24fps.mp4';vw=cv2.VideoWriter(str(mp4),cv2.VideoWriter_fourcc(*'mp4v'),24,(w,h))
for f in out:vw.write(f)
vw.release()
ids=list(range(53,86));tw,th,cols=350,260,7;rows=(len(ids)+cols-1)//cols;sheet=np.full((rows*(th+28),cols*tw,3),32,np.uint8)
for n,i in enumerate(ids):
    im=cv2.resize(out[i],(tw,th),interpolation=cv2.INTER_AREA);y=(n//cols)*(th+28);x=(n%cols)*tw;sheet[y:y+th,x:x+tw]=im
    cv2.putText(sheet,f'f{i}',(x+4,y+th+19),cv2.FONT_HERSHEY_SIMPLEX,.47,(245,245,245),1,cv2.LINE_AA)
cv2.imwrite(str(ROOT/'death_v14_hold_retract_review.png'),sheet)
print('frames',len(out),'seconds',len(out)/24);print(gif);print(mp4);print(DST)
