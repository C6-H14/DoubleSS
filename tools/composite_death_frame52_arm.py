from pathlib import Path
import shutil
import cv2
import numpy as np
from PIL import Image

ROOT=Path(r"D:\StSmod\DoubleSS\art_candidates\video_source\death_splice_analysis")
FRAMES=ROOT/'death_splice_frames_review_v2'
target=cv2.imread(str(FRAMES/'frame_0051.png'))
generated=cv2.imread(str(ROOT/'frame_0052_imagegen_arm_source.png'))
h,w=target.shape[:2]
generated=cv2.resize(generated,(w,h),interpolation=cv2.INTER_LANCZOS4)

# Align the generated illustration back to the locked target frame using the
# whole character. The edit model preserved composition closely enough that a
# small affine correction removes residual scale/translation drift.
gray_t=cv2.cvtColor(target,cv2.COLOR_BGR2GRAY).astype(np.float32)/255
gray_g=cv2.cvtColor(generated,cv2.COLOR_BGR2GRAY).astype(np.float32)/255
warp=np.eye(2,3,dtype=np.float32)
criteria=(cv2.TERM_CRITERIA_EPS|cv2.TERM_CRITERIA_COUNT,120,1e-6)
try:
    cv2.findTransformECC(gray_t,gray_g,warp,cv2.MOTION_AFFINE,criteria,None,5)
except cv2.error:
    pass
aligned=cv2.warpAffine(generated,warp,(w,h),flags=cv2.INTER_LANCZOS4|cv2.WARP_INVERSE_MAP,
                       borderMode=cv2.BORDER_REFLECT)

# Only accept pixels in the left-arm corridor; everything else remains exactly
# frame 51. The corridor includes the sleeve, cuff, forearm, and hand emerging
# beside the raised knee while avoiding the sword hand and most of the torso.
mask=np.zeros((h,w),np.uint8)
poly=np.array([[590,365],[690,375],[735,455],[730,610],[690,680],
               [625,650],[600,570],[585,470]],np.int32)
cv2.fillPoly(mask,[poly],255)
mask=cv2.GaussianBlur(mask,(0,0),2.2).astype(np.float32)/255
mask=mask[...,None]
out=np.clip(aligned*mask+target*(1-mask),0,255).astype(np.uint8)
path=FRAMES/'frame_0052_redraw_candidate.png'
cv2.imwrite(str(path),out)
original=FRAMES/'frame_0052.png'
backup=FRAMES/'frame_0052_original.png'
if not backup.exists():
    shutil.copy2(original,backup)
shutil.copy2(path,original)

ordered=[FRAMES/f'frame_{i:04d}.png' for i in range(123)]
gif=[]
for p in ordered:
    frame=cv2.imread(str(p))
    frame=cv2.resize(frame,(560,416),interpolation=cv2.INTER_AREA)
    gif.append(Image.fromarray(cv2.cvtColor(frame,cv2.COLOR_BGR2RGB)))
gif_path=ROOT/'death_splice_v3_frame52_redraw_preview.gif'
gif[0].save(gif_path,save_all=True,append_images=gif[1:],duration=42,loop=0,
            disposal=2,optimize=False)
print(path)
print(gif_path)
print('warp',warp.tolist())
