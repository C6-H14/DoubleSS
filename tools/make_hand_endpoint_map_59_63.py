from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(r"D:\StSmod\DoubleSS\art_candidates\video_source\death_splice_analysis")
SRC = ROOT / "death_splice_frames_review_v15_real_frames_no_bridges"
OUT = ROOT / "death_hand_endpoint_map_59_63.png"

f59 = Image.open(SRC / "frame_0059.png").convert("RGB")
f63 = Image.open(SRC / "frame_0063.png").convert("RGB")

# Free-hand landmarks measured on the original 1120x832 frames.
# wrist = sleeve/skin junction center; palm = visual center of the hand.
p59 = {"wrist": (706, 584), "palm": (713, 621)}
p63 = {"wrist": (724, 548), "palm": (741, 585)}

crop_box = (600, 430, 820, 700)
cw, ch = crop_box[2] - crop_box[0], crop_box[3] - crop_box[1]
scale = 2
panel_w, panel_h = cw * scale, ch * scale
margin, title_h = 24, 62
canvas = Image.new("RGB", (panel_w * 3 + margin * 4, panel_h + title_h + margin * 2), (28, 28, 28))
d = ImageDraw.Draw(canvas)

def local(pt):
    return ((pt[0] - crop_box[0]) * scale, (pt[1] - crop_box[1]) * scale)

def mark(draw, xy, color, label):
    x, y = xy
    r = 9
    draw.ellipse((x-r, y-r, x+r, y+r), fill=color, outline=(0,0,0), width=3)
    draw.text((x+12, y-16), label, fill=color, stroke_width=2, stroke_fill=(0,0,0))

def panel(frame, x0, title, pts, color):
    crop = frame.crop(crop_box).resize((panel_w, panel_h), Image.Resampling.NEAREST)
    canvas.paste(crop, (x0, title_h + margin))
    pd = ImageDraw.Draw(canvas)
    pd.text((x0, 20), title, fill=color, stroke_width=2, stroke_fill=(0,0,0))
    ox, oy = x0, title_h + margin
    mark(pd, (ox+local(pts["wrist"])[0], oy+local(pts["wrist"])[1]), color, "WRIST")
    mark(pd, (ox+local(pts["palm"])[0], oy+local(pts["palm"])[1]), color, "PALM")

x1 = margin
x2 = margin * 2 + panel_w
x3 = margin * 3 + panel_w * 2
panel(f59, x1, "FRAME 59 / RED", p59, (255, 55, 55))
panel(f63, x2, "FRAME 63 / GREEN", p63, (45, 255, 100))

# Third panel: use frame59 as the plate and plot both endpoint sets plus the exact straight segments.
crop = f59.crop(crop_box).resize((panel_w, panel_h), Image.Resampling.NEAREST)
canvas.paste(crop, (x3, title_h + margin))
d = ImageDraw.Draw(canvas)
d.text((x3, 20), "STRAIGHT-LINE INTERPOLATION", fill=(255,255,255), stroke_width=2, stroke_fill=(0,0,0))
ox, oy = x3, title_h + margin
for key, suffix in (("wrist", "W"), ("palm", "P")):
    a = local(p59[key]); b = local(p63[key])
    aa = (ox+a[0], oy+a[1]); bb = (ox+b[0], oy+b[1])
    d.line((aa, bb), fill=(255, 230, 40), width=5)
    mark(d, aa, (255,55,55), f"59{suffix}")
    mark(d, bb, (45,255,100), f"63{suffix}")
    # Exact 25/50/75 percent targets on the endpoint line.
    for i, t in enumerate((.25, .50, .75), start=60):
        q = (round(aa[0] + (bb[0]-aa[0])*t), round(aa[1] + (bb[1]-aa[1])*t))
        r = 6
        d.rectangle((q[0]-r, q[1]-r, q[0]+r, q[1]+r), fill=(60,180,255), outline=(0,0,0), width=2)
        if key == "palm":
            d.text((q[0]+8, q[1]+4), str(i), fill=(60,180,255), stroke_width=2, stroke_fill=(0,0,0))

canvas.save(OUT)
print(OUT)
print("59", p59)
print("63", p63)
