package wstorm.world;
import arc.graphics.Color;import arc.graphics.g2d.*;import arc.struct.*;import arc.util.io.*;
import mindustry.*;import mindustry.content.*;import mindustry.gen.*;import mindustry.game.Team;import mindustry.type.*;import mindustry.world.blocks.logic.SwitchBlock;
/** One-click team-scoped totem. Native tile configuration enforces multiplayer permissions. */
public class StormCommand extends SwitchBlock{
    private static final Seq<CommandBuild> switches=new Seq<>();
    public static void clear(){switches.clear();}
    public static boolean enabled(Team team){for(CommandBuild b:switches)if(b.isValid()&&b.team==team&&b.enabled)return true;return false;}
    public static void setMode(Team team,boolean value){
        for(CommandBuild b:switches)if(b.isValid()&&b.team==team)b.enabled=value;
        for(Unit u:Groups.unit)if(u instanceof StormUnit s&&u.team==team&&!u.isPlayer()){
            // The production controller is always native CommandAI-compatible; no player controller is touched.
            if(s.controller() instanceof StormAI ai){ai.clearCommands();s.vel.setZero();}
        }
    }
    public StormCommand(String name){
        super(name);size=2;health=720;solid=true;alwaysUnlocked=true;saveConfig=false;
        localizedName="风暴统御图腾";description="点击切换同阵营风暴的指挥待命/自主托管。开启后可正常框选指挥；不干涉已附身玩家。多个图腾保持同阵营状态一致。";
        requirements(Category.logic,ItemStack.with(Items.silicon,70,Items.thorium,45));
        buildType=CommandBuild::new;
        config(Boolean.class,(CommandBuild b,Boolean on)->{b.enabled=on;setMode(b.team,on);});
    }
    public class CommandBuild extends SwitchBuild{
        @Override public void created(){super.created();enabled=StormCommand.enabled(team);if(!switches.contains(this,true))switches.add(this);}
        @Override public void read(Reads r,byte revision){super.read(r,revision);if(!switches.contains(this,true))switches.add(this);}
        @Override public void onRemoved(){switches.remove(this,true);super.onRemoved();}
        @Override public void draw(){
            Draw.color(Color.valueOf("21192b"));Fill.square(x,y,8);Draw.color(Color.valueOf("39313f"));Fill.rect(x,y-4,5,10);
            Draw.color(Color.valueOf("15131b"));Fill.rect(x,y+1,14,3);
            for(int i=-1;i<=1;i++){float xx=x+i*5,yy=y+4+(i==0?1:0);Draw.color(Color.valueOf("302937"));Fill.rect(xx,yy,4.5f,4.5f);Draw.color(enabled?Color.valueOf("bc7cff"):Color.valueOf("66606e"));Fill.rect(xx,yy+.3f,2.7f,.7f);}
            Draw.color(enabled?Color.valueOf("b67ff5"):Color.valueOf("655b70"));Lines.stroke(.7f);Lines.square(x,y,7);Draw.reset();
        }
    }
}
