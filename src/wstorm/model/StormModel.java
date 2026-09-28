package wstorm.model;
import arc.graphics.Color;
import arc.math.*;
import wstorm.*;
import wstorm.g3d.*;
import wstorm.gfx.StormTextures;

/** One procedural organism: three normal Wither heads deform into cycloptic Storm heads.
 * Closed occupancy shell, exposed faces only; bounded topology; no external mesh files. */
public final class StormModel{
    public Rig rig;public UnitRenderer renderer;
    public final int[] heads=new int[3],jaws=new int[3];
    private final int[] normalEyes=new int[3],normalMouth=new int[3],stormEyes=new int[3],upperTeeth=new int[3],lowerTeeth=new int[3];
    public final int[] orbitBones=new int[1];
    public Mesh orbitMesh;public int orbitCount;private int orbitDetail=8;
    public static int orbitCountFor(float matter){return matter<850?0:Math.min(500,32+(int)(100*Math.log1p((matter-850)/850)));}
    private int core,shell,frame,armour;private int[][] limbs;
    public final int[] arms=new int[2];
    public final float[] headYaw=new float[3],headPitch=new float[3];
    private final float[] headSize=new float[3],feedAt={-10000,-10000,-10000};
    private final float[] poseAnchor=new float[3];
    private float previousSkullTimer=Float.NaN,shotAt=-10000;
    private int previousEaten;
    private float animationMass,animationAge;
    private float builtMatter=-1;private int builtSeed,builtDetail;
    public int bodyVoxels,branches;public long rebuilds;public float scale=1,visualRadius;
    private final float[] morph=new float[3],yaw=new float[3],pitch=new float[3],a=new float[3],b=new float[3];
    private static final Color black=Color.valueOf("060609"),violet=Color.valueOf("9227f5"),pale=Color.valueOf("e3b4ff"),ivory=Color.valueOf("d6dbd7");
    private static int hash(int x){x^=x>>>16;x*=0x7feb352d;x^=x>>>15;return x;}
    private static float smooth(float v){v=Mathf.clamp(v);return v*v*(3-2*v);}
    public static float mutation(float mass,int head){return smooth((mass-(head==0?220:head==1?700:1180))/(head==0?400:460));}
    private Mesh part(int bone,int mat){return rig.part(bone).style(0,mat).color(Color.white);}
    public void ensure(float matter,int seed,int detail){
        int bucket=(int)(Math.log1p(matter/220f)*6),old=(int)(Math.log1p(Math.max(0,builtMatter)/220f)*6);
        if(rig==null||seed!=builtSeed||detail!=builtDetail||bucket!=old)build(matter,seed,detail);
    }
    /** The projectile is always a NORMAL Wither skull, never a Storm cyclops head. */
    public static void skull(Rig r,int h,int j){
        r.part(h).style(0,StormTextures.skull).color(Color.white).box(-12,-10,-3.5f,12,10,14,true);
        ordinaryFace(r,h,r.pieceCount-1);
        r.part(j).style(0,StormTextures.skull).color(Color.white).box(-12,-10,-11,12,10,-3.5f,true);
        r.part(j).style(Mesh.emissive,0).color(Color.valueOf("a2aaa8")).box(-8,10.08f,-8.5f,8,10.13f,-5,true);r.on(r.pieceCount-2);
    }
    private static void ordinaryFace(Rig r,int bone,int base){
        r.part(bone).color(black).box(-11,10.03f,2,11,10.07f,7,true);r.on(base);
        for(int side:new int[]{-1,1}){r.part(bone).style(Mesh.emissive,0).color(Color.valueOf("9aa5a4")).box(side*6-3.3f,10.09f,-.1f,side*6+3.3f,10.15f,3.8f,true);r.on(base);}
        r.part(bone).style(0,StormTextures.skull).color(Color.white).box(-2,10.17f,-1,2,10.65f,6,true);r.on(base);
    }
    private void livingHead(int h){
        heads[h]=rig.bone(0,0,0,0);
        normalEyes[h]=rig.bone(heads[h],0,0,0);normalMouth[h]=rig.bone(normalEyes[h],0,0,0);
        skull(rig,normalEyes[h],normalMouth[h]);
        stormEyes[h]=rig.bone(heads[h],0,0,0);jaws[h]=rig.bone(heads[h],0,0,0);
        ImportedStormHead.add(rig,stormEyes[h],jaws[h]);
    }
    private boolean occupied(int x,int y,int z,int n,int seed){
        float X=x/(n*1.50f),Y=y/(n*.90f),Z=z/(n*.90f);
        float q=X*X+Y*Y+Z*Z;
        float lobes=.15f*Mathf.sin(x*.65f+y*.56f+z*.28f)+.10f*Mathf.sin(y*.8f-z*.6f)+.045f*((hash(x*931+y*417+z*101+seed)&255)/255f-.5f);
        return q<1+lobes;
    }
    private static void voxel(Mesh m,float x,float y,float z,float c,boolean[] o){
        int a=m.vert(x,y,z),b=m.vert(x+c,y,z),d=m.vert(x,y+c,z),e=m.vert(x,y,z+c),f=m.vert(x+c,y,z+c),g=m.vert(x+c,y+c,z+c),h=m.vert(x,y+c,z+c),k=m.vert(x+c,y+c,z);
        if(o[0])m.face(d,a,e,h,-1,0,0);if(o[1])m.face(b,k,g,f,1,0,0);
        if(o[2])m.face(a,b,f,e,0,-1,0);if(o[3])m.face(k,d,h,g,0,1,0);
        if(o[4])m.face(a,d,k,b,0,0,-1);if(o[5])m.face(e,f,g,h,0,0,1);
    }
    public void build(float matter,int seed,int detail){
        builtMatter=matter;builtSeed=seed;builtDetail=detail;rebuilds++;
        rig=new Rig();rig.half=190;rig.height=155;rig.bone(-1,0,0,0);
        core=rig.bone(0,0,8,21);CommandModel.add(rig,core,10);
        frame=rig.bone(0,0,0,0);
        // Complete shoulder bar, spine, ribcage and stepped gripping bones, as in the supplied mini model.
        part(frame,StormTextures.skull).box(-26,-4,33,26,4,38,true);
        part(frame,StormTextures.rib).box(-4,-7,6,4,0,39,true);
        for(int side:new int[]{-1,1}){
            int arm=arms[side<0?0:1]=rig.bone(frame,0,0,0);
            part(arm,StormTextures.skull).box(side*13-3.5f,-4,7,side*13+3.5f,13,28,true);
            for(int j=0;j<3;j++)part(arm,StormTextures.skull).box(side*11-3,10,9+j*6,side*11+3,18,13+j*6,true);
        }
        part(frame,StormTextures.skull).box(-12,-7,5,12,12,9,true);
        part(frame,StormTextures.rib).box(-3,-5,-1,3,2,7,true);
        armour=rig.bone(0,0,8,21);part(armour,StormTextures.obsidian).box(-11,-11,-11,11,11,11,true);
        shell=rig.bone(0,0,-10,57);bodyVoxels=0;
        int n=Math.min(detail<=5?4:6,3+(int)Math.log1p(matter/240));float cell=42f/n;
        int nx=(int)Math.ceil(n*1.50f),side=2*nx+3;boolean[][][] field=new boolean[side][side][side];int offset=nx+1;
        for(int x=-nx;x<=nx;x++)for(int y=-n;y<=n;y++)for(int z=-n;z<=n;z++)field[x+offset][y+offset][z+offset]=occupied(x,y,z,n,seed);
        for(int x=-nx;x<=nx;x++)for(int y=-n;y<=n;y++)for(int z=-n;z<=n;z++){
            int X=x+offset,Y=y+offset,Z=z+offset;if(!field[X][Y][Z])continue;
            boolean[] o={!field[X-1][Y][Z],!field[X+1][Y][Z],!field[X][Y-1][Z],!field[X][Y+1][Z],!field[X][Y][Z-1],!field[X][Y][Z+1]};
            boolean any=false;for(boolean q:o)any|=q;if(!any)continue;
            voxel(part(shell,StormTextures.obsidian+(hash(x*37+y*97+z*193+seed)&7)),(x-.5f)*cell,(y-.5f)*cell,(z-.5f)*cell,cell,o);bodyVoxels++;
        }
        for(int h=0;h<3;h++)livingHead(h);
        branches=Math.min(8,3+(int)Math.log1p(matter/200)*2);limbs=new int[branches][7];
        for(int i=0;i<branches;i++)for(int j=0;j<7;j++){limbs[i][j]=rig.bone(0,0,0,0);part(limbs[i][j],StormTextures.rib).box(-1,-1,-1,1,1,0,true);}
        orbitDetail=detail;orbitBones[0]=rig.bone(0,0,0,0);orbitMesh=rig.part(orbitBones[0]);
        // Reserve one bounded batch instead of adding 500 bones or game entities.
        for(int i=0;i<500;i++)orbitMesh.style(0,i%3==0?StormTextures.orbitBlack:StormTextures.orbitViolet).color(Color.white).box(-1,-1,-1,1,1,1,true);
        rig.finish();renderer=new UnitRenderer(rig.half);renderer.faceSort=true;renderer.depthTest=true;pose(matter,0,270,0);
    }
    public static void tentacle(float t,int i,float time,float grow,float[] p){
        float ang=i*137.5f+28+t*t*(i%2==0?38:-38),r=20+t*(75+grow*36);
        p[0]=Mathf.cosDeg(ang)*r+Mathf.sin(time/39+i-t*6)*11*t;
        p[1]=-12+Mathf.sinDeg(ang)*r;
        p[2]=47-58*t+9*Mathf.sin(time/48+i+t*4)*t+20*t*t*t;
    }
    public void pose(float matter,float time,float heading,float fracture){
        animationMass=matter;animationAge=time;rig.reset();scale=WitherStormMod.spec.growth(matter);
        float wrap=smooth((matter-160)/200),growth=smooth((matter-250)/1400);
        float elongation=(float)Math.log1p(matter/2500);
        rig.move(0,0,0,StormAnimation.bob(matter,time));
        float breath=Mathf.sin(time/42),lightBody=1-growth;
        StormAnimation.hinge(rig,frame,1,lightBody*2.2f*Mathf.sin(time/61),0,0,30);
        for(int i=0;i<2;i++)StormAnimation.hinge(rig,arms[i],1,(i==0?-1:1)*(2+3*breath)*lightBody,i==0?-13:13,-1,30);
        rig.rot(core,2,lightBody*1.2f*Mathf.sin(time/61));
        rig.hidden[core]=matter>=360;rig.glow[core]=.65f+.65f*(.5f+.5f*Mathf.sin(time/15));
        rig.hidden[armour]=wrap<=0;rig.scale(armour,Math.max(.01f,wrap),Math.max(.01f,wrap),Math.max(.01f,wrap));
        rig.hidden[frame]=growth>.75f;
        float body=smooth((matter-190)/1150);
        rig.hidden[shell]=body<=0;
        float factor=.24f+.76f*body;
        rig.place(shell,0,-8-9*body,27+37*body+Math.min(12,elongation*5));
        rig.scale(shell,factor*(1+Math.min(.24f,elongation*.07f)),factor,factor*(1+Math.min(.32f,elongation*.10f)));
        rig.rot(shell,1,.85f*growth*Mathf.sin(time/95));
        float expansion=1+.009f*growth*Mathf.sin(time/75);rig.scale(shell,expansion,expansion,1+.015f*growth*Mathf.sin(time/75));
        for(int h=0;h<3;h++){
            float m=morph[h]=mutation(matter,h);
            StormAnimation.anchor(matter,time,h,poseAnchor);float x=poseAnchor[0],y=poseAnchor[1],z=poseAnchor[2];
            float hs=(h==0?1:.74f)*(1+.32f*m);
            headSize[h]=hs;rig.place(heads[h],x,y,z);
            orientHead(h,StormAnimation.idleYaw(matter,time,h),StormAnimation.idlePitch(matter,time,h),StormAnimation.roll(time,h));
            rig.hidden[normalEyes[h]]=rig.hidden[normalMouth[h]]=m>.995f;
            rig.alpha[normalEyes[h]]=rig.alpha[normalMouth[h]]=1-m;
            rig.scale(normalEyes[h],1-.2f*m,1-.2f*m,1-.2f*m);
            rig.hidden[stormEyes[h]]=rig.hidden[jaws[h]]=m<=.001f;
            rig.alpha[stormEyes[h]]=rig.alpha[jaws[h]]=m;
            float grow=.75f+.25f*m;rig.scale(stormEyes[h],grow,grow,grow);rig.scale(jaws[h],grow,grow,grow);
            rig.glow[stormEyes[h]]=1.0f+.08f*Mathf.sin(time/14+h);
            rig.move(jaws[h],0,0,-m*.65f);
            StormAnimation.hinge(rig,jaws[h],0,-m*(1.5f+1.3f*(.5f+.5f*Mathf.sin(time/35+h))),0,-14.4f,-6);
            StormAnimation.hinge(rig,normalMouth[h],0,-(1-m)*(.65f+.65f*Mathf.sin(time/33+h)),0,-8,-3.5f);
        }
        float limbsGrowth=smooth((matter-420)/1100);visualRadius=(110+Math.min(30,elongation*12))*scale;
        for(int i=0;i<branches;i++)for(int j=0;j<7;j++){
            int bone=limbs[i][j];float birth=limbsGrowth*smooth((matter-(280+i*100))/650);rig.hidden[bone]=birth<=.001f;
            float t=j/7f,t1=(j+1)/7f;tentacle(t*birth,i,time,Math.min(2,elongation),a);tentacle(t1*birth,i,time,Math.min(2,elongation),b);
            float len=rig.aim(bone,a[0],a[1],a[2],b[0],b[1],b[2],1,0,0),w=(4.8f*(1-t)+.3f)*birth;
            rig.scale(bone,w,w,len*1.015f);
        }
        float orbitGrowth=smooth((matter-850)/1100);
        orbitCount=orbitCountFor(matter);if(orbitDetail<=5)orbitCount=Math.min(200,orbitCount);
        orbitMesh.verts=orbitMesh.faces=0;rig.hidden[orbitBones[0]]=orbitGrowth<=.001f;
        for(int i=0;i<orbitCount;i++){
            float angle=i*137.507f+time*(.10f+(i%11)*.014f),r=64+(i%13)*7;
            float x=Mathf.cosDeg(angle)*r,y=-15+Mathf.sinDeg(angle)*(42+i%9*7),z=64+(i%9-4)*12+Mathf.sin(time/90+i)*10;
            float c=(.7f+(i%5)*.27f)*orbitGrowth;
            orbitMesh.at(x,y,z).rot(0,time*(.3f+i%3*.17f)+i*17).rot(1,time*.23f+i*13).rot(2,time*.19f+i*29)
                .style(0,i%3==0?StormTextures.orbitBlack:StormTextures.orbitViolet).color(Color.white).box(-c,-c,-c,c,c,c,true);
        }
        if(fracture>0)for(int i=0;i<rig.bones;i++)rig.alpha[i]*=.85f;
        renderer.cam=new Cam(rig.half*scale,8.8f);renderer.reset();renderer.pose(rig,heading,0,0,0,scale);
    }
    private void orientHead(int h,float y,float p,float r){
        int o=heads[h]*12;float[] m=rig.local;float hs=headSize[h];
        m[o]=m[o+5]=m[o+10]=hs;m[o+1]=m[o+2]=m[o+4]=m[o+6]=m[o+8]=m[o+9]=0;
        rig.euler(heads[h],y,p,r);headYaw[h]=y;headPitch[h]=p;
    }
    public void reactMeal(int head,float age){if(head>=0&&head<3)feedAt[head]=age;}
    public void aimHeads(wstorm.world.StormUnit u){
        if(u.breachTicks>0){rig.hidden[core]=false;rig.hidden[armour]=true;}
        if(Float.isFinite(previousSkullTimer)&&u.skullTimer>previousSkullTimer+15&&!u.mature())shotAt=u.age;
        previousSkullTimer=u.skullTimer;
        if(u.eatenUnits>previousEaten){for(int h=0;h<3;h++)if(u.targetId[h]<0)feedAt[h]=Math.max(feedAt[h],u.age);}
        previousEaten=u.eatenUnits;
        for(int h=0;h<3;h++){
            float m=morph[h];boolean feeding=u.beamReady(h),tracking=u.targetId[h]>=0;
            yaw[h]=u.aimYaw[h];pitch[h]=u.aimPitch[h];
            float recoil=m<.8f?StormAnimation.pulse(u.age,shotAt,5,26):0;
            float swallow=StormAnimation.pulse(u.age,feedAt[h],6,32);
            float intake=h==0&&u.debrisTicks>0?StormAnimation.smooth((40-u.debrisTicks)/22):0;
            float rumble=m*StormAnimation.rumble(u.age,h)*(tracking?.2f:1);
            orientHead(h,yaw[h],pitch[h],StormAnimation.roll(u.age,h));
            StormAnimation.hinge(rig,normalMouth[h],0,-(1-m)*(recoil*7+swallow*5+intake*7),0,-8,-3.5f);
            StormAnimation.hinge(rig,jaws[h],0,-m*(swallow*13+intake*10+rumble*8+(feeding?1.6f:0)),0,-14.4f,-6);
        }
        for(int i=0;i<branches;i++)for(int j=0;j<7;j++){
            wstorm.world.StormTentacles.local(u,animationMass,u.age,i,j/7f,a);wstorm.world.StormTentacles.local(u,animationMass,u.age,i,(j+1)/7f,b);
            int bone=limbs[i][j];float len=rig.aim(bone,a[0],a[1],a[2],b[0],b[1],b[2],1,0,0),birth=wstorm.world.StormTentacles.birth(animationMass,i),w=(4.8f*(1-j/7f)+.3f)*birth;
            rig.scale(bone,w,w,len*1.015f);
        }
        renderer.pose(rig,u.bodyFacing(),0,0,0,scale);
    }
    public void eye(int h,float[] out){wstorm.world.StormAim.portLocal(animationMass,h,out);renderer.point(wstorm.world.StormAim.stormPort(animationMass,h)?stormEyes[h]:normalEyes[h],out[0],out[1],out[2],out);}
    public boolean beamVisible(wstorm.world.StormUnit u,int h){
        if(!u.beamReady(h))return false;
        wstorm.world.StormAim.portLocal(animationMass,h,a);int bone=wstorm.world.StormAim.stormPort(animationMass,h)?stormEyes[h]:normalEyes[h];
        renderer.point(bone,a[0],a[1]+1,a[2],b);eye(h,a);
        float dx=u.targetX[h]-u.x-a[0],dy=u.targetY[h]-u.y-a[1],dz=-a[2],vx=b[0]-a[0],vy=b[1]-a[1],vz=b[2]-a[2];
        return (dx*vx+dy*vy+dz*vz)>Mathf.cosDeg(2)*(float)Math.sqrt((dx*dx+dy*dy+dz*dz)*(vx*vx+vy*vy+vz*vz));
    }
    public void mouth(int h,float[] out){renderer.point(heads[h],0,Mathf.lerp(11,16.8f,morph[h]),-6-morph[h]*2,out);}
    public void project(float[] p){float z=p[2];p[0]=renderer.screenX(p[0],z);p[1]=renderer.screenY(p[1],z);}
}
