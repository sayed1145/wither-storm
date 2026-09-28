package wstest;
import arc.*;
import arc.util.*;
import mindustry.*;
import mindustry.game.EventType.*;
import mindustry.server.*;
import java.net.*;
import java.nio.file.*;

/** Test code is deliberately outside the production mod and loaded through its actual loader. */
public class ServerBootstrap{
    public static void main(String[] args){
        Events.on(ServerLoadEvent.class,e->Core.app.post(()->{
            try{
                var mod=Vars.mods.getMod("wither-storm");
                if(mod==null||mod.main==null)throw new AssertionError("Installable JAR was not loaded");
                URL url=Path.of(System.getProperty("ws.tools")).toUri().toURL();
                ClassLoader loader=new URLClassLoader(new URL[]{url},mod.main.getClass().getClassLoader()){
                    @Override protected Class<?> loadClass(String n,boolean resolve)throws ClassNotFoundException{
                        if(n.startsWith("wstest.")){Class<?> c=findLoadedClass(n);if(c==null)c=findClass(n);if(resolve)resolveClass(c);return c;}
                        return super.loadClass(n,resolve);
                    }
                };
                Class.forName("wstest.HeadlessChecks",true,loader).getMethod("run").invoke(null);
                System.exit(0);
            }catch(Throwable t){Log.err(t);System.exit(2);}
        }));
        ServerLauncher.main(new String[0]);
    }
}
