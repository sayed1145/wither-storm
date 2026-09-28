package wstorm.world;
import arc.math.*;import arc.math.geom.*;import mindustry.gen.*;import wstorm.*;import wstorm.model.*;
/** Eight independent windup/swing/impact/recovery timelines. Damage only at animated impact. */
public final class StormTentacles{
    public static final float windup=18,impact=30,recovery=54,cooldown=72;
    private static final float[] p=new float[3],tip=new float[3];
    private static final Vec2 a=new Vec2(),b=new Vec2();
    private static final arc.struct.IntSet hit=new arc.struct.IntSet();private static final arc.struct.Seq<Unit> victims=new arc.struct.Seq<>();
    private static Unit candidate;private static float best;
    public static int count(StormUnit s){return Math.min(8,3+(int)Math.log1p(s.matter/200)*2);}
    public static float birth(float mass,int arm){return StormAnimation.smooth((mass-420)/1100)*StormAnimation.smooth((mass-(280+arm*100))/650);}
    public static void local(StormUnit s,float mass,float age,int arm,float t,float[] out){
        float born=birth(mass,arm),g=WitherStormMod.spec.growth(mass);float length=Math.min(2,(float)Math.log1p(mass/2500));StormModel.tentacle(t*born,arm,age,length,out);
        float elapsed=age-s.tentacleStrike[arm];if(elapsed<0||elapsed>=recovery)return;
        StormModel.tentacle(born,arm,age,length,tip);
        float c=Mathf.cosDeg(s.bodyFacing()-90),sn=Mathf.sinDeg(s.bodyFacing()-90),dx=(s.strikeX[arm]-s.x)/g,dy=(s.strikeY[arm]-s.y)/g;
        float tx=dx*c+dy*sn-tip[0],ty=-dx*sn+dy*c-tip[1],tz=-StormAnimation.bob(mass,age)-tip[2];
        float curl=arm%2==0?20:-20,x,y,z;
        if(elapsed<windup){float f=StormAnimation.smooth(elapsed/windup);x=curl*f;y=-12*f;z=24*f;}
        else if(elapsed<impact){float f=StormAnimation.smooth((elapsed-windup)/(impact-windup));x=Mathf.lerp(curl,tx,f);y=Mathf.lerp(-12,ty,f);z=Mathf.lerp(24,tz,f);}
        else{float f=1-StormAnimation.smooth((elapsed-impact)/(recovery-impact));x=tx*f;y=ty*f;z=tz*f;}
        float weight=t*t;out[0]+=x*weight;out[1]+=y*weight;out[2]+=z*weight;
    }
    public static void point(StormUnit s,int arm,float t,Vec2 out){pointAt(s,s.age,arm,t,out);}
    public static void pointAt(StormUnit s,float age,int arm,float t,Vec2 out){
        float g=s.growth();local(s,s.matter,age,arm,t,p);
        float z=(p[2]+StormAnimation.bob(s.matter,age))*g,c=Mathf.cosDeg(s.bodyFacing()-90),sn=Mathf.sinDeg(s.bodyFacing()-90);
        float x=(p[0]*c-p[1]*sn)*g,y=(p[0]*sn+p[1]*c)*g,D=190*g*7,cy=-190*g*8.8f,f=D/(D-z);
        out.set(s.x+x*f,s.y+cy+(y-cy)*f);
    }
    private static float distance2(float x,float y,Vec2 from,Vec2 to){float dx=to.x-from.x,dy=to.y-from.y,len=dx*dx+dy*dy,t=len<.001f?0:Mathf.clamp(((x-from.x)*dx+(y-from.y)*dy)/len);return Mathf.dst2(x,y,from.x+t*dx,from.y+t*dy);}
    public static void update(StormUnit s,float dt){
        boolean scan=(s.tentacleTimer-=dt)<=0;if(scan)s.tentacleTimer=8;
        for(int arm=0;arm<count(s);arm++){
            if(birth(s.matter,arm)<.1f)continue;
            float elapsed=s.age-s.tentacleStrike[arm];
            if(!s.strikeDone[arm]&&elapsed>=impact&&elapsed<cooldown+dt){
                s.strikeDone[arm]=true;
                // One impact event; no damage during idle, contact, preparation or recovery.
                hit.clear();victims.clear();
                for(int segment=1;segment<=7;segment++){
                    pointAt(s,s.tentacleStrike[arm]+impact,arm,(segment-1)/7f,a);pointAt(s,s.tentacleStrike[arm]+impact,arm,segment/7f,b);
                    float width=Math.max(5,3*s.growth());
                    Groups.unit.intersect(Math.min(a.x,b.x)-width-80,Math.min(a.y,b.y)-width-80,Math.abs(a.x-b.x)+width*2+160,Math.abs(a.y-b.y)+width*2+160,u->{
                        if(u.dead||!StormLogic.enemy(s,u.team)||u instanceof StormUnit)return;
                        float d=distance2(u.x,u.y,a,b),radius=width+u.hitSize*.45f;
                        if(d<radius*radius&&hit.add(u.id))victims.add(u);
                    });
                }
                for(Unit victim:victims){victim.damage(32+24*s.growth());s.tentacleHits++;if(!mindustry.Vars.headless)wstorm.gfx.StormFx.skullBurst.at(victim.x,victim.y);}
            }
            if(!scan||elapsed<cooldown)continue;
            candidate=null;best=Float.MAX_VALUE;
            for(int segment=1;segment<=7;segment++){
                point(s,arm,(segment-1)/7f,a);point(s,arm,segment/7f,b);float reach=Math.max(16,18*s.growth());
                Groups.unit.intersect(Math.min(a.x,b.x)-reach-80,Math.min(a.y,b.y)-reach-80,Math.abs(a.x-b.x)+reach*2+160,Math.abs(a.y-b.y)+reach*2+160,u->{
                    if(u.dead||!StormLogic.enemy(s,u.team)||u instanceof StormUnit)return;
                    float d=distance2(u.x,u.y,a,b),radius=reach+u.hitSize*.45f;if(d<radius*radius&&d<best){best=d;candidate=u;}
                });
            }
            if(candidate!=null){s.tentacleStrike[arm]=s.age;s.strikeX[arm]=candidate.x;s.strikeY[arm]=candidate.y;s.strikeDone[arm]=false;}
        }
    }
}
