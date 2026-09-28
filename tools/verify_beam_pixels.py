from pathlib import Path
from PIL import Image
import json,math
root=Path(__file__).resolve().parents[1]
result={}
for mode in ['pixel','pixel2']:
 folder=root/'.cache/frames'/mode
 base=Image.open(folder/"pixel-0.png").convert("RGB")
 meta=json.loads((folder/'pixel-origin.json').read_text());distances=[]
 for frame in [1,2]:
  image=Image.open(folder/f'pixel-{frame}.png').convert('RGB')
  green=[]
  for y in range(image.height):
   for x in range(image.width):
    c=image.getpixel((x,y));b=base.getpixel((x,y));dr=c[0]-b[0];dg=c[1]-b[1];db=c[2]-b[2]
    if dg>50 and dg>2*abs(dr) and dg>2*abs(db):green.append((x,y))
  distances.append([min(math.hypot(x-p[0],y-p[1]) for x,y in green) for p in meta['points']])
 assert all(d>15 for d in distances[0]),distances
 assert all(d<3 for d in distances[1]),distances
 result[mode]={'oldLayerGapPixels':distances[0],'sharedDepthGapPixels':distances[1],'maxMeshToActualPurpleFaceWorldDistance':meta['maxGeometryDistance'],'passed':True,'diagnosticColor':'green; same installed beam geometry and actual imported head'}
(root/'preview/beam-pixels.json').write_text(json.dumps(result,indent=2));print(json.dumps(result,indent=2))
