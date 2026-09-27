from PIL import Image,ImageDraw
p=r'D:\StSmod\DoubleSS\art_candidates\video_source\death_splice_analysis\death_splice_v11_alpha_refined_v2\alpha_frames\death_0022.png'
im=Image.open(p).convert('RGBA').crop((520,240,730,400)).resize((840,640),Image.Resampling.NEAREST)
bg=Image.new('RGBA',im.size,(40,40,40,255));im=Image.alpha_composite(bg,im);d=ImageDraw.Draw(im)
for x in range(0,211,20):
    X=x*4;d.line((X,0,X,640),fill=(255,0,0,150),width=1);d.text((X+2,2),str(520+x),fill='white')
for y in range(0,161,20):
    Y=y*4;d.line((0,Y,840,Y),fill=(255,0,0,150),width=1);d.text((2,Y+2),str(240+y),fill='white')
im.convert('RGB').save(r'D:\StSmod\DoubleSS\art_candidates\video_source\death_splice_analysis\frame22_chest_grid.png')
