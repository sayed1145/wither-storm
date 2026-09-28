"""Pixel regression from real GL screenshots. Run AFTER animation and animation2 captures."""
from pathlib import Path
from PIL import Image
import json
P=Path(__file__).resolve().parents[1]
def count(path,white=False):
 im=Image.open(path).convert('RGB');out=[0,0]
 for y in range(im.height):
  for x in range(im.width):
   r,g,b=im.getpixel((x,y))
   yes=min(r,g,b)>110 and max(r,g,b)-min(r,g,b)<35 if white else r>180 and g<40 and b>180
   if yes:out[x>=im.width//2]+=1
 return out
report={}
for name in ['animation','animation2']:
 d=P/'.cache/frames'/name
 behind=count(d/'occlusion-behind.png');front=count(d/'occlusion-front-control.png')
 back=count(d/'early-back-before-after.png',True);normal=count(d/'early-front-control.png',True);late=count(d/'late-back-before-after.png')
 assert behind[0]>100 and behind[1]==0,(name,'behind fixture',behind)
 assert front[1]>100 and abs(front[1]-front[0])<10,(name,'front control',front)
 assert back[0]>100 and back[1]==0,(name,'actual rear eyes',back)
 assert normal[1]>100,(name,'actual front eyes',normal)
 assert late[1]==0,(name,'mature rear',late)
 report[name]={'behindMarkerPixelsLegacyAndDepth':behind,'frontControlPixelsLegacyAndDepth':front,'earlyRearLightEyePixelsLegacyAndDepth':back,'earlyFrontEyePixelsLegacyAndDepth':normal,'lateRearPurplePixelsLegacyAndDepth':late,'passed':True}
(P/'preview/occlusion-pixels.json').write_text(json.dumps(report,indent=2));print(json.dumps(report,indent=2))
