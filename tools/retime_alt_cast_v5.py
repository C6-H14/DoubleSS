"""Halve preparation frames 1-33; preserve release and recovery timing."""
from pathlib import Path
import shutil,json
import cv2
import numpy as np
from PIL import Image

ROOT=Path(r'D:\StSmod\art_preview\alternate_fencer_cast')
SOURCE=ROOT/'cast_v4_arc_fade_60f_60fps/alpha_frames_60fps'
OUT=ROOT/'cast_v5_short_windup_44f_60fps'
FRAMES=OUT/'alpha_frames_60fps'
SELECTED=list(range(1,34,2))+list(range(34,61))

if __name__=='__main__':
    FRAMES.mkdir(parents=True,exist_ok=True)
    images=[]
    for i,n in enumerate(SELECTED,1):
        path=SOURCE/f'cast_alpha_{n:03d}.png'
        shutil.copy2(path,FRAMES/f'cast_alpha_{i:03d}.png')
        im=Image.open(path).convert('RGBA')
        images.append(Image.alpha_composite(Image.new('RGBA',im.size,(32,32,32,255)),im).convert('RGB'))
    writer=cv2.VideoWriter(str(OUT/'cast_v5_preview_60fps.mp4'),cv2.VideoWriter_fourcc(*'mp4v'),60,images[0].size)
    if not writer.isOpened(): raise RuntimeError('MP4 writer failed')
    for im in images: writer.write(cv2.cvtColor(np.array(im),cv2.COLOR_RGB2BGR))
    writer.release()
    small=[im.resize((556,417),Image.Resampling.LANCZOS) for im in images]
    sheet=Image.new('RGB',(556*8,417*6))
    for i,im in enumerate(small): sheet.paste(im,((i%8)*556,(i//8)*417))
    palette=sheet.quantize(colors=255)
    gif=[im.quantize(palette=palette,dither=Image.Dither.NONE) for im in small]
    duration=[int(round((i+1)*100/60)-round(i*100/60))*10 for i in range(len(gif))]
    gif[0].save(OUT/'cast_v5_preview_60fps.gif',save_all=True,append_images=gif[1:],duration=duration,loop=0,disposal=2,optimize=False)
    (OUT/'frame_map.json').write_text(json.dumps({'fps':60,'frames':len(SELECTED),'duration':len(SELECTED)/60,'source_frame_numbers':SELECTED,'windup_old_frames':33,'windup_new_frames':17,'jar_exported':False},indent=2))
    print(f'{len(SELECTED)} frames, {len(SELECTED)/60:.3f}s; GIF {sum(duration)}ms')
