package wstorm.world;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.scene.ui.layout.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.meta.*;
import wstorm.*;
import wstorm.gfx.*;

/** Paid, powered summoning, with persisted charge and explicit sandbox hostile option. */
public final class StormSummoner extends Block{
    public StormSummoner(String name){
        super(name);alwaysUnlocked=true;size=4;health=2600;update=true;solid=true;configurable=true;saveConfig=false;
        clipSize=130;hasPower=true;hasItems=true;itemCapacity=220;buildVisibility=BuildVisibility.shown;
        requirements(Category.units,ItemStack.with(Items.copper,650,Items.silicon,420,Items.thorium,260,Items.surgeAlloy,120,Items.phaseFabric,80));
        consumePowerCond(WitherStormMod.spec.powerPerTick,b->((SummonerBuild)b).queued);
        consumeItems(ItemStack.with(Items.thorium,120,Items.surgeAlloy,40,Items.phaseFabric,20));
        buildType=SummonerBuild::new;
        config(Integer.class,(SummonerBuild b,Integer value)->{
            if(value==0){b.queued=false;b.charge=0;return;}
            if(value==1||value==2&&Vars.state.rules.infiniteResources){b.hostile=value==2;b.queued=true;}
        });
    }
    @Override public void setBars(){
        super.setBars();addBar("storm-charge",(SummonerBuild b)->new Bar("召唤充能",StormFx.purple,()->b.charge/WitherStormMod.spec.summonTicks));
    }
    public class SummonerBuild extends Building{
        public boolean queued,hostile;public float charge,spin;
        @Override public void updateTile(){
            if(Vars.net.client())return;
            if(queued&&efficiency>0&&StormLogic.active()<WitherStormMod.spec.maxActive){
                charge+=edelta();
                if(charge>=WitherStormMod.spec.summonTicks){
                    Team target=hostile&&Vars.state.rules.infiniteResources?Team.crux:team;
                    if(StormLogic.spawn(target,x,y+38)!=null){consume();queued=false;charge=0;}
                }
            }
        }
        @Override public void draw(){
            float z=Draw.z();
            try{Draw.z(Layer.block);wstorm.model.AltarModel.draw(x,y,arc.util.Time.time,charge/WitherStormMod.spec.summonTicks);}
            finally{Draw.blend();Draw.reset();Draw.z(z);}

        }
        @Override public void buildConfiguration(Table table){
            table.clearChildren();
            table.button(Icon.play,Styles.clearTogglei,()->configure(1)).size(54);
            table.button(Icon.cancel,Styles.clearTogglei,()->configure(0)).size(54);
            if(Vars.state.rules.infiniteResources)table.button(Icon.warning,Styles.clearTogglei,()->configure(2)).size(54);
            table.row();table.add("召唤 / 取消 / 沙盒敌对\n120钍 + 40巨浪合金 + 20相织布\n1080电力/秒 · 10秒").colspan(Vars.state.rules.infiniteResources?3:2).width(240).wrap().padTop(8);
        }
        @Override public byte version(){return 1;}
        @Override public void write(Writes w){super.write(w);w.bool(queued);w.bool(hostile);w.f(charge);}
        @Override public void read(Reads r,byte revision){super.read(r,revision);queued=r.bool();hostile=r.bool();charge=r.f();if(!Float.isFinite(charge))charge=0;charge=Math.max(0,Math.min(charge,WitherStormMod.spec.summonTicks));}
    }
}
