from pathlib import Path
import json
import cv2
import numpy as np

ROOT = Path(r"D:\StSmod\DoubleSS\art_candidates\video_source")
OUT = ROOT / "death_splice_analysis"
OUT.mkdir(exist_ok=True)


def locate(token):
    matches = list(ROOT.glob(f"*{token}*.mp4"))
    if len(matches) != 1:
        raise RuntimeError(f"Expected one video for {token}, found {matches}")
    return matches[0]


def read_video(path):
    cap = cv2.VideoCapture(str(path))
    fps = cap.get(cv2.CAP_PROP_FPS)
    frames = []
    while True:
        ok, frame = cap.read()
        if not ok:
            break
        frames.append(frame)
    cap.release()
    return fps, frames


def contact_sheet(frames, fps, path, cols=6, max_samples=36):
    ids = np.linspace(0, len(frames) - 1, min(max_samples, len(frames))).round().astype(int)
    thumb_w = 300
    h, w = frames[0].shape[:2]
    thumb_h = round(h * thumb_w / w)
    rows = (len(ids) + cols - 1) // cols
    sheet = np.full((rows * (thumb_h + 34), cols * thumb_w, 3), 35, np.uint8)
    for n, idx in enumerate(ids):
        im = cv2.resize(frames[idx], (thumb_w, thumb_h), interpolation=cv2.INTER_AREA)
        y, x = (n // cols) * (thumb_h + 34), (n % cols) * thumb_w
        sheet[y:y+thumb_h, x:x+thumb_w] = im
        cv2.putText(sheet, f"f{idx}  {idx/fps:.3f}s", (x+6, y+thumb_h+23),
                    cv2.FONT_HERSHEY_SIMPLEX, .55, (240,240,240), 1, cv2.LINE_AA)
    cv2.imwrite(str(path), sheet)


def foreground_mask(frame):
    # JiMeng references use a saturated pink/purple flat background. Estimate it
    # from the image border and retain pixels sufficiently different from it.
    lab = cv2.cvtColor(frame, cv2.COLOR_BGR2LAB).astype(np.float32)
    border = np.concatenate([lab[:12].reshape(-1,3), lab[-12:].reshape(-1,3),
                             lab[:, :12].reshape(-1,3), lab[:, -12:].reshape(-1,3)])
    bg = np.median(border, axis=0)
    dist = np.linalg.norm(lab - bg, axis=2)
    mask = (dist > 18).astype(np.uint8) * 255
    mask = cv2.morphologyEx(mask, cv2.MORPH_OPEN, np.ones((3,3),np.uint8))
    mask = cv2.morphologyEx(mask, cv2.MORPH_CLOSE, np.ones((7,7),np.uint8))
    n, labels, stats, _ = cv2.connectedComponentsWithStats(mask)
    if n > 1:
        keep = 1 + np.argmax(stats[1:, cv2.CC_STAT_AREA])
        mask = (labels == keep).astype(np.uint8) * 255
    return mask


def descriptor(frame):
    mask = foreground_mask(frame)
    ys, xs = np.where(mask > 0)
    if not len(xs):
        return None
    x0, x1, y0, y1 = xs.min(), xs.max()+1, ys.min(), ys.max()+1
    crop = frame[y0:y1, x0:x1]
    cm = mask[y0:y1, x0:x1]
    size = 256
    canvas = np.zeros((size,size,3), np.uint8)
    alpha = np.zeros((size,size), np.uint8)
    scale = min((size-16)/crop.shape[1], (size-16)/crop.shape[0])
    nw, nh = max(1,round(crop.shape[1]*scale)), max(1,round(crop.shape[0]*scale))
    rr = cv2.resize(crop, (nw,nh), interpolation=cv2.INTER_AREA)
    rm = cv2.resize(cm, (nw,nh), interpolation=cv2.INTER_NEAREST)
    ox, oy = (size-nw)//2, (size-nh)//2
    canvas[oy:oy+nh, ox:ox+nw] = rr
    alpha[oy:oy+nh, ox:ox+nw] = rm
    gray = cv2.cvtColor(canvas, cv2.COLOR_BGR2GRAY)
    edge = cv2.Canny(gray, 60, 140)
    edge[alpha == 0] = 0
    return dict(image=canvas, mask=alpha, edge=edge,
                bbox=[int(x0),int(y0),int(x1-x0),int(y1-y0)],
                center=[float((x0+x1)/2),float((y0+y1)/2)])


def pair_cost(a, b):
    union = (a['mask'] > 0) | (b['mask'] > 0)
    inter = (a['mask'] > 0) & (b['mask'] > 0)
    iou = inter.sum() / max(1, union.sum())
    edge_xor = np.logical_xor(a['edge'] > 0, b['edge'] > 0).sum() / max(1, union.sum())
    color = np.abs(a['image'].astype(np.float32)-b['image'].astype(np.float32)).mean(axis=2)
    color = color[inter].mean()/255 if inter.any() else 1
    return float((1-iou)*0.50 + edge_xor*0.25 + color*0.25)


def main():
    paths = {k: locate(k) for k in ('9507','3856')}
    vids = {}
    meta = {}
    for key, path in paths.items():
        fps, frames = read_video(path)
        vids[key] = (fps, frames)
        h,w = frames[0].shape[:2]
        meta[key] = dict(path=str(path), fps=fps, frames=len(frames), duration=len(frames)/fps,
                         width=w, height=h)
        contact_sheet(frames, fps, OUT/f"{key}_contact.png")

    fa, aa = vids['9507']; fb, bb = vids['3856']
    da = [descriptor(f) for f in aa]
    db = [descriptor(f) for f in bb]
    candidates = []
    # Search broad ranges: latter 80% of 9507 against first 80% of 3856.
    for i in range(max(0, int(len(aa)*.20)), len(aa)):
        if da[i] is None: continue
        for j in range(0, max(1,int(len(bb)*.80))):
            if db[j] is None: continue
            cost = pair_cost(da[i], db[j])
            candidates.append((cost,i,j))
    candidates.sort()
    chosen=[]
    for cost,i,j in candidates:
        if all(abs(i-x[1])>4 or abs(j-x[2])>4 for x in chosen):
            chosen.append((cost,i,j))
        if len(chosen)==12: break
    meta['candidates']=[dict(cost=c,i9507=i,t9507=i/fa,i3856=j,t3856=j/fb,
                             bbox9507=da[i]['bbox'],bbox3856=db[j]['bbox']) for c,i,j in chosen]
    (OUT/'analysis.json').write_text(json.dumps(meta,ensure_ascii=False,indent=2),encoding='utf-8')

    # Make side-by-side candidate sheet plus ±2-frame context.
    cellw=260; h0,w0=aa[0].shape[:2]; cellh=round(h0*cellw/w0)
    sheet=np.full((len(chosen)*(cellh+34), 6*cellw, 3),35,np.uint8)
    for row,(cost,i,j) in enumerate(chosen):
        ids=[max(0,i-2),i,min(len(aa)-1,i+2),max(0,j-2),j,min(len(bb)-1,j+2)]
        src=[aa,aa,aa,bb,bb,bb]
        for col,(frames,idx) in enumerate(zip(src,ids)):
            im=cv2.resize(frames[idx],(cellw,cellh),interpolation=cv2.INTER_AREA)
            y=row*(cellh+34); x=col*cellw
            sheet[y:y+cellh,x:x+cellw]=im
            label=("9507" if col<3 else "3856")+f" f{idx}"
            cv2.putText(sheet,label,(x+5,y+cellh+22),cv2.FONT_HERSHEY_SIMPLEX,.5,(240,240,240),1,cv2.LINE_AA)
        cv2.putText(sheet,f"cost {cost:.3f}",(5,row*(cellh+34)+22),cv2.FONT_HERSHEY_SIMPLEX,.55,(0,255,255),1,cv2.LINE_AA)
    cv2.imwrite(str(OUT/'candidate_seams.png'),sheet)

    # Dense review sheet around the only physically compatible phase: kneeling.
    a_ids = list(range(38, 53))
    b_ids = list(range(34, 61))
    tw = 224
    th = round(aa[0].shape[0] * tw / aa[0].shape[1])
    cols = 9
    rows = ((len(a_ids)+cols-1)//cols) + ((len(b_ids)+cols-1)//cols)
    dense = np.full((rows*(th+30), cols*tw, 3), 35, np.uint8)
    row_offset = 0
    for label, frames, ids in [('9507',aa,a_ids),('3856',bb,b_ids)]:
        for n,idx in enumerate(ids):
            row = row_offset + n//cols
            col = n%cols
            im=cv2.resize(frames[idx],(tw,th),interpolation=cv2.INTER_AREA)
            y=row*(th+30); x=col*tw
            dense[y:y+th,x:x+tw]=im
            cv2.putText(dense,f'{label} f{idx} {idx/(fa if label=="9507" else fb):.2f}s',
                        (x+4,y+th+20),cv2.FONT_HERSHEY_SIMPLEX,.42,(240,240,240),1,cv2.LINE_AA)
        row_offset += (len(ids)+cols-1)//cols
    cv2.imwrite(str(OUT/'kneeling_dense.png'),dense)
    print(json.dumps(meta,ensure_ascii=False,indent=2))

if __name__ == '__main__':
    main()
