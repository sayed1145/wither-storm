package wstorm.world;
import mindustry.*;import mindustry.gen.*;import mindustry.type.*;import mindustry.entities.EntityCollisions.SolidPred;
/** Vanilla LegsUnit state/serialization unchanged; a held, elevated leg unit has no ground collision. */
public final class LiftableLegsUnit extends LegsUnit{
    @Override public SolidPred solidity(){return isFlying()&&StormLift.isHeld(id)?null:super.solidity();}
    public static void install(){
        int nativeId=LegsUnit.create().classId();
        // Do not replace a different mod's native entity remapping.
        if(EntityMapping.idMap[nativeId].get().getClass()!=LegsUnit.class)return;
        EntityMapping.idMap[nativeId]=LiftableLegsUnit::new;
        for(UnitType type:Vars.content.units())if(type.minfo.mod==null&&type.constructor!=null&&type.constructor.get().getClass()==LegsUnit.class)type.constructor=LiftableLegsUnit::new;
        // Keep inherited native classId and generated read/write format: older saves and
        // saves loaded without this mod still deserialize as ordinary vanilla leg units.
    }
}
