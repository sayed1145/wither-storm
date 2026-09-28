package wstorm.model;
import wstorm.g3d.*;
import wstorm.gfx.*;
/** Cached command cube. No render targets, shader changes or lazy world mesh construction. */
public final class AltarModel{
    private static Rig rig;private static UnitRenderer renderer;
    public static void prepare(){
        if(rig!=null)return;
        rig=new Rig();rig.half=26;rig.height=28;rig.bone(-1,0,0,14);
        CommandModel.add(rig,0,13);rig.finish();renderer=new UnitRenderer(26);
    }
    public static void draw(float x,float y,float time,float charge){
        prepare();StormTextures.load();rig.reset();rig.glow[0]=.75f+.55f*(.5f+.5f*arc.math.Mathf.sin(time/18f));
        renderer.pose(rig,225,0,0,0,1);renderer.draw(rig,x,y);
    }
}
