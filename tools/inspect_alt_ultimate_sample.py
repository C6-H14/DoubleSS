"""Read sample footage and export diagnostic sheets only."""
from pathlib import Path
import cv2,numpy as np
from PIL import Image,ImageDraw

ROOT=Path(r'D:\StSmod\art_preview\video')
SOURCE=next(ROOT.glob('jimeng-2026-09-28-6284-*.mp4'))
OUT=Path(r'D:\StSmod\art_preview\alternate_fencer_ultimate/sample_6284_review')

if __name__=='__main__':
    OUT.mkdir(parents=True,exist_ok=True)
    cap=cv2.VideoCapture(str(SOURCE)); fps=cap.get(cv2.CAP_PROP_FPS)
    frames=[]
    while True:
        ok,bgr=cap.read()
        if not ok: break
        frames.append(Image.fromarray(cv2.cvtColor(bgr,cv2.COLOR_BGR2RGB)))
    cap.release()
    print('frames',len(frames),'fps',fps,'size',frames[0].size)
    picks=np.linspace(0,len(frames)-1,24).round().astype(int)
    sheet=Image.new('RGB',(4*480,6*290),(25,25,25)); draw=ImageDraw.Draw(sheet)
    for cell,index in enumerate(picks):
        im=frames[index].copy(); im.thumbnail((470,265))
        x,y=(cell%4)*480,(cell//4)*290
        sheet.paste(im,(x,y)); draw.text((x+4,y+266),f'{index+1} / {len(frames)}   {index/fps:.2f}s',fill='white')
    sheet.save(OUT/'overview.png')
    for i in picks: frames[i].save(OUT/f'sample_{i+1:03d}.png')
    dense=Image.new('RGB',(4*480,4*290),(25,25,25)); detail=ImageDraw.Draw(dense)
    for cell,index in enumerate(range(56,72)):
        im=frames[index].copy(); im.thumbnail((470,265))
        x,y=(cell%4)*480,(cell//4)*290
        dense.paste(im,(x,y)); detail.text((x+4,y+266),f'frame {index+1}',fill='white')
    dense.save(OUT/'launch_sequence.png')
    print(OUT)
