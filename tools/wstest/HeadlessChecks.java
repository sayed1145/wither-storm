package wstest;
import arc.*;import arc.files.*;import arc.struct.*;import arc.util.*;import arc.util.io.*;
import mindustry.*;import mindustry.content.*;import mindustry.core.GameState.State;import mindustry.entities.units.*;
import mindustry.game.*;import mindustry.gen.*;import mindustry.io.*;import mindustry.world.*;
import wstorm.*;import wstorm.world.*;import wstorm.world.StormSummoner.SummonerBuild;import wstorm.model.*;
import java.io.*;import java.util.*;

public class HeadlessChecks{
    static ArrayList<String> passed=new ArrayList<>();
    static void check(String s,boolean ok){if(!ok)throw new AssertionError(s);passed.add(s);Log.info("PASS: "+s);}
    static void steps(int n){for(int i=0;i<n;i++){Time.delta=1;Vars.logic.update();}}
    static void playing(){Vars.state.set(State.playing);Vars.state.rules.canGameOver=false;Vars.state.rules.waves=false;Vars.state.rules.waveTimer=false;}
    static Building place(Block b,int x,int y,Team t){Vars.world.tile(x,y).setBlock(b,t,0);return Vars.world.build(x,y);}
    static Unit unit(Team t,float x,float y){Unit u=UnitTypes.dagger.create(t);u.set(x,y);u.controller(new AIController(){@Override public void updateUnit(){}});u.add();return u;}
    static StormUnit storm(float x,float y){StormUnit s=StormLogic.spawn(Team.sharded,x,y);s.controller(new AIController(){@Override public void updateUnit(){}});return s;}
    public static void run()throws Exception{
        check("repository-author-sayed1145",Vars.mods.getMod("wither-storm").meta.author.equals("sayed1145"));
        String credits=WitherStormMod.file("credits","CREDITS.md").readString();
        check("packaged-project-contribution-credits",credits.contains("sayed1145")&&credits.contains("NLM-2b"));
        check("official-engine-v160.5",mindustry.core.Version.build==160&&mindustry.core.Version.revision==5);
        check("installed-jar-content-loader",StormContent.storm==Vars.content.unit("wither-storm-wither-storm"));
        check("one-unit-summoner-counterweapon-and-command-totem",Vars.content.units().count(u->u.minfo.mod!=null&&u.minfo.mod.name.equals("wither-storm"))==1&&Vars.content.blocks().count(b->b.minfo.mod!=null&&b.minfo.mod.name.equals("wither-storm"))==3);
        check("complete-theme-runtime-asset-packaged",WitherStormMod.file("music","wither-storm-theme.ogg").length()>500000);
        boolean rejects=false;try{StormSpec.parse("{version: 1, initialMatter:-1}");}catch(IllegalArgumentException e){rejects=true;}check("blueprint-out-of-range-rejected",rejects);
        rejects=false;try{StormSpec.parse("{version: 99}");}catch(IllegalArgumentException e){rejects=true;}check("blueprint-version-rejected",rejects);
        StormModel m=new StormModel();HashSet<Integer> faces=new HashSet<>();HashSet<Integer> voxels=new HashSet<>();int firstBranches=0,lastBranches=0;boolean finite=true,budgeted=true;
        for(float matter:new float[]{10,90,160,450,1200,4000,12000,100000}){
            m.build(matter,712,8);faces.add(m.rig.faces());voxels.add(m.bodyVoxels);if(firstBranches==0)firstBranches=m.branches;lastBranches=m.branches;budgeted&=m.rig.bones<=128&&m.bodyVoxels<1600;
            for(float a:new float[]{0,90,180,270}){
                m.pose(matter,125,a,0);float[] q=new float[3];
                for(int p=0;p<m.rig.pieceCount;p++){var part=m.rig.pieces[p];for(int v=0;v<part.mesh.verts;v++){
                    m.renderer.point(part.bone,part.mesh.vx[v],part.mesh.vy[v],part.mesh.vz[v],q);
                    finite&=Float.isFinite(m.renderer.screenX(q[0],q[2]))&&Float.isFinite(m.renderer.screenY(q[1],q[2]));
                }}
            }
        }
        check("procedural-topology-not-fixed-stages",faces.size()>=3&&voxels.size()>=2);
        check("new-tentacles-bud-with-matter",lastBranches>firstBranches);
        check("finite-3d-projection-all-tested-poses",finite);
        check("geometry-lod-budget-bounded",budgeted);
        float g=WitherStormMod.spec.growth(1000);check("continuous-volume-growth-not-stage-scale",WitherStormMod.spec.growth(1000.1f)>g&&WitherStormMod.spec.growth(1000.1f)-g<.001f);
        m.build(90,712,8);m.pose(90,10,270,0);
        check("all-initial-head-bones-visible",!m.rig.hidden[m.heads[0]]&&!m.rig.hidden[m.heads[1]]&&!m.rig.hidden[m.heads[2]]);
        check("all-initial-heads-unmutated",StormModel.mutation(90,0)==0&&StormModel.mutation(90,1)==0&&StormModel.mutation(90,2)==0);
        check("middle-central-storm-two-normal-heads",StormModel.mutation(650,0)==1&&StormModel.mutation(650,1)==0&&StormModel.mutation(650,2)==0);
        check("late-three-distinct-storm-heads",StormModel.mutation(1640,0)==1&&StormModel.mutation(1640,1)==1&&StormModel.mutation(1640,2)==1);
        boolean monotonic=true;for(int h=0;h<3;h++)for(int mass=90;mass<2000;mass++)monotonic &= StormModel.mutation(mass+.1f,h)>=StormModel.mutation(mass,h)&&StormModel.mutation(mass+.1f,h)-StormModel.mutation(mass,h)<.001f;
        check("continuous-monotonic-normal-to-imported-head-transition",monotonic);
        check("storm-renderer-face-sort-enabled",m.renderer.faceSort);
        m.build(2600,712,8);int topology=m.rig.faces();m.build(2600,712,8);
        check("deterministic-procedural-topology",topology==m.rig.faces());
        Vars.logic.reset();Vars.state.rules=new Rules();Vars.state.rules.infiniteResources=false;Vars.state.rules.unitCap=100;
        Vars.state.rules.defaultTeam=Team.sharded;Vars.state.rules.waveTeam=Team.crux;Vars.state.rules.teams.get(Team.crux).rtsAi=false;Vars.state.rules.teams.get(Team.crux).buildAi=false;
        Vars.state.map=new mindustry.maps.Map(StringMap.of("name","Wither Storm validation"));
        Vars.world.loadGenerator(180,150,ts->{for(int x=0;x<ts.width;x++)for(int y=0;y<ts.height;y++)ts.set(x,y,new Tile(x,y,Blocks.stone,Blocks.air,Blocks.air));});
        place(Blocks.coreShard,8,8,Team.sharded);place(Blocks.coreShard,170,140,Team.crux);playing();Time.setDeltaProvider(()->1f);
        StormModel animated=new StormModel();float[] anchor=new float[3];arc.math.geom.Vec2 projected=new arc.math.geom.Vec2();
        for(float mass:new float[]{90,2600}){
            StormUnit actor=(StormUnit)StormContent.storm.create(Team.sharded);actor.matter=mass;actor.rotation=270;
            animated.ensure(mass,712,8);animated.pose(mass,0,270,0);float initialYaw=animated.headYaw[1];
            animated.pose(mass,85,270,0);check("independent-idle-head-motion-"+(int)mass,Math.abs(animated.headYaw[1]-initialYaw)>3&&Math.abs(animated.headYaw[1]-animated.headYaw[2])>3);
            for(int h=0;h<3;h++){actor.targetKind[h]=1;actor.targetId[h]=123+h;actor.targetX[h]=arc.math.Mathf.cosDeg(270+(h-1)*50)*180;actor.targetY[h]=arc.math.Mathf.sinDeg(270+(h-1)*50)*180;}
            for(int n=0;n<65;n++){actor.age=n;Time.delta=1;StormAim.update(actor,1);animated.pose(mass,n,270,0);animated.aimHeads(actor);}
            check("all-stages-can-track-left-and-right-"+(int)mass,animated.headYaw[0]<-15&&animated.headYaw[2]>15&&Float.isFinite(animated.headPitch[1]));
            boolean aligned=true;for(int h=0;h<3;h++){actor.headPoint(h,projected);animated.renderer.point(animated.heads[h],0,0,0,anchor);animated.project(anchor);aligned &= projected.dst(anchor[0],anchor[1])<.01f;}
            check("animated-heads-match-server-capture-points-"+(int)mass,aligned);
            check("depth-buffer-enabled-"+(int)mass,animated.renderer.depthTest);
        }
        wstorm.g3d.Rig hinge=new wstorm.g3d.Rig();hinge.bone(-1,0,0,0);hinge.reset();hinge.scale(0,1.7f,1.7f,1.7f);hinge.rot(0,2,29);hinge.move(0,3,4,5);
        float[] beforeHinge=new float[3],afterHinge=new float[3];hinge.apply(0,0,-14.4f,-6,beforeHinge);StormAnimation.hinge(hinge,0,0,-19,0,-14.4f,-6);hinge.apply(0,0,-14.4f,-6,afterHinge);
        check("jaw-hinge-pivot-does-not-drift",Math.abs(beforeHinge[0]-afterHinge[0])+Math.abs(beforeHinge[1]-afterHinge[1])+Math.abs(beforeHinge[2]-afterHinge[2])<.001f);
        check("animation-envelopes-return-to-rest",StormAnimation.pulse(100,0,6,32)==0&&StormAnimation.pulse(6,0,6,32)==1);
        StormUnit s=storm(400,480);float initial=s.matter;steps(60);check("no-food-no-automatic-mass",s.matter==initial);
        s.skullTimer=10000; // isolate ingestion; projectile combat is tested independently below
        Unit enemy=unit(Team.crux,470,480),ally=unit(Team.sharded,330,480);Building wall=place(Blocks.titaniumWall,50,54,Team.crux),friendly=place(Blocks.titaniumWall,50,66,Team.sharded);
        Vars.state.teams.updateTeamStats();steps(30);
        check("initial-three-normal-heads",s.headCount()==3&&s.stormHeadCount()==0);
        check("initial-units-not-absorbed",enemy.isAdded()&&s.eatenUnits==0);
        check("no-withering-slowdown",StormContent.withering.speedMultiplier==1);
        s.matter=420;steps(30);
        check("three-heads-retained-during-mutation",s.headCount()==3&&s.stormHeadCount()<3);
        check("enemy-really-pulled",enemy.x<470);
        check("friendly-unit-and-building-safe",ally.health==ally.maxHealth&&Math.abs(ally.x-330)<.01f&&friendly.health==friendly.maxHealth);
        steps(260);
        check("enemy-unit-actually-removed",!enemy.isAdded()||enemy.dead);
        check("enemy-building-actually-removed",Vars.world.build(50,54)==null);
        check("ingested-matter-recorded",s.matter>initial&&s.eatenUnits>=1&&s.eatenBuildings>=1);
        check("friendlies-still-alive-after-ingestion",ally.isAdded()&&friendly.isValid());
        float counted=s.matter;boolean duplicate=StormLogic.ingest(s,0,wall);check("no-double-credit",!duplicate&&s.matter==counted);
        s.health=s.maxHealth*.8f;Unit meal=unit(Team.crux,s.x+22,s.y);Vars.state.teams.updateTeamStats();float hp=s.health;check("manual-ingestion-valid",StormLogic.ingest(s,1,meal));check("absorption-regenerates-shell",s.health>hp);
        check("cannot-ingest-friendly",!StormLogic.ingest(s,0,ally));
        float saved=s.matter;int savedUnits=s.eatenUnits;s.genome=89123;s.coreIntegrity=1742;int sid=s.id;
        Fi save=new Fi("storm-check.msav");SaveIO.write(save);Vars.logic.reset();SaveIO.load(save);playing();
        s=(StormUnit)Groups.unit.find(u->u instanceof StormUnit);
        check("save-reload-custom-unit-type",s!=null&&s.type==StormContent.storm);
        check("save-reload-growth-genome-and-counters",s.matter==saved&&s.eatenUnits==savedUnits&&s.genome==89123&&s.coreIntegrity==1742);
        ByteArrayOutputStream buf=new ByteArrayOutputStream();s.writeSync(Writes.get(new DataOutputStream(buf)));
        StormUnit other=(StormUnit)StormContent.storm.create(Team.sharded);other.readSync(Reads.get(new DataInputStream(new ByteArrayInputStream(buf.toByteArray()))));
        check("network-sync-custom-growth-state",other.matter==s.matter&&other.coreIntegrity==s.coreIntegrity&&other.genome==s.genome&&other.maxHealth==s.maxHealth&&other.hitSize==s.hitSize);
        s.matter=600;s.age=100;s.reassembly=0;float core=s.coreIntegrity;s.damage(300);check("protected-core-does-not-lose-integrity",s.coreIntegrity==core);
        s.matter=90;s.age=380;s.damage(300);check("exposed-core-takes-permanent-damage",s.coreIntegrity<core);
        s.matter=1640;check("three-heads-and-no-skulls",s.headCount()==3&&s.mature());
        s.matter=100000;check("head-count-hard-cap",s.headCount()==3);
        s.matter=600;s.breachTicks=0;core=s.coreIntegrity;s.pierceCore(420);
        check("first-counter-shot-opens-shell",s.breachTicks==240&&s.coreIntegrity==core);
        s.pierceCore(420);check("second-counter-shot-damages-internal-core",s.coreIntegrity==core-420);
        steps(241);check("breach-reseals",s.breachTicks==0&&!s.exposed());
        s.age=100;s.matter=3000;s.health=s.maxHealth*.15f;s.reassembly=0;StormLogic.fracture(s);
        check("damaged-shell-reassembly-starts",s.reassembly>0&&!s.dead);
        final int[] rootId={s.id};check("one-generation-fission-creates-two-children",s.fissions==1&&Groups.unit.count(u->u instanceof StormUnit c&&c.rootId==rootId[0])==2);
        Fi family=new Fi("family-check.msav");SaveIO.write(family);Vars.logic.reset();SaveIO.load(family);playing();
        s=(StormUnit)Groups.unit.find(u->u instanceof StormUnit q&&q.rootId<0&&q.fissions==1);rootId[0]=s.id;
        check("fission-family-survives-save-reload",Groups.unit.count(u->u instanceof StormUnit c&&c.rootId==rootId[0])==2);
        steps(2);check("loaded-children-keep-parent-link",StormLogic.active()==3);
        int count=StormLogic.active();s.reassembly=0;StormLogic.fracture(s);check("fission-does-not-recurse-forever",StormLogic.active()==count);
        s.reassembly=0;s.age=390;s.coreIntegrity=2;s.breachTicks=120;s.pierceCore(10000);steps(5);check("core-destruction-kills-main-and-children",s.dead&&!Groups.unit.contains(u->u instanceof StormUnit c&&c.rootId==rootId[0]));
        for(Unit u:Groups.unit.copy())if(u instanceof StormUnit)Call.unitDespawn(u);
        SummonerBuild altar=(SummonerBuild)place(StormContent.summoner,85,45,Team.sharded);altar.queued=true;
        steps(40);check("summoner-does-not-run-without-power-or-items",altar.charge==0&&StormLogic.active()==0);
        altar.items.add(Items.thorium,120);altar.items.add(Items.surgeAlloy,40);altar.items.add(Items.phaseFabric,20);steps(40);check("summoner-items-alone-not-enough",altar.charge==0);
        place(Blocks.powerSource,83,45,Team.sharded);steps(65);check("summoner-real-power-charge",altar.charge>40&&altar.power.status>.99f);
        float charge=altar.charge;Vars.world.tile(83,45).remove();steps(20);check("power-loss-pauses-ritual",Math.abs(altar.charge-charge)<.01f);
        Fi ritual=new Fi("ritual-check.msav");SaveIO.write(ritual);Vars.logic.reset();SaveIO.load(ritual);playing();altar=(SummonerBuild)Vars.world.build(85,45);
        check("ritual-save-reload",altar.queued&&Math.abs(altar.charge-charge)<.01f);
        place(Blocks.powerSource,83,45,Team.sharded);steps(600);
        check("paid-summon-spawns-once",StormLogic.active()==1&&!altar.queued);
        check("summon-consumes-exact-recipe",altar.items.get(Items.thorium)==0&&altar.items.get(Items.surgeAlloy)==0&&altar.items.get(Items.phaseFabric)==0);
        StormUnit probe=storm(300,800);Unit lure=unit(Team.crux,760,800);Vars.state.teams.updateTeamStats();
        probe.rotation=probe.bodyYaw=0;probe.matter=90;probe.skullTimer=0;probe.scanTimer=0;int bullets=Groups.bullet.size();Time.delta=1;StormLogic.update(probe);
        check("all-three-initial-normal-heads-fire",Groups.bullet.size()-bullets==3);
        probe.matter=1640;probe.skullTimer=0;bullets=Groups.bullet.size();StormLogic.update(probe);
        check("mature-no-new-skull-projectiles",Groups.bullet.size()==bullets);
        probe.matter=90;float fast=probe.travelSpeed();float shortRange=probe.reach();probe.matter=5000;probe.absorbedMatter=5000;
        check("late-speed-decreases-range-increases",probe.travelSpeed()<fast&&probe.reach()>shortRange);
        probe.set(80,80);lure.set(3500,3000);probe.rotation=0;probe.controller(new StormAI());probe.vel.setZero();
        probe.controller().updateUnit();check("global-pursuit-beyond-old-1100-radius",probe.vel.len()>0);
        float integrity=probe.coreIntegrity;probe.breachTicks=100;
        ByteArrayOutputStream sync2=new ByteArrayOutputStream();probe.writeSync(Writes.get(new DataOutputStream(sync2)));
        StormUnit replica=(StormUnit)StormContent.storm.create(Team.sharded);replica.readSync(Reads.get(new DataInputStream(new ByteArrayInputStream(sync2.toByteArray()))));
        check("breach-timer-network-sync",replica.breachTicks==100&&replica.coreIntegrity==integrity);
        long builds=m.rebuilds;m.ensure(5000,712,8);builds=m.rebuilds;for(int i=0;i<100;i++)m.ensure(5000,712,8);
        check("unchanged-model-reuses-mesh",m.rebuilds==builds);
        float terrain=Vars.world.tile(55,55).floorID();check("terrain-floor-preserved",terrain==Blocks.stone.id);
        check("downloaded-head-46-original-cubes",wstorm.model.ImportedStormHead.cubeCount()==46);
        m.build(100000,712,8);check("500-independent-cubes-in-one-bounded-batch",m.orbitCount==500&&m.orbitMesh.verts==4000&&m.orbitMesh.faces==3000&&m.rig.bones<=128);
        probe.controller(new AIController(){@Override public void updateUnit(){}});probe.set(600,600);probe.matter=1640;probe.scanTimer=10000;probe.debrisTicks=0;probe.buildingTarget=-1;
        for(int h=0;h<3;h++){probe.targetId[h]=-1;probe.targetKind[h]=0;probe.locked[h]=0;}
        arc.math.geom.Vec2 capturePoint=new arc.math.geom.Vec2();probe.headPoint(0,capturePoint);
        Unit tough=unit(Team.crux,capturePoint.x+80,capturePoint.y);tough.health=tough.maxHealth=1e30f;tough.shield=1e30f;
        probe.targetId[0]=tough.id;probe.targetKind[0]=1;for(int i=0;i<100;i++)StormAim.update(probe,1);float startX=tough.x,startY=tough.y;Time.delta=1;StormLogic.update(probe);
        float moved=arc.math.Mathf.dst(startX,startY,tough.x,tough.y);
        check("tractor-pull-slow-bounded-step",moved>0&&moved<.6f);
        check("tractor-no-damage-before-head",tough.isAdded()&&tough.health==1e30f&&tough.shield==1e30f);
        probe.headPoint(0,capturePoint);tough.set(capturePoint);probe.locked[0]=0;int eaten=probe.eatenUnits;StormLogic.update(probe);
        check("arrival-annihilates-with-zero-lock-time",!tough.isAdded()&&tough.dead&&probe.eatenUnits==eaten+1);
        check("annihilation-ignores-enormous-shield",tough.health==0);
        check("extreme-victim-health-keeps-growth-finite",Float.isFinite(probe.matter)&&Float.isFinite(probe.health)&&Float.isFinite(probe.maxHealth));
        probe.matter=90;probe.scanTimer=10000;probe.genome=776655;int buildingsBefore=probe.eatenBuildings;float beforeMatter=probe.matter;
        Building near=place(Blocks.titaniumWall,78,75,Team.crux);
        check("building-detaches-without-beam",StormLogic.beginBuilding(probe,near)&&!near.isValid()&&!probe.beamCapable(0));
        check("building-credit-deferred-until-animation-arrival",probe.matter==beforeMatter&&probe.debrisTicks==95&&probe.eatenBuildings==buildingsBefore);
        Fi pendingSave=new Fi("inflight-building.msav");SaveIO.write(pendingSave);Vars.logic.reset();SaveIO.load(pendingSave);playing();
        probe=(StormUnit)Groups.unit.find(q->q instanceof StormUnit v&&v.genome==776655);
        check("building-flight-survives-world-save-load",probe!=null&&probe.debrisTicks==95&&probe.debrisValue>0&&probe.debrisBlock==Blocks.titaniumWall.id);
        probe.scanTimer=10000;probe.buildingTarget=-1;probe.skullTimer=10000;
        for(int n=0;n<94;n++){Time.delta=1;StormLogic.update(probe);}check("building-not-credited-early",probe.eatenBuildings==buildingsBefore);
        StormLogic.update(probe);check("building-arrival-credited-once",probe.eatenBuildings==buildingsBefore+1&&probe.matter>beforeMatter);
        float credited=probe.matter;for(int n=0;n<10;n++)StormLogic.update(probe);check("building-flight-cannot-double-credit",probe.matter==credited&&probe.debrisValue==0);
        for(Unit q:Groups.unit.copy())if(q!=probe)q.remove();
        probe.controller(new StormAI());probe.matter=90;probe.absorbedMatter=0;probe.scanTimer=0;probe.set(200,600);probe.vel.setZero();
        Building approach=place(Blocks.titaniumWall,170,75,Team.crux);float oldDistance=probe.dst(approach);int oldBuildings=probe.eatenBuildings;
        float minimumDistance=oldDistance;for(int k=0;k<500;k++){steps(1);minimumDistance=Math.min(minimumDistance,probe.dst(1360,600));}
        Log.info("APPROACH probe x="+probe.x+" y="+probe.y+" min="+minimumDistance+" eaten="+probe.eatenBuildings+" targetValid="+approach.isValid()+" added="+probe.isAdded()+" dead="+probe.dead);
        check("AI-approaches-building-instead-of-stopping-at-beam-range",probe.x>200&&minimumDistance<oldDistance-40);
        check("AI-absorbs-building-with-no-beam-capable-stage",!approach.isValid()&&probe.eatenBuildings>oldBuildings&&probe.matter<420);
        MechanicsChecks.run();ControlChecks.run();LegacyChecks.run();AimChecks.run();BeamAndLayoutChecks.run();
        while(StormLogic.active()<WitherStormMod.spec.maxActive)storm(800+StormLogic.active()*15,600);
        check("active-organism-budget",StormLogic.spawn(Team.sharded,900,600)==null);
        Vars.logic.reset();check("world-reset-cleans-units",StormLogic.active()==0);
        StringBuilder out=new StringBuilder("{\"engine\":\"Mindustry v160.5 official server\",\"mode\":\"installed JAR / real power graphs / actual unit and tile removal\",\"passed\":[");
        for(int i=0;i<passed.size();i++){if(i>0)out.append(',');out.append('"').append(passed.get(i)).append('"');}
        out.append("],\"count\":").append(passed.size()).append(",\"failed\":0,\"uniqueTopologyCounts\":").append(faces.size()).append('}');
        new Fi(System.getProperty("ws.result","headless-result.json")).writeString(out.toString());Log.info("WITHER_STORM_HEADLESS_ALL_PASS: "+passed.size());
    }
}
