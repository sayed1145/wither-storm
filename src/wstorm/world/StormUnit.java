package wstorm.world;

import arc.util.io.*;
import mindustry.*;
import mindustry.gen.*;
import wstorm.*;

/** Real game entity; all persistent growth variables are serialized and synchronized. */
public class StormUnit extends UnitEntity{
    public static int mapping;
    public float matter=90,age,coreIntegrity=2600,reassembly,scanTimer,skullTimer;
    public int eatenUnits,eatenBuildings,rootId=-1,fissions,genome=12573;
    public final int[] targetId={-1,-1,-1};
    public final byte[] targetKind={0,0,0};
    public final float[] targetX=new float[3],targetY=new float[3],locked=new float[3];
    public transient float visualMatter=90;
    public transient boolean forceCollapse;
    @Override public int classId(){return mapping;}
    public float breachTicks;
    public int buildingTarget=-1,debrisBlock=-1;
    public float debrisTicks,debrisValue,debrisX,debrisY;
    public static final int directSlots=12;
    public static final int maxBuildingSlots=32;
    // Slot zero remains the v3/v4 flight fields for backwards-compatible loading.
    public final float[] flightTicks=new float[maxBuildingSlots],flightValue=new float[maxBuildingSlots],flightX=new float[maxBuildingSlots],flightY=new float[maxBuildingSlots];
    public final int[] flightBlock=new int[maxBuildingSlots],captureCandidates=new int[maxBuildingSlots];
    public final float[] tentacleStrike=new float[8];
    public float bodyYaw=Float.NaN;
    public final float[] aimYaw=new float[3],aimPitch=new float[3],strikeX=new float[8],strikeY=new float[8];
    public final boolean[] strikeDone=new boolean[8];
    public float bodyFacing(){return Float.isFinite(bodyYaw)?bodyYaw:rotation;}
    public boolean beamReady(int h){return StormAim.ready(this,h);}
    public float tentacleTimer;public int tentacleHits;
    {java.util.Arrays.fill(captureCandidates,-1);java.util.Arrays.fill(tentacleStrike,-10000);}
    public int buildingCapacity(){return Math.min(maxBuildingSlots,5+(int)(3*Math.log1p(Math.max(0,matter-90)/400)));}
    public float flightTime(int i){return i==0?debrisTicks:flightTicks[i];}
    public float flightOriginX(int i){return i==0?debrisX:flightX[i];}
    public float flightOriginY(int i){return i==0?debrisY:flightY[i];}
    public int flightContent(int i){return i==0?debrisBlock:flightBlock[i];}
    public int buildingsInFlight(){int n=0;for(int i=0;i<maxBuildingSlots;i++)if(flightTime(i)>0)n++;return n;}
    @Override public boolean isCommandable(){return !isPlayer()&&controller() instanceof StormAI&&StormCommand.enabled(team);}
    @Override public void resetController(){super.resetController();if(StormCommand.enabled(team))vel.setZero();}
    @Override public float speed(){return travelSpeed()*floorSpeedMultiplier();}
    public float absorbedMatter;
    public final int[] directId=new int[directSlots];
    public final byte[] directHead=new byte[directSlots];
    public final float[] directX=new float[directSlots],directY=new float[directSlots];
    {java.util.Arrays.fill(directId,-1);}
    public float rangeGrowth(){return (float)Math.log1p(Math.max(0,absorbedMatter)/WitherStormMod.spec.initialMatter);}
    public float initialBuildingReach(){return 52+18*WitherStormMod.spec.growth(WitherStormMod.spec.initialMatter);}
    public float buildingReach(){return 15*initialBuildingReach()*(1+.03f*rangeGrowth());}
    public float creatureReach(){return 7.5f*initialBuildingReach()*(1+.015f*rangeGrowth());}
    public float stopReach(mindustry.gen.Teamc target){return target instanceof Building?buildingReach():matter>=420?reach():creatureReach();}
    public float pullSpeed(){return .30f+.10f*(float)Math.sqrt(growth());}
    private final float[] animatedAnchor=new float[3];
    /** Authoritative 2D head capture point uses the same perspective as the live 3D rig. */
    public void headPoint(int h,arc.math.geom.Vec2 out){
        float g=growth();wstorm.model.StormAnimation.anchor(matter,age,h,animatedAnchor);
        float lx=animatedAnchor[0],ly=animatedAnchor[1];
        float z=(animatedAnchor[2]+wstorm.model.StormAnimation.bob(matter,age))*g;
        float c=arc.math.Mathf.cosDeg(bodyFacing()-90),sn=arc.math.Mathf.sinDeg(bodyFacing()-90);
        float X=(lx*c-ly*sn)*g,Y=(lx*sn+ly*c)*g,D=190*g*7,cy=-190*g*8.8f;
        float projection=D/(D-z);out.set(x+X*projection,y+cy+(Y-cy)*projection);
    }
    
