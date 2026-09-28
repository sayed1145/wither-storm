package wstorm.world;
import arc.math.*;import mindustry.gen.*;import wstorm.model.*;
/** Authoritative body bearing + independent bounded head gimbals. No GPU-dependent gameplay. */
public final class StormAim{
    public static final float bodyRate=1.8f,headLimit=40f,bodyCone=55f,alignment=2f;
    private static final float[] a=new float[3],p=new float[3];
    public static float delta(float angle,float base){return Mathf.mod(angle-base+180,360)-180;}
    public static boolean stormPort(float mass,int h){return StormModel.mutation(mass,h)>.001f;}
    public static void portLocal(float mass,int h,float[] out){
        if(stormPort(mass,h)){out[0]=0;out[1]=16.82f;out[2]=0;}
        else{out[0]=h==1?-6:6;out[1]=10.17f;out[2]=1.85f;}
    }
    public static float headScale(float mass,int h){return (h==0?1:.74f)*(1+.32f*StormModel.mutation(mass,h));}
    public static void port(StormUnit s,int h,float[] out){
        float m=StormModel.mutation(s.matter,h);portLocal(s.matter,h,out);
        float scale=headScale(s.matter,h)*(stormPort(s.matter,h)?.75f+.25f*m:1-.2f*m);
        float x=out[0]*scale,y=out[1]*scale,z=out[2]*scale;
        float r=StormAnimation.roll(s.age,h),c=Mathf.cosDeg(r),sn=Mathf.sinDeg(r),xx=x*c+z*sn,zz=-x*sn+z*c;x=xx;z=zz;
        c=Mathf.cosDeg(s.aimPitch[h]);sn=Mathf.sinDeg(s.aimPitch[h]);float yy=y*c-z*sn;zz=y*sn+z*c;y=yy;z=zz;
        c=Mathf.cosDeg(s.aimYaw[h]);sn=Mathf.sinDeg(s.aimYaw[h]);xx=x*c-y*sn;yy=x*sn+y*c;
        StormAnimation.anchor(s.matter,s.age,h,a);x=xx+a[0];y=yy+a[1];z+=a[2]+StormAnimation.bob(s.matter,s.age);
        c=Mathf.cosDeg(s.bodyFacing()-90);sn=Mathf.sinDeg(s.bodyFacing()-90);float g=s.growth();out[0]=(x*c-y*sn)*g;out[1]=(x*sn+y*c)*g;out[2]=z*g;
    }
    public static float desiredBody(StormUnit s){
        if(s.isPlayer())return s.isShooting&&Mathf.dst2(s.x,s.y,s.aimX,s.aimY)>1?s.angleTo(s.aimX,s.aimY):s.rotation;
        if(s.controller() instanceof StormAI ai){
            Teamc t=StormCommand.enabled(s.team)?ai.attackTarget:ai.currentTarget();
            if(t!=null&&(t instanceof Unit u?u.isAdded()&&!u.dead:t instanceof Building b&&b.isValid()))return s.angleTo(t);
        }
        Teamc best=null;float distance=Float.MAX_VALUE;
        for(int h=0;h<3;h++){Teamc t=StormLogic.resolve(s,h);if(t instanceof Unit u&&!u.dead&&u.isAdded()&&s.dst2(u)<distance){distance=s.dst2(u);best=u;}}
        return best==null?s.rotation:s.angleTo(best);
    }
    public static void update(StormUnit s,float dt){
        if(!Float.isFinite(s.bodyYaw))s.bodyYaw=s.rotation;
        s.bodyYaw=Angles.moveToward(s.bodyYaw,desiredBody(s),bodyRate*dt);
        for(int h=0;h<3;h++){
            float yaw=StormAnimation.idleYaw(s.matter,s.age,h),pitch=StormAnimation.idlePitch(s.matter,s.age,h);
            if(s.targetId[h]>=0){
                Teamc target=StormLogic.resolve(s,h);port(s,h,p);float dx=(target==null?s.targetX[h]:target.x())-s.x-p[0],dy=(target==null?s.targetY[h]:target.y())-s.y-p[1];
                yaw=Mathf.clamp(delta(Angles.angle(dx,dy),s.bodyFacing()),-headLimit,headLimit);
                pitch=Mathf.clamp(-(float)Math.atan2(p[2],Mathf.len(dx,dy))*Mathf.radDeg,-75,25);
            }
            s.aimYaw[h]=Mathf.approach(s.aimYaw[h],yaw,2.8f*dt);s.aimPitch[h]=Mathf.approach(s.aimPitch[h],pitch,2.4f*dt);
        }
    }
    public static boolean ready(StormUnit s,int h){
        if(!s.beamCapable(h)||s.targetKind[h]!=1||s.targetId[h]<0)return false;
        Unit victim=Groups.unit.getByID(s.targetId[h]);if(victim==null||victim.dead||!victim.isAdded()||!StormLogic.enemy(s,victim.team)||!s.within(victim,s.reach()+StormLogic.rangeEpsilon))return false;
        if(Math.abs(delta(s.angleTo(victim),s.bodyFacing()))>bodyCone)return false;
        port(s,h,p);float dx=victim.x-s.x-p[0],dy=victim.y-s.y-p[1],dz=-p[2],length=(float)Math.sqrt(dx*dx+dy*dy+dz*dz);
        if(length<1)return false;float angle=s.bodyFacing()+s.aimYaw[h],pitch=s.aimPitch[h];
        float dot=(dx*Mathf.cosDeg(angle)*Mathf.cosDeg(pitch)+dy*Mathf.sinDeg(angle)*Mathf.cosDeg(pitch)+dz*Mathf.sinDeg(pitch))/length;
        return dot>=Mathf.cosDeg(alignment);
    }
}
