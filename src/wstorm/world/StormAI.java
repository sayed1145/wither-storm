package wstorm.world;
import arc.math.*;import arc.math.geom.*;import arc.util.Time;
import mindustry.*;import mindustry.entities.*;import mindustry.ai.types.CommandAI;import mindustry.gen.*;
/** Native command controller with an independent autonomous fallback. Never replaces a Player. */
public class StormAI extends CommandAI{
    private final Vec2 drive=new Vec2();private Teamc prey;private float timer;
    @Override public boolean hasCommand(){return targetPos!=null||attackTarget!=null;}
    public Teamc currentTarget(){return prey;}
    private boolean alive(Teamc t){return t instanceof Unit u?u.isAdded()&&!u.dead:t instanceof Building b&&b.isValid();}
    private void move(StormUnit s,float x,float y,float speed,float stop){
        float distance=s.dst(x,y);s.rotation=Angles.moveToward(s.rotation,s.angleTo(x,y),1.8f*Time.delta);
        if(distance<=stop){s.vel.setZero();return;}
        drive.set(x-s.x,y-s.y).setLength(Math.min(speed,(distance-stop)/Math.max(.01f,Time.delta)));s.vel.set(drive);
    }
    @Override public void updateUnit(){
        if(Vars.net.client()||!(unit instanceof StormUnit s)||s.isPlayer())return;
        if(StormCommand.enabled(s.team)){
            if(attackTarget!=null&&!alive(attackTarget)){attackTarget=null;targetPos=null;}
            if(attackTarget!=null){move(s,attackTarget.x(),attackTarget.y(),s.travelSpeed()*(s.within(attackTarget,s.stopReach(attackTarget))?.5f:1),12);return;}
            if(targetPos!=null){
                move(s,targetPos.x,targetPos.y,s.travelSpeed(),4);
                if(s.within(targetPos,4.1f)){targetPos=null;if(commandQueue.size>0){Position next=commandQueue.remove(0);if(next instanceof Teamc t)commandTarget(t);else commandPosition(new Vec2(next.getX(),next.getY()));}}
            }else s.vel.setZero();
            return;
        }
        if(prey!=null&&!alive(prey))timer=0;
        if((timer-=Time.delta)<=0){
            prey=null;float nearest=Float.MAX_VALUE;
            // An eligible enemy unit anywhere takes priority over every building.
            for(Unit other:Groups.unit){if(other.dead||!other.isAdded()||!StormLogic.enemy(s,other.team))continue;float d=s.dst2(other);if(d<nearest){nearest=d;prey=other;}}
            if(prey==null){final float[] best={Float.MAX_VALUE};float radius=Math.max(Vars.world.width(),Vars.world.height())*Vars.tilesize*2f;
                Units.nearbyBuildings(s.x,s.y,radius,b->{if(b.isValid()&&StormLogic.enemy(s,b.team)&&s.dst2(b)<best[0]){best[0]=s.dst2(b);prey=b;}});}
            timer=12;
        }
        if(prey==null){
            int nearest=-1;float distance=Float.MAX_VALUE;
            for(int i=0;i<StormUnit.maxBuildingSlots;i++)if(s.flightTime(i)>0){float d=s.dst2(s.flightOriginX(i),s.flightOriginY(i));if(d<distance){distance=d;nearest=i;}}
            if(nearest>=0)move(s,s.flightOriginX(nearest),s.flightOriginY(nearest),s.travelSpeed()*.5f,12);else s.vel.setZero();return;
        }
        move(s,prey.x(),prey.y(),s.travelSpeed()*(s.within(prey,s.stopReach(prey)+StormLogic.rangeEpsilon)?.5f:1f),12);
    }
}