    public int headCount(){return 3;}
    public int stormHeadCount(){int n=0;for(int h=0;h<3;h++)if(wstorm.model.StormModel.mutation(matter,h)>.99f)n++;return n;}
    public boolean beamCapable(int head){return head>=0&&head<3&&matter>=420;}
    public boolean mature(){return matter>=1640;}
    public boolean exposed(){return matter<360 || breachTicks>0;}
    public float travelSpeed(){return 1.25f/(1f+growth()*.65f);}
    public void pierceCore(float damage){
        if(Vars.net.client()||dead)return;
        if(!exposed()){breachTicks=240;wstorm.gfx.StormFx.fracture.at(x,y,.5f);return;}
        coreIntegrity=Math.max(0,coreIntegrity-damage);breachTicks=240;
        if(coreIntegrity<=0){forceCollapse=true;kill();}
    }
    public float growth(){return WitherStormMod.spec.growth(matter);}
    public float reach(){return 15*WitherStormMod.spec.reach*(.75f+WitherStormMod.spec.growth(WitherStormMod.spec.initialMatter)*.38f)*(1+.03f*rangeGrowth());}
    @Override public void rawDamage(float amount){
        if(!Float.isFinite(amount)||amount<=0)return;
        if(rootId>=0||forceCollapse){super.rawDamage(amount);return;}
        if(matter<360&&!Vars.net.client())coreIntegrity=Math.max(0,coreIntegrity-amount*.70f);
        if(coreIntegrity<=0){forceCollapse=true;super.rawDamage(Math.max(amount,health+shield+100));return;}
        // Hook rawDamage, not damage(amount, effect): the vanilla overload calls
        // damage virtually, so overriding both would create recursion.
        super.rawDamage(Math.min(exposed()?amount:amount*.08f,Math.max(0,health-2)));
        if(health<maxHealth*.22f&&reassembly<=0&&!Vars.net.client())StormLogic.fracture(this);
    }
    private void writeState(Writes w){
        w.b(6);w.f(breachTicks);w.f(matter);w.f(age);w.f(coreIntegrity);w.f(reassembly);
        w.i(eatenUnits);w.i(eatenBuildings);w.i(rootId);w.i(fissions);w.i(genome);
        for(int h=0;h<3;h++){w.i(targetId[h]);w.b(targetKind[h]);w.f(targetX[h]);w.f(targetY[h]);w.f(locked[h]);}
        w.i(buildingTarget);w.i(debrisBlock);w.f(debrisTicks);w.f(debrisValue);w.f(debrisX);w.f(debrisY);
        w.f(absorbedMatter);for(int i=0;i<directSlots;i++){w.i(directId[i]);w.b(directHead[i]);w.f(directX[i]);w.f(directY[i]);}
        for(int i=1;i<maxBuildingSlots;i++){w.f(flightTicks[i]);w.f(flightValue[i]);w.f(flightX[i]);w.f(flightY[i]);w.i(flightBlock[i]);}
        for(float t:tentacleStrike)w.f(t);w.i(tentacleHits);
        w.f(bodyFacing());for(int h=0;h<3;h++){w.f(aimYaw[h]);w.f(aimPitch[h]);}
        for(int i=0;i<8;i++){w.f(strikeX[i]);w.f(strikeY[i]);w.bool(strikeDone[i]);}
    }
    private void readState(Reads r){
        int version=r.ub();if(version<1||version>6)throw new IllegalArgumentException("Storm entity data version "+version);breachTicks=version>=2?finite(r.f(),0,0,240):0;
        matter=finite(r.f(),90,0,1e12f);age=finite(r.f(),0,0,1e12f);coreIntegrity=finite(r.f(),2600,0,1e9f);reassembly=finite(r.f(),0,0,600);
        eatenUnits=Math.max(0,r.i());eatenBuildings=Math.max(0,r.i());rootId=r.i();fissions=Math.max(0,r.i());genome=r.i();
        for(int h=0;h<3;h++){targetId[h]=r.i();targetKind[h]=r.b();targetX[h]=finite(r.f(),x,-1e6f,1e6f);targetY[h]=finite(r.f(),y,-1e6f,1e6f);locked[h]=finite(r.f(),0,0,5000);}
        if(version>=3){buildingTarget=r.i();debrisBlock=r.i();debrisTicks=finite(r.f(),0,0,95);debrisValue=finite(r.f(),0,0,1e6f);debrisX=finite(r.f(),x,-1e6f,1e6f);debrisY=finite(r.f(),y,-1e6f,1e6f);}
        absorbedMatter=version>=4?finite(r.f(),0,0,1e12f):Math.max(0,matter-WitherStormMod.spec.initialMatter);
        java.util.Arrays.fill(directId,-1);
        if(version>=4)for(int i=0;i<directSlots;i++){directId[i]=r.i();directHead[i]=(byte)Math.max(0,Math.min(2,r.b()));directX[i]=finite(r.f(),x,-1e6f,1e6f);directY[i]=finite(r.f(),y,-1e6f,1e6f);}
        java.util.Arrays.fill(flightTicks,0);java.util.Arrays.fill(tentacleStrike,-10000);
        if(version>=5){for(int i=1;i<maxBuildingSlots;i++){flightTicks[i]=finite(r.f(),0,0,95);flightValue[i]=finite(r.f(),0,0,1e6f);flightX[i]=finite(r.f(),x,-1e6f,1e6f);flightY[i]=finite(r.f(),y,-1e6f,1e6f);flightBlock[i]=r.i();}for(int i=0;i<8;i++)tentacleStrike[i]=finite(r.f(),-10000,-10000,1e12f);tentacleHits=Math.max(0,r.i());}
        bodyYaw=rotation;java.util.Arrays.fill(aimYaw,0);java.util.Arrays.fill(aimPitch,0);
        if(version>=6){bodyYaw=finite(r.f(),rotation,-36000,36000);for(int h=0;h<3;h++){aimYaw[h]=finite(r.f(),0,-40,40);aimPitch[h]=finite(r.f(),0,-75,25);}for(int j=0;j<8;j++){strikeX[j]=finite(r.f(),x,-1e6f,1e6f);strikeY[j]=finite(r.f(),y,-1e6f,1e6f);strikeDone[j]=r.bool();}}
        else{java.util.Arrays.fill(tentacleStrike,-10000);java.util.Arrays.fill(strikeDone,true);}
    }
    private void refreshShape(){maxHealth=WitherStormMod.spec.initialHealth+matter*WitherStormMod.spec.healthPerMatter;hitSize=36+18*growth();}
    private void restoreStormController(){
        if(controller() instanceof mindustry.ai.types.CommandAI old&&!(old instanceof StormAI)){
            StormAI next=new StormAI();controller(next);next.command=old.command;next.targetPos=old.targetPos;next.attackTarget=old.attackTarget;next.readAttackTarget=old.readAttackTarget;next.commandQueue.addAll(old.commandQueue);next.stances.or(old.stances);next.group=old.group;next.groupIndex=old.groupIndex;
        }
    }
    @Override public void afterRead(){super.afterRead();restoreStormController();}
    @Override public void afterSync(){super.afterSync();refreshShape();restoreStormController();}
    private static float finite(float x,float fallback,float min,float max){return Float.isFinite(x)?Math.max(min,Math.min(max,x)):fallback;}
    @Override public void write(Writes w){super.write(w);writeState(w);}
    @Override public void read(Reads r){super.read(r);readState(r);refreshShape();visualMatter=matter;}
    @Override public void writeSync(Writes w){super.writeSync(w);writeState(w);}
    @Override public void readSync(Reads r){super.readSync(r);readState(r);refreshShape();}
}
