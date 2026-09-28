package wstorm.gfx;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.ctype.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import wstorm.g3d.*;
import wstorm.g3d.Mesh;
import wstorm.model.*;
import wstorm.world.*;

/** Three-dimensional tractor volumes and original storm phenomena. */
public final class StormVisual{
    private static final float[] eye=new float[3],v=new float[3],axis=new float[3];
    private static final IntMap<BeamRig> beams=new IntMap<>();
    private static final Seq<Meal> meals=new Seq<>();
    private static final IntSet seen=new IntSet();
    public static long beamFaces,ghostsDrawn,packetsAccepted;
    private static class Meal{int owner,head,kind,content,key,victim;float x,y,born;Rig rig;UnitRenderer renderer;}
    private static class BeamRig{
        Rig rig=new Rig();UnitRenderer renderer=new UnitRenderer(130);Mesh[] mesh=new Mesh[3];boolean[] active=new boolean[3];
        BeamRig(){rig.bone(-1,0,0,0);for(int i=0;i<3;i++){int b=rig.bone(0,0,0,0);mesh[i]=rig.part(b);}rig.finish();}
    }
    /** Defensive parser for a fixed data-only server visual message; replica removal is accepted only through the server-to-client handler. */
    public static void receive(String packet){
        if(Vars.headless||packet==null||packet.length()>180)return;
        try{
            String[] a=packet.split(";",-1);if(a.length!=8)return;
            Meal m=new Meal();m.owner=Integer.parseInt(a[0]);m.head=Integer.parseInt(a[1]);m.kind=Integer.parseInt(a[2]);m.content=Integer.parseInt(a[3]);
            m.x=Float.parseFloat(a[4]);m.y=Float.parseFloat(a[5]);m.born=Float.parseFloat(a[6]);m.victim=Integer.parseInt(a[7]);
            if(m.head<0||m.head>2||m.kind<1||m.kind>2||m.content<0||m.content>32767||!Float.isFinite(m.x)||!Float.isFinite(m.y)||!Float.isFinite(m.born))return;
            if(Math.abs(m.x)>1e6f||Math.abs(m.y)>1e6f)return;
            if(Vars.net.client()&&m.kind==1&&m.victim>=0){
                Unit prey=Groups.unit.getByID(m.victim);
                if(prey!=null&&prey.type.id==m.content){prey.remove();}
                Vars.netClient.addRemovedEntity(m.victim);
            }
            if(m.kind==2){
                Block block=Vars.content.block(m.content);if(block==null)return;
                m.rig=new Rig();m.rig.bone(-1,0,0,0);float r=block.size*4f;
                m.rig.part(0).style(0,20000+m.content).color(Color.white).box(-r,-r,-r,r,r,r,true);
                m.rig.finish();m.renderer=new UnitRenderer(130);m.renderer.faceSort=true;
            }
            wstorm.model.StormModel ownerModel=wstorm.StormContent.storm.models.get(m.owner);
            if(ownerModel!=null)ownerModel.reactMeal(m.head,m.born+(m.kind==2?95:0));
            m.key=packet.hashCode();if(!seen.add(m.key))return;
            if(seen.size>1024)seen.clear();
            if(meals.size>=384)meals.remove(0);meals.add(m);packetsAccepted++;
        }catch(RuntimeException ignored){/* malformed cosmetics are simply ignored */}
    }
    public static void vanish(String text){
        if(Vars.headless||text==null||text.length()>100)return;
        try{
            String[] a=text.split(";");if(a.length!=4)return;
            int id=Integer.parseInt(a[0]);float x=Float.parseFloat(a[1]),y=Float.parseFloat(a[2]),g=Float.parseFloat(a[3]);
            if(id<0||!Float.isFinite(x)||!Float.isFinite(y)||!Float.isFinite(g)||g<0||g>1e5f)return;
            Unit unit=Groups.unit.getByID(id);
            if(unit instanceof StormUnit){unit.dead=true;unit.remove();}
            Vars.netClient.addRemovedEntity(id);StormFx.fracture.at(x,y,g,StormFx.purple);StormAudio.ruptureAt(x,y);
        }catch(RuntimeException ignored){}
    }
    public static void forget(int id){beams.remove(id);meals.removeAll(m->m.owner==id);}
    public static void clear(){beams.clear();meals.clear();seen.clear();}
    public static void ground(StormUnit s,StormModel model){
        // Restore every persisted in-flight block, not just the first one.
        for(int i=0;i<StormUnit.maxBuildingSlots;i++)if(s.flightTime(i)>0){
            float x=s.flightOriginX(i),y=s.flightOriginY(i);
            if(!meals.contains(q->q.owner==s.id&&q.kind==2&&q.x==x&&q.y==y))receive(s.id+";"+(i%3)+";2;"+s.flightContent(i)+";"+x+";"+y+";"+(s.age-(95-s.flightTime(i)))+";-1");
        }
        float g=model.scale,age=s.age;
        Draw.color(.035f,.014f,.065f,.36f);Fill.poly(s.x,s.y+18*g,40,42*g,0);
        Draw.color(.012f,.007f,.019f,.18f);Fill.poly(s.x,s.y+18*g,40,60*g,0);
        if(Core.settings.getBool("ws-atmosphere",true)){
            for(int i=0;i<12;i++){
                float a=i*137.5f+age*.14f,r=35*g+(i%4)*15*g;
                Draw.color(.08f,.028f,.12f,.055f);Fill.circle(s.x+Mathf.cosDeg(a)*r,s.y+Mathf.sinDeg(a)*r,25*g+(i%3)*6*g);
            }

        }
        Draw.reset();
    }
    public static void beamMesh(Mesh mesh,float[] origin,float ex,float ey,float spread,float scale){
        float dx=ex-origin[0],dy=ey-origin[1],dz=-origin[2],length=(float)Math.sqrt(dx*dx+dy*dy+dz*dz);
        mesh.faces=mesh.verts=0;mesh.style(Mesh.emissive,0).color(.52f,.16f,.98f).axis(origin[0],origin[1],origin[2],ex,ey,0).lathe(10,0,.85f*scale,0,spread,length);
    }
    public static void beams(StormUnit s,StormModel model){
        BeamRig fx=beams.get(s.id);if(fx==null){fx=new BeamRig();beams.put(s.id,fx);}
        boolean[] active=fx.active;
        for(int h=0;h<3;h++){
            Mesh m=fx.mesh[h];m.faces=0;m.verts=0;m.at(0,0,0);
            active[h]=model.beamVisible(s,h);
            if(!active[h])continue;
            model.eye(h,eye);
            float ex=s.targetX[h]-s.x,ey=s.targetY[h]-s.y;
            float dx=ex-eye[0],dy=ey-eye[1],dz=-eye[2];float length=(float)Math.sqrt(dx*dx+dy*dy+dz*dz);
            if(length<1)continue;
            float spread=13+model.scale*4+Math.min(6,s.locked[h]*.011f);
            beamMesh(m,eye,ex,ey,spread,model.scale);
            Draw.blend(Blending.additive);Draw.color(StormFx.purple,.14f);Fill.poly(s.targetX[h],s.targetY[h],18,spread*1.25f,s.age);
            Lines.stroke(.7f);Draw.color(StormFx.pale,.34f);Lines.poly(s.targetX[h],s.targetY[h],12,spread*.80f,-s.age*.7f);
            // Rising particulate spirals track the same 3D perspective as the actual beam.
            for(int i=0;i<14;i++){
                float t=(i/14f+s.age/(105+i%3*13f))%1f;
                float swirl=s.age*2.6f+i*131.5f,r=spread*(1-t)*.66f;
                v[0]=Mathf.lerp(ex,eye[0],t)+Mathf.cosDeg(swirl)*r;
                v[1]=Mathf.lerp(ey,eye[1],t)+Mathf.sinDeg(swirl)*r;
                v[2]=eye[2]*t;
                model.project(v);
                Draw.color(i%4==0?StormFx.pale:StormFx.purple,.25f+.55f*t);
                Fill.square(s.x+v[0],s.y+v[1],(1.1f+(i%3)*.4f)*(1-.55f*t),swirl);
            }
            Draw.blend();
        }
        fx.rig.finish();
        for(int h=0;h<3;h++)fx.rig.alpha[h+1]=active[h]?.235f:0;
        fx.renderer.cam=model.renderer.cam;fx.renderer.pose(fx.rig,90,0,0,0,1);
        Draw.reset();
    }
    public static void drawBody(StormUnit s,StormModel model){
        BeamRig fx=beams.get(s.id);long before=fx==null?0:fx.renderer.quads;
        model.renderer.draw(model.rig,s.x,s.y,fx==null?null:fx.renderer,fx==null?null:fx.rig);
        if(fx!=null)beamFaces+=fx.renderer.quads-before;
    }
    public static void front(StormUnit s,StormModel model){
        // Eye luminosity is part of the depth-tested head surface, never a foreground billboard.
        Draw.blend(Blending.additive);
        // Branching corona bolts: deterministic, brief, violet rather than generic lightning FX.
        float flash=s.age%113;
        if(s.mature()&&flash<13&&Core.settings.getBool("ws-atmosphere",true)){
            Draw.color(StormFx.purple,(1-flash/13)*.72f);Lines.stroke(1.2f*model.scale);
            for(int bolt=0;bolt<3;bolt++){
                float angle=bolt*119+s.genome%80;
                float px=s.x,py=s.y+55*model.scale;
                for(int j=1;j<=6;j++){
                    float r=j*11*model.scale;
                    float nx=s.x+Mathf.cosDeg(angle)*r+Mathf.sin(j*4.73f+bolt)*5*model.scale;
                    float ny=s.y+55*model.scale+Mathf.sinDeg(angle)*r;
                    Lines.line(px,py,nx,ny);px=nx;py=ny;
                }
            }
        }
        Draw.blend();
        // Real swallowed targets retain their icon while being lifted, rolled and dissolved.
        for(int i=meals.size-1;i>=0;i--){
            Meal m=meals.get(i);Unit owner=Groups.unit.getByID(m.owner);
            if(!(owner instanceof StormUnit live)||live.age-m.born>(m.kind==2?95:18)){meals.remove(i);continue;}
            if(m.owner!=s.id)continue;
            float t=Mathf.clamp((s.age-m.born)/(m.kind==2?95f:18f));float p=t*t*(3-2*t);
            model.mouth(m.head,eye);float ox=m.x-s.x,oy=m.y-s.y;
            float whirl=(1-p)*p*24*model.scale,angle=t*390+m.content*31;
            v[0]=Mathf.lerp(ox,eye[0],p)+Mathf.cosDeg(angle)*whirl;
            v[1]=Mathf.lerp(oy,eye[1],p)+Mathf.sinDeg(angle)*whirl;
            v[2]=eye[2]*(float)Math.sqrt(p);
            if(m.kind==2){
                float visible=Math.max(0,1-t*t*t*t),shrink=1-.86f*p;
                m.rig.reset();m.rig.euler(0,t*150,t*210,t*75);m.rig.alpha[0]=visible;
                m.renderer.cam=model.renderer.cam;m.renderer.pose(m.rig,90,v[0],v[1],v[2],shrink);m.renderer.draw(m.rig,s.x,s.y);ghostsDrawn++;continue;
            }
            model.project(v);
            TextureRegion icon;float size;
            if(m.kind==1){UnitType type=Vars.content.unit(m.content);if(type==null)continue;icon=type.fullIcon;size=type.hitSize*2f;}
            else{Block block=Vars.content.block(m.content);if(block==null)continue;icon=block.fullIcon;size=block.size*8f;}
            float visible=Math.max(.04f,1-t*t);float x=s.x+v[0],y=s.y+v[1];
            Draw.color(1,1,1,visible);Draw.mixcol(StormFx.purple,.35f+.55f*t);
            Draw.rect(icon,x,y,size*(1-.75f*t),size*(1-.75f*t),t*260+m.content*9);
            Draw.mixcol();Draw.blend(Blending.additive);Draw.color(StormFx.purple,.32f*visible);Lines.stroke(.8f);Lines.square(x,y,size*.6f*(1-.7f*t),t*260+m.content*9);Draw.blend();ghostsDrawn++;
        }
        // Exposed command heart is visible below the central jaw, with its own charge halo.
        if(s.exposed()){
            Draw.blend(Blending.additive);Draw.color(StormFx.pale,.45f);Lines.stroke(1.1f);
            Lines.poly(s.x,s.y+20*model.scale,4,10*model.scale,s.age*.8f);Draw.blend();
        }
        Draw.reset();
    }
}
