from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(r"D:\StSmod\DoubleSS\art_candidates\matting_tests\matanyone_v14_three\final_rgba")
PICKS = [
    ("attack_heavy", 1), ("attack_heavy", 7), ("attack_heavy", 20),
    ("cast_debuff", 1), ("cast_debuff", 20), ("cast_debuff", 24),
    ("cast_buff", 1), ("cast_buff", 31), ("cast_buff", 43),
]

def main():
    tiles=[]
    for name,index in PICKS:
        rgba=Image.open(ROOT/name/f"{name}_{index:03d}.png").convert("RGBA")
        black=Image.alpha_composite(Image.new("RGBA",rgba.size,"black"),rgba).convert("RGB")
        # Crop loosely around all visible pixels, retaining enough dark margin
        # to make even a one-pixel coloured fringe obvious.
        box=rgba.getchannel("A").getbbox()
        x0,y0,x1,y1=box; pad=30
        box=(max(0,x0-pad),max(0,y0-pad),min(rgba.width,x1+pad),min(rgba.height,y1+pad))
        crop=black.crop(box); crop.thumbnail((520,430),Image.Resampling.LANCZOS)
        tile=Image.new("RGB",(540,470),"black"); tile.paste(crop,((540-crop.width)//2,10))
        ImageDraw.Draw(tile).text((8,447),f"{name} {index:03d}",fill="white")
        tiles.append(tile)
    sheet=Image.new("RGB",(1620,1410),"black")
    for i,t in enumerate(tiles): sheet.paste(t,((i%3)*540,(i//3)*470))
    out=ROOT.parent/"edge_zoom_black_v2.jpg"; sheet.save(out,quality=97); print(out)
if __name__=="__main__": main()
