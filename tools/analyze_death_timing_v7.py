from pathlib import Path
import csv
import cv2
import numpy as np
import sys

ROOT = Path(r"D:\StSmod\DoubleSS\art_candidates\video_source\death_splice_analysis")
SRC = ROOT / (sys.argv[1] if len(sys.argv) > 1 else "death_splice_frames_review_v6_regen_26_59")
TAG = sys.argv[2] if len(sys.argv) > 2 else "death_v7"
BG = np.array([213, 0, 132], np.float32)


def subject_mask(im):
    d = np.linalg.norm(im.astype(np.float32) - BG, axis=2)
    m = (d > 34).astype(np.uint8) * 255
    return cv2.morphologyEx(m, cv2.MORPH_OPEN, np.ones((3, 3), np.uint8))


frames = [cv2.imread(str(p)) for p in sorted(SRC.glob("frame_*.png"))]
scores = [0.0]
for a, b in zip(frames, frames[1:]):
    aa = cv2.resize(a, (280, 208), interpolation=cv2.INTER_AREA)
    bb = cv2.resize(b, (280, 208), interpolation=cv2.INTER_AREA)
    ma = cv2.resize(subject_mask(a), (280, 208), interpolation=cv2.INTER_NEAREST)
    mb = cv2.resize(subject_mask(b), (280, 208), interpolation=cv2.INTER_NEAREST)
    mask = cv2.dilate(cv2.bitwise_or(ma, mb), np.ones((7, 7), np.uint8)) > 0
    ga = cv2.cvtColor(aa, cv2.COLOR_BGR2GRAY)
    gb = cv2.cvtColor(bb, cv2.COLOR_BGR2GRAY)
    flow = cv2.calcOpticalFlowFarneback(ga, gb, None, .5, 3, 15, 3, 5, 1.2, 0)
    mag = np.linalg.norm(flow, axis=2)
    vals = mag[mask]
    scores.append(float(np.percentile(vals, 75)) if vals.size else 0.0)

with (ROOT / f"{TAG}_motion_scores.csv").open("w", newline="", encoding="utf-8-sig") as fp:
    wr = csv.writer(fp); wr.writerow(["frame", "motion_p75"])
    wr.writerows(enumerate(scores))

# contact sheet: every fourth frame, with motion score.
ids = list(range(0, len(frames), 4))
tw, th, cols = 280, 208, 8
rows = (len(ids) + cols - 1) // cols
sheet = np.full((rows * (th + 28), cols * tw, 3), 35, np.uint8)
for n, i in enumerate(ids):
    im = cv2.resize(frames[i], (tw, th), interpolation=cv2.INTER_AREA)
    y, x = (n // cols) * (th + 28), (n % cols) * tw
    sheet[y:y+th, x:x+tw] = im
    cv2.putText(sheet, f"f{i}  v={scores[i]:.2f}", (x+4, y+th+19),
                cv2.FONT_HERSHEY_SIMPLEX, .45, (245,245,245), 1, cv2.LINE_AA)
cv2.imwrite(str(ROOT / f"{TAG}_timing_contact.png"), sheet)

# compact velocity plot without matplotlib.
H, W = 420, 1600
plot = np.full((H, W, 3), 245, np.uint8)
mx = max(scores) or 1
pts = []
for i, s in enumerate(scores):
    x = 50 + round(i * (W-80) / (len(scores)-1))
    y = H-40-round(min(s, mx) * (H-80) / mx)
    pts.append((x,y))
cv2.polylines(plot, [np.array(pts,np.int32)], False, (30,90,220), 2, cv2.LINE_AA)
for f in [25,26,59,60]:
    x=50+round(f*(W-80)/(len(scores)-1)); cv2.line(plot,(x,25),(x,H-35),(30,170,30),1)
    cv2.putText(plot,str(f),(x+3,25),cv2.FONT_HERSHEY_SIMPLEX,.45,(20,120,20),1,cv2.LINE_AA)
cv2.imwrite(str(ROOT / f"{TAG}_motion_curve.png"), plot)
arr=np.array(scores[1:],np.float32)
print(len(frames), max(scores), 'median',float(np.median(arr)), 'p90',float(np.percentile(arr,90)), 'cv',float(arr.std()/max(arr.mean(),1e-6)), ROOT / f"{TAG}_timing_contact.png")
