from pathlib import Path
import cv2
import numpy as np
from PIL import Image

ROOT = Path(r"D:\StSmod\DoubleSS\art_candidates\video_source")
OUT = ROOT / "death_splice_analysis"
OUT.mkdir(exist_ok=True)


def locate(token):
    paths = list(ROOT.glob(f"*{token}*.mp4"))
    if len(paths) != 1:
        raise RuntimeError((token, paths))
    return paths[0]


def read(path):
    cap = cv2.VideoCapture(str(path))
    frames = []
    while True:
        ok, frame = cap.read()
        if not ok:
            break
        frames.append(frame)
    cap.release()
    return frames


def mask_subject(frame):
    lab = cv2.cvtColor(frame, cv2.COLOR_BGR2LAB).astype(np.float32)
    border = np.concatenate((lab[:10].reshape(-1,3), lab[-10:].reshape(-1,3),
                             lab[:,:10].reshape(-1,3), lab[:,-10:].reshape(-1,3)))
    bg = np.median(border, axis=0)
    dist = np.linalg.norm(lab-bg, axis=2)
    m = (dist > 16).astype(np.uint8)*255
    m = cv2.morphologyEx(m, cv2.MORPH_CLOSE, np.ones((5,5),np.uint8))
    n, labels, stats, _ = cv2.connectedComponentsWithStats(m)
    if n > 1:
        # Keep all meaningful components because the sword may be detached by glow.
        good = np.where(stats[:,cv2.CC_STAT_AREA] > max(30, frame.shape[0]*frame.shape[1]*.00008))[0]
        good = good[good != 0]
        m = np.isin(labels, good).astype(np.uint8)*255
    return cv2.GaussianBlur(m, (3,3), .6)


def bbox(mask):
    y,x = np.where(mask > 80)
    return (x.min(), y.min(), x.max()+1, y.max()+1)


def background(frame, h, w):
    # Bilinear purple field estimated from the four source corners.
    k = 24
    c = [np.median(frame[:k,:k],axis=(0,1)), np.median(frame[:k,-k:],axis=(0,1)),
         np.median(frame[-k:,:k],axis=(0,1)), np.median(frame[-k:,-k:],axis=(0,1))]
    xx = np.linspace(0,1,w,dtype=np.float32)[None,:,None]
    yy = np.linspace(0,1,h,dtype=np.float32)[:,None,None]
    top = c[0]*(1-xx)+c[1]*xx
    bot = c[2]*(1-xx)+c[3]*xx
    return np.clip(top*(1-yy)+bot*yy,0,255).astype(np.uint8)


def put_subject(frame, target_hw, M, bg):
    h,w = target_hw
    m = mask_subject(frame)
    fg = cv2.warpAffine(frame, M, (w,h), flags=cv2.INTER_LANCZOS4,
                        borderMode=cv2.BORDER_CONSTANT, borderValue=0)
    a = cv2.warpAffine(m, M, (w,h), flags=cv2.INTER_LINEAR,
                       borderMode=cv2.BORDER_CONSTANT, borderValue=0).astype(np.float32)/255
    a = a[...,None]
    return np.clip(fg*a + bg*(1-a),0,255).astype(np.uint8)


def optical_blend(a, b, count):
    ga = cv2.cvtColor(a, cv2.COLOR_BGR2GRAY)
    gb = cv2.cvtColor(b, cv2.COLOR_BGR2GRAY)
    flow_ab = cv2.calcOpticalFlowFarneback(ga,gb,None,.5,4,25,4,7,1.5,0)
    flow_ba = cv2.calcOpticalFlowFarneback(gb,ga,None,.5,4,25,4,7,1.5,0)
    h,w = ga.shape
    x,y = np.meshgrid(np.arange(w,dtype=np.float32),np.arange(h,dtype=np.float32))
    out=[]
    for n in range(1,count+1):
        t=n/(count+1)
        wa=cv2.remap(a,x+flow_ab[...,0]*t,y+flow_ab[...,1]*t,cv2.INTER_LINEAR,
                     borderMode=cv2.BORDER_REFLECT)
        wb=cv2.remap(b,x+flow_ba[...,0]*(1-t),y+flow_ba[...,1]*(1-t),cv2.INTER_LINEAR,
                     borderMode=cv2.BORDER_REFLECT)
        out.append(cv2.addWeighted(wa,1-t,wb,t,0))
    return out


def main():
    a = read(locate('9507'))
    b = read(locate('3856'))
    h,w = a[0].shape[:2]

    # Tested splice: 9507 f0..46, then 3856 f49 onward.
    cut_a, cut_b = 46, 49
    ma, mb = mask_subject(a[cut_a]), mask_subject(b[cut_b])
    ax0,ay0,ax1,ay1 = bbox(ma)
    bx0,by0,bx1,by1 = bbox(mb)
    # Match overall character scale conservatively, then lock ground contact and center.
    scale = np.sqrt(((ax1-ax0)/(bx1-bx0))*((ay1-ay0)/(by1-by0)))
    scale = float(np.clip(scale, .95, 1.20))
    acx, abot = (ax0+ax1)/2, ay1
    bcx, bbot = (bx0+bx1)/2, by1
    tx, ty = acx-scale*bcx, abot-scale*bbot
    M = np.array([[scale,0,tx],[0,scale,ty]],np.float32)

    bg = background(a[0],h,w)
    a_std = [put_subject(f,(h,w),np.array([[1,0,0],[0,1,0]],np.float32),bg) for f in a[:cut_a+1]]
    b_std = [put_subject(f,(h,w),M,bg) for f in b[cut_b:]]
    cv2.imwrite(str(OUT/'death_bridge_start_9507_f046.png'), a[cut_a])
    cv2.imwrite(str(OUT/'death_bridge_end_3856_f049_aligned.png'), b_std[0])
    # A hard pose-matched cut is cleaner than optical flow here: the latter
    # duplicates the sword and hair during the large topology change.
    frames = a_std + b_std

    # Half-size GIF keeps the diagnostic portable while retaining hand/sword detail.
    size = (560,416)
    rgb = [Image.fromarray(cv2.cvtColor(cv2.resize(f,size,interpolation=cv2.INTER_AREA),
                                       cv2.COLOR_BGR2RGB)) for f in frames]
    out = OUT / 'death_splice_9507_to_3856_preview.gif'
    rgb[0].save(out,save_all=True,append_images=rgb[1:],duration=42,loop=0,
                disposal=2,optimize=False)
    review = frames[max(0,cut_a-4):cut_a+9]
    tw, th = 280, 208
    sheet = np.full((2*(th+28),7*tw,3),32,np.uint8)
    for n,f in enumerate(review):
        im=cv2.resize(f,(tw,th),interpolation=cv2.INTER_AREA)
        yy=(n//7)*(th+28); xx=(n%7)*tw
        sheet[yy:yy+th,xx:xx+tw]=im
        cv2.putText(sheet,f'preview f{cut_a-4+n}',(xx+5,yy+th+19),
                    cv2.FONT_HERSHEY_SIMPLEX,.45,(240,240,240),1,cv2.LINE_AA)
    cv2.imwrite(str(OUT/'death_splice_seam_review.png'),sheet)
    print(out)
    print(f"frames={len(frames)} scale={scale:.5f} translate=({tx:.2f},{ty:.2f})")


if __name__ == '__main__':
    main()
