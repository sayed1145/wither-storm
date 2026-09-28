package wstorm.model;
import arc.math.Mathf;
import wstorm.g3d.Rig;

/** Dedicated animation curves for the small skeletal Wither and the heavy Storm.
 * Head anchor motion is shared by the renderer and authoritative ingestion, not a render-only offset. */
public final class StormAnimation{
    public static float smooth(float x){x=Mathf.clamp(x);return x*x*(3-2*x);}
    public static float maturity(float mass){return smooth((mass-250)/1400);}
    public static float bob(float mass,float age){float g=maturity(mass);return Mathf.lerp(1.6f,.8f,g)*Mathf.sin(age/Mathf.lerp(28,55,g));}
    public static void anchor(float mass,float age,int head,float[] out){
        float g=maturity(mass),phase=age/Mathf.lerp(37,69,g)+head*1.9f;
        out[0]=(head==0?0:(head==1?-27:27)*(1+.30f*g))+(head==0?.45f:1.25f)*Mathf.sin(phase);
        out[1]=-3+21*g+Mathf.lerp(.65f,1.6f,g)*Mathf.sin(phase*.83f+.6f);
        out[2]=(head==0?48:40)+g*(head==0?10:16)+Mathf.lerp(1.0f,1.65f,g)*Mathf.sin(phase+head*.4f);
    }
    public static float idleYaw(float mass,float age,int head){
        float g=maturity(mass),phase=age/Mathf.lerp(48,86,g)+head*2.1f;
        return (head==0?7:17)*Mathf.sin(phase)+(head==0?0:head==1?-5:5);
    }
    public static float idlePitch(float mass,float age,int head){return (2+2*maturity(mass))*Mathf.sin(age/(44+head*9f)+head*1.7f);}
    public static float roll(float age,int head){return (head==0?1.3f:3f)*Mathf.sin(age/(63+head*7f)+head*2.3f);}
    public static float pulse(float age,float event,float attack,float release){
        float t=age-event;if(t<0||t>attack+release)return 0;return t<attack?smooth(t/attack):1-smooth((t-attack)/release);
    }
    public static float rumble(float age,int head){float t=Mathf.mod(age+head*117,720);return pulse(t,580,24,46);}
    /** Postmultiply a hinge rotation without moving its pivot; preserves existing local scale. */
    public static void hinge(Rig r,int bone,int axis,float angle,float x,float y,float z){
        int o=bone*12;float[] m=r.local;
        float px=m[o]*x+m[o+1]*y+m[o+2]*z,py=m[o+4]*x+m[o+5]*y+m[o+6]*z,pz=m[o+8]*x+m[o+9]*y+m[o+10]*z;
        r.rot(bone,axis,angle);
        r.move(bone,px-(m[o]*x+m[o+1]*y+m[o+2]*z),py-(m[o+4]*x+m[o+5]*y+m[o+6]*z),pz-(m[o+8]*x+m[o+9]*y+m[o+10]*z));
    }
}
