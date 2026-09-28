package wstorm.model;
import arc.graphics.Color;
import wstorm.g3d.*;
import wstorm.gfx.StormTextures;
/** Same nine dots on front/top; surrounding pixels are original, deterministic. */
public final class CommandModel{
    public static void add(Rig r,int bone,float half){
        int base=r.pieceCount;
        r.part(bone).style(0,StormTextures.command).color(Color.white).box(-half,-half,-half,half,half,half,true);
        float d=half*.24f,step=half*.43f;
        for(int row=0;row<3;row++)for(int col=0;col<3;col++){
            Color c=Color.valueOf(row==0&&col==2?"53853c":row==1&&col==0?"d29632":"b72b30");
            float x=(1-col)*step,z=(1-row)*step;
            r.part(bone).style(Mesh.emissive,0).color(c).box(x-d/2,half+.025f,z-d/2,x+d/2,half+.04f,z+d/2,true);r.on(base);
            r.part(bone).style(Mesh.emissive,0).color(c).box(x-d/2,-z-d/2,half+.025f,x+d/2,-z+d/2,half+.04f,true);r.on(base);
        }
    }
}
