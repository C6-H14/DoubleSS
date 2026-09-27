from pathlib import Path
import csv, json, shutil
import cv2
import numpy as np
from PIL import Image, ImageDraw
from scipy.ndimage import distance_transform_edt

ROOT=Path(r'D:\StSmod\DoubleSS\art_candidates\video_source\death_splice_analysis')
SRC=ROOT/'death_splice_frames_review_v10_support_20f'
OUT=ROOT/'death_splice_v11_alpha_refined_v2'
FRAMES=OUT/'alpha_frames'


def matte(path):
    rgb=np.asarray(Image.open(path).convert('RGB'),np.uint8)
    hsv=cv2.cvtColor(rgb,cv2.COLOR_RGB2HSV)
    h,s,v=cv2.split(hsv)
    r,g,b=(rgb[...,i].astype(np.float32) for i in range(3))
    purple_bias=np.minimum(r-g,b-g)
    bg_rgb=np.array([132,0,213],np.float32)
    bg_dist=np.linalg.norm(rgb.astype(np.float32)-bg_rgb,axis=2)
    # Broad enough for the slightly varying video background, but this mask is
    # only accepted when connected to the canvas border.
    candidate=(((h>=126)&(h<=168)&(s>=55)&(v>=45)&(purple_bias>=28))).astype(np.uint8)
    count,labels,stats,_=cv2.connectedComponentsWithStats(candidate,8)
    border=set(np.unique(np.concatenate((labels[0],labels[-1],labels[:,0],labels[:,-1]))))
    bg=np.zeros(candidate.shape,bool)
    for lab in border:
        if lab: bg|=labels==lab
    # Clear enclosed regions when they contain the actual flat background
    # colour. This catches small elbow/arm windows without deleting the chest
    # glow, whose pixels are violet/pink rather than the background RGB.
    for lab in range(1,count):
        if lab in border: continue
        region=labels==lab
        area=stats[lab,cv2.CC_STAT_AREA]
        exact_fraction=float(np.mean(bg_dist[region]<=18.0))
        if area>=500 or (area>=6 and exact_fraction>=0.22):
            bg|=region

    fg=~bg
    # Remove only isolated, tiny compression islands outside the main subject.
    n,fl,fs,_=cv2.connectedComponentsWithStats(fg.astype(np.uint8),8)
    keep=np.zeros_like(fg)
    for lab in range(1,n):
        if fs[lab,cv2.CC_STAT_AREA]>=20: keep|=fl==lab
    fg=keep

    inward=cv2.distanceTransform(fg.astype(np.uint8),cv2.DIST_L2,5)
    alpha=np.uint8(np.clip(inward/1.05,0,1)*255)
    alpha[~fg]=0; alpha[alpha>250]=255

    # Replace RGB only on semitransparent contour pixels. Alpha geometry remains
    # untouched, so this removes purple color spill without shrinking the body.
    core=(inward>=2.2)&fg
    _,nearest=distance_transform_edt(~core,return_indices=True)
    clean=rgb.copy()
    edge=(alpha>0)&(alpha<255)
    clean[edge]=rgb[nearest[0],nearest[1]][edge]
    clean[alpha==0]=0
    return Image.fromarray(np.dstack((clean,alpha)),'RGBA'),int((alpha>0).sum()),int(edge.sum())


def checker(size,cell=24):
    a=Image.new('RGBA',size,(225,225,225,255)); d=ImageDraw.Draw(a)
    for y in range(0,size[1],cell):
        for x in range(0,size[0],cell):
            if (x//cell+y//cell)%2:d.rectangle((x,y,x+cell-1,y+cell-1),fill=(165,165,165,255))
    return a


def main():
    if OUT.exists(): shutil.rmtree(OUT)
    FRAMES.mkdir(parents=True)
    paths=sorted(SRC.glob('frame_*.png'))
    frames=[]; stats=[]
    for i,p in enumerate(paths):
        im,area,edge=matte(p); im.save(FRAMES/f'death_{i:04d}.png',optimize=True)
        frames.append(im); stats.append((i,area,edge))
    with (OUT/'matte_stats.csv').open('w',newline='',encoding='utf-8-sig') as fp:
        wr=csv.writer(fp); wr.writerow(['frame','opaque_or_partial_area','antialias_edge_pixels']); wr.writerows(stats)

    bg_black=Image.new('RGBA',frames[0].size,(0,0,0,255))
    views=[Image.alpha_composite(bg_black.copy(),f).resize((560,416),Image.Resampling.LANCZOS).convert('RGB') for f in frames]
    views[0].save(OUT/'death_black_preview.gif',save_all=True,append_images=views[1:],duration=42,loop=0,disposal=2,optimize=False)

    picks=np.linspace(0,len(frames)-1,30).round().astype(int)
    cw,ch,cols=320,268,6; rows=(len(picks)+cols-1)//cols
    sheet=Image.new('RGB',(cols*cw,rows*ch),(28,28,28)); draw=ImageDraw.Draw(sheet); cb=checker(frames[0].size)
    for n,i in enumerate(picks):
        view=Image.alpha_composite(cb.copy(),frames[i]).resize((320,240),Image.Resampling.LANCZOS).convert('RGB')
        x=(n%cols)*cw;y=(n//cols)*ch;sheet.paste(view,(x,y));draw.text((x+5,y+244),f'frame {i}',fill='white')
    sheet.save(OUT/'death_checker_contact.jpg',quality=97)
    (OUT/'report.json').write_text(json.dumps({'frames':len(frames),'fps':24,'duration':len(frames)/24,'method':'border-connected conservative chroma matte'},indent=2)+'\n',encoding='utf-8')
    print(OUT); print('frames',len(frames),'area range',min(x[1] for x in stats),max(x[1] for x in stats))

if __name__=='__main__':main()
