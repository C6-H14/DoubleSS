from pathlib import Path
import json

import cv2
import numpy as np
from PIL import Image, ImageDraw


ROOT = Path(r"D:\StSmod\DoubleSS")
SOURCE = ROOT / "art_candidates" / "video_source" / "idle" / "short_blink_final_v15_garment_color_stable" / "alpha_frames"
OUT = ROOT / "art_candidates" / "video_source" / "idle" / "diagnostics_local_artifacts_v16"
SAMPLES = [1, 16, 41, 57, 81, 101, 120, 151, 180]
REGIONS = {
    "hair_face": (330, 65, 775, 350),
    "neck_collar": (430, 165, 685, 355),
    "hands_guard": (395, 335, 665, 555),
    "skirt_hips": (365, 365, 735, 600),
    "blade_mid_tip": (500, 405, 930, 735),
    "legs_socks_shoes": (315, 475, 855, 830),
}


def exterior_and_holes(alpha):
    solid = alpha > 16
    transparent = (~solid).astype(np.uint8)
    flood = transparent.copy()
    ffmask = np.zeros((alpha.shape[0]+2, alpha.shape[1]+2), np.uint8)
    cv2.floodFill(flood, ffmask, (0,0), 2)
    exterior = flood == 2
    holes = (~solid) & (~exterior)
    return solid, exterior, holes


def component_boxes(mask, min_area=1, max_area=400):
    n, labels, stats, _ = cv2.connectedComponentsWithStats(mask.astype(np.uint8), 8)
    out=[]
    for i in range(1,n):
        area=int(stats[i,cv2.CC_STAT_AREA])
        if min_area <= area <= max_area:
            out.append({"x":int(stats[i,cv2.CC_STAT_LEFT]),"y":int(stats[i,cv2.CC_STAT_TOP]),
                        "w":int(stats[i,cv2.CC_STAT_WIDTH]),"h":int(stats[i,cv2.CC_STAT_HEIGHT]),"area":area})
    return out


def analyze(rgba):
    alpha=rgba[:,:,3]
    solid, exterior, holes=exterior_and_holes(alpha)
    # Detached opaque islands: all components except the largest character.
    n, labels, stats, _=cv2.connectedComponentsWithStats(solid.astype(np.uint8),8)
    islands=[]
    if n>1:
        largest=1+int(np.argmax(stats[1:,cv2.CC_STAT_AREA]))
        for i in range(1,n):
            if i==largest: continue
            area=int(stats[i,cv2.CC_STAT_AREA])
            if area<=500:
                islands.append({"x":int(stats[i,cv2.CC_STAT_LEFT]),"y":int(stats[i,cv2.CC_STAT_TOP]),
                                "w":int(stats[i,cv2.CC_STAT_WIDTH]),"h":int(stats[i,cv2.CC_STAT_HEIGHT]),"area":area})
    low=(alpha>0)&(alpha<32)
    # Low-alpha pixels isolated from any robust subject pixel are likely matte fragments.
    robust=alpha>=128
    near=cv2.dilate(robust.astype(np.uint8),np.ones((5,5),np.uint8))>0
    stray_low=low&(~near)
    return holes,islands,component_boxes(holes,1,500),component_boxes(stray_low,1,200),stray_low


def intersects(box, region):
    return box["x"] < region[2] and box["x"]+box["w"] > region[0] and box["y"] < region[3] and box["y"]+box["h"] > region[1]


def main():
    OUT.mkdir(parents=True,exist_ok=True)
    paths={int(p.stem.split('_')[-1]):p for p in SOURCE.glob('idle_*.png')}
    report={"samples":SAMPLES,"regions":{},"frames":{}}
    for region_name,box in REGIONS.items():
        x1,y1,x2,y2=box;w,h=x2-x1,y2-y1
        cols=3;rows=(len(SAMPLES)+2)//3
        sheet=Image.new('RGB',(w*cols,(h+24)*rows),(36,36,38));draw=ImageDraw.Draw(sheet)
        for k,n in enumerate(SAMPLES):
            rgba=np.asarray(Image.open(paths[n]).convert('RGBA'),dtype=np.uint8)
            holes,islands,hole_boxes,stray_boxes,stray=analyze(rgba)
            vis=rgba.copy()
            # Red = enclosed transparent holes; lime = isolated low-alpha fragments.
            vis[holes,:3]=[255,40,40];vis[holes,3]=255
            vis[stray,:3]=[50,255,60];vis[stray,3]=255
            crop=Image.fromarray(vis,'RGBA').crop(box)
            bg=Image.new('RGBA',crop.size,(45,45,45,255));bg.alpha_composite(crop)
            cx=(k%cols)*w;cy=(k//cols)*(h+24)
            sheet.paste(bg.convert('RGB'),(cx,cy+24));draw.text((cx+4,cy+4),f'frame {n:03d}  red=holes green=stray alpha',fill='white')
            frame=report["frames"].setdefault(str(n),{})
            frame[region_name]={"holes":[b for b in hole_boxes if intersects(b,box)],
                                "islands":[b for b in islands if intersects(b,box)],
                                "stray_low_alpha":[b for b in stray_boxes if intersects(b,box)]}
        sheet.save(OUT/f'{region_name}_sample_audit.png',optimize=True)

    # Compact counts by region for routing the actual repair work.
    for region_name in REGIONS:
        counts={"holes":0,"islands":0,"stray_low_alpha":0}
        affected={k:[] for k in counts}
        for n in SAMPLES:
            data=report["frames"][str(n)][region_name]
            for key in counts:
                counts[key]+=len(data[key])
                if data[key]: affected[key].append(n)
        report["regions"][region_name]={"component_counts":counts,"affected_sample_frames":affected}
    (OUT/'local_artifact_report.json').write_text(json.dumps(report,indent=2)+"\n",encoding='utf-8')
    print(json.dumps(report["regions"],indent=2))


if __name__=='__main__':
    main()
