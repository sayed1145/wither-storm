"""Convert the downloaded MIT-licensed Minecraft Java cube model, not a recreated lookalike."""
from pathlib import Path
import re,json,base64,uuid,hashlib
P=Path(__file__).resolve().parents[1];source=P/'third_party/witherbean/WitherStormHeadModel.java';text=source.read_text()
cubes=[]
for group in ['top','jaw']:
 match=re.search(r'PartDefinition '+group+r' = .*?CubeListBuilder.create\(\)(.*?), PartPose.offset\((.*?)\)\);',text,re.S)
 offset=[float(x.strip().rstrip('F')) for x in match[2].split(',')]
 for c in re.finditer(r'\.texOffs\((\d+), (\d+)\)\.addBox\((.*?), new CubeDeformation\((.*?)\)\)',match[1]):
  values=[float(x.strip().rstrip('F')) for x in c[3].split(',')];assert len(values)==6;assert float(c[4].strip().rstrip('F'))==0
  x,y,z=[values[i]+offset[i] for i in range(3)];w,h,d=values[3:];u,v=int(c[1]),int(c[2]);k=2.4
  box=[x*k,-(z+d+7)*k,-(y+h)*k,(x+w)*k,-(z+7)*k,-y*k]
  uv=[[u+d,v,w,d],[u+2*d+w,v+d,w,h],[u+d,v+d,w,h],[u+d+w,v+d,d,h],[u,v+d,d,h],[u+d+w,v,w,d]]
  cubes.append(dict(group=group,box=[round(a,5) for a in box],uv=uv,source=[x,y,z,w,h,d],uv_offset=[u,v],emissive=(u,v) in [(152,156),(0,158)]))
assert len(cubes)>30
(P/'assets/models/witherbean-head.json').write_text(json.dumps({'sourceCommit':'4db69ee473d88aa28aeb3d7dc6aed315d90e0d03','author':'Witherbean / TheCheesyChip','license':'MIT','sourceSHA256':hashlib.sha256(source.read_bytes()).hexdigest(),'scale':2.4,'cubes':cubes},indent=2))
rows=[]
for c in cubes:rows.append('        {'+','.join(str(float(v))+'f' for v in [0 if c['group']=='top' else 1,*c['box'],1 if c['emissive'] else 0,*sum(c['uv'],[])])+'}')
java='''package wstorm.model;
import arc.graphics.*;import arc.graphics.g2d.*;
import wstorm.g3d.*;import wstorm.g3d.Mesh;
/** Converted from Witherbean's actual downloaded MIT model. See bundled license and original source. */
public final class ImportedStormHead{
    public static final int materialBase=1000;
    private static Texture texture;private static TextureRegion[] regions;
    private static final float[][] boxes={
'''+',\n'.join(rows)+'''
    };
    public static int cubeCount(){return boxes.length;}
    public static void add(Rig rig,int upper,int jaw){
        for(int i=0;i<boxes.length;i++){float[] b=boxes[i];rig.part(b[0]==0?upper:jaw).style(b[7]==1?Mesh.emissive:0,materialBase+i*6).color(Color.white).box(b[1],b[2],b[3],b[4],b[5],b[6],true);}
    }
    public static TextureRegion region(int material,int face){
        if(regions==null){texture=new Texture(wstorm.WitherStormMod.file("models","witherbean-head.png"));texture.setFilter(Texture.TextureFilter.nearest);regions=new TextureRegion[boxes.length*6];
            for(int i=0;i<boxes.length;i++)for(int f=0;f<6;f++){float[] b=boxes[i];int o=8+f*4;regions[i*6+f]=new TextureRegion(texture,(int)b[o],(int)b[o+1],(int)b[o+2],(int)b[o+3]);}}
        int index=material-materialBase+face;return index>=0&&index<regions.length?regions[index]:null;
    }
}
'''
(P/'src/wstorm/model/ImportedStormHead.java').write_text(java)
elements=[];groups={g:[] for g in ['top','jaw']}
for i,c in enumerate(cubes):
 x,y,z,w,h,d=c['source'];id=str(uuid.uuid5(uuid.NAMESPACE_URL,'witherbean-head-cube-'+str(i)));groups[c['group']].append(id)
 # Explicit face UV rectangles, original texture dimensions and named editable jaw groups.
 names=['up','south','north','east','west','down']
 faces={name:{'uv':[uv[0],uv[1],uv[0]+uv[2],uv[1]+uv[3]],'texture':0} for name,uv in zip(names,c['uv'])}
 elements.append({'name':c['group']+'_'+str(i),'type':'cube','uuid':id,'from':[x,-y-h,z+7],'to':[x+w,-y,z+d+7],'origin':[0,0,0],'uv_offset':c['uv_offset'],'box_uv':False,'faces':faces})
model={'meta':{'format_version':'4.10','model_format':'free','box_uv':False},'name':'Witherbean Storm Head (converted Java model)','resolution':{'width':160,'height':160},'elements':elements,'outliner':[{'name':g,'origin':[0,0,0],'uuid':str(uuid.uuid5(uuid.NAMESPACE_URL,'witherbean-'+g)),'children':ids} for g,ids in groups.items()],'textures':[{'path':'wither_storm_head.png','name':'wither_storm_head.png','id':'0','uuid':str(uuid.uuid5(uuid.NAMESPACE_URL,'witherbean-texture')),'source':'data:image/png;base64,'+base64.b64encode((P/'third_party/witherbean/wither_storm_head.png').read_bytes()).decode()}]}
(P/'third_party/witherbean/Witherbean_Head_converted.bbmodel').write_text(json.dumps(model))
print('Imported',len(cubes),'cubes;',sum(c['group']=='top' for c in cubes),'upper,',sum(c['group']=='jaw' for c in cubes),'jaw')
