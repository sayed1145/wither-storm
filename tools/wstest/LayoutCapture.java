package wstest;
import arc.*;import arc.files.*;import arc.graphics.*;import arc.math.geom.*;import arc.scene.*;import arc.scene.event.*;import arc.scene.ui.*;import arc.struct.*;import mindustry.*;import mindustry.content.*;import mindustry.game.*;import mindustry.game.EventType.*;import wstorm.*;import wstorm.world.*;import wstorm.gfx.*;
public class LayoutCapture{
 static int frame;static StringBuilder rows=new StringBuilder();static float oldX,oldY;static int savedX,savedY;static boolean drag,saved,clamped;static StormUnit s;
 static void event(InputEvent.InputEventType type,float x,float y){InputEvent e=new InputEvent();e.type=type;e.stageX=x;e.stageY=y;e.pointer=0;e.keyCode=arc.input.KeyCode.mouseLeft;StormHud.panels[0].fire(e);}
 public static void start(){
  for(Element e:Core.scene.getElements().copy())if(e instanceof Dialog d)d.hide();
  s=MechanicsChecks.fresh();s.matter=s.visualMatter=2600;s.bodyYaw=s.rotation=270;s.set(800,600);
  Vars.world.tile(10,10).setBlock(Blocks.coreShard,Team.sharded);Vars.player.team(Team.sharded);Vars.player.set(80,80);Vars.player.add();Vars.state.teams.updateTeamStats();Vars.disableUI=false;Vars.ui.loadfrag.hide();Vars.renderer.landTime=0;
  Core.settings.put("ws-hud",true);Core.settings.put("ws-hud-allies",true);Core.settings.put("ws-hud-enemies",true);Core.settings.put("ws-hud-width",360);Core.settings.put("ws-hud-edit",false);StormHud.resetPositions();
  ControlCapture.output=new Fi(System.getProperty("ws.capture"));ControlCapture.output.mkdirs();
  Events.run(Trigger.preDraw,()->{Core.camera.position.set(800,750);Core.camera.width=1500;Core.camera.height=1500f*Core.graphics.getHeight()/Core.graphics.getWidth();Core.camera.update();});
  Core.app.addListener(new ApplicationListener(){@Override public void update(){tick();}});
 }
 static void shot(String name){if(ControlCapture.pixels!=null){ControlCapture.pixels.dispose();ControlCapture.writer.dispose();ControlCapture.pixels=null;}ControlCapture.shot(name);}
 @SuppressWarnings("unchecked") static void verify(){try{
  StormHud.layoutNow();var f=StormHud.class.getDeclaredField("obstacles");f.setAccessible(true);Seq<Rect> obstacles=(Seq<Rect>)f.get(null);int visible=0;
  for(var box:StormHud.panels)if(box.visible){visible++;Rect r=new Rect(box.x,box.y,box.getWidth(),box.getHeight());if(r.x<7||r.y<7||r.x+r.width>Core.scene.getWidth()-7||r.y+r.height>Core.scene.getHeight()-7)throw new AssertionError("outside viewport");for(Rect o:obstacles)if(!r.equals(o)&&r.overlaps(o))throw new AssertionError("HUD overlap: "+r+" / "+o);}
  if(visible!=2)throw new AssertionError("default panels unavailable");if(rows.length()>0)rows.append(',');rows.append("{\"width\":").append(Core.graphics.getWidth()).append(",\"height\":").append(Core.graphics.getHeight()).append(",\"nativeAndPanelRects\":").append(obstacles.size).append(",\"visiblePanels\":").append(visible).append(",\"overlaps\":0}");
 }catch(Exception e){throw new RuntimeException(e);}}
 static void tick(){try{
  if(frame==0)Core.graphics.setWindowSize(1120,700);if(frame==20)Core.graphics.setWindowSize(800,600);if(frame==40)Core.graphics.setWindowSize(640,480);if(frame==60)Core.graphics.setWindowSize(1280,720);
  if(frame==12||frame==32||frame==52||frame==72){verify();shot("layout-"+Core.graphics.getWidth()+".png");}
  if(frame==80){Core.settings.put("ws-hud-edit",true);oldX=StormHud.panels[0].x;oldY=StormHud.panels[0].y;}
  if(frame==82){var b=StormHud.panels[0];float x=b.x+20,y=b.y+b.getHeight()-15;event(InputEvent.InputEventType.touchDown,x,y);event(InputEvent.InputEventType.touchDragged,x+170,y-110);event(InputEvent.InputEventType.touchUp,x+170,y-110);}
  if(frame==85){drag=Core.settings.getInt("ws-panel-0-x",-1)>=0&&Math.abs(StormHud.panels[0].x-oldX)+Math.abs(StormHud.panels[0].y-oldY)>20;savedX=Core.settings.getInt("ws-panel-0-x");savedY=Core.settings.getInt("ws-panel-0-y");Core.settings.manualSave();verify();shot("layout-dragged.png");}
  if(frame==86){Core.settings.remove("ws-panel-0-x");Core.settings.remove("ws-panel-0-y");Core.settings.loadValues();saved=Core.settings.getInt("ws-panel-0-x",-1)==savedX&&Core.settings.getInt("ws-panel-0-y",-1)==savedY;Core.settings.put("ws-panel-0-x",20000);Core.settings.put("ws-panel-0-y",-1000);}
  if(frame==89){verify();clamped=true;StormHud.resetPositions();Core.settings.put("ws-hud-edit",false);}
  if(frame==94){verify();if(!drag||!saved||!clamped||WitherStormMod.hud.touchable!=Touchable.disabled)throw new AssertionError("drag/persistence/normal input failure "+drag+saved+clamped);ControlCapture.output.child("layout-result.json").writeString("{\"actualSceneDragEvents\":true,\"positionsReloadedFromSettingsFile\":true,\"offscreenPositionClamped\":true,\"normalModeDoesNotInterceptInput\":true,\"viewports\":["+rows+"]}");Core.app.exit();}
  frame++;
 }catch(Throwable e){arc.util.Log.err(e);System.exit(5);}}
}
