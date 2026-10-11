"""Fix buff blade length and remove the forced last-frame artwork switch."""

from pathlib import Path
import json
import cv2
import numpy as np
from PIL import Image, ImageDraw
import bridge_alt_buff_v3 as media

ROOT = Path(r"D:\StSmod\art_preview\alternate_fencer_buff")
SOURCE = ROOT / "buff_v2_alpha_60f_60fps" / "alpha_frames_60fps"
OUT = ROOT / "buff_v4_sword_stable_60f_60fps"
FRAMES = OUT / "alpha_frames_60fps"
ORIGIN_KEYS = [(1,443),(4,452),(17,456),(35,456),(45,452),(54,443),(60,443)]
TARGET_LENGTH = float(np.hypot(578-443,302-317))

def line_points(rgba, index):
    rgb = rgba[:,:,:3]
    gray = cv2.cvtColor(rgb, cv2.COLOR_RGB2GRAY)
    edges = cv2.Canny(gray,70,180)
    edges[:270] = 0
    edges[395:] = 0
    edges[:,:460] = 0
    lines = cv2.HoughLinesP(edges,1,np.pi/180,30,minLineLength=45,maxLineGap=14)
    candidates=[]
    if lines is not None:
        for x1,y1,x2,y2 in lines.reshape(-1,4):
            if x1>x2: x1,y1,x2,y2=x2,y2,x1,y1
            if x2-x1<45: continue
            slope=(y2-y1)/(x2-x1)
            if abs(slope)<.22:
                candidates.append((x2-x1,slope,y1-slope*x1))
    if not candidates: raise RuntimeError(f"No sword axis for frame {index}")
    best=sorted(candidates,reverse=True)[:3]
    slope=float(np.median([p[1] for p in best]))
    intercept=float(np.median([p[2] for p in best]))
    ox=float(np.interp(index,[k[0] for k in ORIGIN_KEYS],[k[1] for k in ORIGIN_KEYS]))
    oy=slope*ox+intercept
    yy,xx=np.mgrid[:rgba.shape[0],:rgba.shape[1]]
    band=(np.abs(yy-(slope*xx+intercept))<14)&(xx>ox+10)&(rgba[:,:,3]>32)
    tipx=float(np.where(band)[1].max())
    return ox,oy,slope,tipx

def repair(rgba,index,donor):
    ox,oy,slope,tipx=line_points(rgba,index)
    yy,xx=np.mgrid[:rgba.shape[0],:rgba.shape[1]]
    axis=slope*xx+(oy-slope*ox)
    distance=yy-axis
    r,g,b=rgba[:,:,0].astype(float),rgba[:,:,1].astype(float),rgba[:,:,2].astype(float)
    # Preserve skin/fingers above the blade. Orange flame is a separate layer.
    warm=(r>b+22)&(g>b+12)&(r>150)&(g>65)
    flame_region=(xx>ox+12)&(np.abs(distance)<60)&((xx>610)|(distance>=-12))
    flame_mask=warm&flame_region&(rgba[:,:,3]>0)
    flame=np.zeros_like(rgba)
    flame[flame_mask]=rgba[flame_mask]
    repaired=rgba.copy()
    repaired[flame_mask]=0
    neutral=(np.maximum.reduce([r,g,b])-np.minimum.reduce([r,g,b])<65)
    old_blade=(xx>ox)&(np.abs(distance)<15)&neutral
    repaired[old_blade]=0
    # Clear the far-tip afterglow/duplicated tip during recovery.
    if index>=43:
        repaired[(xx>ox+TARGET_LENGTH+18)&(np.abs(distance)<30)]=0
        flame[:]=0
    unit=np.array([1.0,slope],dtype=float); unit/=np.linalg.norm(unit)
    tip=np.array([ox,oy])+TARGET_LENGTH*unit
    source_origin=np.array([443.,317.])
    source_tip=np.array([578.,302.])
    src_angle=np.arctan2(*(source_tip-source_origin)[::-1])
    dst_angle=np.arctan2(unit[1],unit[0])
    theta=dst_angle-src_angle
    rot=np.array([[np.cos(theta),-np.sin(theta)],[np.sin(theta),np.cos(theta)]],dtype=np.float32)
    matrix=np.column_stack((rot,np.array([ox,oy])-rot@source_origin)).astype(np.float32)
    blade=cv2.warpAffine(donor,matrix,(1112,834),flags=cv2.INTER_LINEAR,borderMode=cv2.BORDER_CONSTANT,borderValue=(0,0,0,0))
    result=Image.alpha_composite(Image.fromarray(repaired,"RGBA"),Image.fromarray(blade,"RGBA"))
    if flame_mask.any() and index<43:
        factor=min(1.0,TARGET_LENGTH/max(1,tipx-ox))
        fm=np.float32([[factor,0,ox*(1-factor)],[slope*(factor-1),1,slope*ox*(1-factor)]])
        compressed=cv2.warpAffine(flame,fm,(1112,834),flags=cv2.INTER_LINEAR,borderMode=cv2.BORDER_CONSTANT,borderValue=(0,0,0,0))
        result=Image.alpha_composite(result,Image.fromarray(compressed,"RGBA"))
    return result,{"frame":index,"origin":[round(ox,2),round(oy,2)],"old_tip_x":tipx,"new_tip":tip.round(2).tolist(),"length":round(float(np.linalg.norm(tip-[ox,oy])),3)}

