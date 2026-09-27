from pathlib import Path
import csv
import cv2
from PIL import Image

ROOT=Path(r'D:\StSmod\DoubleSS\art_candidates\video_source\death_splice_analysis')
SRC=ROOT/'death_splice_frames_review_v22_trimmed_64_odds65_78'
SOURCE_OLD=ROOT/'death_splice_frames_review_v21_real_bridge_62_65'/'frame_0064.png'
DST=ROOT/'death_splice_frames_review_v24_true_head_transition'
DST.mkdir(exist_ok=True)

base=[cv2.imread(str(p)) for p in sorted(SRC.glob('frame_*.png'))]
transition=cv2.imread(str(SOURCE_OLD))
frames=base[:64]+[transition]+base[64:]
for p in DST.glob('frame_*.png'):p.unlink()
for i,f in enumerate(frames):cv2.imwrite(str(DST/f'frame_{i:04d}.png'),f)
with (DST/'frame_manifest.csv').open('w',newline='',encoding='utf-8-sig') as fp:
    wr=csv.writer(fp);wr.writerow(['output_frame','source'])
    for i in range(len(frames)):
        if i==64:src='old_frame64_true_head_transition'
        elif i<64:src=f'previous:{i}'
        else:src=f'previous:{i-1}'
        wr.writerow([i,src])

thumbs=[Image.fromarray(cv2.cvtColor(cv2.resize(f,(560,416),interpolation=cv2.INTER_AREA),cv2.COLOR_BGR2RGB)) for f in frames]
gif=ROOT/'death_splice_v25_true_head_transition_full_24fps.gif'
dur=[50 if i%3==2 else 40 for i in range(len(thumbs))]
thumbs[0].save(gif,save_all=True,append_images=thumbs[1:],duration=dur,loop=0,disposal=2,optimize=False)
im=Image.open(gif);enc=[];ds=[]
for i in range(im.n_frames):im.seek(i);enc.append(im.convert('RGBA').copy());ds.append(im.info.get('duration',40))
target_ms=round(len(thumbs)/24*1000/10)*10;scale=target_ms/sum(ds)
fixed=[max(10,round(d*scale/10)*10) for d in ds];delta=target_ms-sum(fixed);step=10 if delta>0 else -10
for i in range(abs(delta)//10):fixed[i%len(fixed)]+=step
enc[0].save(gif,save_all=True,append_images=enc[1:],duration=fixed,loop=0,disposal=2,optimize=False)
print('frames',len(frames),'target_ms',target_ms);print(gif);print(DST)
