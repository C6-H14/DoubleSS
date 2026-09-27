from pathlib import Path
import csv
import cv2
import numpy as np
from PIL import Image
from analyze_death_video_splice import read_video
from splice_death_2087_into_v11 import normalize_bg,fit,BG

ROOT=Path(r'D:\StSmod\DoubleSS\art_candidates\video_source')
OUT=ROOT/'death_splice_analysis'
SRC=OUT/'death_splice_frames_review_v20_real_hand_bridge'
DST=OUT/'death_splice_frames_review_v21_real_bridge_62_65'
DST.mkdir(exist_ok=True)
frames=[cv2.imread(str(p)) for p in sorted(SRC.glob('frame_*.png'))]
h,w=frames[0].shape[:2]
_,raw=read_video(list(ROOT.glob('*2087*.mp4'))[0])
raw=[normalize_bg(cv2.resize(f,(w,h),interpolation=cv2.INTER_LANCZOS4)) for f in raw]

mapping={63:63,64:62}
for dst,src in mapping.items():
    t=(dst-62)/3
    target=cv2.addWeighted(frames[62],1-t,frames[65],t,0)
    sc,tr=fit(raw[src],target)
    M=np.array([[sc,0,tr[0]],[0,sc,tr[1]]],np.float32)
    frames[dst]=cv2.warpAffine(raw[src],M,(w,h),flags=cv2.INTER_LANCZOS4,
                               borderMode=cv2.BORDER_CONSTANT,borderValue=tuple(map(int,BG)))

for p in DST.glob('frame_*.png'):p.unlink()
for i,f in enumerate(frames):cv2.imwrite(str(DST/f'frame_{i:04d}.png'),f)
with (DST/'frame_manifest.csv').open('w',newline='',encoding='utf-8-sig') as fp:
    wr=csv.writer(fp);wr.writerow(['output_frame','source'])
    for i in range(len(frames)):wr.writerow([i,f'2087_reverse_real:{mapping[i]}' if i in mapping else f'previous:{i}'])

ids=range(62,66);cx0,cy0,cx1,cy1=600,390,830,700;sw,sh=460,620
detail=np.full((sh+34,4*sw,3),32,np.uint8)
for n,i in enumerate(ids):
    c=cv2.resize(frames[i][cy0:cy1,cx0:cx1],(sw,sh),interpolation=cv2.INTER_NEAREST);x=n*sw
    detail[:sh,x:x+sw]=c;cv2.putText(detail,f'f{i}',(x+8,sh+24),cv2.FONT_HERSHEY_SIMPLEX,.65,(245,245,245),1,cv2.LINE_AA)
detail_path=OUT/'death_v22_real_bridge_62_65_detail.png';cv2.imwrite(str(detail_path),detail)

thumbs=[Image.fromarray(cv2.cvtColor(cv2.resize(frames[i],(560,416),interpolation=cv2.INTER_AREA),cv2.COLOR_BGR2RGB)) for i in ids]
short=OUT/'death_v22_real_bridge_62_65_preview.gif';thumbs[0].save(short,save_all=True,append_images=thumbs[1:],duration=140,loop=0,disposal=2,optimize=False)

all_frames=[Image.fromarray(cv2.cvtColor(cv2.resize(f,(560,416),interpolation=cv2.INTER_AREA),cv2.COLOR_BGR2RGB)) for f in frames]
full=OUT/'death_splice_v22_real_hand_bridge_full_24fps.gif'
dur=[50 if i%3==2 else 40 for i in range(len(all_frames))]
all_frames[0].save(full,save_all=True,append_images=all_frames[1:],duration=dur,loop=0,disposal=2,optimize=False)
im=Image.open(full);enc=[];ds=[]
for i in range(im.n_frames):im.seek(i);enc.append(im.convert('RGBA').copy());ds.append(im.info.get('duration',40))
target_ms=round(len(all_frames)/24*1000/10)*10;scale=target_ms/sum(ds)
fixed=[max(10,round(d*scale/10)*10) for d in ds];delta=target_ms-sum(fixed);step=10 if delta>0 else -10
for i in range(abs(delta)//10):fixed[i%len(fixed)]+=step
enc[0].save(full,save_all=True,append_images=enc[1:],duration=fixed,loop=0,disposal=2,optimize=False)
print(detail_path);print(short);print(full);print(DST)
