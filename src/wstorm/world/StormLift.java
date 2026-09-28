package wstorm.world;
import arc.math.*;import arc.struct.*;import mindustry.*;import mindustry.gen.*;import mindustry.core.World;
/** Per-victim flight leases: no shared UnitType mutation, status slowdown or permanent flight. */
public final class StormLift{
    private static class Lease{Unit unit;int owner;float safeX,safeY;}
    private static final IntMap<Lease> leases=new IntMap<>();
    private static final IntSeq remove=new IntSeq();
    public static boolean isHeld(int id){return leases.containsKey(id);}
    public static void clear(){leases.clear();}
    public static void hold(StormUnit s,Unit u){
        Lease l=leases.get(u.id);if(l==null){l=new Lease();l.unit=u;l.safeX=u.x;l.safeY=u.y;leases.put(u.id,l);}l.owner=s.id;
        u.elevation=1;u.drownTime=0;
    }
    private static boolean held(StormUnit s,Unit u){
        if(s.dead||!s.isAdded()||!StormLogic.enemy(s,u.team))return false;
        if(StormLogic.directHeld(s,u.id))return true;
        if(s.beamCapable(0))for(int i=0;i<3;i++)if(s.beamReady(i)&&s.targetKind[i]==1&&s.targetId[i]==u.id)return true;
        return false;
    }
    public static void move(Unit u,float x,float y){
        u.elevation=1;u.vel.setZero();
        if(u.canPass(World.toTile(x),World.toTile(y))){u.set(x,y);return;}
        // Some native leg entities ignore elevation in solidity(). Safely cross a solid span
        // without ever leaving their real entity inside an instant-kill collision tile.
        float dx=x-u.x,dy=y-u.y,len=Mathf.len(dx,dy);if(len<.0001f)return;dx/=len;dy/=len;
        for(float d=8;d<=256;d+=4){float nx=x+dx*d,ny=y+dy*d;
            if(nx>=0&&ny>=0&&nx<Vars.world.unitWidth()&&ny<Vars.world.unitHeight()&&u.canPass(World.toTile(nx),World.toTile(ny))){u.set(nx,ny);return;}}
    }
    private static void release(Lease l){
        Unit u=l.unit;if(!u.isAdded()||u.dead)return;
        u.elevation=u.type.flying?1:0;u.vel.setZero();
        if(u.type.flying||u.canPassOn()&&u.canLand())return;
        float x=u.x,y=u.y;
        for(int radius=1;radius<=40;radius++)for(int i=0;i<16;i++){
            float nx=x+Mathf.cosDeg(i*22.5f)*radius*8,ny=y+Mathf.sinDeg(i*22.5f)*radius*8;
            if(nx<0||ny<0||nx>=Vars.world.unitWidth()||ny>=Vars.world.unitHeight())continue;
            u.set(nx,ny);if(u.canPassOn()&&u.canLand())return;
        }
        u.set(l.safeX,l.safeY);if(!u.canPassOn()||!u.canLand())u.elevation=1;
    }
    public static void update(){
        if(!Vars.state.isGame())return;
        // Reconstruct leases after world load and on remote clients before local collision updates.
        for(Unit owner:Groups.unit)if(owner instanceof StormUnit s&&!s.dead){
            for(int id:s.directId){Unit u=Groups.unit.getByID(id);if(u!=null&&!u.dead&&StormLogic.enemy(s,u.team))hold(s,u);}
            if(s.beamCapable(0))for(int h=0;h<3;h++)if(s.beamReady(h)&&s.targetKind[h]==1){Unit u=Groups.unit.getByID(s.targetId[h]);if(u!=null&&!u.dead&&StormLogic.enemy(s,u.team))hold(s,u);}
        }
        remove.clear();for(var entry:leases.entries()){
            Lease l=entry.value;Unit owner=Groups.unit.getByID(l.owner);
            if(!l.unit.isAdded()||l.unit.dead||!(owner instanceof StormUnit s)||!held(s,l.unit)){release(l);remove.add(entry.key);}
            else{l.unit.elevation=1;l.unit.drownTime=0;}
        }
        for(int i=0;i<remove.size;i++)leases.remove(remove.get(i));
    }
}
