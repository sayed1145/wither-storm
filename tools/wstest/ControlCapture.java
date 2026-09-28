package wstest;
import arc.*;import arc.files.*;import arc.graphics.*;import arc.input.*;import arc.math.*;import arc.math.geom.*;import arc.scene.*;import arc.scene.ui.*;import arc.struct.*;import arc.util.*;
import mindustry.*;import mindustry.content.*;import mindustry.core.GameState.State;import mindustry.entities.units.*;import mindustry.game.*;import mindustry.game.EventType.*;import mindustry.gen.*;import mindustry.input.InputHandler;import mindustry.world.*;
import wstorm.*;import wstorm.world.*;import wstorm.gfx.*;import wstorm.model.*;
/** Scripted scene, real input keyboard and native control/command/configuration APIs. */
public class ControlCapture{
    static int frame=-8;static Fi output;static Pixmap pixels;static PixmapIO.PngWriter writer;static StormUnit main;
    static StormCommand.CommandBuild totem;static boolean five,idle,command,possessed,toggleSafe,manualMoved,detached,autonomous,tentacle,wallCross;
    static float possessedX,idleX,idleY;static Unit ground,air,wallVictim;static int maxFlights;static StringBuilder samples=new StringBuilder();
    static Unit food(mindustry.type.UnitType type,float x,float y){Unit u=type.create(Team.crux);u.set(x,y);u.health=u.maxHealth=10000;u.controller(new AIController(){@Override public void updateUnit(){}});u.add();return u;}
    public static void start(){
        output=new Fi(System.getProperty("ws.capture"));output.mkdirs();for(Element e:Core.scene.getElements().copy())if(e instanceof Dialog d)d.hide();
        Vars.logic.reset();Vars.state.rules=new Rules();Vars.state.rules.canGameOver=false;Vars.state.rules.waves=false;Vars.state.rules.waveTimer=false;Vars.state.rules.fog=false;Vars.state.rules.lighting=false;
        Vars.state.rules.teams.get(Team.sharded).rtsAi=false;Vars.state.rules.teams.get(Team.crux).rtsAi=false;
        Vars.state.map=new mindustry.maps.Map(StringMap.of("name","v1.6 · 指挥 / 附身 / 并行吸收验证"));
        Vars.world.loadGenerator(180,140,ts->{for(int x=0;x<ts.width;x++)for(int y=0;y<ts.height;y++)ts.set(x,y,new Tile(x,y,Math.abs(y-35)<=1||Math.abs(x-70)<=1?Blocks.metalFloor:Blocks.darksand,Blocks.air,Blocks.air));});
        Vars.world.tile(9,9).setBlock(Blocks.coreShard,Team.sharded);Vars.world.tile(40,35).setBlock(StormContent.commandTotem,Team.sharded);
        totem=(StormCommand.CommandBuild)Vars.world.build(40,35);
        for(int y=38;y<72;y++)Vars.world.tile(70,y).setBlock(Blocks.titaniumWall,Team.derelict);
        Vars.player.team(Team.sharded);Vars.player.set(72,72);Vars.player.add();Vars.state.teams.updateTeamStats();Vars.state.set(State.playing);Vars.ui.loadfrag.hide();Vars.renderer.landTime=0;Vars.disableUI=true;
        Core.scene.setKeyboardFocus(null);Time.setDeltaProvider(()->4f);Time.delta=4;
        Events.run(Trigger.preDraw,ControlCapture::pre);Events.run(Trigger.postDraw,ControlCapture::post);
    }
    static void pre(){
        try{
            float x0=270,x1=960,y0=170,y1=710;float[] v=new float[3];
            if(main!=null){StormModel m=StormContent.storm.models.get(main.id);if(m!=null)for(int i=0;i<m.rig.pieceCount;i++){var p=m.rig.pieces[i];if(m.rig.hidden[p.bone])continue;for(int k=0;k<p.mesh.verts;k++){m.renderer.point(p.bone,p.mesh.vx[k],p.mesh.vy[k],p.mesh.vz[k],v);m.project(v);x0=Math.min(x0,main.x+v[0]-50);x1=Math.max(x1,main.x+v[0]+50);y0=Math.min(y0,main.y+v[1]-50);y1=Math.max(y1,main.y+v[1]+50);}}}
            float width=Math.max(920,Math.max(x1-x0,(y1-y0)*1.6f));Core.camera.position.set((x0+x1)/2,(y0+y1)/2);Core.camera.width=width;Core.camera.height=width/1.6f;Core.camera.update();
            if(frame==0){
                main=StormLogic.spawn(Team.sharded,480,420);main.genome=81539;
                for(int i=0;i<5;i++)Vars.world.tile(65+i*4,42).setBlock(Blocks.titaniumWall,Team.crux);
                wallVictim=food(UnitTypes.dagger,610,475);food(UnitTypes.dagger,730,450);food(UnitTypes.flare,760,470);Vars.state.teams.updateTeamStats();
            }
            if(frame==2)five=main.buildingsInFlight()==5&&main.vel.len()>0;
            if(frame==20){if(totem.configTapped())throw new AssertionError("Switch opened a configuration panel");}
            if(frame==28)idle=main.isCommandable()&&main.vel.isZero();
            if(frame==40)InputHandler.commandUnits(Vars.player,new int[]{main.id},null,null,new Vec2(720,420),false,true);
            if(frame==48)command=main.command().hasCommand()&&main.vel.len()>0;
            if(frame==70){Call.unitControl(Vars.player,main);possessed=main.isPlayer()&&Vars.player.unit()==main;possessedX=main.x;Core.input.getKeyboard().keyDown(KeyCode.d);}
            if(frame==80){totem.configTapped();toggleSafe=main.isPlayer()&&Vars.player.unit()==main;}
            if(frame==90){totem.configTapped();toggleSafe&=main.isPlayer()&&Vars.player.unit()==main;}
            if(frame==100){Core.input.getKeyboard().keyUp(KeyCode.d);manualMoved=main.x>possessedX+8;Call.unitClear(Vars.player);detached=!main.isPlayer()&&main.controller() instanceof StormAI;}
            if(frame==110){idleX=main.x;idleY=main.y;}
            if(frame==120)detached&=main.isCommandable()&&main.vel.isZero()&&Mathf.dst(main.x,main.y,idleX,idleY)<3;
            if(frame==124){food(UnitTypes.dagger,1080,420);Vars.state.teams.updateTeamStats();}
            if(frame==125)totem.configTapped();
            if(frame==133)autonomous=!main.isCommandable()&&main.vel.len()>0;
            if(frame==155){main.matter=5000;main.visualMatter=5000;main.maxHealth=WitherStormMod.spec.initialHealth+main.matter*WitherStormMod.spec.healthPerMatter;main.health=main.maxHealth;}
            if(frame==170){Vec2 point=new Vec2();StormTentacles.point(main,0,.86f,point);ground=food(UnitTypes.dagger,point.x,point.y);air=food(UnitTypes.flare,point.x+3,point.y);Vars.state.teams.updateTeamStats();main.tentacleTimer=0;}
            if(frame==184)tentacle=main.tentacleHits>=2&&ground.health<10000&&air.health<10000;
        }catch(Throwable e){Log.err(e);System.exit(5);}
    }
    static void shot(String name){
        if(pixels==null){pixels=new Pixmap(Core.graphics.getWidth(),Core.graphics.getHeight());writer=new PixmapIO.PngWriter(800000);writer.setFlipY(true);writer.setCompression(2);}
        Gl.pixelStorei(Gl.packAlignment,1);pixels.pixels.clear();Gl.readPixels(0,0,pixels.width,pixels.height,Gl.rgba,Gl.unsignedByte,pixels.pixels);
        try{writer.write(output.child(name),pixels);}catch(Exception e){throw new RuntimeException(e);}
    }
    static void post(){
        try{
            if(frame>=0&&frame<230){
                maxFlights=Math.max(maxFlights,main.buildingsInFlight());
                if(wallVictim.isAdded()&&wallVictim.x<555&&wallVictim.elevation>.9f)wallCross=true;
                if(samples.length()>0)samples.append(',');samples.append("{\"frame\":").append(frame).append(",\"player\":").append(main.isPlayer()).append(",\"commandMode\":").append(StormCommand.enabled(main.team)).append(",\"flights\":").append(main.buildingsInFlight()).append(",\"x\":").append(main.x).append(",\"y\":").append(main.y).append(",\"tentacleHits\":").append(main.tentacleHits).append('}');
                if(!Boolean.getBoolean("ws.quick"))shot(String.format("control-%04d.png",frame));
            }
            if(frame==8)shot("five-flights.png");if(frame==32)shot("idle.png");if(frame==88)shot("possessed.png");if(frame==185)shot("tentacle.png");if(frame==205)shot("new-body-material.png");
            if(frame==230){
                String report="{\"version\":\"1.7.1\",\"actualInstalledJar\":true,\"nativeKeyboardMovement\":"+manualMoved+",\"fiveParallelBuildingsWhileMoving\":"+five+",\"oneClickNoPanel\":true,\"commandIdle\":"+idle+",\"nativeCommandMovement\":"+command+",\"nativePossession\":"+possessed+",\"togglePreservesPlayer\":"+toggleSafe+",\"detachToIdle\":"+detached+",\"switchOffResumesAI\":"+autonomous+",\"groundUnitLiftedAcrossWall\":"+wallCross+",\"tentacleHitsAirAndGround\":"+tentacle+",\"maxConcurrentBuildingFlights\":"+maxFlights+",\"scriptedFixturesAndLateMass\":true,\"frames\":230,\"samples\":["+samples+"]}";
                output.child("control-result.json").writeString(report);Log.info(report.substring(0,report.indexOf("samples")));
                if(!five||!idle||!command||!possessed||!toggleSafe||!manualMoved||!detached||!autonomous||!tentacle||!wallCross)throw new AssertionError("Actual control / concurrency scene failed");
                Log.info("WITHER_STORM_NATIVE_CONTROL_ALL_PASS");pixels.dispose();writer.dispose();Core.app.exit();
            }
            frame++;
        }catch(Throwable e){Log.err(e);System.exit(5);}
    }
}
