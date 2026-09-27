from pathlib import Path
import csv
import cv2
import numpy as np
from PIL import Image
from analyze_death_video_splice import read_video
from splice_death_2087_into_v11 import normalize_bg, fit, BG

ROOT=Path(r'D:\StSmod\DoubleSS\art_candidates\video_source')
OUT=ROOT/'death_splice_analysis'
SRC=OUT/'death_splice_frames_review_v15_real_frames_no_bridges'
DST=OUT/'death_splice_frames_review_v20_real_hand_bridge'
DST.mkdir(exist_ok=True)

frames=[cv2.imread(str(p)) for p in sorted(SRC.glob('frame_*.png'))]
h,w=frames[0].shape[:2]
video=list(ROOT.glob('*2087*.mp4'))[0]
_,raw=read_video(video)
raw=[normalize_bg(cv2.resize(f,(w,h),interpolation=cv2.INTER_LANCZOS4)) for f in raw]

# Align each real source frame to a linearly interpolated whole-character target,
# preventing the source clip's camera/body drift from reappearing at frame63.
def aligned(i,t):
    target=cv2.addWeighted(frames[59],1-t,frames[63],t,0)
    sc,tr=fit(raw[i],target)
    M=np.array([[sc,0,tr[0]],[0,sc,tr[1]]],np.float32)
    return cv2.warpAffine(raw[i],M,(w,h),flags=cv2.INTER_LANCZOS4,
                          borderMode=cv2.BORDER_CONSTANT,borderValue=tuple(map(int,BG)))

# Three genuine frames from the same continuous hand lift, read backward from the endpoint.
mapping={60:69,61:66,62:64}
for dst,src in mapping.items(): frames[dst]=aligned(src,(dst-59)/4)

for p in DST.glob('frame_*.png'): p.unlink()
for i,f in enumerate(frames): cv2.imwrite(str(DST/f'frame_{i:04d}.png'),f)
with (DST/'frame_manifest.csv').open('w',newline='',encoding='utf-8-sig') as fp:
    wr=csv.writer(fp);wr.writerow(['output_frame','source'])
    for i in range(len(frames)):
        wr.writerow([i,f'2087_reverse_real:{mapping[i]}' if i in mapping else f'previous:{i}'])

ids=range(59,64);tw,th=560,416
sheet=np.full((th+34,5*tw,3),32,np.uint8)
for n,i in enumerate(ids):
    im=cv2.resize(frames[i],(tw,th),interpolation=cv2.INTER_AREA);x=n*tw
    sheet[:th,x:x+tw]=im
    cv2.putText(sheet,f'f{i}',(x+8,th+24),cv2.FONT_HERSHEY_SIMPLEX,.65,(245,245,245),1,cv2.LINE_AA)
full=OUT/'death_v21_real_hand_bridge_review.png';cv2.imwrite(str(full),sheet)

cx0,cy0,cx1,cy1=620,430,810,700;sw,sh=380,540
detail=np.full((sh+34,5*sw,3),32,np.uint8)
for n,i in enumerate(ids):
    c=cv2.resize(frames[i][cy0:cy1,cx0:cx1],(sw,sh),interpolation=cv2.INTER_NEAREST);x=n*sw
    detail[:sh,x:x+sw]=c
    cv2.putText(detail,f'f{i}',(x+8,sh+24),cv2.FONT_HERSHEY_SIMPLEX,.65,(245,245,245),1,cv2.LINE_AA)
detail_path=OUT/'death_v21_real_hand_bridge_detail.png';cv2.imwrite(str(detail_path),detail)

thumbs=[Image.fromarray(cv2.cvtColor(cv2.resize(frames[i],(560,416),interpolation=cv2.INTER_AREA),cv2.COLOR_BGR2RGB)) for i in ids]
gif=OUT/'death_v21_real_hand_bridge_59_63.gif'
thumbs[0].save(gif,save_all=True,append_images=thumbs[1:],duration=120,loop=0,disposal=2,optimize=False)

# Complete 139-frame preview at the sequence's 24 fps timing.
all_thumbs=[Image.fromarray(cv2.cvtColor(cv2.resize(f,(560,416),interpolation=cv2.INTER_AREA),cv2.COLOR_BGR2RGB)) for f in frames]
full_gif=OUT/'death_splice_v21_real_hand_bridge_full_24fps.gif'
# GIF timing is centisecond-based. 40/40/50 ms averages 41.67 ms = 24 fps.
durations=[(50 if i%3==2 else 40) for i in range(len(all_thumbs))]
all_thumbs[0].save(full_gif,save_all=True,append_images=all_thumbs[1:],duration=durations,loop=0,disposal=2,optimize=False)

# Pillow may merge identical consecutive frames. Normalize durations after that
# merge so the complete animation remains exactly 139/24 seconds long.
merged=Image.open(full_gif)
encoded=[]; encoded_durations=[]
for i in range(merged.n_frames):
    merged.seek(i); encoded.append(merged.convert('RGBA').copy()); encoded_durations.append(merged.info.get('duration',40))
target_ms=round(len(all_thumbs)/24*1000/10)*10
scale=target_ms/sum(encoded_durations)
fixed=[max(10,round(d*scale/10)*10) for d in encoded_durations]
delta=target_ms-sum(fixed); step=10 if delta>0 else -10
for i in range(abs(delta)//10): fixed[i%len(fixed)]+=step
encoded[0].save(full_gif,save_all=True,append_images=encoded[1:],duration=fixed,loop=0,disposal=2,optimize=False)
print(full);print(detail_path);print(gif);print(full_gif);print(DST)
