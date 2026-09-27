from pathlib import Path
import cv2,numpy as np
root=Path(r'D:\StSmod\DoubleSS\art_candidates\video_source\death_splice_analysis')
src=root/'death_splice_frames_review_v12_hold55_bridge';fs=[cv2.imread(str(p)) for p in sorted(src.glob('frame_*.png'))]
ids=list(range(55,91));tw,th,cols=350,260,6;rows=(len(ids)+cols-1)//cols
sheet=np.full((rows*(th+28),cols*tw,3),32,np.uint8)
for n,i in enumerate(ids):
 im=cv2.resize(fs[i],(tw,th),interpolation=cv2.INTER_AREA);y=(n//cols)*(th+28);x=(n%cols)*tw;sheet[y:y+th,x:x+tw]=im
 cv2.putText(sheet,f'f{i}',(x+4,y+th+19),cv2.FONT_HERSHEY_SIMPLEX,.48,(245,245,245),1,cv2.LINE_AA)
cv2.imwrite(str(root/'death_v13_retract_range.png'),sheet)
