from pathlib import Path
import cv2
from PIL import Image, ImageDraw
import numpy as np

root = Path(r'D:\StSmod\art_preview\video')
out = Path(r'D:\StSmod\art_preview\alternate_fencer_ultimate\segments_review')
out.mkdir(parents=True, exist_ok=True)
for tag in ('9341', '5636'):
    source = next(root.glob(f'*{tag}*.mp4'))
    cap = cv2.VideoCapture(str(source))
    fps = cap.get(cv2.CAP_PROP_FPS)
    frames = []
    while True:
        ok, bgr = cap.read()
        if not ok:
            break
        frames.append(Image.fromarray(cv2.cvtColor(bgr, cv2.COLOR_BGR2RGB)))
    cap.release()
    folder = out / tag
    folder.mkdir(exist_ok=True)
    for i, im in enumerate(frames):
        im.save(folder / f'frame_{i+1:03d}.png')
    for name, picks in [('overview', np.linspace(0, len(frames)-1, 20).round().astype(int)), ('ending', np.linspace(max(0,len(frames)-30),len(frames)-1,12).round().astype(int))]:
        rows = (len(picks)+3)//4
        sheet = Image.new('RGB', (1600, rows*325), '#202020')
        draw = ImageDraw.Draw(sheet)
        for cell, i in enumerate(picks):
            im = frames[i].copy()
            im.thumbnail((395,295))
            x,y = cell%4*400, cell//4*325
            sheet.paste(im,(x,y))
            draw.text((x+5,y+298),f'{tag} frame {i+1} / {len(frames)} {i/fps:.2f}s',fill='white')
        sheet.save(out/f'{tag}_{name}.png')
    print(tag, len(frames),fps,frames[0].size)
