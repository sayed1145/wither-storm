package wstorm.world;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import wstorm.*;
import wstorm.model.*;
import wstorm.gfx.*;

public final class StormType extends UnitType{
    public final IntMap<StormModel> models=new IntMap<>();
    public static long facesDrawn,renderedFrames;
    private float prune;
    public StormType(String name){
        super(name);alwaysUnlocked=true;constructor=StormUnit::new;aiController=StormAI::new;controller=u->new StormAI();
        flying=true;lowAltitude=false;speed=1.25f;accel=.12f;drag=.07f;rotateSpeed=2f;
        health=15000;armor=12;hitSize=52;range=420;itemCapacity=0;engineSize=0;
        drawBody=false;drawCell=false;drawSoftShadow=false;drawShields=false;drawItems=false;outlines=false;
        targetAir=true;targetGround=true;isEnemy=true;playerControllable=true;logicControllable=false;
        allowedInPayloads=false;useUnitCap=false;clipSize=10000;
        fallEffect=Fx.none;fallEngineEffect=Fx.none;deathExplosionEffect=Fx.none;
    }
    @Override public void update(Unit unit){super.update(unit);StormLogic.update((StormUnit)unit);}
    @Override public void killed(Unit unit){super.killed(unit);StormLogic.died((StormUnit)unit);}
    @Override public void draw(Unit unit){
        if(!(unit instanceof StormUnit s)||s.dead)return;
        if(Vars.player!=null&&unit.inFogTo(Vars.player.team()))return;
        float savedZ=Draw.z();
        StormTextures.load();StormModel model=models.get(unit.id);
        if(model==null){model=new StormModel();models.put(unit.id,model);s.visualMatter=s.matter;StormAudio.roarAt(s.x,s.y);}
        s.visualMatter=arc.math.Mathf.lerpDelta(s.visualMatter,s.matter,.035f);
        int detail=Core.settings.getBool("ws-detailed",true)?WitherStormMod.spec.voxelDetail:5;
        model.ensure(s.visualMatter,s.genome,detail);model.pose(s.visualMatter,s.age,s.bodyFacing(),s.reassembly);model.aimHeads(s);
        Draw.z(Layer.flyingUnitLow-5);
        StormVisual.ground(s,model);
        Draw.z(Layer.flyingUnitLow-2);
        StormVisual.beams(s,model);
        Draw.z(Layer.flyingUnitLow);
        Draw.color();long before=model.renderer.quads;
        StormVisual.drawBody(s,model);
        facesDrawn+=model.renderer.quads-before;renderedFrames++;
        Draw.z(Layer.flyingUnitLow+.03f);
        StormVisual.front(s,model);
        StormHud.drawHealth(s,model);
        Draw.reset();Draw.z(savedZ);
        if((prune+=Time.delta)>360){prune=0;IntSeq deadIds=new IntSeq();for(var e:models.entries())if(Groups.unit.getByID(e.key)==null)deadIds.add(e.key);for(int i=0;i<deadIds.size;i++){int id=deadIds.get(i);models.remove(id);StormVisual.forget(id);}}
    }
    public void clear(){models.clear();}
}
