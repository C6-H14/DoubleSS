"""Shorten only distant fire, fading its truncated source edge to zero."""
from pathlib import Path
import json
import cv2
import numpy as np
from PIL import Image
from matte_alt_idle_v1 import matte
import bridge_alt_buff_v3 as media

ROOT=Path(r'D:\StSmod\art_preview\alternate_fencer_cast')
SOURCE=ROOT/'cast_v1_60f_60fps/frames_60fps'
OUT=ROOT/'cast_v2_fire_repaired_60f_60fps'
FRAMES=OUT/'alpha_frames_60fps'

def edge_mask(xx, yy):
    t=np.clip((xx-880)/220.,0,1)
    return 1-t*t*(3-2*t)

def repair(rgb):
    rgba=np.array(matte(cv2.cvtColor(rgb,cv2.COLOR_RGB2BGR)))
    h,w=rgb.shape[:2]
    yy,xx=np.mgrid[:h,:w]
    # Remove generated floor lighting outside the actual shoes.
    shoes=((xx>=110)&(xx<=235))|((xx>=440)&(xx<=590))
    rgba[(yy>=620)&~shoes]=0
    rgba[695:]=0
    # No body, hand or sword pixels reach this far-right upper corridor.
    region=(xx>=650)&(yy<470)
    effect=np.zeros_like(rgba)
    effect[region]=rgba[region]
    rgba[region]=0
    # Fade BEFORE compressing: the cut video edge becomes an invisible tip.
    fade=edge_mask(xx, yy)
    effect[:,:,3]=np.rint(effect[:,:,3]*fade).astype(np.uint8)
    premul=media.to_premultiplied(Image.fromarray(effect))
    matrix=np.float32([[.42,0,650*(1-.42)],[0,1,0]])
    shortened=cv2.warpAffine(premul,matrix,(w,h),flags=cv2.INTER_LINEAR,
                            borderMode=cv2.BORDER_CONSTANT,borderValue=(0,0,0,0))
    return Image.alpha_composite(Image.fromarray(rgba),media.to_straight(shortened))

def main():
    FRAMES.mkdir(parents=True,exist_ok=True)
    paths=sorted(SOURCE.glob('cast_*.png'))
    if len(paths)!=60: raise RuntimeError('Expected 60 frames')
    frames=[]
    for i,path in enumerate(paths,1):
        frame=repair(np.array(Image.open(path).convert('RGB')))
        frame.save(FRAMES/f'cast_alpha_{i:03d}.png',optimize=True)
        frames.append(frame)
    media.contact(frames,OUT/'contact_dark.png',(32,32,32,255))
    dark=[Image.alpha_composite(Image.new('RGBA',im.size,(32,32,32,255)),im).convert('RGB') for im in frames]
    writer=cv2.VideoWriter(str(OUT/'cast_v2_preview_60fps.mp4'),cv2.VideoWriter_fourcc(*'mp4v'),60,frames[0].size)
    if not writer.isOpened(): raise RuntimeError('MP4 writer failed')
    for im in dark: writer.write(cv2.cvtColor(np.array(im),cv2.COLOR_RGB2BGR))
    writer.release()
    small=[im.resize((556,417),Image.Resampling.LANCZOS) for im in dark]
    sheet=Image.new('RGB',(556*10,417*6))
    for i,im in enumerate(small): sheet.paste(im,((i%10)*556,(i//10)*417))
    palette=sheet.quantize(colors=255)
    gif=[im.quantize(palette=palette,dither=Image.Dither.NONE) for im in small]
    gif[0].save(OUT/'cast_v2_preview_60fps.gif',save_all=True,append_images=gif[1:],duration=[20,20,10]*20,loop=0,disposal=2,optimize=False)
    (OUT/'repair.json').write_text(json.dumps({'frames':60,'fps':60,'duration':1,'fire_anchor_x':650,'far_fire_scale_x':.42,'edge_fade_source_x':[880,1100],'jar_exported':False},indent=2))
    print(OUT)

if __name__=='__main__': main()
