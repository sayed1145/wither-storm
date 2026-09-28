package wstorm.gfx;
import arc.*;import arc.audio.*;import arc.math.*;import arc.util.*;
import mindustry.*;import mindustry.gen.*;import wstorm.*;import wstorm.world.*;

/** Intact supplied theme + original synthesized rumble. Never loads audio on a server. */
public final class StormAudio{
    public static Music music;public static Sound roar,rupture;
    public static boolean loaded,failed;public static float decodedLength;
    private static float gain,lastRoar=-1000;private static boolean active,paused;
    public static void load(){
        if(Vars.headless||Core.audio==null||!Core.audio.initialized())return;
        try{
            music=Core.audio.newMusic(WitherStormMod.file("music","wither-storm-theme.ogg"));
            music.setLooping(true);music.setVolume(0);decodedLength=music.getLength();loaded=decodedLength>170;failed=!loaded;
            roar=Core.audio.newSound(WitherStormMod.file("sounds","storm-roar.wav"));
            rupture=Core.audio.newSound(WitherStormMod.file("sounds","storm-rupture.wav"));
        }catch(Throwable t){failed=true;Log.warn("[Wither Storm] Audio: @",t.toString());}
    }
    public static void roarAt(float x,float y){if(roar!=null&&Time.time-lastRoar>120){roar.at(x,y,.90f,.80f);lastRoar=Time.time;}}
    public static void ruptureAt(float x,float y){if(rupture!=null)rupture.at(x,y,.84f,.80f);}
    public static void update(){
        if(Vars.headless||music==null)return;
        boolean shouldPause=Vars.state.isGame()&&Vars.state.isPaused();
        if(shouldPause!=paused){music.pause(shouldPause);paused=shouldPause;}
        if(paused){if(active)Vars.control.sound.keepSilent();return;}
        boolean wanted=Vars.state.isGame()&&Core.settings.getBool("ws-music",true)&&Groups.unit.contains(u->u instanceof StormUnit&&!u.dead);
        if(wanted)Vars.control.sound.keepSilent();
        float target=wanted?WitherStormMod.spec.musicGain*Core.settings.getInt("ws-volume",70)/100f*Core.settings.getInt("musicvol",100)/100f:0;
        if(wanted&&!active){Vars.control.sound.stop();music.play();active=true;}
        gain=Mathf.lerpDelta(gain,target,.025f);music.setVolume(gain);
        if(!wanted&&gain<.0015f&&active){music.stop();active=false;}
    }
    public static void stop(){if(music!=null)music.stop();gain=0;active=false;paused=false;lastRoar=-1000;}
}
