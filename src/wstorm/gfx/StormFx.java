package wstorm.gfx;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import mindustry.entities.*;
import mindustry.graphics.*;

/** Original violet storm effects, not aliases of vanilla explosion effects. */
public final class StormFx{
    public static final Color purple=Color.valueOf("ad54ff"),pale=Color.valueOf("edc9ff"),dark=Color.valueOf("2d153d");
    public static final Effect birth=new Effect(110,500,e->{
        Draw.color(dark,purple,e.fin());Lines.stroke(3*e.fout());Lines.circle(e.x,e.y,10+e.finpow()*115);
        Draw.blend(Blending.additive);Draw.color(purple,e.fout()*.55f);
        for(int i=0;i<3;i++){Lines.stroke((3-i*.7f)*e.fout());Lines.poly(e.x,e.y,6,14+e.fin()*90+i*8,e.fin()*80+i*30);}
        Angles.randLenVectors(e.id,26,12+e.fin()*90,(x,y)->{Fill.square(e.x+x,e.y+y,1.6f*e.fout(),45+e.fin()*150);});
        Draw.blend();Draw.reset();
    });
    public static final Effect fracture=new Effect(135,700,e->{
        float k=Math.max(1,e.rotation);Draw.color(dark);Fill.circle(e.x,e.y,35*k*e.fout());
        Draw.blend(Blending.additive);Draw.color(purple,e.fout()*.7f);
        Lines.stroke(4*e.fout());Lines.circle(e.x,e.y,12+e.finpow()*150*k);
        Draw.color(pale);Lines.stroke(1.5f*e.fout());Lines.circle(e.x,e.y,6+e.fin()*125*k);
        Angles.randLenVectors(e.id,34,e.fin()*120*k,(x,y)->{Draw.color(purple,pale,e.fout());Fill.square(e.x+x,e.y+y,(1+e.fout()*3)*k,e.fin()*180+x);});
        Draw.blend();Draw.reset();
    });
    public static final Effect skullBurst=new Effect(42,150,e->{
        // Hand-authored voxel smoke puffs, white flash and charcoal fragments.
        Draw.color(Color.white,Color.lightGray,e.fin());
        if(e.fin()<.22f)Fill.square(e.x,e.y,8*e.fout(),0);
        Angles.randLenVectors(e.id,14,5+e.fin()*32,(x,y)->{
            Draw.color(Color.valueOf("252a2b"),Color.gray,e.fout());Draw.alpha(e.fout());
            Fill.square(e.x+x,e.y+y,1+4*e.fout(),0);
        });
        Draw.color(pale,e.fout());Lines.stroke(2*e.fout());Lines.circle(e.x,e.y,3+e.fin()*23);
        Draw.blend();Draw.reset();
    });
    public static final Effect sickness=new Effect(35,e->{
        Draw.color(purple,e.fout());Fill.square(e.x,e.y+e.fin()*7,.7f+e.fout()*.7f,45);Draw.reset();
    });
}
