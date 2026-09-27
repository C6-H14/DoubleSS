from pathlib import Path
import csv, hashlib
import cv2
import numpy as np
from PIL import Image

ROOT=Path(r'D:\StSmod\DoubleSS\art_candidates\video_source\death_splice_analysis')
SRC=ROOT/'death_splice_frames_review_v6_regen_26_59'
MAP=ROOT/'death_splice_frames_review_v8_retimed_phase_adjusted'/'frame_manifest.csv'
DST=ROOT/'death_splice_frames_review_v10_support_20f'
DST.mkdir(exist_ok=True)


def main():
    src=[cv2.imread(str(p),cv2.IMREAD_UNCHANGED) for p in sorted(SRC.glob('frame_*.png'))]
    with MAP.open(encoding='utf-8-sig') as fp:
        positions=[float(r['source_position']) for r in csv.DictReader(fp)]
    base=[int(np.clip(round(x),0,len(src)-1)) for x in positions]

    # Requested ranges refer to the previous 115-frame preview.
    # f44..47: restore/extend support-to-hand-withdrawal to 12 real source frames.
    support_ids=np.rint(np.linspace(55,66,20)).astype(int).tolist()
    # f95..114: keep endpoints but remove one third (20 -> 13 real frames).
    fall_ids=np.rint(np.linspace(111,128,13)).astype(int).tolist()
    ids=base[:44]+support_ids+base[48:95]+fall_ids

    for p in DST.glob('frame_*.png'): p.unlink()
    for i,j in enumerate(ids):
        cv2.imwrite(str(DST/f'frame_{i:04d}.png'),src[j])
    with (DST/'frame_manifest.csv').open('w',newline='',encoding='utf-8-sig') as fp:
        wr=csv.writer(fp); wr.writerow(['output_frame','source_frame','pixel_exact_source_copy'])
        wr.writerows((i,j,'yes') for i,j in enumerate(ids))

    # Verify every output decodes pixel-identically to its listed source frame.
    for i,j in enumerate(ids):
        out=cv2.imread(str(DST/f'frame_{i:04d}.png'),cv2.IMREAD_UNCHANGED)
        if not np.array_equal(out,src[j]): raise RuntimeError(f'pixel mismatch output {i} source {j}')

    h,w=src[0].shape[:2]
    thumbs=[Image.fromarray(cv2.cvtColor(cv2.resize(src[j],(560,416),interpolation=cv2.INTER_AREA),cv2.COLOR_BGR2RGB)) for j in ids]
    gif=ROOT/'death_splice_v11_support_20f_preview.gif'
    thumbs[0].save(gif,save_all=True,append_images=thumbs[1:],duration=42,loop=0,disposal=2,optimize=False)
    mp4=ROOT/'death_splice_v11_support_20f_24fps.mp4'
    vw=cv2.VideoWriter(str(mp4),cv2.VideoWriter_fourcc(*'mp4v'),24,(w,h))
    for j in ids: vw.write(src[j][:,:,:3])
    vw.release()

    review_ids=list(range(40,60))+list(range(99,len(ids)))
    tw,th,cols=280,208,10
    rows=(len(review_ids)+cols-1)//cols
    sheet=np.full((rows*(th+28),cols*tw,3),32,np.uint8)
    for n,i in enumerate(review_ids):
        im=cv2.resize(src[ids[i]][:,:,:3],(tw,th),interpolation=cv2.INTER_AREA)
        y=(n//cols)*(th+28); x=(n%cols)*tw; sheet[y:y+th,x:x+tw]=im
        cv2.putText(sheet,f'f{i} src={ids[i]}',(x+4,y+th+19),cv2.FONT_HERSHEY_SIMPLEX,.43,(245,245,245),1,cv2.LINE_AA)
    cv2.imwrite(str(ROOT/'death_v11_support_20f_ranges.png'),sheet)
    print('frames',len(ids),'seconds',len(ids)/24)
    print('support',support_ids)
    print('fall',fall_ids)
    print(gif); print(mp4); print(DST)

if __name__=='__main__': main()
