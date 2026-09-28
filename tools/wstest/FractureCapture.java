package wstest;
import arc.*;import arc.files.*;import arc.graphics.*;import arc.scene.*;import arc.scene.ui.*;import arc.struct.*;import arc.util.*;
import mindustry.*;import mindustry.content.*;import mindustry.core.GameState.State;import mindustry.entities.units.*;import mindustry.game.*;import mindustry.game.EventType.*;import mindustry.gen.*;import mindustry.world.*;
import wstorm.*;import wstorm.world.*;import wstorm.gfx.*;

/** Separate controlled damage demonstration; not presented as the unscripted growth run. */
public class FractureCapture{
    static int frame=-10;static Fi output;static Pixmap pixels;static PixmapIO.PngWriter writer;static StormUnit main;
    static boolean split,rebuilding,coreKilled;static float splitMatter;
    public static void start(){
        output=new Fi(System.getProperty("ws.capture"));output.mkdirs();
        for(Element e:Core.scene.getElements().copy())if(e instanceof Dialog)((Dialog)e).hide();
        Vars.logic.reset();Vars.state.rules=new Rules();Vars.state.rules.canGameOver=false;Vars.state.rules.waves=false;Vars.state.rules.waveTimer=false;Vars.state.rules.fog=false;Vars.state.rules.lighting=false;
        Vars.state.rules.teams.get(Team.sharded).rtsAi=false;Vars.state.rules.teams.get(Team.crux).rtsAi=false;
        Vars.state.map=new mindustry.maps.Map(StringMap.of("name","凋零风暴 · 核心破裂试验"));
        Vars.world.loadGenerator(140,105,ts->{for(int x=0;x<ts.width;x++)for(int y=0;y<ts.height;y++)ts.set(x,y,new Tile(x,y,Math.abs(y-34)<=1||Math.abs(x-69)<=1?Blocks.metalFloor:Blocks.darksand,Blocks.air,Blocks.air));});
        Vars.world.tile(8,8).setBlock(Blocks.coreShard,Team.sharded);Vars.world.tile(130,96).setBlock(Blocks.coreShard,Team.crux);
        for(int x=28;x<113;x+=3){Vars.world.tile(x,25).setBlock(Blocks.titaniumWall,Team.sharded);Vars.world.tile(x,84).setBlock(Blocks.titaniumWall,Team.sharded);}
        Vars.player.team(Team.sharded);Vars.player.set(64,64);Vars.player.add();Vars.state.teams.updateTeamStats();
        Vars.state.set(State.playing);Vars.ui.loadfrag.hide();Vars.renderer.landTime=0;Vars.disableUI=true;
        Time.setDeltaProvider(()->5f);Time.delta=5;
        Events.run(Trigger.preDraw,FractureCapture::pre);Events.run(Trigger.postDraw,FractureCapture::post);
    }
    static void pre(){
        float xmin=250,xmax=850,ymin=200,ymax=1080;float[] point=new float[3];
        for(Unit unit:Groups.unit)if(unit instanceof StormUnit q){wstorm.model.StormModel model=StormContent.storm.models.get(q.id);if(model==null)continue;
            for(int i=0;i<model.rig.pieceCount;i++){var part=model.rig.pieces[i];if(model.rig.hidden[part.bone]||model.rig.alpha[part.bone]<.01f)continue;
                for(int k=0;k<part.mesh.verts;k++){model.renderer.point(part.bone,part.mesh.vx[k],part.mesh.vy[k],part.mesh.vz[k],point);model.project(point);xmin=Math.min(xmin,q.x+point[0]-45);xmax=Math.max(xmax,q.x+point[0]+45);ymin=Math.min(ymin,q.y+point[1]-45);ymax=Math.max(ymax,q.y+point[1]+45);}}}
        float width=Math.max(1440,Math.max(xmax-xmin,(ymax-ymin)*1.6f));
        Core.camera.position.set((xmin+xmax)/2,(ymin+ymax)/2);Core.camera.width=width;Core.camera.height=width*Core.graphics.getHeight()/Core.graphics.getWidth();Core.camera.update();
        if(frame==0){main=StormLogic.spawn(Team.crux,552,472);main.matter=4200;main.visualMatter=4200;main.maxHealth=WitherStormMod.spec.initialHealth+4200*WitherStormMod.spec.healthPerMatter;main.health=main.maxHealth;main.genome=81539;main.controller(new AIController(){@Override public void updateUnit(){}});}
        if(frame==8)main.damage(main.maxHealth*11); // real damage API, command core is protected
        if(frame==13){split=StormLogic.active()==3;rebuilding=main.reassembly>0;for(Unit u:Groups.unit)if(u instanceof StormUnit s)splitMatter+=s.matter;}
        if(frame==78){main.pierceCore(420);main.pierceCore(main.coreIntegrity+1);}
    }
    static void shot(String name){
        if(pixels==null){pixels=new Pixmap(Core.graphics.getWidth(),Core.graphics.getHeight());writer=new PixmapIO.PngWriter(800000);writer.setFlipY(true);writer.setCompression(2);}
        Gl.pixelStorei(Gl.packAlignment,1);pixels.pixels.clear();Gl.readPixels(0,0,pixels.width,pixels.height,Gl.rgba,Gl.unsignedByte,pixels.pixels);
        try{writer.write(output.child(name),pixels);}catch(Exception e){throw new RuntimeException(e);}
    }
    static void post(){
        try{
            if(frame>=0&&frame<110)shot(String.format("%04d.png",frame));
            if(frame==12)shot("fission.png");if(frame==60)shot("reassembled.png");
            if(frame==110){
                coreKilled=main.dead&&main.coreIntegrity<=0&&StormLogic.active()==0;
                if(!split||!rebuilding||!coreKilled||Math.abs(splitMatter-4200)>.1f)throw new AssertionError("Fission / core destruction failed");
                output.child("fracture-result.json").writeString("{\"engine\":\"Mindustry v160.5 installed JAR\",\"fixture\":\"Starting matter 4200; scripted severe shell strike at frame8 and special core-breach shots at frame78; not the unscripted growth demonstration\",\"frames\":110,\"ticksPerFrame\":5,\"splitIntoThree\":"+split+",\"reassembly\":"+rebuilding+",\"massAfterFission\":"+splitMatter+",\"coreDestroyedAndChildrenRemoved\":"+coreKilled+",\"live3dFaces\":"+StormType.facesDrawn+"}");
                Log.info("WITHER_STORM_FISSION_ALL_PASS");pixels.dispose();writer.dispose();Core.app.post(()->Core.app.exit());
            }
            frame++;
        }catch(Throwable t){Log.err(t);System.exit(3);}
    }
}
