package wstest;
import arc.*;import arc.files.*;import arc.math.*;import arc.struct.*;import arc.util.*;import arc.util.io.*;
import mindustry.*;import mindustry.content.*;import mindustry.game.*;import mindustry.gen.*;import mindustry.io.*;import mindustry.world.*;
import wstorm.*;import wstorm.world.*;import wstorm.model.*;import wstorm.gfx.*;
import java.io.*;

/** v1.5 targeted regression; real engine entities, persistence and damage, not mock result flags. */
public class MechanicsChecks{
    static void check(String name,boolean ok){HeadlessChecks.check(name,ok);}
    static StormUnit fresh(){
        Vars.logic.reset();Vars.state.rules=new Rules();Vars.state.rules.canGameOver=false;Vars.state.rules.waves=false;Vars.state.rules.unitCap=200;
        Vars.state.map=new mindustry.maps.Map(StringMap.of("name","v1.5 mechanics audit"));
        Vars.world.loadGenerator(400,200,ts->{for(int x=0;x<ts.width;x++)for(int y=0;y<ts.height;y++)ts.set(x,y,new Tile(x,y,Blocks.stone,Blocks.air,Blocks.air));});
        HeadlessChecks.playing();Time.setDeltaProvider(()->1f);Time.delta=1;
        StormUnit s=HeadlessChecks.storm(400,400);s.skullTimer=10000;return s;
    }
    static boolean unique(StormUnit s){return s.targetId[0]>=0&&s.targetId[1]>=0&&s.targetId[2]>=0&&s.targetId[0]!=s.targetId[1]&&s.targetId[0]!=s.targetId[2]&&s.targetId[1]!=s.targetId[2];}
    static void update(StormUnit s){Time.delta=1;StormLogic.update(s);}
    public static void run()throws Exception{
        StormUnit s=fresh();float g=WitherStormMod.spec.growth(WitherStormMod.spec.initialMatter),oldB=52+18*g,oldU=WitherStormMod.spec.reach*(.75f+.38f*g);
        check("building-radius-exactly-fifteen-times-original-start",Math.abs(s.buildingReach()-oldB*15)<.001f);
        check("beam-radius-exactly-fifteen-times-original-start",Math.abs(s.reach()-oldU*15)<.001f);
        check("beamless-creature-radius-is-half-boosted-buildings",Math.abs(s.creatureReach()-s.buildingReach()*.5f)<.001f);
        float b=s.buildingReach(),u=s.creatureReach(),beam=s.reach();s.absorbedMatter=5000;
        check("ranges-grow-slowly-with-absorbed-strength",s.buildingReach()>b&&s.buildingReach()<b*1.15f&&s.reach()>beam);
        check("passive-creature-radius-grows-more-slowly",s.creatureReach()/u<s.buildingReach()/b&&s.creatureReach()>u);
        s.absorbedMatter=0;
        Unit a=HeadlessChecks.unit(Team.crux,1100,400),c=HeadlessChecks.unit(Team.crux,1150,430),d=HeadlessChecks.unit(Team.crux,1200,370);Vars.state.teams.updateTeamStats();
        StormLogic.acquire(s);check("initial-three-normal-heads-have-distinct-locks",unique(s)&&s.stormHeadCount()==0);
        c.remove();d.remove();StormLogic.acquire(s);check("share-only-when-no-other-targets-exist",s.targetId[0]==a.id&&s.targetId[1]==a.id&&s.targetId[2]==a.id);
        s.matter=420;s.scanTimer=10000;for(int i=0;i<100;i++)StormAim.update(s,1);float x=a.x,y=a.y;update(s);
        check("all-three-beams-enable-in-the-same-update",s.beamCapable(0)&&s.beamCapable(1)&&s.beamCapable(2)&&s.locked[0]==s.locked[1]&&s.locked[1]==s.locked[2]);
        check("shared-target-pull-is-not-tripled",Mathf.dst(x,y,a.x,a.y)>0&&Mathf.dst(x,y,a.x,a.y)<=s.pullSpeed()+.001f);
        c=HeadlessChecks.unit(Team.crux,1150,430);d=HeadlessChecks.unit(Team.crux,1200,370);StormLogic.acquire(s);
        check("shared-locks-split-when-extra-targets-appear",unique(s));s.matter=2600;StormLogic.acquire(s);check("mature-heads-retain-three-distinct-locks",unique(s));
        s.controller(new StormAI());s.vel.set(3,2);s.controller().updateUnit();check("inside-beam-range-moves-at-half-speed",Math.abs(s.vel.len()-s.travelSpeed()*.5f)<.001f);
        s.matter=90;a.set(400+s.creatureReach()-10,400);s.vel.set(3,2);s.controller().updateUnit();check("inside-passive-range-still-approaches",s.vel.len()>0);
        a.set(400+s.creatureReach()+20,400);s.controller().updateUnit();check("outside-prescribed-range-resumes-pursuit",s.vel.len()>0);
        Building wall=HeadlessChecks.place(Blocks.titaniumWall,62,50,Team.crux);s.controller(new StormAI());s.controller().updateUnit();
        check("unit-priority-wins-over-nearer-building",((StormAI)s.controller()).currentTarget()==a&&s.vel.len()>0);
        s.scanTimer=0;update(s);s.controller().updateUnit();check("building-capture-does-not-stop-movement",s.debrisTicks>0&&s.vel.len()>0);
        s=fresh();s.matter=400;arc.math.geom.Vec2 arrival=new arc.math.geom.Vec2();s.headPoint(0,arrival);
        Unit thresholdFood=HeadlessChecks.unit(Team.crux,arrival.x,arrival.y);
        HeadlessChecks.unit(Team.crux,1100,400);HeadlessChecks.unit(Team.crux,1150,430);HeadlessChecks.unit(Team.crux,1200,370);
        StormLogic.acquire(s);check("threshold-test-starts-with-an-assigned-soon-swallowed-target",s.targetId[0]==thresholdFood.id&&!s.beamCapable(0));
        s.scanTimer=12;update(s);
        check("natural-threshold-ingestion-refills-all-three-beams-same-tick",!thresholdFood.isAdded()&&s.matter>=420&&unique(s)&&s.targetKind[0]==1&&s.targetKind[1]==1&&s.targetKind[2]==1);
        s=fresh();s.genome=551155;Unit passive=HeadlessChecks.unit(Team.crux,700,400);Vars.state.teams.updateTeamStats();update(s);
        check("ordinary-stage-captures-creature-with-no-purple-beam",StormLogic.directHeld(s,passive.id)&&!s.beamCapable(0)&&passive.x<700);
        int held=passive.id;float holdX=passive.x;Fi file=new Fi("v15-passive.msav");SaveIO.write(file);Vars.logic.reset();SaveIO.load(file);HeadlessChecks.playing();
        s=(StormUnit)Groups.unit.find(q->q instanceof StormUnit q0&&q0.genome==551155);passive=Groups.unit.getByID(held);
        check("passive-creature-hold-survives-world-save-load",s!=null&&passive!=null&&StormLogic.directHeld(s,held)&&Math.abs(passive.x-holdX)<.001f);
        s.skullTimer=10000;int ticks=0;while(passive.isAdded()&&!passive.dead&&ticks++<1000)update(s);
        check("beamless-creature-reaches-head-and-is-removed",!passive.isAdded()&&s.eatenUnits==1&&s.matter<420);
        check("actual-ingestion-strength-expands-range",s.absorbedMatter>0&&s.buildingReach()>15*oldB&&s.creatureReach()>7.5f*oldB);
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();s.writeSync(Writes.get(new DataOutputStream(bytes)));
        StormUnit copy=(StormUnit)StormContent.storm.create(Team.sharded);copy.readSync(Reads.get(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()))));
        check("strength-and-expanded-ranges-network-sync",copy.absorbedMatter==s.absorbedMatter&&copy.reach()==s.reach()&&copy.creatureReach()==s.creatureReach());
        StormModel m=new StormModel();m.build(100000,123,8);check("orbit-cap-is-500-real-cubes",m.orbitCount==500&&m.orbitMesh.verts==4000&&m.orbitMesh.faces==3000);
        int black=0,purple=0;for(int face=0;face<m.orbitMesh.faces;face++){if(m.orbitMesh.mat[face]==StormTextures.orbitBlack)black++;if(m.orbitMesh.mat[face]==StormTextures.orbitViolet)purple++;}
        check("both-charcoal-and-purple-veined-cubes-present",black>0&&purple>black&&black+purple==3000);
        float vertex=m.orbitMesh.vx[0],vertexY=m.orbitMesh.vy[0];m.pose(100000,80,270,0);check("orbit-batch-really-moves-and-rotates",Mathf.dst(m.orbitMesh.vx[0],m.orbitMesh.vy[0],vertex,vertexY)>1&&m.rig.bones<=128);
        m.build(100000,123,5);check("low-detail-retains-bounded-orbit-budget",m.orbitCount==200);
        s.matter=4200;update(s);float matter=s.matter;while(StormLogic.active()<WitherStormMod.spec.maxActive)HeadlessChecks.storm(2000+StormLogic.active()*20,400);
        StormLogic.fracture(s);check("full-unit-budget-does-not-burn-fission",s.fissions==0&&s.matter==matter);
        int removed=0;for(Unit q:Groups.unit.copy())if(q!=s&&q instanceof StormUnit&&removed++<2)q.remove();
        s.reassembly=0;StormLogic.fracture(s);final int id=s.id;float sum=s.matter;int children=0;boolean healthy=true;
        for(Unit q:Groups.unit)if(q instanceof StormUnit child&&child.rootId==id){children++;sum+=child.matter;healthy &= child.health==child.maxHealth;}
        check("reserved-capacity-retries-two-child-fission",children==2&&s.fissions==1);
        check("fission-conserves-matter-and-initializes-children",Math.abs(sum-matter)<.01f&&healthy);
        int count=StormLogic.active();s.reassembly=0;StormLogic.fracture(s);check("fission-remains-one-generation-only",StormLogic.active()==count);
        s.breachTicks=100;s.pierceCore(s.coreIntegrity+1);HeadlessChecks.steps(3);check("core-death-still-removes-both-split-children",!Groups.unit.contains(q->q instanceof StormUnit child&&child.rootId==id));
    }
}
