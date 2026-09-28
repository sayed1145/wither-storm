package wstest;
import arc.*;import arc.graphics.*;import arc.graphics.g2d.*;import arc.util.*;
import mindustry.game.EventType.*;
import wstorm.model.*;import wstorm.g3d.*;import wstorm.g3d.Mesh;import wstorm.gfx.*;
/** Fixed-fit 500-cube upper-bound rendering and the two actual runtime materials. */
public class OrbitCapture{
    static int frame;static StormModel model=new StormModel();static float zoom,x,y;static float[] v=new float[3];
    static Rig[] swatch=new Rig[2];static UnitRenderer[] renderer=new UnitRenderer[2];static double millis;
    public static void start(){
        ClientCapture.start();model.ensure(100000,81539,8);float x0=1e9f,y0=x0,x1=-x0,y1=-x0;
        for(int n=0;n<120;n+=3){model.pose(100000,n*4,250+n*.35f,0);model.renderer.cam=new Cam(190,8.8f);model.renderer.pose(model.rig,250+n*.35f,0,0,0,1);
            for(int p=0;p<model.rig.pieceCount;p++){var q=model.rig.pieces[p];if(model.rig.hidden[q.bone]||model.rig.alpha[q.bone]<.01f)continue;
                for(int k=0;k<q.mesh.verts;k++){model.renderer.point(q.bone,q.mesh.vx[k],q.mesh.vy[k],q.mesh.vz[k],v);model.project(v);x0=Math.min(x0,v[0]);x1=Math.max(x1,v[0]);y0=Math.min(y0,v[1]);y1=Math.max(y1,v[1]);}}}
        zoom=Math.min(960/(x1-x0+15),515/(y1-y0+15));x=560-(x0+x1)*zoom/2;y=345-(y0+y1)*zoom/2;
        for(int i=0;i<2;i++){swatch[i]=new Rig();swatch[i].bone(-1,0,0,0);swatch[i].part(0).style(0,i==0?StormTextures.orbitBlack:StormTextures.orbitViolet).color(Color.white).box(-4,-4,-4,4,4,4,true);swatch[i].finish();renderer[i]=new UnitRenderer(190*7);renderer[i].cam=new Cam(190*7,8.8f);renderer[i].depthTest=true;}
        Events.run(Trigger.postDraw,OrbitCapture::draw);
    }
    static void draw(){
        try{
            Draw.flush();Draw.proj(0,0,1120,700);Draw.color(Color.valueOf("1c252e"));Fill.rect(560,350,1120,700);Draw.reset();long start=System.nanoTime();
            model.pose(100000,frame*4,250+frame*.35f,0);model.renderer.cam=new Cam(190*zoom,8.8f);model.renderer.pose(model.rig,250+frame*.35f,0,0,0,zoom);model.renderer.draw(model.rig,x,y);
            for(int i=0;i<2;i++){swatch[i].reset();swatch[i].euler(0,frame*.5f,15,5);renderer[i].pose(swatch[i],250,0,0,0,7);renderer[i].draw(swatch[i],i==0?110:1010,100);}
            Draw.flush();if(frame>=10)millis+=(System.nanoTime()-start)/1e6;
            if(frame==30)ClientCapture.shot("500-cubes.png");if(!Boolean.getBoolean("ws.quick"))ClientCapture.shot(String.format("orbit-%04d.png",frame));
            if(frame==119){
                if(model.orbitCount!=500||model.orbitMesh.verts!=4000||model.orbitMesh.faces!=3000||model.rig.bones>128)throw new AssertionError("Orbit budget");
                ClientCapture.output.child("orbit-result.json").writeString("{\"installedJar\":true,\"scriptedMatter\":100000,\"frames\":120,\"orbitCubes\":500,\"orbitVertices\":4000,\"orbitQuads\":3000,\"bones\":"+model.rig.bones+",\"cpuSubmissionMillisMeanNotGameFps\":"+millis/110+",\"gl2\":"+Boolean.getBoolean("ws.gl2")+"}");
                Log.info("ORBIT_500_RENDER_PASS bones="+model.rig.bones);Core.app.exit();
            }
            frame++;
        }catch(Throwable t){Log.err(t);System.exit(4);}
    }
}
