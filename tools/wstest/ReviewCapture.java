package wstest;
import arc.*;import arc.graphics.*;import arc.graphics.g2d.*;import mindustry.game.EventType.*;
import wstorm.model.*;import wstorm.g3d.Cam;
/** Direct runtime renderer audit: closeups, four camera-relative angles, continuous mass sweep. */
public class ReviewCapture{
    static int frame;static StormModel[] m=new StormModel[4];static float[] bounds=new float[4],v=new float[3];
    static float fixedScale,fixedX,fixedY;
    static wstorm.g3d.Rig imported;static wstorm.g3d.UnitRenderer importedRenderer;
    public static void start(){
        imported=new wstorm.g3d.Rig();imported.bone(-1,0,0,0);int top=imported.bone(0,0,0,0),jaw=imported.bone(0,0,0,0);ImportedStormHead.add(imported,top,jaw);imported.finish();importedRenderer=new wstorm.g3d.UnitRenderer(190);importedRenderer.faceSort=true;
        ClientCapture.start();for(int i=0;i<4;i++)m[i]=new StormModel();
        float minX=Float.MAX_VALUE,minY=minX,maxX=-minX,maxY=maxX;
        for(int n=0;n<=180;n+=5){float t=Math.min(179,n)/179f,mass=90+3910*t*t;StormModel s=m[0];s.ensure(mass,81539,8);s.pose(mass,Math.min(179,n)*3,250,0);
            for(int i=0;i<s.rig.pieceCount;i++){var part=s.rig.pieces[i];if(s.rig.hidden[part.bone]||s.rig.alpha[part.bone]<.05f)continue;
                for(int j=0;j<part.mesh.verts;j++){s.renderer.point(part.bone,part.mesh.vx[j],part.mesh.vy[j],part.mesh.vz[j],v);s.project(v);minX=Math.min(minX,v[0]);maxX=Math.max(maxX,v[0]);minY=Math.min(minY,v[1]);maxY=Math.max(maxY,v[1]);}}
        }
        fixedScale=Math.min(970/(maxX-minX),490/(maxY-minY));fixedX=560-(minX+maxX)*.5f*fixedScale;fixedY=320-(minY+maxY)*.5f*fixedScale;
        Events.run(Trigger.postDraw,ReviewCapture::draw);
    }
    static void model(int index,float mass,float angle,float x,float y,float w,float h){
        StormModel s=m[index];s.ensure(mass,81539,8);s.pose(mass,frame*2,angle,0);
        s.renderer.cam=new Cam(s.rig.half,8.8f);s.renderer.pose(s.rig,angle,0,0,0,1);
        bounds[0]=bounds[2]=Float.MAX_VALUE;bounds[1]=bounds[3]=-Float.MAX_VALUE;
        for(int i=0;i<s.rig.pieceCount;i++){
            var part=s.rig.pieces[i];if(s.rig.hidden[part.bone]||s.rig.alpha[part.bone]<.05f)continue;
            for(int j=0;j<part.mesh.verts;j++){
                s.renderer.point(part.bone,part.mesh.vx[j],part.mesh.vy[j],part.mesh.vz[j],v);s.project(v);
                bounds[0]=Math.min(bounds[0],v[0]);bounds[1]=Math.max(bounds[1],v[0]);bounds[2]=Math.min(bounds[2],v[1]);bounds[3]=Math.max(bounds[3],v[1]);
            }
        }
        float sc=Math.min(w/(bounds[1]-bounds[0]),h/(bounds[3]-bounds[2]));
        s.renderer.cam=new Cam(s.rig.half*sc,8.8f);s.renderer.pose(s.rig,angle,0,0,0,sc);
        s.renderer.draw(s.rig,x+w*.5f-(bounds[0]+bounds[1])*.5f*sc,y+h*.5f-(bounds[2]+bounds[3])*.5f*sc);
    }
    static void draw(){
        try{
            Draw.flush();Draw.proj(0,0,1120,700);Draw.color(Color.valueOf("1c252e"));Fill.rect(560,350,1120,700);
            if(frame<4)model(0,90,250,250,100,620,475);
            else if(frame<8)model(1,650,250,180,80,760,490);
            else if(frame<12)model(2,2600,250,90,65,940,525);
            else if(frame<16){model(0,90,250,35,85,320,420);model(1,650,250,400,85,320,420);model(2,2600,250,765,85,320,420);}
            else if(frame<20){for(int i=0;i<4;i++)model(i,2600,new float[]{270,225,90,0}[i],35+(i%2)*555,50+(i/2)*295,495,260);}
            else if(frame<200){
                float t=(frame-20)/179f,mass=90+3910*t*t;StormModel s=m[0];s.ensure(mass,81539,8);s.pose(mass,(frame-20)*3,250,0);
                float sc=s.scale*fixedScale;s.renderer.cam=new Cam(s.rig.half*sc,8.8f);s.renderer.pose(s.rig,250,0,0,0,sc);s.renderer.draw(s.rig,fixedX,fixedY);
            }
            if(frame>=200){importedRenderer.cam=new Cam(190*8,8.8f);importedRenderer.pose(imported,250,0,0,0,8);importedRenderer.draw(imported,560,340);}
            Draw.reset();Draw.flush();
            if(frame==3)ClientCapture.shot("initial-close.png");if(frame==7)ClientCapture.shot("mutation-close.png");if(frame==11)ClientCapture.shot("adult-close.png");
            if(frame==15)ClientCapture.shot("model-sheet.png");if(frame==19)ClientCapture.shot("four-angles.png");
            if(frame>=20&&frame<200)ClientCapture.shot(String.format("growth-%04d.png",frame-20));
            if(frame==203){ClientCapture.shot("imported-head.png");Core.app.exit();}frame++;
        }catch(Throwable t){arc.util.Log.err(t);System.exit(4);}
    }
}
