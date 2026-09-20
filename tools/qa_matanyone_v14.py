from pathlib import Path
import cv2
import numpy as np
from PIL import Image, ImageDraw

ROOT = Path(r"D:\StSmod\DoubleSS")
C = ROOT / "art_candidates" / "video_source" / "new_animation_candidates_v1"
W = ROOT / "art_candidates" / "matting_tests" / "matanyone_v14_three"
SETS = {
    "attack_heavy": (C / "attack_heavy" / "pink_frames", "attack_heavy_*.png"),
    "cast_debuff": (C / "cast_debuff" / "pink_frames", "cast_debuff_*.png"),
    "cast_buff": (C / "cast_buff_transition_test_v6" / "pink_frames", "cast_buff_*.png"),
}

def main():
    tiles=[]
    for name,(folder,pattern) in SETS.items():
        src=sorted(folder.glob(pattern))
        pha_dirs=list((W/name/"output").glob("*/pha"))
        if len(pha_dirs)!=1: raise RuntimeError((name,pha_dirs))
        phas=sorted(pha_dirs[0].glob("*.png"))
        for idx in np.linspace(0,len(src)-1,min(12,len(src))).round().astype(int):
            rgb=np.asarray(Image.open(src[idx]).convert("RGB"))
            a=cv2.imread(str(phas[idx]),cv2.IMREAD_GRAYSCALE)
            a=cv2.resize(a,(rgb.shape[1],rgb.shape[0]),interpolation=cv2.INTER_LINEAR)
            rgba=np.dstack((rgb,a))
            im=Image.fromarray(rgba,"RGBA").resize((280,210),Image.Resampling.LANCZOS)
            black=Image.alpha_composite(Image.new("RGBA",im.size,"black"),im).convert("RGB")
            white=Image.alpha_composite(Image.new("RGBA",im.size,"white"),im).convert("RGB")
            tile=Image.new("RGB",(280,444),(32,32,32)); tile.paste(black,(0,0)); tile.paste(white,(0,210))
            ImageDraw.Draw(tile).text((5,423),f"{name} {idx+1:03d}",fill="white")
            tiles.append(tile)
    cols=6; rows=(len(tiles)+cols-1)//cols
    sheet=Image.new("RGB",(cols*280,rows*444),(20,20,20))
    for i,t in enumerate(tiles): sheet.paste(t,((i%cols)*280,(i//cols)*444))
    out=W/"semantic_alpha_black_white.jpg"; sheet.save(out,quality=94); print(out)
if __name__=="__main__": main()
