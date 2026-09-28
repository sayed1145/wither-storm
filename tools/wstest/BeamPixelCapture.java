package wstest;
import arc.*;import arc.files.*;import arc.graphics.*;import arc.graphics.g2d.*;import arc.scene.*;import arc.scene.ui.*;import arc.util.*;import mindustry.*;import mindustry.game.*;import mindustry.game.EventType.*;import mindustry.gen.*;import wstorm.*;import wstorm.world.*;import wstorm.model.*;import wstorm.gfx.*;import wstorm.g3d.*;
/** Diagnostic green light replaces ONLY beam color; geometry and opaque head are the installed renderer. */
public class BeamPixelCapture{
 static int frame;static StormUnit s;static StormModel m;static Rig beam;static UnitRenderer renderer;static Object fx;static float left,bottom;static String centers="";
 static Object field(Object o,String name)throws Exception{var f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}
 public static void start()throws Exception{
  for(Element e:Core.scene.getElements().copy())if(e instanceof Dialog d)d.hide();
  s=MechanicsChecks.fresh();s.set(1600,1000);s.matter=s.visualMatter=2600;s.bodyYaw=s.rotation=270;s.age=123;
  for(int h=0;h<3;h++)HeadlessChecks.unit(Team.crux,1460+h*140,100);
  Vars.state.teams.updateTeamStats();StormLogic.acquire(s);for(int i=0;i<120;i++)StormAim.update(s,1);
  Vars.state.set(mindustry.core.GameState.State.paused);Vars.disableUI=true;Vars.ui.loadfrag.hide();Vars.renderer.landTime=0;
  m=new StormModel();m.build(2600,81539,8);m.pose(2600,s.age,s.bodyFacing(),0);m.aimHeads(s);
  ControlCapture.output=new Fi(System.getProperty("ws.capture"));ControlCapture.output.mkdirs();
  Events.run(Trigger.postDraw,BeamPixelCapture::draw);
 }
 static void draw(){try{
  Draw.flush();StormVisual.beams(s,m);Draw.flush();
  var mapField=StormVisual.class.getDeclaredField("beams");mapField.setAccessible(true);fx=((arc.struct.IntMap<?>)mapField.get(null)).get(s.id);beam=(Rig)field(fx,"rig");renderer=(UnitRenderer)field(fx,"renderer");
  float[] p=new float[3];float minX=1e9f,maxX=-1e9f,minY=1e9f;float[][] points=new float[3][3];
  int[] bones=(int[])field(m,"stormEyes");double maxPort=0,maxAngle=0;
  for(int h=0;h<3;h++){
   // Independently locate the imported PURPLE BOX, not StormAim.port() or model.eye().
   Rig.Piece purple=null;for(int i=0;i<m.rig.pieceCount;i++){var part=m.rig.pieces[i];if(part.bone==bones[h]&&Math.abs(part.minX+2.4f)<.001f&&Math.abs(part.maxX-2.4f)<.001f&&Math.abs(part.minY-12)<.001f&&Math.abs(part.maxY-16.8f)<.001f)purple=part;}
   if(purple==null)throw new AssertionError("purple geometry not found");m.renderer.point(bones[h],(purple.minX+purple.maxX)/2,purple.maxY,(purple.minZ+purple.maxZ)/2,p);
   var mesh=beam.pieces[h].mesh;if(mesh.verts==0)throw new AssertionError("beam not active");float x=0,y=0,z=0;for(int k=0;k<mesh.verts;k+=4){x+=mesh.vx[k]+mesh.vx[k+1];y+=mesh.vy[k]+mesh.vy[k+1];z+=mesh.vz[k]+mesh.vz[k+1];}float n=mesh.verts/2f;x/=n;y/=n;z/=n;
   maxPort=Math.max(maxPort,Math.sqrt((x-p[0])*(x-p[0])+(y-p[1])*(y-p[1])+(z-p[2])*(z-p[2])));
   m.project(p);points[h]=p.clone();minX=Math.min(minX,p[0]);maxX=Math.max(maxX,p[0]);minY=Math.min(minY,p[1]);
   for(int k=0;k<mesh.faces;k++){mesh.cr[k]=.05f;mesh.cg[k]=1;mesh.cb[k]=.05f;}beam.alpha[h+1]=.95f;
  }
  if(maxPort>.06)throw new AssertionError("mesh port distance "+maxPort);
  left=(minX+maxX)/2-110;bottom=minY-90;Draw.proj(left,bottom,220,137.5f);Draw.z(200);Draw.color(Color.valueOf("151c25"));Fill.rect(left+110,bottom+68.75f,220,137.5f);Draw.reset();Draw.flush();
  if(frame==0)m.renderer.draw(m.rig,0,0);
  else if(frame==1){Draw.z(201);renderer.draw(beam,0,0);Draw.z(202);m.renderer.draw(m.rig,0,0);}
  else{Draw.z(202);m.renderer.draw(m.rig,0,0,renderer,beam);}
  Draw.flush();ControlCapture.shot("pixel-"+frame+".png");
  if(frame==2){StringBuilder b=new StringBuilder("{\"maxGeometryDistance\":"+maxPort+",\"diagnosticGreenOnly\":true,\"points\":[");for(int h=0;h<3;h++){if(h>0)b.append(',');b.append('[').append((points[h][0]-left)/220*1120).append(',').append(700-(points[h][1]-bottom)/137.5f*700).append(']');}b.append("]}");ControlCapture.output.child("pixel-origin.json").writeString(b.toString());Core.app.exit();}frame++;
 }catch(Throwable e){Log.err(e);System.exit(5);}}
}
