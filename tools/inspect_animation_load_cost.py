"""Read-only atlas sizing diagnostic; no runtime changes."""
from pathlib import Path
import re,json

root=Path(r'D:\StSmod\DoubleSS\src\main\resources\img\char')
for form in ['spine','spine_inner']:
    folder=root/form
    text=(folder/'character.atlas').read_text()
    sizes=re.findall(r'character(?:[0-9]+)?\.png\s+size: (\d+),\s*(\d+)',text)
    pixels=sum(int(w)*int(h) for w,h in sizes)
    pages=list(folder.glob('character*.png'))
    print(form,'pages=',len(sizes),'RGBA_MiB=',round(pixels*4/1048576,1),
          'PNG_MiB=',round(sum(p.stat().st_size for p in pages)/1048576,1))
    data=json.loads((folder/'character.json').read_text())
    print('animations:',list(data['animations']))
