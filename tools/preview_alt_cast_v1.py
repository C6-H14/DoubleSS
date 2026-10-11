"""Review the new clenched-fire cast, without modifying runtime assets."""
from pathlib import Path
import retime_alt_buff_v1 as preview
from PIL import Image

preview.SOURCE = next(Path(r'D:\StSmod\art_preview\video').glob('jimeng-2026-09-28-1625-*.mp4'))
preview.OUT = Path(r'D:\StSmod\art_preview\alternate_fencer_cast\cast_v1_60f_60fps')
preview.FRAMES = preview.OUT / 'frames_60fps'
preview.NORMALIZE_SCALE = False
preview.BASENAME = 'cast_v1_60f_60fps'
preview.SELECTED = (preview.phase(3,23,14) + preview.phase(24,51,18)
                    + preview.phase(52,70,12) + preview.phase(71,93,16))

if __name__ == '__main__':
    preview.main()
    paths = sorted(preview.FRAMES.glob('buff_*.png'))
    for index,path in enumerate(paths,1):
        path.rename(preview.FRAMES / f'cast_{index:03d}.png')
    images = [Image.open(path).convert('RGB').resize((556,417),Image.Resampling.LANCZOS)
              for path in sorted(preview.FRAMES.glob('cast_*.png'))]
    # Derive a common palette from all phases, including the bright fire.
    sheet = Image.new('RGB',(556*10,417*6))
    for index,image in enumerate(images):
        sheet.paste(image,((index%10)*556,(index//10)*417))
    palette = sheet.quantize(colors=255)
    frames = [image.quantize(palette=palette,dither=Image.Dither.NONE) for image in images]
    frames[0].save(preview.OUT / f'{preview.BASENAME}.gif',save_all=True,
                   append_images=frames[1:],duration=[20,20,10]*20,
                   loop=0,disposal=2,optimize=False)
