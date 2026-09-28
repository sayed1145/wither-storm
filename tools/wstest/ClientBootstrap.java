package wstest;
import arc.*;
import arc.backend.sdl.*;
import arc.util.*;
import mindustry.*;
import mindustry.core.*;
import mindustry.desktop.*;
import mindustry.game.EventType.*;
import java.net.*;
import java.nio.file.*;

public class ClientBootstrap{
    public static void main(String[] args){
        System.setProperty("nodiscord","true");
        Events.on(ClientCreateEvent.class,e->{Core.settings.put("locale","zh_CN");Core.settings.put("fpscap",0);Core.settings.put("vsync",false);Core.settings.put("bloom",true);Core.settings.put("pixelate",false);Core.settings.put("lasersopacity",45);});
        Events.on(ClientLoadEvent.class,e->Core.app.post(()->{
            try{
                var mod=Vars.mods.getMod("wither-storm");if(mod==null||mod.main==null)throw new AssertionError("JAR did not load");
                URL url=Path.of(System.getProperty("ws.tools")).toUri().toURL();
                ClassLoader loader=new URLClassLoader(new URL[]{url},mod.main.getClass().getClassLoader()){
                    @Override protected Class<?> loadClass(String n,boolean resolve)throws ClassNotFoundException{
                        if(n.startsWith("wstest.")){Class<?> c=findLoadedClass(n);if(c==null)c=findClass(n);if(resolve)resolveClass(c);return c;}
                        return super.loadClass(n,resolve);
                    }
                };
                Class.forName(Boolean.getBoolean("ws.layout")?"wstest.LayoutCapture":Boolean.getBoolean("ws.pixel")?"wstest.BeamPixelCapture":Boolean.getBoolean("ws.aim")?"wstest.AimCapture":Boolean.getBoolean("ws.control")?"wstest.ControlCapture":Boolean.getBoolean("ws.orbit")?"wstest.OrbitCapture":Boolean.getBoolean("ws.fracture")?"wstest.FractureCapture":Boolean.getBoolean("ws.animation")?"wstest.AnimationCapture":Boolean.getBoolean("ws.review")?"wstest.ReviewCapture":Boolean.getBoolean("ws.sheet")?"wstest.ModelSheet":"wstest.ClientCapture",true,loader).getMethod("start").invoke(null);
            }catch(Throwable t){Log.err(t);System.exit(2);}
        }));
        Version.init();
        SdlConfig cfg=new SdlConfig();cfg.title="Wither Storm — v160.5 validation";cfg.width=1120;cfg.height=700;cfg.maximized=false;cfg.disableAudio=!Boolean.getBoolean("ws.audio");cfg.vSyncEnabled=false;cfg.coreProfile=true;
        cfg.glVersions=new int[][]{{3,3},{3,2},{2,1}};
        if(Boolean.getBoolean("ws.gl2")){cfg.coreProfile=false;cfg.allowGl30=false;cfg.glVersions=new int[][]{{2,1}};}
        new SdlApplication(new DesktopLauncher(new String[0]),cfg);
    }
}
