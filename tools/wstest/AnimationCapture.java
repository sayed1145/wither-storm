package wstest;
import arc.*;import arc.graphics.*;import arc.graphics.g2d.*;import arc.math.*;import arc.util.*;
import mindustry.game.*;import mindustry.game.EventType.*;
import wstorm.*;import wstorm.world.*;import wstorm.model.*;import wstorm.g3d.*;import wstorm.g3d.Mesh;

/** Actual installed renderer; synthetic animation inputs are deliberately not a gameplay claim. */
public class AnimationCapture{
    static int frame;
    static StormModel[] models={new StormModel(),new StormModel()};
    static StormUnit[] actors=new StormUnit[2];
    static float[] zoom=new float[2],cx=new float[2],cy=new float[2],point=new float[3];
    public static void start(){
        ClientCapture.start();
        for(int i=0;i<2;i++){
            actors[i]=(StormUnit)StormContent.storm.create(Team.sharded);actors[i].matter=actors[i].visualMatter=i==0?90:2600;
            actors[i].rotation=270;actors[i].skullTimer=100;models[i].ensure(actors[i].matter,81539,8);
            float x0=1e9f,x1=-x0,y0=x0,y1=-x0;
            for(int n=0;n<120;n++){
                StormModel m=models[i];m.pose(actors[i].matter,n*6,n*3,0);m.renderer.cam=new Cam(190,8.8f);m.renderer.pose(m.rig,n*3,0,0,0,1);
                for(int p=0;p<m.rig.pieceCount;p++){var piece=m.rig.pieces[p];if(m.rig.hidden[piece.bone]||m.rig.alpha[piece.bone]<.01f)continue;
                    for(int v=0;v<piece.mesh.verts;v++){m.renderer.point(piece.bone,piece.mesh.vx[v],piece.mesh.vy[v],piece.mesh.vz[v],point);m.project(point);x0=Math.min(x0,point[0]);x1=Math.max(x1,point[0]);y0=Math.min(y0,point[1]);y1=Math.max(y1,point[1]);}}
            }
            zoom[i]=Math.min(475/(x1-x0+16),505/(y1-y0+16));cx[i]=280+i*560-(x0+x1)*.5f*zoom[i];cy[i]=330-(y0+y1)*.5f*zoom[i];
        }
        Events.run(Trigger.postDraw,AnimationCapture::draw);
    }
    static void body(int index,float mass,float age,float heading,boolean depth,boolean track){
        StormUnit u=actors[index];StormModel m=models[index];u.matter=u.visualMatter=mass;u.age=age;u.rotation=u.bodyYaw=heading;
        m.ensure(mass,81539,8);m.pose(mass,age,heading,0);if(track){StormAim.update(u,4);m.aimHeads(u);}
        m.renderer.depthTest=depth;m.renderer.cam=new Cam(190*zoom[index],8.8f);m.renderer.pose(m.rig,heading,0,0,0,zoom[index]);m.renderer.draw(m.rig,cx[index],cy[index]);
    }
    static void occlusionProbe(boolean front,boolean depth,float x){
        Rig r=new Rig();r.bone(-1,0,0,0);
        r.part(0).style(Mesh.emissive,0).color(.08f,.22f,.52f).box(-12,-45,20,12,15,30,true);
        r.part(0).style(Mesh.emissive,0).color(1,0,1).box(-4,-4,front?40:8,4,4,front?43:11,true);r.on(0);
        r.finish();UnitRenderer d=new UnitRenderer(100*4);d.cam=new Cam(100*4,8.8f);d.faceSort=true;d.depthTest=depth;d.pose(r,90,0,0,0,4);d.draw(r,x,320);
    }
    static void draw(){
        try{
            Draw.flush();Draw.proj(0,0,1120,700);Draw.color(Color.valueOf("1c252e"));Fill.rect(560,350,1120,700);Draw.reset();
            if(frame<2){occlusionProbe(frame==1,false,280);occlusionProbe(frame==1,true,840);}
            else if(frame<5){
                float mass=frame==3?2600:90,heading=frame==4?270:90;
                for(int i=0;i<2;i++){float z=zoom[i],x=cx[i],y=cy[i];zoom[i]=mass==90?4.5f:2.0f;cx[i]=280+i*560;cy[i]=mass==90?145:190;body(i,mass,120,heading,i==1,false);zoom[i]=z;cx[i]=x;cy[i]=y;}
            }else{
                int n=frame-5;float age=n*4,heading=n<180?270:270+(n-180)*6;
                for(int i=0;i<2;i++){
                    StormUnit u=actors[i];StormModel m=models[i];u.skullTimer=Math.max(1,u.skullTimer-4);
                    if(n==120||n==150){u.skullTimer=100;if(i==1)for(int h=0;h<3;h++)m.reactMeal(h,age+h*5);}
                    for(int h=0;h<3;h++){
                        boolean target=n>=60&&n<180;u.targetKind[h]=(byte)(target?1:0);u.targetId[h]=target?1900000+h:-1;
                        float a=heading+Mathf.sin(n/19f+h*1.6f)*65;
                        u.targetX[h]=Mathf.cosDeg(a)*190;u.targetY[h]=Mathf.sinDeg(a)*190;u.locked[h]=target?30:0;
                    }
                    body(i,i==0?90:2600,age,heading,true,true);
                }
            }
            Draw.reset();Draw.flush();
            if(frame==0)ClientCapture.shot("occlusion-behind.png");if(frame==1)ClientCapture.shot("occlusion-front-control.png");
            if(frame==2)ClientCapture.shot("early-back-before-after.png");if(frame==3)ClientCapture.shot("late-back-before-after.png");if(frame==4)ClientCapture.shot("early-front-control.png");
            if(frame>=5&&!Boolean.getBoolean("ws.quick"))ClientCapture.shot(String.format("anim-%04d.png",frame-5));
            if(frame==215)ClientCapture.shot("turn-back.png");
            if(frame==244){
                if(DepthSurface.passes<400)throw new AssertionError("Depth passes missing");
                ClientCapture.output.child("animation-result.json").writeString("{\"renderer\":\"installed v1.7.1 JAR\",\"frames\":240,\"syntheticAnimationInputs\":true,\"depthPasses\":"+DepthSurface.passes+",\"depthFaces\":"+DepthSurface.faces+",\"gl2\":"+Boolean.getBoolean("ws.gl2")+"}");
                Log.info("ANIMATION_DEPTH_CAPTURE_PASS "+DepthSurface.passes);Core.app.exit();
            }
            frame++;
        }catch(Throwable t){Log.err(t);System.exit(4);}
    }
}
