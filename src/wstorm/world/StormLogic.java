package wstorm.world;

import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.type.*;
import wstorm.*;
import wstorm.gfx.*;

/** Server-authoritative ingestion. Visual ghosts never determine damage or growth. */
public final class StormLogic{
    private static final Vec2 pull=new Vec2(),mouth=new Vec2();
    public static long realUnitsTaken,realBuildingsTaken;
    public static final float rangeEpsilon=.25f;
    public static boolean enemy(StormUnit s,Team team){return team!=s.team&&team!=Team.derelict;}
    public static boolean taken(StormUnit s,int id,int kind,int except){for(int i=0;i<3;i++)if(i!=except&&s.targetId[i]==id&&s.targetKind[i]==kind)return true;return false;}
    public static Teamc resolve(StormUnit s,int h){
        if(s.targetId[h]<0)return null;
        if(s.targetKind[h]==1)return Groups.unit.getByID(s.targetId[h]);
        if(s.targetKind[h]==2)return Vars.world.build(s.targetId[h]);
        return null;
    }
    private static boolean valid(StormUnit s,Teamc target){
        return target!=null&&enemy(s,target.team())&&s.within(target,Math.max(s.reach()+rangeEpsilon,110*s.growth()))
            &&(!(target instanceof Unit u)||(u.isAdded()&&!u.dead&&!(u instanceof StormUnit)))
            &&(!(target instanceof Building b)||b.isValid());
    }
    private static final Teamc[] nearUnits=new Teamc[3],nearBuildings=new Teamc[3],chosen=new Teamc[3];
    private static final arc.struct.Seq<StormUnit> owners=new arc.struct.Seq<>();
    private static final Teamc[] nearCapture=new Teamc[StormUnit.maxBuildingSlots];
    private static final Unit[] nearDirect=new Unit[StormUnit.directSlots];
    private static final boolean[] used=new boolean[3];
    private static final int[] assigned=new int[3];
    private static int key(Teamc t){return t instanceof Building b?b.pos():((Unit)t).id;}
    private static byte kind(Teamc t){return (byte)(t instanceof Building?2:1);}
    private static boolean same(Teamc a,Teamc b){return a!=null&&b!=null&&kind(a)==kind(b)&&key(a)==key(b);}
    private static void nearest(StormUnit s,Teamc[] out,Teamc t){
        for(int i=0;i<out.length;i++)if(out[i]==null||(s.isPlayer()&&out!=nearCapture?Mathf.dst2(s.aimX,s.aimY,t.x(),t.y())<Mathf.dst2(s.aimX,s.aimY,out[i].x(),out[i].y()):s.dst2(t)<s.dst2(out[i]))){for(int j=out.length-1;j>i;j--)out[j]=out[j-1];out[i]=t;return;}
    }
    private static int addChosen(Teamc t,int n){if(t==null||n==3)return n;for(int i=0;i<n;i++)if(same(chosen[i],t))return n;chosen[n]=t;return n+1;}
    public static boolean directHeld(StormUnit s,int id){for(int value:s.directId)if(value==id)return true;return false;}
    private static boolean otherOwner(StormUnit s,Unit u){
        for(StormUnit other:owners)if(other!=s&&!other.dead){
            if(directHeld(other,u.id))return true;
            if(other.beamCapable(0))for(int h=0;h<3;h++)if(other.beamReady(h)&&other.targetKind[h]==1&&other.targetId[h]==u.id)return true;
        }return false;
    }
    /** Three locks are planned together and committed before any pulling/render state changes. */
    public static void acquire(StormUnit s){
        owners.clear();for(Unit q:Groups.unit)if(q instanceof StormUnit other&&!other.dead)owners.add(other);
        java.util.Arrays.fill(nearCapture,null);java.util.Arrays.fill(nearUnits,null);java.util.Arrays.fill(nearBuildings,null);java.util.Arrays.fill(nearDirect,null);java.util.Arrays.fill(chosen,null);
        for(int i=0;i<StormUnit.directSlots;i++){
            Unit u=Groups.unit.getByID(s.directId[i]);
            if(u==null||u.dead||!u.isAdded()||!enemy(s,u.team)||!s.within(u,Math.max(s.creatureReach(),110*s.growth())))s.directId[i]=-1;
        }
        for(Unit u:Groups.unit){
            if(u.dead||!u.isAdded()||u instanceof StormUnit||!enemy(s,u.team)||otherOwner(s,u))continue;
            if(s.within(u,s.reach()+rangeEpsilon))nearest(s,nearUnits,u);
            if(s.within(u,s.creatureReach()+rangeEpsilon)&&!directHeld(s,u.id))nearest(s,nearDirect,u);
        }
        final Building[] capture={null};final float[] distance={Float.MAX_VALUE};
        Units.nearbyBuildings(s.x,s.y,Math.max(s.reach(),s.buildingReach()),q->{
            if(!q.isValid()||!enemy(s,q.team))return;
            if(s.within(q,s.reach()+rangeEpsilon))nearest(s,nearBuildings,q);
            if(s.within(q,s.buildingReach()+rangeEpsilon))nearest(s,nearCapture,q);
            if(s.within(q,s.buildingReach()+rangeEpsilon)&&s.dst2(q)<distance[0]){distance[0]=s.dst2(q);capture[0]=q;}
        });
        s.buildingTarget=capture[0]==null?-1:capture[0].pos();
        for(int i=0;i<s.captureCandidates.length;i++)s.captureCandidates[i]=nearCapture[i]==null?-1:((Building)nearCapture[i]).pos();
        int n=0;
        // Keep valid unit locks stable, but repair duplicates as soon as another candidate exists.
        for(int h=0;h<3;h++){Teamc t=resolve(s,h);if(!s.isPlayer()&&t instanceof Unit u&&valid(s,t)&&!otherOwner(s,u))n=addChosen(t,n);}
        for(Teamc t:nearUnits)n=addChosen(t,n);
        for(int h=0;h<3;h++){Teamc t=resolve(s,h);if(t instanceof Building&&valid(s,t))n=addChosen(t,n);}
        for(Teamc t:nearBuildings)n=addChosen(t,n);
        java.util.Arrays.fill(used,false);java.util.Arrays.fill(assigned,-1);
        for(int h=0;h<3;h++)for(int i=0;i<n;i++)if(!used[i]&&same(resolve(s,h),chosen[i])){assigned[h]=i;used[i]=true;break;}
        for(int h=0;h<3;h++)if(assigned[h]<0)for(int i=0;i<n;i++)if(!used[i]){assigned[h]=i;used[i]=true;break;}
        for(int h=0;h<3;h++){
            int index=assigned[h]>=0?assigned[h]:n>0?h%n:-1;Teamc t=index<0?null:chosen[index];
            if(t==null){s.targetId[h]=-1;s.targetKind[h]=0;s.locked[h]=0;continue;}
            if(s.targetId[h]!=key(t)||s.targetKind[h]!=kind(t)){s.locked[h]=0;s.targetX[h]=t.x();s.targetY[h]=t.y();}
            s.targetId[h]=key(t);s.targetKind[h]=kind(t);
        }
        for(int i=0;i<StormUnit.directSlots;i++)if(s.directId[i]>=0){Unit u=Groups.unit.getByID(s.directId[i]);if(u!=null)s.directHead[i]=(byte)destination(s,u);}
        int candidate=0;
        for(int i=0;i<StormUnit.directSlots&&candidate<nearDirect.length;i++)if(s.directId[i]<0){
            Unit u=nearDirect[candidate++];if(u==null)break;s.directId[i]=u.id;s.directX[i]=u.x;s.directY[i]=u.y;
            s.directHead[i]=(byte)destination(s,u);
        }
    }
    private static int destination(StormUnit s,Unit u){
        boolean linked=false;for(int h=0;h<3;h++)if(s.targetKind[h]==1&&s.targetId[h]==u.id)linked=true;
        int head=0;float closest=Float.MAX_VALUE;
        for(int h=0;h<3;h++)if(!linked||s.targetKind[h]==1&&s.targetId[h]==u.id){s.headPoint(h,mouth);float d=mouth.dst2(u.x,u.y);if(d<closest){closest=d;head=h;}}
        return head;
    }
    private static boolean pullCreature(StormUnit s,int head,Unit u,float heldX,float heldY,float dt){
        StormLift.hold(s,u);
        s.headPoint(head,mouth);float step=s.pullSpeed()*dt,arrival=5.5f*s.growth()+step;
        if(Mathf.dst(u.x,u.y,mouth.x,mouth.y)<=arrival){ingest(s,head,u);return false;}
        float remaining=Mathf.dst(heldX,heldY,mouth.x,mouth.y);
        pull.set(mouth.x-heldX,mouth.y-heldY).setLength(Math.min(remaining,step));u.vel.setZero();StormLift.move(u,heldX+pull.x,heldY+pull.y);
        if(remaining<=arrival){ingest(s,head,u);return false;}return true;
    }
    private static float foodValue(float health,float size,float base){
        float hp=Float.isFinite(health)?Mathf.clamp(health,0,9e6f):9e6f;
        return Math.min(1e6f,base+hp*.10f+Mathf.clamp(size,0,1000)*1.3f);
    }
    private static void credit(StormUnit s,float value){
        value=Mathf.clamp(value,0,1e6f);s.absorbedMatter=Math.min(1e12f,s.absorbedMatter+value);s.matter=Math.min(1e12f,s.matter+value);
        s.maxHealth=WitherStormMod.spec.initialHealth+s.matter*WitherStormMod.spec.healthPerMatter;s.heal(value*35);
    }
    public static boolean beginBuilding(StormUnit s,Building b){
        if(Vars.net.client()||s.buildingsInFlight()>=s.buildingCapacity()||b==null||!b.isValid()||!enemy(s,b.team)||!s.within(b,s.buildingReach()+rangeEpsilon))return false;
        int slot=0;while(slot<StormUnit.maxBuildingSlots&&s.flightTime(slot)>0)slot++;
        if(slot==StormUnit.maxBuildingSlots)return false;
        float value=Math.min(1e6f,24*b.block.size*b.block.size+(Float.isFinite(b.maxHealth)?Mathf.clamp(b.maxHealth,0,9e6f):9e6f)*.028f);
        float bx=b.x,by=b.y;int block=b.block.id;
        if(slot==0){s.debrisX=bx;s.debrisY=by;s.debrisBlock=block;s.debrisValue=value;s.debrisTicks=95;}
        else{s.flightX[slot]=bx;s.flightY[slot]=by;s.flightBlock[slot]=block;s.flightValue[slot]=value;s.flightTicks[slot]=95;}
        b.tile.removeNet();s.buildingTarget=-1;
        String packet=s.id+";"+(slot%3)+";2;"+block+";"+bx+";"+by+";"+s.age+";-1";
        if(!Vars.headless)StormVisual.receive(packet);if(Vars.net.server())Call.clientPacketReliable("wither-storm-ingest",packet);
        return true;
    }
    public static void update(StormUnit s){
        if(Vars.net.client()||s.dead)return;
        float dt=Math.min(Time.delta,6f);
        s.age+=dt;s.breachTicks=Math.max(0,s.breachTicks-dt);
        s.maxHealth=WitherStormMod.spec.initialHealth+s.matter*WitherStormMod.spec.healthPerMatter;
        s.hitSize=36+18*s.growth();
        if(s.debrisTicks>0){s.debrisTicks=Math.max(0,s.debrisTicks-dt);if(s.debrisTicks==0){credit(s,s.debrisValue);s.debrisValue=0;s.eatenBuildings++;realBuildingsTaken++;}}
        for(int i=1;i<StormUnit.maxBuildingSlots;i++)if(s.flightTicks[i]>0){s.flightTicks[i]=Math.max(0,s.flightTicks[i]-dt);if(s.flightTicks[i]==0){credit(s,s.flightValue[i]);s.flightValue[i]=0;s.eatenBuildings++;realBuildingsTaken++;}}
        if(s.rootId>=0){
            Unit root=Groups.unit.getByID(s.rootId);
            if(root==null||root.dead){collapse(s);return;}
        }
        if(s.reassembly>0){
            s.reassembly=Math.max(0,s.reassembly-dt);s.heal(s.maxHealth*.0016f*dt);
            // Reassembly no longer serializes or disables movement, feeding and combat.
        }
        if((s.scanTimer-=dt)<=0){acquire(s);s.scanTimer=12;}
        StormAim.update(s,dt);
        float range=s.reach();
        beginBuilding(s,Vars.world.build(s.buildingTarget));
        for(int id:s.captureCandidates){if(s.buildingsInFlight()>=s.buildingCapacity())break;if(id>=0)beginBuilding(s,Vars.world.build(id));}
        // Passive capture is independent of purple beams, including the initial ordinary-Wither stage.
        for(int i=0;i<StormUnit.directSlots;i++){
            Unit u=Groups.unit.getByID(s.directId[i]);
            if(!valid(s,u)||!s.within(u,Math.max(s.creatureReach()+rangeEpsilon,110*s.growth()))){s.directId[i]=-1;continue;}
            if(pullCreature(s,s.directHead[i],u,s.directX[i],s.directY[i],dt)){s.directX[i]=u.x;s.directY[i]=u.y;}
        }
        for(int h=0;h<3;h++){
            Teamc t=resolve(s,h);if(!valid(s,t)){if(s.targetId[h]>=0)s.scanTimer=0;s.targetId[h]=-1;s.targetKind[h]=0;s.locked[h]=0;continue;}
            if(t instanceof Unit u&&s.beamCapable(h)&&!directHeld(s,u.id)){
                s.headPoint(h,mouth);if(Mathf.dst(u.x,u.y,mouth.x,mouth.y)<=5.5f*s.growth()+s.pullSpeed()*dt){ingest(s,h,u);continue;}
            }
            if(t instanceof Unit u&&s.beamReady(h)&&!directHeld(s,u.id)){
                boolean already=false;for(int q=0;q<h;q++)if(s.beamReady(q)&&s.targetKind[q]==1&&s.targetId[q]==u.id)already=true;
                if(!already){float heldX=s.locked[h]>0?s.targetX[h]:u.x,heldY=s.locked[h]>0?s.targetY[h]:u.y;
                    if(!pullCreature(s,h,u,heldX,heldY,dt))continue;}
            }
            s.targetX[h]=t.x();s.targetY[h]=t.y();s.locked[h]+=dt;
        }
        // A swallowed target may unlock beam evolution in this very tick. Refill all vacant heads
        // before publishing the state, rather than leaving one head dark until the next scan.
        if(s.scanTimer<=0){acquire(s);s.scanTimer=12;}
        StormTentacles.update(s,dt);
        if(!s.mature()&&(!s.isPlayer()||s.isShooting)&&(s.skullTimer-=dt)<=0){
            s.skullTimer=Math.max(45,100-s.growth()*12);
            for(int h=0;h<3;h++){
                if(wstorm.model.StormModel.mutation(s.matter,h)>.8f)continue;
                Teamc t=resolve(s,h);if(!valid(s,t)||Math.abs(StormAim.delta(s.angleTo(t),s.bodyFacing()))>55)continue;
                // Three ordinary heads may fire while moving or simultaneously feeding.

                float g=s.growth(),side=h==0?0:h==1?-27:27;
                float sx=s.x+Mathf.cosDeg(s.bodyFacing())*12*g+Mathf.cosDeg(s.bodyFacing()+90)*side*g;
                float sy=s.y+Mathf.sinDeg(s.bodyFacing())*12*g+Mathf.sinDeg(s.bodyFacing()+90)*side*g;
                // Per-head locks, one volley clock. Lifetime is extended only as far as its own target.
                StormContent.skull.createNet(s.team,sx,sy,Angles.angle(sx,sy,t.x(),t.y()),80*(1+g*.4f),1,
                    Math.max(1,Mathf.dst(sx,sy,t.x(),t.y())/(StormContent.skull.speed*StormContent.skull.lifetime)+.1f));
            }
        }
    }
    public static boolean ingest(StormUnit s,int head,Teamc target){
        if(target instanceof Building building)return beginBuilding(s,building);
        if(Vars.net.client()||!valid(s,target))return false;
        float value,x=target.x(),y=target.y();int kind,content,victim=-1;
        if(target instanceof Unit u){
            if(u.dead||!u.isAdded())return false;
            value=foodValue(u.maxHealth,u.hitSize,18);kind=1;content=u.type.id;
            // Quiet authoritative removal. Vanilla unitDespawn emits an unrelated
            // orange shrink explosion, so replicas receive our explicit reliable event.
            victim=u.id;
            if(u.isPlayer())Call.unitClear(u.getPlayer());
            u.health=0;u.dead=true;u.remove();s.eatenUnits++;realUnitsTaken++;
        }else if(target instanceof Building b){
            if(!b.isValid())return false;
            value=24*b.block.size*b.block.size+b.maxHealth*.028f;kind=2;content=b.block.id;
            b.tile.removeNet();s.eatenBuildings++;realBuildingsTaken++;
        }else return false;
        // Credit once when removed; transient swallowing animation cannot lose credit on save.
        credit(s,value);s.scanTimer=0;
        for(int h=0;h<3;h++)if(s.targetKind[h]==kind&&s.targetId[h]==victim){s.targetId[h]=-1;s.targetKind[h]=0;s.locked[h]=0;}
        for(int i=0;i<StormUnit.directSlots;i++)if(s.directId[i]==victim)s.directId[i]=-1;
        String packet=s.id+";"+head+";"+kind+";"+content+";"+x+";"+y+";"+s.age+";"+victim;
        if(!Vars.headless)StormVisual.receive(packet);
        if(Vars.net.server())Call.clientPacketReliable("wither-storm-ingest",packet);
        return true;
    }
    public static int active(){return Groups.unit.count(u->u instanceof StormUnit&&!u.dead);}
    public static StormUnit spawn(Team team,float x,float y){
        if(Vars.net.client()||active()>=WitherStormMod.spec.maxActive)return null;
        StormUnit s=(StormUnit)StormContent.storm.create(team);
        s.matter=WitherStormMod.spec.initialMatter;s.visualMatter=s.matter;s.coreIntegrity=WitherStormMod.spec.initialCore;
        s.genome=(int)(x*7919+y*97+s.id*83);s.maxHealth=WitherStormMod.spec.initialHealth+s.matter*WitherStormMod.spec.healthPerMatter;s.health=s.maxHealth;
        s.set(x,y);s.rotation=270;s.add();
        Call.effectReliable(StormFx.birth,x,y,0,StormFx.purple);return s;
    }
    public static void fracture(StormUnit s){
        if(Vars.net.client()||s.reassembly>0||s.rootId>=0)return;
        java.util.Arrays.fill(s.directId,-1);
        s.reassembly=210;s.health=Math.max(s.health,s.maxHealth*.24f);
        Call.effectReliable(StormFx.fracture,s.x,s.y,s.growth(),StormFx.purple);
        if(!Vars.headless)StormAudio.ruptureAt(s.x,s.y);
        if(s.fissions==0&&s.matter>=2800&&active()+2<=WitherStormMod.spec.maxActive){
            s.fissions=1;float amount=s.matter*.18f,spread=75*s.growth();
            for(int side:new int[]{-1,1}){
                StormUnit child=spawn(s.team,s.x+side*spread,s.y-spread*.63f);
                if(child!=null){child.rootId=s.id;child.matter=amount;child.visualMatter=amount;child.fissions=1;child.absorbedMatter=s.absorbedMatter*.18f;child.maxHealth=WitherStormMod.spec.initialHealth+amount*WitherStormMod.spec.healthPerMatter;child.health=child.maxHealth;s.matter-=amount;}
            }
        }
    }
    public static void collapse(StormUnit s){
        if(Vars.net.client()||!s.isAdded())return;
        StormFx.fracture.at(s.x,s.y,s.growth(),StormFx.purple);s.forceCollapse=true;
        if(Vars.net.server())Call.clientPacketReliable("wither-storm-vanish",s.id+";"+s.x+";"+s.y+";"+s.growth());
        if(s.isPlayer())Call.unitClear(s.getPlayer());
        s.dead=true;s.remove();
    }
    public static void died(StormUnit s){
        StormFx.fracture.at(s.x,s.y,s.growth(),StormFx.purple);
        if(!Vars.net.client())Groups.unit.copy().each(u->{if(u instanceof StormUnit child&&child.rootId==s.id)collapse(child);});
    }
}
