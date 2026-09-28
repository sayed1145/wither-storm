package wstest;
import arc.*;import arc.graphics.*;import arc.graphics.g2d.*;import arc.util.*;import mindustry.*;import mindustry.game.EventType.*;
import wstorm.model.*;import wstorm.gfx.*;
/** Draws the actual Java rig through the actual runtime renderer, not an artist mockup. */
public class ModelSheet{
    static int frames;static StormModel[] models=new StormModel[3];static float[] masses={90,650,2600};
    public static void start(){
        ClientCapture.start();
        for(int i=0;i<3;i++){models[i]=new StormModel();models[i].build(masses[i],81539,8);}
        Events.run(Trigger.postDraw,()->{
            Draw.flush();Draw.proj(0,0,1120,700);Draw.color(Color.valueOf("11191d"));Fill.rect(560,350,1120,700);
            for(int i=0;i<3;i++){
                StormModel m=models[i];m.pose(masses[i],45,250,0);
                // Normalize showcase scale only; topology is exactly the in-game mesh.
                float sc=i==0?3.0f:i==1?2.5f:1.55f;
                m.renderer.cam=new wstorm.g3d.Cam(m.rig.half*sc,8.8f);m.renderer.pose(m.rig,250,0,0,0,sc);
                m.renderer.draw(m.rig,190+i*365,200);
            }
            Draw.reset();Draw.flush();
            if(++frames==12){ClientCapture.output=new arc.files.Fi(System.getProperty("ws.capture"));ClientCapture.output.mkdirs();ClientCapture.shot("model-sheet.png");Core.app.exit();}
        });
    }
}
