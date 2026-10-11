from pathlib import Path
from PIL import Image, ImageDraw
import json

p = Path(r'D:\StSmod\art_preview\alternate_fencer_death\death_v1')
indices = [239,241,243,245,248,251,430,490,509,521]
sheet = Image.new('RGB',(1250,460))
draw = ImageDraw.Draw(sheet)
for k,i in enumerate(indices):
    im = Image.open(p/'frames_60fps'/f'death_{i:04d}.png')
    im.thumbnail((250,205))
    x,y = (k%5)*250,(k//5)*230
    sheet.paste(im,(x,y+22))
    draw.text((x+5,y+4),f'frame {i}',fill='white')
sheet.save(p/'seam_tail_check.png')
print(json.loads((p/'source_report.json').read_text())[-1])
