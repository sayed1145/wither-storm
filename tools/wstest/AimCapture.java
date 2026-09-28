package wstest;
import arc.*;import arc.files.*;import arc.graphics.*;import arc.math.*;import arc.math.geom.*;import arc.scene.*;import arc.scene.ui.*;import arc.struct.*;import arc.util.*;import mindustry.*;import mindustry.content.*;import mindustry.core.GameState.State;import mindustry.entities.units.*;import mindustry.game.*;import mindustry.game.EventType.*;import mindustry.gen.*;import mindustry.world.*;import wstorm.*;import wstorm.world.*;import wstorm.model.*;import wstorm.gfx.*;
/** Actual installed JAR scene. Mass presets and controlled target placement are disclosed. */
public final class AimCapture{
    static int frame=-8;static StormUnit main;static Unit rear;static float startX,rearX,rearY;static boolean gated=true,moving=true,aligned,frontThree,windupClean,impact,settings;static int beforeHits;static Fi output;static Seq<Unit> prey=new Seq<>();static StringBuilder samples=new StringBuilder();
    static Unit food(float x,float y,int i){Unit u=(i%2==0?UnitTypes.dagger:UnitTypes.flare).create(Team.crux);u.set(x,y);u.health=u.maxHealth=10000;u.controller(new AIController(){@Override public void updateUnit(){}});u.add();prey.add(u);return u;}
    static void clearFood(){for(Unit u:prey)u.remove();prey.clear();for(int h=0;h<3;h++){main.targetId[h]=-1;main.targetKind[h]=0;}java.util.Arrays.fill(main.directId,-1);main.scanTimer=0;}
    public static void start(){
        output=new Fi(System.getProperty("ws.capture"));ControlCapture.output=output;output.mkdirs();for(Element e:Core.scene.getElements().copy())if(e instanceof Dialog d)d.hide();
        Vars.logic.reset();Vars.state.rules=new Rules();Vars.state.rules.canGameOver=false;Vars.state.rules.waves=false;Vars.state.rules.fog=false;Vars.state.rules.unitCap=200;
        Vars.state.map=new mindustry.maps.Map(StringMap.of("name","v1.7 · 发射孔 / 独立身体 / 触手 / 敌我汇总"));
        Vars.world.loadGenerator(400,260,ts->{for(int x=0;x<ts.width;x++)for(int y=0;y<ts.height;y++)ts.set(x,y,new Tile(x,y,Blocks.darksand,Blocks.air,Blocks.air));});
        Vars.world.tile(10,10).setBlock(Blocks.coreShard,Team.sharded);Vars.world.tile(15,15).setBlock(StormContent.commandTotem,Team.sharded);((StormCommand.CommandBuild)Vars.world.build(15,15)).configTapped();
        Vars.player.team(Team.sharded);Vars.player.set(80,80);Vars.player.add();Vars.state.set(State.playing);Vars.ui.loadfrag.hide();Vars.renderer.landTime=0;Vars.disableUI=false;
        Core.settings.put("ws-hud",true);Core.settings.put("ws-hud-allies",true);Core.settings.put("ws-hud-enemies",true);Core.settings.put("ws-health-bars",true);Core.settings.put("ws-health-text",true);Core.settings.put("ws-hud-width",360);Core.settings.put("ws-hud-opacity",95);
        Time.setDeltaProvider(()->2f);Time.delta=2;Events.run(Trigger.preDraw,AimCapture::pre);Events.run(Trigger.postDraw,AimCapture::post);
        Core.app.addListener(new ApplicationListener(){@Override public void update(){int f=frame-1;if(f<0||f>260)return;
            if(!Boolean.getBoolean("ws.quick"))ControlCapture.shot(String.format("aim-%04d.png",f));
            if(f==40||f==82||f==132||f==167||f==175||f==186||f==236||f==250)ControlCapture.shot("proof-"+f+".png");
        }});
    }
    static void pre(){try{
        Core.camera.position.set(frame<70?1500:frame<160?main.x:1600,frame<70?840:frame<160?1300:1200);Core.camera.width=frame<160?1950:1450;Core.camera.height=Core.camera.width*700/1120;Core.camera.update();
        if(frame==0){main=StormLogic.spawn(Team.sharded,1500,1000);main.matter=main.visualMatter=2600;main.bodyYaw=main.rotation=270;main.genome=81539;main.health=35000;for(int i=0;i<3;i++)food(1360+i*140,120,i);
            StormUnit ally=StormLogic.spawn(Team.sharded,2800,1900);ally.eatenUnits=3;ally.eatenBuildings=5;StormUnit enemy=StormLogic.spawn(Team.crux,300,1800);enemy.matter=900;enemy.health=19000;enemy.controller(new AIController(){@Override public void updateUnit(){}});Vars.state.teams.updateTeamStats();}
        if(frame==70){clearFood();rear=food(1500,1900,0);rearX=rear.x;rearY=rear.y;main.command().commandPosition(new Vec2(2450,1000));startX=main.x;Vars.state.teams.updateTeamStats();}
        if(frame==160){clearFood();main.command().targetPos=null;main.command().attackTarget=null;main.vel.setZero();main.set(1600,1050);main.bodyYaw=main.rotation=270;main.matter=main.visualMatter=6000;main.scanTimer=10000;main.skullTimer=10000;beforeHits=main.tentacleHits;Vec2 p=new Vec2();for(int i=0;i<8;i++){StormTentacles.point(main,i,.96f,p);food(p.x,p.y,i);}Groups.unit.updatePhysics();Vars.state.teams.updateTeamStats();}
        if(frame>=160&&frame<230){main.scanTimer=10000;java.util.Arrays.fill(main.directId,-1);}
        if(frame==231){Core.settings.put("ws-hud-enemies",false);Core.settings.put("ws-health-text",false);Core.settings.put("ws-hud-width",300);}
        if(frame==241){settings=!Core.settings.getBool("ws-hud-enemies")&&!Core.settings.getBool("ws-health-text")&&!WitherStormMod.hud.getChildren().get(1).visible&&Math.abs(WitherStormMod.hud.getChildren().get(0).getWidth()-arc.scene.ui.layout.Scl.scl(300))<2;Core.settings.put("ws-hud-enemies",true);Core.settings.put("ws-health-text",true);Core.settings.put("ws-hud-width",360);}
    }catch(Throwable e){Log.err(e);System.exit(5);}}
    static void post(){try{
        if(frame>=0){int active=0;for(int h=0;h<3;h++)if(main.beamReady(h))active++;
            if(frame>12&&frame<70)frontThree|=active==3;
            if(frame>70&&frame<159){moving&=main.vel.x>0;float error=Math.abs(StormAim.delta(main.angleTo(rear),main.bodyFacing()));if(error>55)gated&=active==0&&rear.x==rearX&&rear.y==rearY;aligned|=active>0;}
            if(frame==167)windupClean=main.tentacleHits==beforeHits;
            if(frame==186)impact=main.tentacleHits>beforeHits;
            if(samples.length()>0)samples.append(',');samples.append("{\"frame\":").append(frame).append(",\"bodyYaw\":").append(main.bodyFacing()).append(",\"x\":").append(main.x).append(",\"activeBeams\":").append(active).append(",\"hits\":").append(main.tentacleHits).append('}');

        }
        if(frame==260){StormHud.Total[] totals=StormHud.collect(Team.sharded);boolean hud=totals[0].count==2&&totals[1].count==1;boolean translate=main.x>startX+20;
            // Main was intentionally repositioned at phase 3; motion itself is checked at every phase-2 frame.
            String result="{\"version\":\"1.7.1\",\"actualInstalledJar\":true,\"scriptedMassAndTargets\":true,\"gl2\":"+Boolean.getBoolean("ws.gl2")+",\"threeFrontAperturesActive\":"+frontThree+",\"rearBeamAndPullBlocked\":"+gated+",\"translationDuringTurn\":"+moving+",\"firesAfterAlignment\":"+aligned+",\"windupBeforeDamage\":"+windupClean+",\"animatedImpact\":"+impact+",\"twoTeamPanels\":"+hud+",\"displayToggles\":"+settings+",\"frames\":261,\"samples\":["+samples+"]}";
            output.child("aim-result.json").writeString(result);if(!frontThree||!gated||!moving||!aligned||!windupClean||!impact||!hud||!settings)throw new AssertionError(result.substring(0,result.indexOf("samples")));
            Log.info("WITHER_STORM_V17_AIM_HUD_ALL_PASS");Core.app.exit();}
        frame++;
    }catch(Throwable e){Log.err(e);System.exit(5);}}
}
