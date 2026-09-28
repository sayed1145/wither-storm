package wstest;
import arc.*;import arc.files.*;import arc.graphics.*;import arc.graphics.g2d.*;import arc.math.*;import arc.scene.*;import arc.scene.ui.*;import arc.struct.*;import arc.util.*;
import mindustry.*;import mindustry.content.*;import mindustry.core.GameState.State;import mindustry.entities.units.*;import mindustry.game.*;import mindustry.game.EventType.*;import mindustry.gen.*;import mindustry.world.*;
import wstorm.*;import wstorm.world.*;import wstorm.model.*;import wstorm.gfx.*;

/** A real v160.5 game capture. Food is real enemy units/buildings; stage mass presets are explicitly reported. */
public class ClientCapture{
    static int frame=-12;static Fi output;static Pixmap pixels;static PixmapIO.PngWriter writer;static StormUnit storm;
    static float cameraX=552,cameraY=475,view=620;
    static boolean audioPlayed;static int firstBeamCount=-1,simultaneousBeamFrames,distinctLockFrames;
    static final StringBuilder sample=new StringBuilder();
    static Building place(Block b,int x,int y,Team team,int rot){Vars.world.tile(x,y).setBlock(b,team,rot);return Vars.world.build(x,y);}
    static void food(float x,float y,int i){Unit u=(i%6==0?UnitTypes.scepter:i%4==0?UnitTypes.mace:i%4==1?UnitTypes.flare:UnitTypes.dagger).create(Team.sharded);u.set(x,y);u.rotation=90;u.apply(StatusEffects.disarmed,999999);u.controller(new AIController(){@Override public void updateUnit(){}});u.add();}
    public static void start(){
        output=new Fi(System.getProperty("ws.capture"));output.mkdirs();
        Core.settings.put("ws-detailed",true);Core.settings.put("ws-atmosphere",true);Core.settings.put("ws-music",true);Core.settings.put("musicvol",55);
        for(Element el:Core.scene.getElements().copy())if(el instanceof Dialog)((Dialog)el).hide();
        Vars.logic.reset();Vars.state.rules=new Rules();Vars.state.rules.canGameOver=false;Vars.state.rules.infiniteResources=false;Vars.state.rules.waves=false;Vars.state.rules.waveTimer=false;Vars.state.rules.fog=false;Vars.state.rules.lighting=false;Vars.state.rules.unitCap=200;
        Vars.state.rules.teams.get(Team.sharded).rtsAi=false;Vars.state.rules.teams.get(Team.sharded).buildAi=false;Vars.state.rules.teams.get(Team.crux).rtsAi=false;Vars.state.rules.teams.get(Team.crux).buildAi=false;
        Vars.state.map=new mindustry.maps.Map(StringMap.of("name","凋零风暴 · 吞噬前线"));
        Vars.world.loadGenerator(150,116,ts->{for(int x=0;x<ts.width;x++)for(int y=0;y<ts.height;y++){
            Block floor=Blocks.stone;
            boolean road=Math.abs(x-63)<=1||Math.abs(y-39)<=1;
            if(road)floor=Blocks.metalFloor;
            else floor=Blocks.darksand;
            ts.set(x,y,new Tile(x,y,floor,Blocks.air,Blocks.air));
        }});
        place(Blocks.coreNucleus,17,18,Team.sharded,0);place(Blocks.coreShard,139,104,Team.crux,0);
        // Detailed, destructible enemy industrial blocks, separate from the purely decorative floor.
        for(int y=35;y<=63;y+=7)for(int x=48;x<=88;x+=8){
            if(Mathf.dst(x*8,y*8,552,472)<108)continue;
            if((x+y)%3==0)place(Blocks.titaniumWallLarge,x,y,Team.sharded,0);
            else place((x+y)%2==0?Blocks.siliconSmelter:Blocks.graphitePress,x,y,Team.sharded,0);
        }
        for(int x=44;x<=92;x+=2){place(Blocks.titaniumWall,x,32,Team.sharded,0);place(Blocks.titaniumWall,x,69,Team.sharded,0);}
        for(int y=32;y<=69;y+=2){place(Blocks.titaniumWall,43,y,Team.sharded,0);place(Blocks.titaniumWall,93,y,Team.sharded,0);}
        for(int x=45;x<91;x++)if(Vars.world.tile(x,39).block()==Blocks.air)place(Blocks.titaniumConveyor,x,39,Team.sharded,0);
        for(int i=0;i<16;i++){float a=185+i*10;food(552+Mathf.cosDeg(a)*140,457+Mathf.sinDeg(a)*140,i);}
        for(int i=0;i<7;i++)place(Blocks.powerNode,46+i*7,67,Team.sharded,0);
        place(StormContent.summoner,102,44,Team.sharded,0);
        Vars.player.team(Team.sharded);Vars.player.set(136,144);Vars.player.add();Vars.state.teams.updateTeamStats();
        Vars.state.set(State.playing);Vars.ui.loadfrag.hide();Vars.renderer.landTime=0;Vars.disableUI=true;
        Time.setDeltaProvider(()->4f);Time.delta=4f;
        StormTextures.export(output.child("procedural-atlas.png"));
        Events.run(Trigger.preDraw,ClientCapture::pre);Events.run(Trigger.postDraw,ClientCapture::post);
        Core.app.addListener(new ApplicationListener(){@Override public void update(){if(frame==333)shot("hud.png");}});
        Log.info("WITHER_STORM_CAPTURE_STARTED");
    }
    static void pre(){
        if(Boolean.getBoolean("ws.review"))return;
        if(storm!=null&&Boolean.getBoolean("ws.trackcamera")){
            StormModel model=StormContent.storm.models.get(storm.id);
            if(model!=null){
                float xmin=-210,xmax=210,ymin=-200,ymax=100;float[] q=new float[3];
                for(int i=0;i<model.rig.pieceCount;i++){
                    var piece=model.rig.pieces[i];if(model.rig.hidden[piece.bone]||model.rig.alpha[piece.bone]<.15f)continue;
                    for(int j=0;j<piece.mesh.verts;j++){
                        model.renderer.point(piece.bone,piece.mesh.vx[j],piece.mesh.vy[j],piece.mesh.vz[j],q);model.project(q);
                        xmin=Math.min(xmin,q[0]-22);xmax=Math.max(xmax,q[0]+22);ymin=Math.min(ymin,q[1]-22);ymax=Math.max(ymax,q[1]+22);
                    }
                }
                float desired=Math.max(620,Math.max(xmax-xmin,(ymax-ymin)*1.6f));
                view=desired*1.08f;cameraX=storm.x+(xmin+xmax)*.5f;cameraY=storm.y+(ymin+ymax)*.5f;
            }
        }
        Core.camera.position.set(cameraX,cameraY);Core.camera.width=view;Core.camera.height=view*Core.graphics.getHeight()/Core.graphics.getWidth();Core.camera.update();
        if(frame==0){storm=StormLogic.spawn(Team.crux,552,472);storm.controller(new AIController(){@Override public void updateUnit(){}});storm.rotation=270;storm.genome=81539;}
        // Explicit stage showcase, NOT evidence of natural mass progression.
        if(frame==60){storm.matter=500;storm.visualMatter=500;}
        if(frame==120){storm.matter=1600;storm.visualMatter=1600;}
        if(frame==175){storm.matter=5000;storm.visualMatter=5000;view=920;}
        if(frame==94){for(int i=0;i<9;i++)food(478+i*18,340+i%2*20,i+17);Vars.state.teams.updateTeamStats();}
        if(frame>=325){Vars.state.set(State.paused);}
        if(frame==326){Core.settings.put("ws-detailed",false);}
        if(frame==327){Core.settings.put("ws-detailed",true);Core.settings.put("ws-atmosphere",false);}
        if(frame==328){Core.settings.put("ws-atmosphere",true);}
        if(frame==329){Vars.disableUI=false;}
    }
    static void shot(String name){
        if(pixels==null){pixels=new Pixmap(Core.graphics.getWidth(),Core.graphics.getHeight());writer=new PixmapIO.PngWriter(1024*1024);writer.setFlipY(true);writer.setCompression(2);}
        Gl.pixelStorei(Gl.packAlignment,1);pixels.pixels.clear();Gl.readPixels(0,0,pixels.width,pixels.height,Gl.rgba,Gl.unsignedByte,pixels.pixels);
        try{writer.write(output.child(name),pixels);}catch(Exception e){throw new RuntimeException(e);}
    }
    static void post(){
        if(Boolean.getBoolean("ws.review"))return;
        try{
            boolean quick=Boolean.getBoolean("ws.quick");
            if(StormAudio.music!=null&&StormAudio.music.isPlaying())audioPlayed=true;
            if(frame>=0&&frame<325&&!quick)shot(String.format("%04d.png",frame));
            if(frame==2)shot("initial.png");if(frame==75)shot("middle.png");if(frame==150)shot("three-heads.png");if(frame==55)shot("feeding.png");if(frame==180)shot("grown.png");if(frame==25)shot("beams.png");
            if(frame==326)shot("low-detail.png");if(frame==327)shot("atmosphere-off.png");if(frame==328)shot("atmosphere-on.png");
            if(frame>=0&&storm!=null){int active=0;for(int h=0;h<3;h++)if(storm.beamCapable(h)&&storm.targetKind[h]==1&&storm.targetId[h]>=0&&storm.reassembly<=0)active++;
                if(active>0&&firstBeamCount<0)firstBeamCount=active;if(active==3)simultaneousBeamFrames++;
                if(active==3&&storm.targetId[0]!=storm.targetId[1]&&storm.targetId[0]!=storm.targetId[2]&&storm.targetId[1]!=storm.targetId[2])distinctLockFrames++;
            }
            if(frame>=0&&frame%20==0&&storm!=null){
                StormModel m=StormContent.storm.models.get(storm.id);if(sample.length()>0)sample.append(',');
                sample.append("{\"frame\":").append(frame).append(",\"matter\":").append(storm.matter).append(",\"units\":").append(storm.eatenUnits).append(",\"buildings\":").append(storm.eatenBuildings).append(",\"faces\":").append(m==null?0:m.rig.faces()).append(",\"branches\":").append(m==null?0:m.branches).append('}');
            }
            if(frame==330){
                arc.scene.ui.layout.Table config=new arc.scene.ui.layout.Table();
                StormSummoner.SummonerBuild altar=(StormSummoner.SummonerBuild)Vars.world.build(102,44);
                for(int test=0;test<30;test++){altar.buildConfiguration(config);if(config.getChildren().size!=3)throw new AssertionError("Configuration children leaked");}
                if(WitherStormMod.hud.touchable!=arc.scene.event.Touchable.disabled)throw new AssertionError("HUD consumes touch");
                float z=Draw.z();altar.draw();if(Draw.z()!=z)throw new AssertionError("Altar z leaked");
                output.child("ui-checks.json").writeString("{\"configurationRebuilds\":30,\"childrenStable\":true,\"hudNonInteractive\":true,\"altarZRestored\":true,\"manualPhoneTouchesTested\":false}");
            }
            if(frame==335){
                if(firstBeamCount!=3||simultaneousBeamFrames<10||distinctLockFrames<10)throw new AssertionError("Three-head synchronized distinct locks not observed");
                output.child("beam-sync.json").writeString("{\"firstActivationHeadCount\":"+firstBeamCount+",\"simultaneousBeamFrames\":"+simultaneousBeamFrames+",\"distinctLockFrames\":"+distinctLockFrames+"}");
                Log.info("CAPTURE METRICS faces="+StormType.facesDrawn+" textured="+StormTextures.texturedFaces+" beams="+StormVisual.beamFaces+" units="+storm.eatenUnits+" buildings="+storm.eatenBuildings+" age="+storm.age+" mass="+storm.matter+" samples="+sample);if(Boolean.getBoolean("ws.audio")&&(!Core.audio.initialized()||!StormAudio.loaded||StormAudio.failed||!audioPlayed))throw new AssertionError("Native theme decoding/playback not verified");
                if(StormType.facesDrawn<5000||StormTextures.texturedFaces<2000||StormVisual.beamFaces<100||storm.eatenUnits<2||storm.eatenBuildings<1)throw new AssertionError("Missing actual geometry / texture / absorption");
                output.child("client-result.json").writeString("{\"engine\":\"Mindustry v160.5 desktop\",\"actualGameplay\":true,\"massPresetForStageShowcase\":true,\"live3dFaces\":"+StormType.facesDrawn+",\"texturedFaces\":"+StormTextures.texturedFaces+",\"beam3dFaces\":"+StormVisual.beamFaces+",\"ghostDraws\":"+StormVisual.ghostsDrawn+",\"unitsAbsorbed\":"+storm.eatenUnits+",\"buildingsAbsorbed\":"+storm.eatenBuildings+",\"matter\":"+storm.matter+",\"capturedFrames\":"+(quick?0:325)+",\"ticksPerFrame\":4,\"musicLoaded\":"+StormAudio.loaded+",\"musicFailed\":"+StormAudio.failed+",\"decodedMusicSeconds\":"+StormAudio.decodedLength+",\"audioInitialized\":"+Core.audio.initialized()+",\"musicWasPlaying\":"+audioPlayed+",\"gl\":\""+Core.graphics.getGLVersion().toString().replace("\n"," ").replace('"','\'')+"\",\"samples\":["+sample+"]}");
                Log.info("WITHER_STORM_CLIENT_ALL_PASS faces="+StormType.facesDrawn+" units="+storm.eatenUnits+" buildings="+storm.eatenBuildings);
                pixels.dispose();writer.dispose();Core.app.post(()->Core.app.exit());
            }
            frame++;
        }catch(Throwable t){Log.err(t);System.exit(3);}
    }
}
