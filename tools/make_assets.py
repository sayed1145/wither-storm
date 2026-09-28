#!/usr/bin/env python3
"""Original UI pixel art and synthesized effects. Runtime textures are Java-generated."""
from pathlib import Path
from PIL import Image,ImageDraw
import math,random,wave,struct
ROOT=Path(__file__).resolve().parents[1];p=ROOT/'assets/sprites';p.mkdir(parents=True,exist_ok=True)
im=Image.new('RGBA',(128,128));d=ImageDraw.Draw(im)
d.ellipse((20,18,111,93),fill='#21152c',outline='#533c66',width=3)
for i in range(8):
 x=10+i*15;d.line([(63,65),(x,85),(x+8,110),(x-3,123)],fill='#25182f',width=6)
for x,y,w,h in ((12,36,32,29),(83,36,32,29),(42,52,44,38)):
 d.rectangle((x,y,x+w,y+h),fill='#18131f',outline='#554363',width=2)
 d.rectangle((x+w*.4,y+5,x+w*.6,y+15),fill='#ad51ff');d.rectangle((x+w*.47,y+7,x+w*.53,y+13),fill='#f6d2ff')
 d.rectangle((x+4,y+h-11,x+w-4,y+h-3),fill='#07050b')
 for j in range(6):
  xx=x+5+j*(w-9)/6;d.rectangle((xx,y+h-10,xx+2,y+h-8),fill='#d8d0e1');d.rectangle((xx,y+h-5,xx+2,y+h-3),fill='#d8d0e1')
im.save(p/'wither-storm.png');im.save(ROOT/'icon.png')
im=Image.new('RGBA',(128,128));d=ImageDraw.Draw(im)
d.polygon([(64,8),(118,38),(118,88),(64,119),(10,88),(10,38)],fill='#332440',outline='#766181',width=3)
d.polygon([(64,12),(114,40),(64,69),(14,40)],fill='#514061')
for x,y in ((27,38),(90,38),(27,81),(90,81)):
 d.rectangle((x-6,y-12,x+6,y+9),fill='#181220',outline='#876992',width=2);d.rectangle((x-4,y-14,x+4,y-10),fill='#b46aff')
d.polygon([(64,27),(80,37),(80,57),(64,68),(48,57),(48,37)],fill='#c4793c',outline='#ebbb79',width=2)
for x,y in ((55,45),(65,51),(72,40)):d.rectangle((x,y,x+3,y+3),fill='#fae8a9')
im.save(p/'command-altar.png')
im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im);d.rectangle((9,3,22,23),fill='#8a3fd2');d.rectangle((12,5,19,18),fill='#edbaff');d.line((8,25,15,20,23,28),fill='#c186e8',width=3);im.save(p/'wither-sickness.png')
out=ROOT/'assets/sounds';out.mkdir(exist_ok=True)
for name,duration,seed in [('storm-roar',2.2,831),('storm-rupture',1.4,451)]:
 rng=random.Random(seed);rate=22050;data=bytearray();phase=0.;filtered=0.
 for i in range(int(duration*rate)):
  t=i/rate;q=t/duration;freq=(74-37*q) if name=='storm-roar' else (125-94*q)
  phase+=2*math.pi*freq/rate;filtered=.94*filtered+.06*rng.uniform(-1,1)
  env=min(1,t/.035)*(1-q)**1.5;wobble=.68+.32*math.sin(2*math.pi*(7-2*q)*t)
  raw=(.42*math.sin(phase)+.19*math.sin(phase*1.503)+.12*math.sin(phase*2.001)+filtered*.8)*env*wobble
  data+=struct.pack('<h',int(max(-.95,min(.95,raw))*32767))
 with wave.open(str(out/(name+'.wav')),'wb') as w:w.setparams((1,2,rate,0,'NONE','not compressed'));w.writeframes(data)
print('Rebuilt UI sprites and original synthetic WAV effects. No downloaded textures.')
