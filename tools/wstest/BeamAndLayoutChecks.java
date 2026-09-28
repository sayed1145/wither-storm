package wstest;
import arc.math.*;import arc.math.geom.*;import arc.struct.*;import wstorm.world.*;import wstorm.model.*;import wstorm.gfx.*;import wstorm.g3d.*;
public class BeamAndLayoutChecks{
 public static void run()throws Exception{
  StormUnit s=MechanicsChecks.fresh();StormModel model=new StormModel();Mesh beam=new Mesh();float[] p=new float[3],q=new float[3];int cases=0;boolean geometry=true,projected=true;
  var field=StormModel.class.getDeclaredField("stormEyes");field.setAccessible(true);
  for(float mass:new float[]{650,1200,2600,12000}){
   s.matter=mass;model.ensure(mass,81539,8);
   for(int heading=0;heading<360;heading+=30)for(int pitch:new int[]{-65,-25,0})for(int yaw:new int[]{-35,0,35}){
    s.bodyYaw=s.rotation=heading;s.age=123;for(int h=0;h<3;h++){s.aimYaw[h]=yaw;s.aimPitch[h]=pitch;}model.pose(mass,s.age,heading,0);model.aimHeads(s);int[] bones=(int[])field.get(model);
    for(int h=0;h<3;h++)if(StormAim.stormPort(mass,h)){
     Rig.Piece purple=null;for(int i=0;i<model.rig.pieceCount;i++){Rig.Piece piece=model.rig.pieces[i];if(piece.bone==bones[h]&&Math.abs(piece.minX+2.4f)<.001f&&Math.abs(piece.maxX-2.4f)<.001f&&Math.abs(piece.minY-12)<.001f&&Math.abs(piece.maxY-16.8f)<.001f)purple=piece;}
     if(purple==null)throw new AssertionError("actual purple geometry missing");model.renderer.point(bones[h],0,purple.maxY,0,q);model.eye(h,p);StormVisual.beamMesh(beam,p,600,-900,25,model.scale);
     float x=0,y=0,z=0,ex=0,ey=0,ez=0;for(int i=0;i<beam.verts;i+=4){x+=beam.vx[i]+beam.vx[i+1];y+=beam.vy[i]+beam.vy[i+1];z+=beam.vz[i]+beam.vz[i+1];ex+=beam.vx[i+2]+beam.vx[i+3];ey+=beam.vy[i+2]+beam.vy[i+3];ez+=beam.vz[i+2]+beam.vz[i+3];}float n=beam.verts/2f;x/=n;y/=n;z/=n;ex/=n;ey/=n;ez/=n;
     geometry&=Mathf.dst(x,y,p[0],p[1])<.002f&&Math.abs(z-p[2])<.002f&&Mathf.dst(ex,ey,600,-900)<.003f&&Math.abs(ez)<.003f;
     model.project(q);p[0]=x;p[1]=y;p[2]=z;model.project(p);projected&=Mathf.dst(p[0],p[1],q[0],q[1])<.2f;cases++;
    }
   }
  }
  HeadlessChecks.check("actual-purple-mesh-to-emitted-beam-centroid-and-ground-endpoint-"+cases+"-poses",geometry);
  HeadlessChecks.check("projected-beam-origin-under-point-two-world-units-from-purple-surface",projected);
  for(int[] dim:new int[][]{{640,480},{800,600},{1120,700},{1280,720},{1920,1080}}){Seq<Rect> obstacles=Seq.with(new Rect(0,dim[1]-82,328,82),new Rect(dim[0]-148,dim[1]-148,148,148),new Rect(dim[0]-312,0,312,252));Rect a=new Rect(),b=new Rect();boolean first=HudLayout.place(a,8,dim[1]-230,200,140,dim[0],dim[1],obstacles);if(first)obstacles.add(new Rect(a));boolean second=HudLayout.place(b,8,dim[1]-380,200,32,dim[0],dim[1],obstacles);HeadlessChecks.check("default-hud-rectangle-packing-with-native-panels-"+dim[0]+"x"+dim[1],first&&second&&!a.overlaps(b)&&HudLayout.fits(b,dim[0],dim[1],obstacles));}
  Seq<Rect> full=Seq.with(new Rect(0,0,320,240));HeadlessChecks.check("no-space-reports-unavailable-instead-of-overlaying-native-controls",!HudLayout.place(new Rect(),0,0,200,100,320,240,full));
 }
}
