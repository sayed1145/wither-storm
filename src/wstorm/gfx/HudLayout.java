package wstorm.gfx;
import arc.math.*;import arc.math.geom.*;import arc.struct.*;
/** Screen-space rectangle packing. Shared by live layout and regression tests. */
public final class HudLayout{
    public static boolean fits(Rect r,float sw,float sh,Seq<Rect> obstacles){
        if(r.x<8||r.y<8||r.x+r.width>sw-8||r.y+r.height>sh-8)return false;
        for(Rect o:obstacles)if(r.x<o.x+o.width+6&&r.x+r.width+6>o.x&&r.y<o.y+o.height+6&&r.y+r.height+6>o.y)return false;
        return true;
    }
    public static boolean place(Rect out,float x,float y,float w,float h,float sw,float sh,Seq<Rect> obstacles){
        x=Mathf.clamp(x,8,Math.max(8,sw-w-8));y=Mathf.clamp(y,8,Math.max(8,sh-h-8));out.set(x,y,w,h);
        if(fits(out,sw,sh,obstacles))return true;
        float best=Float.MAX_VALUE,bx=0,by=0;
        for(float yy=8;yy+h<=sh-8;yy+=12)for(float xx=8;xx+w<=sw-8;xx+=12){float distance=Mathf.dst2(xx,yy,x,y);if(distance>=best)continue;out.set(xx,yy,w,h);if(fits(out,sw,sh,obstacles)){best=distance;bx=xx;by=yy;}}
        if(best==Float.MAX_VALUE)return false;out.set(bx,by,w,h);return true;
    }
}
