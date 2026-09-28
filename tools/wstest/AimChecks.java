package wstest;
import arc.*;import arc.math.*;import arc.math.geom.*;import arc.util.*;import arc.util.io.*;import mindustry.*;import mindustry.content.*;import mindustry.game.*;import mindustry.gen.*;import mindustry.io.*;import wstorm.*;import wstorm.world.*;import wstorm.model.*;import wstorm.gfx.*;import java.io.*;
/** v1.7 gameplay/rig regressions; no precomputed success flags. */
public final class AimChecks{
    static void check(String label,boolean value){HeadlessChecks.check(label,value);}
    public static void run()throws Exception{
        StormUnit s=MechanicsChecks.fresh();StormModel m=new StormModel();float[] actual=new float[3],expected=new float[3];boolean ports=true,bounds=true;
        for(float mass:new float[]{90,420,650,900,1400,2600,12000})for(float heading:new float[]{0,43,90,180,270,359}){
            s.matter=mass;s.age=123;s.bodyYaw=heading;s.rotation=heading;
            for(int h=0;h<3;h++){s.aimYaw[h]=(h-1)*35;s.aimPitch[h]=-9-h*18;}
            m.build(mass,81539,8);m.pose(mass,s.age,heading,0);m.aimHeads(s);
            for(int h=0;h<3;h++){m.eye(h,actual);StormAim.port(s,h,expected);ports&=Mathf.dst(actual[0],actual[1],expected[0],expected[1])<.001f&&Math.abs(actual[2]-expected[2])<.001f;}
        }
        check("laser-aperture-child-transform-matches-authority-126-poses",ports);
        boolean neckRemoved=true;try{StormModel.class.getDeclaredField("neck");neckRemoved=false;}catch(NoSuchFieldException correct){}
        check("neck-bones-and-neck-mesh-removed",neckRemoved);
        StormAnimation.anchor(2600,0,1,actual);check("mature-heads-embedded-at-body-front-not-extended-necks",Math.abs(actual[0])<37&&actual[1]<21&&actual[2]>53);
        s=MechanicsChecks.fresh();s.set(1400,600);s.matter=2600;s.rotation=s.bodyYaw=0;
        Unit rear=HeadlessChecks.unit(Team.crux,450,600);Vars.state.teams.updateTeamStats();StormLogic.acquire(s);float x=rear.x,y=rear.y;StormLogic.update(s);StormLift.update();
        check("rear-target-not-attracted-or-lifted-before-body-turn",rear.x==x&&rear.y==y&&!rear.isFlying()&&!s.beamReady(0)&&s.bodyFacing()<=2);
        HeadlessChecks.place(StormContent.commandTotem,20,20,Team.sharded);((StormCommand.CommandBuild)Vars.world.build(20,20)).configTapped();s.resetController();s.command().commandPosition(new Vec2(2400,600));
        float fromX=s.x;boolean blocked=true,moving=true,turned=false,fired=false;float previous=s.bodyFacing();
        for(int tick=0;tick<140;tick++){
            Time.delta=1;s.controller().updateUnit();moving&=s.vel.x>0&&Math.abs(s.vel.y)<.001f;s.move(s.vel.x,s.vel.y);StormLogic.update(s);StormLift.update();
            bounds&=Math.abs(StormAim.delta(s.bodyFacing(),previous))<=1.801f;previous=s.bodyFacing();
            for(int h=0;h<3;h++){bounds&=Math.abs(s.aimYaw[h])<=40.001f;if(Math.abs(StormAim.delta(s.angleTo(rear),s.bodyFacing()))>55)blocked&=!s.beamReady(h);fired|=s.beamReady(h);}
            turned|=Math.abs(StormAim.delta(s.bodyFacing(),180))<4;
        }
        check("body-turn-rate-and-independent-head-gimbals-bounded",bounds);
        check("rear-laser-and-attraction-gated-until-body-facing",blocked&&turned&&fired&&rear.x>x);
        check("commanded-east-translation-continues-while-facing-and-firing-west",moving&&s.x>fromX+20&&s.command().hasCommand());
        m.build(s.matter,81539,8);m.pose(s.matter,s.age,s.bodyFacing(),0);m.aimHeads(s);boolean visible=false;for(int h=0;h<3;h++)visible|=m.beamVisible(s,h);check("aligned-aperture-can-render-active-beam",visible);
        // Player-controlled body: rotation does not mutate the independently supplied movement vector.
        Player player=Player.create();player.team(Team.sharded);player.add();mindustry.input.InputHandler.unitControl(player,s);s.rotation=90;s.bodyYaw=0;s.vel.set(-1,.25f);s.isShooting=false;StormAim.update(s,1);
        check("native-player-controls-body-intent-without-stealing-velocity",s.isPlayer()&&Math.abs(s.bodyYaw-1.8f)<.001f&&s.vel.x==-1&&s.vel.y==.25f);
        s=MechanicsChecks.fresh();s.matter=6000;s.age=100;s.bodyYaw=33;Vec2 point=new Vec2();StormTentacles.point(s,0,1,point);Unit victim=HeadlessChecks.unit(Team.crux,point.x,point.y);victim.health=victim.maxHealth=10000;Groups.unit.updatePhysics();StormTentacles.update(s,1);
        float start=s.tentacleStrike[0];check("nearby-target-starts-tentacle-windup-not-contact-damage",start==s.age&&victim.health==10000);
        s.age=start+17;StormTentacles.update(s,1);check("tentacle-windup-has-no-damage",victim.health==10000);
        s.age=start+29;StormTentacles.update(s,1);check("tentacle-swing-before-impact-has-no-damage",victim.health==10000);
        s.age=start+30;StormTentacles.update(s,1);check("tentacle-impact-applies-real-damage",victim.health<10000&&s.strikeDone[0]);float hp=victim.health;
        s.age=start+31;StormTentacles.update(s,1);check("tentacle-recovery-does-not-reapply-damage",victim.health==hp);
        boolean armGeometry=true;var field=StormModel.class.getDeclaredField("limbs");field.setAccessible(true);
        for(int phase:new int[]{0,9,18,24,30,42,54}){
            s.age=start+phase;m.build(s.matter,81539,8);m.pose(s.matter,s.age,s.bodyFacing(),0);m.aimHeads(s);int[][] bones=(int[][])field.get(m);
            for(int arm=0;arm<m.branches;arm++)for(int j=0;j<7;j++){m.renderer.point(bones[arm][j],0,0,0,actual);m.project(actual);StormTentacles.point(s,arm,j/7f,point);armGeometry&=Mathf.dst(actual[0]+s.x,actual[1]+s.y,point.x,point.y)<.015f;}
        }
        check("all-eight-animated-arms-share-render-and-hit-curve-at-seven-phases",m.branches==8&&armGeometry);
        // A dodge must miss: the attack snapshots a world-space strike, not homing touch damage.
        s=MechanicsChecks.fresh();s.matter=6000;s.age=100;StormTentacles.point(s,0,1,point);victim=HeadlessChecks.unit(Team.crux,point.x,point.y);victim.health=victim.maxHealth=10000;Groups.unit.updatePhysics();StormTentacles.update(s,1);victim.set(2500,1400);Groups.unit.updatePhysics();s.age+=30;StormTentacles.update(s,1);check("dodged-tentacle-strike-misses",victim.health==10000&&s.tentacleHits==0);
        s.bodyYaw=123;s.aimYaw[1]=-27;s.aimPitch[2]=-42;s.tentacleStrike[4]=s.age-12;s.strikeX[4]=456;s.strikeY[4]=789;s.strikeDone[4]=false;s.genome=717171;
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();s.writeSync(Writes.get(new DataOutputStream(bytes)));StormUnit replica=(StormUnit)StormContent.storm.create(s.team);replica.readSync(Reads.get(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()))));
        check("v6-sync-preserves-body-head-and-pre-impact-strike-state",replica.bodyYaw==123&&replica.aimYaw[1]==-27&&replica.aimPitch[2]==-42&&replica.strikeX[4]==456&&replica.strikeY[4]==789&&!replica.strikeDone[4]&&replica.tentacleStrike[4]==s.tentacleStrike[4]);
        arc.files.Fi save=new arc.files.Fi("v17-strike.msav");SaveIO.write(save);Vars.logic.reset();SaveIO.load(save);HeadlessChecks.playing();s=(StormUnit)Groups.unit.find(u->u instanceof StormUnit v&&v.genome==717171);
        check("v6-world-save-preserves-in-progress-attack-without-early-impact",s!=null&&s.bodyYaw==123&&!s.strikeDone[4]&&s.age-s.tentacleStrike[4]==12&&s.strikeX[4]==456);
        s=MechanicsChecks.fresh();s.matter=90;s.health=100;s.maxHealth=200;s.eatenUnits=2;s.eatenBuildings=3;
        StormUnit allied=StormLogic.spawn(Team.sharded,900,900);allied.matter=300;allied.health=400;allied.maxHealth=800;allied.eatenUnits=5;allied.eatenBuildings=7;
        StormUnit enemy=StormLogic.spawn(Team.crux,1600,900);enemy.matter=900;enemy.health=600;enemy.maxHealth=1000;enemy.eatenUnits=11;enemy.eatenBuildings=13;
        StormHud.Total[] totals=StormHud.collect(Team.sharded);
        check("hud-allied-sums-all-storms-not-first-entity",totals[0].count==2&&totals[0].mass==390&&totals[0].hp==500&&totals[0].maxHp==1000&&totals[0].units==7&&totals[0].buildings==10&&totals[0].fraction()==.5f);
        check("hud-enemy-summary-is-separate",totals[1].count==1&&totals[1].mass==900&&totals[1].hp==600&&totals[1].units==11&&totals[1].buildings==13);
        totals=StormHud.collect(Team.crux);check("hud-reclassifies-after-player-team-change",totals[0].mass==900&&totals[1].mass==390);
        enemy.remove();totals=StormHud.collect(Team.sharded);check("hud-removes-dead-or-removed-storms-and-handles-zero-total",totals[1].count==0&&totals[1].fraction()==0);
    }
}