def main():
    paths=sorted(SOURCE.glob("buff_alpha_*.png"))
    if len(paths)!=60: raise RuntimeError("Expected 60 frames")
    FRAMES.mkdir(parents=True,exist_ok=True)
    first=np.array(Image.open(paths[0]).convert("RGBA"))
    donor=np.zeros_like(first)
    yy,xx=np.mgrid[:834,:1112]
    canonical_axis=317+(xx-443)*(-15/135)
    donor_mask=(xx>=443)&(xx<=584)&(np.abs(yy-canonical_axis)<8)
    donor[donor_mask]=first[donor_mask]
    frames=[]; report=[]
    for index,path in enumerate(paths,1):
        fixed,stats=repair(np.array(Image.open(path).convert("RGBA")),index,donor)
        # Use one common scale instead of changing artwork in the last 5 frames.
        premul=media.to_premultiplied(fixed)
        scale=.975
        matrix=np.float32([[scale,0,142*(1-scale)],[0,scale,685*(1-scale)]])
        warped=cv2.warpAffine(premul,matrix,(1112,834),flags=cv2.INTER_LINEAR,borderMode=cv2.BORDER_CONSTANT,borderValue=(0,0,0,0))
        image=media.to_straight(warped)
        image.save(FRAMES/f"buff_alpha_{index:03d}.png",optimize=True)
        frames.append(image); report.append(stats)
    for name,color in {"dark":(32,32,32,255),"white":(255,255,255,255)}.items():
        media.contact(frames,OUT/f"contact_{name}.png",color)
    dark=[Image.alpha_composite(Image.new("RGBA",im.size,(32,32,32,255)),im).convert("RGB") for im in frames]
    writer=cv2.VideoWriter(str(OUT/"buff_v4_preview_60fps.mp4"),cv2.VideoWriter_fourcc(*"mp4v"),60,(1112,834))
    if not writer.isOpened(): raise RuntimeError("MP4 writer failed")
    for im in dark: writer.write(cv2.cvtColor(np.asarray(im),cv2.COLOR_RGB2BGR))
    writer.release()
    gif=[im.resize((556,417),Image.Resampling.LANCZOS) for im in dark]
    palette=gif[0].quantize(colors=255)
    encoded=[im.quantize(palette=palette,dither=Image.Dither.NONE) for im in gif]
    encoded[0].save(OUT/"buff_v4_preview_60fps.gif",save_all=True,append_images=encoded[1:],duration=[20,20,10]*20,loop=0,disposal=2,optimize=False)
    (OUT/"sword_landmarks.json").write_text(json.dumps({"fps":60,"frames":60,"scale_all_frames":.975,"forced_idle_frames":False,"target_blade_length_before_scale":TARGET_LENGTH,"landmarks":report},indent=2),encoding="utf-8")
    print(OUT)

if __name__=="__main__": main()
