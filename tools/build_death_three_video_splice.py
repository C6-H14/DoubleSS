from pathlib import Path
import cv2
import numpy as np
from PIL import Image

from build_death_splice_preview import read, locate, mask_subject, bbox, background, put_subject
from analyze_death_video_splice import descriptor, pair_cost

ROOT = Path(r"D:\StSmod\DoubleSS\art_candidates\video_source")
OUT = ROOT / "death_splice_analysis"


def main():
    a = read(locate('9507'))
    bridge = read(locate('1520'))
    hand_bridge = read(locate('3034'))
    b = read(locate('3856'))
    h,w = a[0].shape[:2]
    cut_a, cut_b = 46, 49

    # Transform 3856 into the exact coordinate system used for the bridge tail.
    ma, mb = mask_subject(a[cut_a]), mask_subject(b[cut_b])
    ax0,ay0,ax1,ay1 = bbox(ma); bx0,by0,bx1,by1 = bbox(mb)
    scale = np.sqrt(((ax1-ax0)/(bx1-bx0))*((ay1-ay0)/(by1-by0)))
    scale = float(np.clip(scale,.95,1.20))
    tx = (ax0+ax1)/2 - scale*(bx0+bx1)/2
    ty = ay1 - scale*by1
    M=np.array([[scale,0,tx],[0,scale,ty]],np.float32)
    bg=background(a[0],h,w)
    b_aligned=[put_subject(f,(h,w),M,bg) for f in b[cut_b:]]

    # 1520 f5->f8 contains a hand teleport. Replace exactly that interval with
    # the complete, continuous 3034 hand arc, sparsely sampled into ~0.37 s.
    bridge_pre_ids=list(range(0,6))
    hand_ids=list(range(6,97,6))
    bridge_post_ids=list(range(8,97,4))
    bridge_ids=bridge_pre_ids + bridge_post_ids
    bridge_frames=[cv2.resize(bridge[i],(w,h),interpolation=cv2.INTER_LANCZOS4)
                   for i in bridge_pre_ids]
    bridge_frames += [cv2.resize(hand_bridge[i],(w,h),interpolation=cv2.INTER_LANCZOS4)
                      for i in hand_ids]
    bridge_frames += [cv2.resize(bridge[i],(w,h),interpolation=cv2.INTER_LANCZOS4)
                      for i in bridge_post_ids]
    # Keep bridge f0: although nominally the same pose, it is required to avoid
    # jumping directly from 9507 f46 to 1520 f1/f2.
    # 3856 starts clipping the rightmost hair at f81, so finish at f80 and hold.
    last_safe_b=80
    safe_b=b_aligned[1:last_safe_b-cut_b+1]
    frames=a[:cut_a+1]+bridge_frames+safe_b

    # Export lossless review frames with an exact source-frame manifest.
    frame_dir=OUT/'death_splice_frames_review_v2'
    frame_dir.mkdir(exist_ok=True)
    labels=[f'9507,f{i}' for i in range(cut_a+1)]
    labels += [f'1520,f{i}' for i in bridge_pre_ids]
    labels += [f'3034,f{i}' for i in hand_ids]
    labels += [f'1520,f{i}' for i in bridge_post_ids]
    labels += [f'3856,f{i}' for i in range(cut_b+1,last_safe_b+1)]
    if len(labels) != len(frames):
        raise RuntimeError(f'label/frame mismatch: {len(labels)} != {len(frames)}')
    manifest=['output_frame,source_video,source_frame']
    for i,(f,label) in enumerate(zip(frames,labels)):
        cv2.imwrite(str(frame_dir/f'frame_{i:04d}.png'),f)
        video,source_frame=label.split(',')
        manifest.append(f'{i},{video},{source_frame[1:]}')
    (frame_dir/'frame_manifest.csv').write_text('\n'.join(manifest)+'\n',encoding='utf-8-sig')

    gif_frames=[]
    for f in frames:
        small=cv2.resize(f,(560,416),interpolation=cv2.INTER_AREA)
        gif_frames.append(Image.fromarray(cv2.cvtColor(small,cv2.COLOR_BGR2RGB)))
    out=OUT/'death_splice_v2_dense_safe_hair_preview.gif'
    gif_frames[0].save(out,save_all=True,append_images=gif_frames[1:],duration=42,
                       loop=0,disposal=2,optimize=False)

    # Review both seams and all retained bridge frames.
    review=a[43:47]+bridge_frames+b_aligned[:4]
    tw,th,cols=280,208,7
    rows=(len(review)+cols-1)//cols
    sheet=np.full((rows*(th+28),cols*tw,3),32,np.uint8)
    for n,f in enumerate(review):
        im=cv2.resize(f,(tw,th),interpolation=cv2.INTER_AREA)
        y=(n//cols)*(th+28); x=(n%cols)*tw
        sheet[y:y+th,x:x+tw]=im
        if n<4: label=f'9507 f{43+n}'
        elif n<4+len(bridge_pre_ids): label=f'1520 f{bridge_pre_ids[n-4]}'
        elif n<4+len(bridge_pre_ids)+len(hand_ids):
            label=f'3034 f{hand_ids[n-4-len(bridge_pre_ids)]}'
        elif n<4+len(bridge_frames):
            label=f'1520 f{bridge_post_ids[n-4-len(bridge_pre_ids)-len(hand_ids)]}'
        else: label=f'3856 f{cut_b+n-4-len(bridge_frames)}'
        cv2.putText(sheet,label,(x+4,y+th+19),cv2.FONT_HERSHEY_SIMPLEX,.45,
                    (240,240,240),1,cv2.LINE_AA)
    cv2.imwrite(str(OUT/'death_splice_with_1520_seams.png'),sheet)

    # Rank consecutive-frame discontinuities after normalizing the subject bbox.
    desc=[descriptor(f) for f in frames]
    jumps=[]
    for i in range(1,len(frames)):
        if desc[i-1] is None or desc[i] is None: continue
        p=pair_cost(desc[i-1],desc[i])
        c0=np.array(desc[i-1]['center']); c1=np.array(desc[i]['center'])
        center=float(np.linalg.norm(c1-c0))
        jumps.append((p+center/400.0,p,center,i))
    jumps.sort(reverse=True)
    top=jumps[:18]
    tw2,th2=252,187
    jump_sheet=np.full((len(top)*(th2+28),3*tw2,3),32,np.uint8)
    for row,(score,p,center,i) in enumerate(top):
        for col,idx in enumerate((max(0,i-1),i,min(len(frames)-1,i+1))):
            im=cv2.resize(frames[idx],(tw2,th2),interpolation=cv2.INTER_AREA)
            y=row*(th2+28); x=col*tw2
            jump_sheet[y:y+th2,x:x+tw2]=im
            cv2.putText(jump_sheet,f'f{idx}',(x+4,y+th2+18),cv2.FONT_HERSHEY_SIMPLEX,
                        .45,(240,240,240),1,cv2.LINE_AA)
        cv2.putText(jump_sheet,f'{score:.3f} d={center:.1f}',(4,row*(th2+28)+18),
                    cv2.FONT_HERSHEY_SIMPLEX,.45,(0,255,255),1,cv2.LINE_AA)
    cv2.imwrite(str(OUT/'death_splice_jump_diagnostics.png'),jump_sheet)

    # Compare the last source frames against aligned composites to reveal whether
    # any hair loss is in the source or introduced by the purple-key mask.
    tail_ids=list(range(max(cut_b,len(b)-16),len(b),2))
    tail=np.full((2*(th2+28),len(tail_ids)*tw2,3),32,np.uint8)
    for col,src_idx in enumerate(tail_ids):
        raw=cv2.resize(b[src_idx],(tw2,th2),interpolation=cv2.INTER_AREA)
        aligned=cv2.resize(b_aligned[src_idx-cut_b],(tw2,th2),interpolation=cv2.INTER_AREA)
        tail[0:th2,col*tw2:(col+1)*tw2]=raw
        tail[th2+28:2*th2+28,col*tw2:(col+1)*tw2]=aligned
        cv2.putText(tail,f'raw {src_idx}',(col*tw2+4,th2+18),cv2.FONT_HERSHEY_SIMPLEX,
                    .42,(240,240,240),1,cv2.LINE_AA)
        cv2.putText(tail,f'aligned {src_idx}',(col*tw2+4,2*th2+46),cv2.FONT_HERSHEY_SIMPLEX,
                    .42,(240,240,240),1,cv2.LINE_AA)
    cv2.imwrite(str(OUT/'death_splice_tail_hair_diagnostics.png'),tail)
    print(out)
    print(frame_dir)
    print(f'frames={len(frames)} pre={bridge_pre_ids} hand={hand_ids} post={bridge_post_ids}')
    print('top_jumps='+str([(round(s,3),i) for s,_,_,i in top]))


if __name__=='__main__': main()
