package wstorm;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.type.*;
import wstorm.world.*;
import wstorm.gfx.*;
import wstorm.g3d.*;
import wstorm.g3d.Mesh;

public final class StormContent{
    public static mindustry.world.blocks.defense.turrets.ItemTurret coreBreaker;
    public static StormCommand commandTotem;public static StormType storm;public static StormSummoner summoner;public static StatusEffect withering;public static BulletType skull;
    private static Rig skullRig;private static UnitRenderer skullRenderer;
    public static void load(){
        withering=new StatusEffect("wither-sickness");withering.color=StormFx.purple;withering.speedMultiplier=1f;withering.reloadMultiplier=1f;
        withering.damage=.028f;withering.effect=StormFx.sickness;withering.effectChance=.025f;
        skull=new BasicBulletType(3.5f,60){
            {lifetime=130;width=8;height=8;collidesAir=true;collidesGround=true;hitEffect=despawnEffect=StormFx.skullBurst;shootEffect=smokeEffect=Fx.none;
             trailColor=StormFx.purple;trailLength=0;splashDamage=60;splashDamageRadius=24;status=withering;statusDuration=180;}
            @Override public void draw(Bullet b){
                StormTextures.load();if(skullRig==null){
                    skullRig=new Rig();skullRig.bone(-1,0,0,0);skullRig.half=10;
                    int jaw=skullRig.bone(0,0,0,0);wstorm.model.StormModel.skull(skullRig,0,jaw);
                    skullRig.finish();skullRenderer=new UnitRenderer(12);
                }
                skullRig.reset();skullRig.rot(0,1,Mathf.sin(b.time/4)*8);skullRenderer.pose(skullRig,b.rotation(),0,0,4,.32f);
                skullRenderer.draw(skullRig,b.x,b.y);
                Draw.blend(Blending.additive);Draw.color(StormFx.purple,.30f);
                for(int i=1;i<=4;i++)Fill.square(b.x-Mathf.cosDeg(b.rotation())*i*4,b.y-Mathf.sinDeg(b.rotation())*i*4,2.7f-i*.45f,45+b.time*2);
                Draw.blend();Draw.reset();
            }
        };
        storm=new StormType("wither-storm");storm.localizedName="凋零风暴";
        storm.description="吞噬敌方单位和建筑，将真实物质量转化为连续生长的体素躯壳、触须和再生能力。初始三首普通凋零逐步异变为三首独眼风暴；包裹核心须以破核炮破防。";
        coreBreaker=new mindustry.world.blocks.defense.turrets.ItemTurret("core-breaker");
        coreBreaker.localizedName="命令破核炮";
        coreBreaker.description="相织布弹药。命中已包裹风暴先打开4秒缺口，后续专用穿透弹进入内部损伤命令核心；普通炮火不能穿透核心装甲。";
        coreBreaker.size=3;coreBreaker.health=1800;coreBreaker.range=600;coreBreaker.reload=85;coreBreaker.rotateSpeed=4;
        coreBreaker.alwaysUnlocked=true;coreBreaker.requirements(mindustry.type.Category.turret,ItemStack.with(Items.silicon,260,Items.thorium,220,Items.phaseFabric,90,Items.surgeAlloy,100));
        coreBreaker.ammo(Items.phaseFabric,new BasicBulletType(8,160){
            {lifetime=90;ammoMultiplier=1;width=9;height=20;hitEffect=StormFx.skullBurst;despawnEffect=StormFx.skullBurst;frontColor=StormFx.pale;backColor=StormFx.purple;collidesAir=true;}
            @Override public void hitEntity(Bullet b,Hitboxc entity,float health){
                if(entity instanceof StormUnit s && !Vars.net.client())s.pierceCore(420);
                super.hitEntity(b,entity,health);
            }
        });
        commandTotem=new StormCommand("storm-command");
        summoner=new StormSummoner("command-altar");summoner.localizedName="命令核心祭坛";
        summoner.description="需要120钍、40巨浪合金、20相织布与1080电力/秒，充能10秒召唤同阵营凋零风暴。沙盒可额外选择敌对体。";
    }
}
