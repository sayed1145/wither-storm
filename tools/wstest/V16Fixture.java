package wstest;
import arc.*;import arc.files.*;import arc.struct.*;import mindustry.*;import mindustry.content.*;import mindustry.game.*;import mindustry.world.*;import mindustry.io.*;import wstorm.*;import wstorm.world.*;
/** Generator deliberately run ONLY against the public historical 1.6.0 JAR. */
public class V16Fixture{
 public static void run(){
  if(!Vars.mods.getMod("wither-storm").meta.version.equals("1.6.0"))throw new AssertionError("Historical JAR required");
  Vars.logic.reset();Vars.state.rules=new Rules();Vars.state.rules.canGameOver=false;Vars.state.rules.waves=false;
  Vars.state.map=new mindustry.maps.Map(StringMap.of("name","Historical v1.6 pending flights"));
  Vars.world.loadGenerator(160,120,ts->{for(int x=0;x<ts.width;x++)for(int y=0;y<ts.height;y++)ts.set(x,y,new Tile(x,y,Blocks.stone,Blocks.air,Blocks.air));});
  Vars.state.set(mindustry.core.GameState.State.playing);
  StormUnit s=StormLogic.spawn(Team.sharded,640,400);s.genome=616717;s.matter=6000;s.age=80;s.rotation=120;s.eatenUnits=19;s.eatenBuildings=23;s.tentacleStrike[0]=75;s.tentacleHits=4;
  for(int i=0;i<5;i++){Vars.world.tile(90+i*2,50).setBlock(Blocks.titaniumWall,Team.crux);StormLogic.beginBuilding(s,Vars.world.build(90+i*2,50));}
  SaveIO.write(new Fi(System.getProperty("ws.fixture")));arc.util.Log.info("Actual v1.6 historical fixture written");
 }
}
