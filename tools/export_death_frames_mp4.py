from pathlib import Path
import cv2

ROOT=Path(r"D:\StSmod\DoubleSS\art_candidates\video_source\death_splice_analysis")
SRC=ROOT/'death_splice_frames_review_v4_repaired'
OUT=ROOT/'death_splice_v5_deblur_scale_repaired_24fps.mp4'

first=cv2.imread(str(SRC/'frame_0000.png'))
h,w=first.shape[:2]
writer=cv2.VideoWriter(str(OUT),cv2.VideoWriter_fourcc(*'mp4v'),24.0,(w,h))
if not writer.isOpened():
    raise RuntimeError('OpenCV could not open MP4 writer')
for i in range(123):
    frame=cv2.imread(str(SRC/f'frame_{i:04d}.png'))
    writer.write(frame)
writer.release()

cap=cv2.VideoCapture(str(OUT))
print(OUT)
print('fps',cap.get(cv2.CAP_PROP_FPS),'frames',int(cap.get(cv2.CAP_PROP_FRAME_COUNT)),
      'size',int(cap.get(cv2.CAP_PROP_FRAME_WIDTH)),int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT)))
cap.release()
