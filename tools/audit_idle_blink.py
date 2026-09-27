from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(r"D:\StSmod\DoubleSS")
SRC = ROOT / "art_candidates" / "video_source" / "idle" / "short_blink_final_v5" / "alpha_frames"
OUT = ROOT / "art_candidates" / "video_source" / "idle" / "short_blink_final_v5"
OUT.mkdir(parents=True, exist_ok=True)

paths = sorted(SRC.glob("idle_*.png"))
picks = list(range(88, 124, 2))
box = (555, 115, 705, 260)
scale = 3
cw, ch = (box[2]-box[0])*scale, (box[3]-box[1])*scale
rows = (len(picks) + 3) // 4
sheet = Image.new("RGB", (cw*4, (ch+28)*rows), (45,45,45))
d = ImageDraw.Draw(sheet)
for n, i in enumerate(picks):
    frame = Image.open(paths[i]).convert("RGBA")
    view = Image.alpha_composite(Image.new("RGBA", frame.size, (45,45,45,255)), frame)
    crop = view.crop(box).resize((cw,ch), Image.Resampling.NEAREST).convert("RGB")
    x, y = (n%4)*cw, (n//4)*(ch+28)
    sheet.paste(crop, (x,y+28))
    d.text((x+5,y+5), f"frame {i+1:03d}  {i/60:.3f}s", fill="white")
sheet.save(OUT / "idle_short_blink_eye_audit.png", optimize=True)
