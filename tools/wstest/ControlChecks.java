package wstest;
import arc.*;import arc.files.*;import arc.graphics.*;import arc.math.*;import arc.math.geom.*;import arc.util.*;import arc.util.io.*;
import mindustry.*;import mindustry.content.*;import mindustry.game.*;import mindustry.gen.*;import mindustry.input.InputHandler;import mindustry.io.*;import mindustry.world.*;
import wstorm.*;import wstorm.world.*;import wstorm.model.*;import wstorm.gfx.*;
import java.io.*;
/** v1.6 real native possession/command APIs, simultaneous flight, collision and contact damage. */
public class ControlChecks{
    static void check(String name,boolean ok){HeadlessChecks.check(name,ok);}
    static StormUnit fresh(){return MechanicsChecks.fresh();}
    static void update(StormUnit s){Time.delta=1;StormLogic.update(s);}
    static StormCommand.CommandBuild totem(int x,int y,Team team){return (StormCommand.CommandBuild)HeadlessChecks.place(StormContent.commandTotem,x,y,team);}
    static Player player(){Player p=Player.create();p.name="Possession regression";p.team(Team.sharded);p.add();return p;}
    public static void run()throws Exception{
        StormUnit s=fresh();s.resetController();Player p=player();StormUnit foe=StormLogic.spawn(Team.crux,2400,500);
        check("native-player-controllable-allied-storm",s.playerControllable()&&s.controller() instanceof StormAI);
        InputHandler.unitControl(p,foe);check("enemy-possession-rejected-by-native-api",!foe.isPlayer()&&p.unit()!=foe);
        InputHandler.unitControl(p,s);check("native-allied-possession-before-switch",s.isPlayer()&&s.getPlayer()==p&&p.unit()==s);
        StormCommand.CommandBuild switchA=totem(20,20,Team.sharded),switchB=totem(24,20,Team.sharded);
        StormCommand.CommandBuild enemySwitch=totem(28,20,Team.crux);
        check("totem-starts-off-and-no-auto-command",!switchA.enabled&&!StormCommand.enabled(Team.sharded));
        switchA.configure(true);check("switch-on-preserves-current-player",s.controller()==p&&p.unit()==s&&StormCommand.enabled(Team.sharded));
        check("allied-switches-agree-enemy-switch-independent",switchB.enabled&&!enemySwitch.enabled&&!StormCommand.enabled(Team.crux));
        s.vel.set(.2f,.3f);update(s);check("absorption-logic-never-overwrites-player-motion",s.controller()==p&&Math.abs(s.vel.x-.2f)<.0001f&&Math.abs(s.vel.y-.3f)<.0001f);
        switchB.configure(false);check("switch-off-cannot-steal-player",s.controller()==p&&!StormCommand.enabled(Team.sharded));
        InputHandler.unitClear(p);check("native-detach-restores-autonomous-controller",!s.isPlayer()&&s.controller() instanceof StormAI&&!s.isCommandable());
        Unit lure=HeadlessChecks.unit(Team.crux,900,400);Vars.state.teams.updateTeamStats();s.controller().updateUnit();check("detached-off-state-pursues-again",s.vel.len()>0);
        switchA.configure(true);s.controller().updateUnit();check("on-state-default-is-motionless-commandable",s.isCommandable()&&s.vel.isZero());
        InputHandler.commandUnits(p,new int[]{s.id},null,null,new Vec2(650,650),false,true);s.controller().updateUnit();check("native-RTS-position-command-actually-moves",s.vel.len()>0&&s.command().hasCommand());
        InputHandler.commandUnits(p,new int[]{s.id},null,lure,null,false,true);s.controller().updateUnit();check("native-RTS-attack-command-retained",s.command().attackTarget==lure&&s.vel.len()>0);
        InputHandler.unitControl(p,s);switchA.configure(false);switchA.configure(true);check("repeated-toggles-still-preserve-possession",s.isPlayer()&&p.unit()==s);
        InputHandler.unitClear(p);check("detach-in-command-mode-clears-residual-velocity-immediately",s.vel.isZero());s.controller().updateUnit();check("detach-while-on-returns-to-idle-not-autohunt",s.isCommandable()&&s.vel.isZero()&&!s.command().hasCommand());
        Fi controlSave=new Fi("v16-command.msav");s.genome=616161;SaveIO.write(controlSave);Vars.logic.reset();SaveIO.load(controlSave);HeadlessChecks.playing();
        s=(StormUnit)Groups.unit.find(q->q instanceof StormUnit st&&st.genome==616161);
        Log.info("RELOAD s="+s+" mode="+StormCommand.enabled(Team.sharded)+" controller="+(s==null?null:s.controller())+" switches="+Groups.build.count(b->b instanceof StormCommand.CommandBuild));
        check("totem-state-and-commandability-survive-world-save",s!=null&&StormCommand.enabled(s.team)&&s.isCommandable());
        for(Building b:Groups.build.copy())if(b instanceof StormCommand.CommandBuild&&b.team==s.team)b.tile.remove();
        check("last-totem-removal-restores-autonomous-mode",!StormCommand.enabled(s.team)&&!s.isCommandable());
        s=fresh();s.resetController();Unit near=HeadlessChecks.unit(Team.crux,700,400);Building closer=HeadlessChecks.place(Blocks.titaniumWall,52,50,Team.crux);Vars.state.teams.updateTeamStats();
        s.controller().updateUnit();float slow=s.vel.len();check("global-unit-priority-over-close-building",((StormAI)s.controller()).currentTarget()==near);
        check("in-range-motion-exactly-half-normal-chase",Math.abs(slow-s.travelSpeed()*.5f)<.001f);
        near.set(400+s.creatureReach()+80,400);s.controller().updateUnit();check("escaped-unit-chase-is-two-times-in-range-speed",Math.abs(s.vel.len()-slow*2)<.001f);
        near.remove();s.controller().updateUnit();check("no-units-falls-back-to-buildings",((StormAI)s.controller()).currentTarget()==closer);
        closer.tile.remove();s.controller().updateUnit();check("empty-world-does-not-wander",s.vel.isZero());
        near=HeadlessChecks.unit(Team.crux,700,400);s.controller(new StormAI());s.controller().updateUnit();check("unit-only-world-still-pursues",((StormAI)s.controller()).currentTarget()==near&&s.vel.len()>0);
        s=fresh();s.resetController();Unit escape=HeadlessChecks.unit(Team.crux,700,400);update(s);
        escape.set(s.x+s.creatureReach()+20,s.y);float escapedX=escape.x;s.scanTimer=12;update(s);StormLift.update();
        check("passive-escape-releases-immediately-without-snapback",!StormLogic.directHeld(s,escape.id)&&!escape.isFlying()&&Math.abs(escape.x-escapedX)<.001f);
        s.controller().updateUnit();check("actual-passive-escape-restores-normal-chase-speed",Math.abs(s.vel.len()-s.travelSpeed())<.001f);
        s.set(8,400);s.matter=420;s.scanTimer=0;update(s);escape.set(s.x+s.reach()+2,s.y);escapedX=escape.x;s.scanTimer=12;update(s);StormLift.update();
        check("beam-escape-clears-locks-and-restores-ground-state",s.targetId[0]!=escape.id&&s.targetId[1]!=escape.id&&s.targetId[2]!=escape.id&&!escape.isFlying()&&Math.abs(escape.x-escapedX)<.001f);
        s=fresh();s.resetController();s.skullTimer=0;Unit meal=HeadlessChecks.unit(Team.crux,760,400);meal.health=meal.maxHealth=10000;
        for(int i=0;i<6;i++)HeadlessChecks.place(Blocks.titaniumWall,54+i*2,54,Team.crux);Vars.state.teams.updateTeamStats();
        s.rotation=s.bodyYaw=0;s.controller().updateUnit();float beforeX=meal.x;int bullets=Groups.bullet.size();update(s);
        check("initial-stage-five-building-flights-not-one",s.buildingCapacity()==5&&s.buildingsInFlight()==5);
        check("initial-move-fire-unit-pull-and-five-blocks-concurrent",s.vel.len()>0&&meal.x!=beforeX&&Groups.bullet.size()>bullets&&s.buildingsInFlight()==5&&s.matter==90&&!s.beamCapable(0));
        int remaining=0;for(int i=0;i<6;i++)if(Vars.world.build(54+i*2,54)!=null)remaining++;check("flight-cap-prevents-sixth-initial-detach",remaining==1);
        check("multi-flight-no-premature-matter-credit",s.eatenBuildings==0&&s.absorbedMatter==0);
        s.skullTimer=10000;s.genome=660066;
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();s.writeSync(Writes.get(new DataOutputStream(bytes)));StormUnit replica=(StormUnit)StormContent.storm.create(Team.sharded);
        replica.readSync(Reads.get(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()))));check("all-five-flight-slots-network-sync",replica.buildingsInFlight()==5&&replica.flightX[4]==s.flightX[4]&&replica.flightValue[4]==s.flightValue[4]);
        Fi flightSave=new Fi("v16-five-flights.msav");SaveIO.write(flightSave);Vars.logic.reset();SaveIO.load(flightSave);HeadlessChecks.playing();
        s=(StormUnit)Groups.unit.find(q->q instanceof StormUnit st&&st.genome==660066);check("all-five-flights-survive-world-save-load",s!=null&&s.buildingsInFlight()==5);
        s.scanTimer=10000;s.skullTimer=10000;s.buildingTarget=-1;java.util.Arrays.fill(s.captureCandidates,-1);java.util.Arrays.fill(s.directId,-1);java.util.Arrays.fill(s.targetId,-1);java.util.Arrays.fill(s.targetKind,(byte)0);
        for(int i=0;i<94;i++)update(s);check("five-flights-still-uncredited-before-arrival",s.eatenBuildings==0);
        update(s);float credited=s.matter;check("five-buildings-credit-on-same-arrival-tick",s.eatenBuildings==5&&s.buildingsInFlight()==0&&s.matter>90);
        for(int i=0;i<15;i++)update(s);check("multi-flight-no-double-credit",s.matter==credited&&s.eatenBuildings==5);
        s.matter=12000;check("growth-increases-parallel-building-capacity",s.buildingCapacity()>5);s.matter=1e12f;check("multi-building-workload-bounded-at-32",s.buildingCapacity()==32);
        // Actual engine movement through a solid wall, with live ground entities, not merely elevation flags.
        for(int mode=0;mode<2;mode++){
            s=fresh();s.skullTimer=10000;
            for(int y=35;y<=75;y++)HeadlessChecks.place(mode==0?Blocks.titaniumWall:Blocks.sandWall,62,y,Team.derelict);
            Unit victim=(mode==0?UnitTypes.dagger:UnitTypes.atrax).create(Team.crux);victim.set(560,470);victim.controller(new mindustry.entities.units.AIController(){@Override public void updateUnit(){}});victim.add();Vars.state.teams.updateTeamStats();
            if(mode==1){
                HeadlessChecks.steps(10);int victimId=victim.id;s.genome=668899;Fi carried=new Fi("v16-carried-leg.msav");SaveIO.write(carried);Vars.logic.reset();SaveIO.load(carried);HeadlessChecks.playing();
                s=(StormUnit)Groups.unit.find(q->q instanceof StormUnit st&&st.genome==668899);victim=Groups.unit.getByID(victimId);
                check("carried-leg-reloads-with-native-save-id-and-flight-state",victim instanceof LiftableLegsUnit&&s!=null&&StormLogic.directHeld(s,victimId));s.skullTimer=10000;
            }
            boolean crossed=false,lifted=false,prematureDeath=false;int ticks=0;float wallStep=0;
            while(victim.isAdded()&&ticks++<1200){float px=victim.x,py=victim.y;HeadlessChecks.steps(1);if(victim.isAdded()){if(px>480&&px<520)wallStep=Math.max(wallStep,Mathf.dst(px,py,victim.x,victim.y));lifted|=victim.elevation>.9f;crossed|=victim.x<490;if(victim.dead)prematureDeath=true;}}
            check((mode==0?"ground":"legged")+"-victim-crosses-solid-wall-while-lifted",lifted&&crossed&&!prematureDeath);
            check((mode==0?"ground":"legged")+"-wall-crossing-remains-slow-not-teleporting",wallStep>0&&wallStep<.6f);
            check((mode==0?"ground":"legged")+"-victim-credited-only-at-head",!victim.isAdded()&&s.eatenUnits==1);
        }
        check("native-leg-save-id-and-layout-unchanged",UnitTypes.atrax.constructor.get() instanceof LiftableLegsUnit&&UnitTypes.atrax.constructor.get().classId()==LegsUnit.create().classId());
        s=fresh();Unit released=HeadlessChecks.unit(Team.crux,550,450);update(s);check("captured-ground-unit-has-flight-lease",released.isFlying());
        s.remove();StormLift.update();check("owner-removal-restores-safe-ground-movement",!released.isFlying()&&released.canPassOn()&&released.isAdded());
        s=fresh();s.matter=6000;s.age=100;Vec2 point=new Vec2();StormTentacles.point(s,0,.86f,point);
        Unit ground=HeadlessChecks.unit(Team.crux,point.x,point.y);ground.health=ground.maxHealth=10000;
        Unit air=UnitTypes.flare.create(Team.crux);air.set(point.x+3,point.y);air.add();air.health=air.maxHealth=10000;
        Unit ally=HeadlessChecks.unit(Team.sharded,point.x,point.y),far=HeadlessChecks.unit(Team.crux,2100,1100);Vars.state.teams.updateTeamStats();Groups.unit.updatePhysics();
        StormTentacles.update(s,1);
        check("tentacle-windup-contact-causes-no-damage",ground.health==10000&&air.health==10000&&s.tentacleHits==0);
        for(int i=0;i<30;i++){s.age++;StormTentacles.update(s,1);}
        check("animated-tentacle-impact-damages-ground-and-air-together",ground.health<10000&&air.health<10000&&s.tentacleHits>=2);
        check("tentacles-do-not-hit-friendlies-or-distant-units",ally.health==ally.maxHealth&&far.health==far.maxHealth);
        float hp=ground.health;s.tentacleTimer=0;StormTentacles.update(s,1);check("tentacle-hit-cooldown-prevents-every-frame-damage",ground.health==hp);
        StormModel model=new StormModel();var field=StormModel.class.getDeclaredField("limbs");field.setAccessible(true);boolean aligned=true;float[] projected=new float[3];
        for(float mass:new float[]{900,3000,12000})for(float heading:new float[]{0,90,180,270}){
            s.matter=mass;s.rotation=s.bodyYaw=heading;model.build(mass,71,8);model.pose(mass,s.age,heading,0);model.aimHeads(s);int[][] bones=(int[][])field.get(model);
            for(int arm=0;arm<model.branches;arm++)for(int j=0;j<7;j++){model.renderer.point(bones[arm][j],0,0,0,projected);model.project(projected);StormTentacles.point(s,arm,j/7f,point);aligned&=Mathf.dst(point.x-s.x,point.y-s.y,projected[0],projected[1])<.015f;}
        }
        check("tentacle-hit-geometry-matches-visible-procedural-mesh",aligned);
        Pixmap source=new Pixmap(WitherStormMod.file("textures","supplied-material-atlas.png")),atlas=StormTextures.atlas();boolean same=true,excluded=true;
        for(int tile=0;tile<32;tile++){int from=tile==14||tile==15||tile==24?8:tile==25?0:tile;for(int y=0;y<16;y++)for(int x=0;x<16;x++)same&=atlas.get(tile%8*16+x,tile/8*16+y)==source.get(from%8*16+x,from/8*16+y);}
        for(int tile:new int[]{14,15})for(int y=0;y<16;y++)for(int x=0;x<16;x++)excluded&=atlas.get(tile%8*16+x,tile/8*16+y)==source.get(x,16+y);
        check("body-and-orbit-atlas-uses-exact-uploaded-texels",same);check("second-row-last-two-input-tiles-excluded",excluded);source.dispose();atlas.dispose();
    }
}
