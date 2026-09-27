from pathlib import Path
import cv2, numpy as np, csv
root=Path(r'D:\StSmod\DoubleSS\art_candidates\video_source\death_splice_analysis')
src=root/'death_splice_frames_review_v6_regen_26_59'
frames=[cv2.imread(str(p)) for p in sorted(src.glob('frame_*.png'))]
scores={int(r['frame']):float(r['motion_p75']) for r in csv.DictReader((root/'death_v7_motion_scores.csv').open(encoding='utf-8-sig'))}
ids=list(range(88,129))
tw,th,cols=420,312,5
rows=(len(ids)+cols-1)//cols
sheet=np.full((rows*(th+30),cols*tw,3),32,np.uint8)
for n,i in enumerate(ids):
    im=cv2.resize(frames[i],(tw,th),interpolation=cv2.INTER_AREA)
    y=(n//cols)*(th+30); x=(n%cols)*tw
    sheet[y:y+th,x:x+tw]=im
    cv2.putText(sheet,f'f{i} v={scores[i]:.2f}',(x+5,y+th+21),cv2.FONT_HERSHEY_SIMPLEX,.55,(245,245,245),1,cv2.LINE_AA)
cv2.imwrite(str(root/'death_v7_timing_detail_88_128.png'),sheet)
