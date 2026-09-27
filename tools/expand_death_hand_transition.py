from pathlib import Path
import csv
import cv2
from PIL import Image

ROOT=Path(r"D:\StSmod\DoubleSS\art_candidates\video_source\death_splice_analysis")
SRC=ROOT/'death_splice_frames_review_v4_repaired'
DST=ROOT/'death_splice_frames_review_v5_hand_transition'
DST.mkdir(exist_ok=True)


def read(i):
    return cv2.imread(str(SRC/f'frame_{i:04d}.png'))


def blend(a,b,t):
    return cv2.addWeighted(a,1-t,b,t,0)


def main():
    frames=[]
    labels=[]
    for i in range(51):
        frames.append(read(i)); labels.append(f'original,{i}')

    f51,f52,f53=read(51),read(52),read(53)
    frames.append(f51); labels.append('original,51')
    # Hold the pre-movement pose for two extra frames (~83 ms at 24 fps).
    frames.extend([f51.copy(),f51.copy()])
    labels.extend(['hold,51','hold,51'])
    # Two evenly spaced cross-dissolve frames on either side of the redrawn 52.
    for t in (1/3,2/3):
        frames.append(blend(f51,f52,t)); labels.append(f'blend_51_52,{t:.3f}')
    frames.append(f52); labels.append('original,52')
    for t in (1/3,2/3):
        frames.append(blend(f52,f53,t)); labels.append(f'blend_52_53,{t:.3f}')
    frames.append(f53); labels.append('original,53')

    for i in range(54,123):
        frames.append(read(i)); labels.append(f'original,{i}')

    for i,frame in enumerate(frames):
        cv2.imwrite(str(DST/f'frame_{i:04d}.png'),frame)
    with (DST/'frame_manifest.csv').open('w',newline='',encoding='utf-8-sig') as f:
        writer=csv.writer(f); writer.writerow(['output_frame','kind','source'])
        for i,label in enumerate(labels): writer.writerow([i,*label.split(',',1)])

    gif_frames=[]
    for frame in frames:
        small=cv2.resize(frame,(560,416),interpolation=cv2.INTER_AREA)
        gif_frames.append(Image.fromarray(cv2.cvtColor(small,cv2.COLOR_BGR2RGB)))
    gif_path=ROOT/'death_splice_v6_hand_transition_preview.gif'
    gif_frames[0].save(gif_path,save_all=True,append_images=gif_frames[1:],
                       duration=42,loop=0,disposal=2,optimize=False)

    h,w=frames[0].shape[:2]
    mp4_path=ROOT/'death_splice_v6_hand_transition_24fps.mp4'
    vw=cv2.VideoWriter(str(mp4_path),cv2.VideoWriter_fourcc(*'mp4v'),24.0,(w,h))
    for frame in frames: vw.write(frame)
    vw.release()
    ids=list(range(49,62)); tw,th,cols=280,208,7
    rows=(len(ids)+cols-1)//cols
    import numpy as np
    sheet=np.full((rows*(th+28),cols*tw,3),32,np.uint8)
    for n,i in enumerate(ids):
        im=cv2.resize(frames[i],(tw,th),interpolation=cv2.INTER_AREA)
        y=(n//cols)*(th+28); x=(n%cols)*tw
        sheet[y:y+th,x:x+tw]=im
        cv2.putText(sheet,f'f{i} {labels[i]}',(x+4,y+th+18),
                    cv2.FONT_HERSHEY_SIMPLEX,.38,(240,240,240),1,cv2.LINE_AA)
    review=ROOT/'death_hand_transition_review.png'
    cv2.imwrite(str(review),sheet)
    print(DST); print(gif_path); print(mp4_path); print('frames',len(frames))

if __name__=='__main__': main()
