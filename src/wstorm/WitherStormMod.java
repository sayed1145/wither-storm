package wstorm;

import arc.*;
import arc.files.*;
import arc.graphics.*;
import arc.scene.ui.layout.*;
import mindustry.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.mod.*;
import mindustry.ui.*;
import wstorm.world.*;
import wstorm.gfx.*;

public final class WitherStormMod extends Mod{
    public static Table hud;
    public static StormSpec spec=new StormSpec();
    public WitherStormMod(){
        StormUnit.mapping=EntityMapping.register("wither-storm-organism",StormUnit::new);
        Events.on(ResetEvent.class,e->{StormVisual.clear();StormAudio.stop();StormCommand.clear();StormLift.clear();if(StormContent.storm!=null)StormContent.storm.clear();});
        Events.run(Trigger.beforeGameUpdate,StormLift::update);
        Events.run(Trigger.afterGameUpdate,StormLift::update);
        Events.run(Trigger.update,()->{if(!Vars.headless)StormAudio.update();});
        Events.on(ClientLoadEvent.class,e->{
            StormTextures.load();wstorm.model.AltarModel.prepare();StormAudio.load();
            Vars.netClient.addPacketHandler("wither-storm-ingest",StormVisual::receive);
            Vars.netClient.addPacketHandler("wither-storm-vanish",StormVisual::vanish);
            Vars.ui.settings.addCategory("凋零风暴",Icon.warning,t->{
                t.checkPref("ws-detailed",true);t.checkPref("ws-atmosphere",true);t.checkPref("ws-music",true);
                t.sliderPref("ws-volume",70,0,100,5,i->i+"%");
                for(String key:new String[]{"ws-hud","ws-hud-allies","ws-hud-enemies","ws-hud-details","ws-health-bars","ws-health-text"})t.checkPref(key,true);
                t.checkPref("ws-hud-edit",false);
                t.button("重置敌我面板位置",StormHud::resetPositions).growX().row();
                t.sliderPref("ws-hud-width",360,280,520,20,i->i+"px");
                t.sliderPref("ws-hud-opacity",90,30,100,5,i->i+"%");
            });
            Core.scene.table(table->{hud=table;StormHud.install(table);});
        });
    }
    public static Fi file(String... path){Fi f=Vars.mods.getMod(WitherStormMod.class).root;for(String p:path)f=f.child(p);return f;}
    private static StormUnit focus(){
        if(!Vars.state.isGame())return null;
        Unit best=Groups.unit.find(u->u instanceof StormUnit&&!u.dead);return best instanceof StormUnit s?s:null;
    }
    @Override public void init(){LiftableLegsUnit.install();}
    @Override public void loadContent(){spec=StormSpec.parse(file("blueprints","wither-storm.hjson").readString());StormContent.load();}
}
