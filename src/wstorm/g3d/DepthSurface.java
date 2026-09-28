package wstorm.g3d;

import arc.Core;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.gl.*;
import arc.math.Mat;
import arc.struct.Seq;

/** Deferred, pooled GL2-compatible per-pixel depth rendering. No ordering bias for eyes/decals.
 * The depth-only pass also prevents fading surfaces from exposing their own rear details.
 * One shared framebuffer; each deferred packet owns its data until the render-layer callback runs. */
public final class DepthSurface{
    private static FrameBuffer buffer;
    private static Shader shader;
    private static arc.graphics.Mesh mesh;
    private static int capacity;
    private static final Seq<Packet> free=new Seq<>();
    private static final Blending premultiplied=new Blending(Gl.one,Gl.oneMinusSrcAlpha);
    public static long passes,faces;
    public static Packet obtain(){Packet p=free.isEmpty()?new Packet():free.pop();p.size=0;p.groups=0;p.opaqueGroups=-1;p.transparentVertex=-1;p.minX=p.minY=Float.MAX_VALUE;p.maxX=p.maxY=-Float.MAX_VALUE;p.projection.set(Draw.proj());return p;}
    public static class Packet{
        float[] vertices=new float[8192];int size,groups,opaqueGroups=-1,transparentVertex=-1;
        public void beginTransparent(){opaqueGroups=groups;transparentVertex=size;}
        float minX,minY,maxX,maxY;final TextureRegion composite=new TextureRegion();
        Texture[] textures=new Texture[32];int[] starts=new int[32],counts=new int[32];
        final Mat projection=new Mat();
        void vertex(float x,float y,float z,float color,float u,float v){
            minX=Math.min(minX,x);minY=Math.min(minY,y);maxX=Math.max(maxX,x);maxY=Math.max(maxY,y);
            if(size+6>vertices.length)vertices=java.util.Arrays.copyOf(vertices,vertices.length*2);
            vertices[size++]=x;vertices[size++]=y;vertices[size++]=z;vertices[size++]=color;vertices[size++]=u;vertices[size++]=v;
        }
        public void quad(int material,int face,float ax,float ay,float az,float ac,float bx,float by,float bz,float bc,float cx,float cy,float cz,float cc,float dx,float dy,float dz,float dc){
            TextureRegion r=wstorm.gfx.StormTextures.region(material,face);if(r==null)r=Core.atlas.white();
            if(groups==0||textures[groups-1]!=r.texture||size==transparentVertex){
                if(groups==textures.length){textures=java.util.Arrays.copyOf(textures,groups*2);starts=java.util.Arrays.copyOf(starts,groups*2);counts=java.util.Arrays.copyOf(counts,groups*2);}
                textures[groups]=r.texture;starts[groups]=size/6;counts[groups++]=0;
            }
            counts[groups-1]+=6;
            vertex(ax,ay,az,ac,r.u,r.v2);vertex(bx,by,bz,bc,r.u2,r.v2);vertex(cx,cy,cz,cc,r.u2,r.v);
            vertex(cx,cy,cz,cc,r.u2,r.v);vertex(dx,dy,dz,dc,r.u,r.v);vertex(ax,ay,az,ac,r.u,r.v2);
            faces++;
        }
        public void submit(){if(size==0){free.add(this);return;}Draw.draw(Draw.z(),this::render);}
        private void geometry(int from,int to){for(int i=from;i<to;i++){textures[i].bind(0);mesh.render(shader,Gl.triangles,starts[i],counts[i]);}}
        private void render(){
            Draw.flush();Mat previous=new Mat(Draw.proj());Blending oldBlend=Draw.getBlend();Shader oldShader=Draw.getShader();
            float color=Draw.getColorPacked(),mix=Draw.getMixColorPacked();boolean begun=false;
            try{
                int width=Core.graphics.getWidth(),height=Core.graphics.getHeight();
                if(buffer==null)buffer=new FrameBuffer(width,height,true);else buffer.resize(width,height);
                if(shader==null)shader=new Shader(
                    "attribute vec3 a_position; attribute vec4 a_color; attribute vec2 a_texCoord0; uniform mat3 u_proj; varying vec4 v_color; varying vec2 v_uv; void main(){vec3 p=u_proj*vec3(a_position.xy,1.0);gl_Position=vec4(p.xy,a_position.z,1.0);v_color=a_color;v_color.a*=255.0/254.0;v_uv=a_texCoord0;}",
                    "varying lowp vec4 v_color; varying vec2 v_uv; uniform sampler2D u_texture; void main(){vec4 c=texture2D(u_texture,v_uv)*v_color;if(c.a<0.004)discard;gl_FragColor=c;}");
                if(mesh==null||capacity<size/6){if(mesh!=null)mesh.dispose();capacity=Integer.highestOneBit(size/6)*2;mesh=new arc.graphics.Mesh(false,capacity,0,VertexAttribute.position3,VertexAttribute.color,VertexAttribute.texCoords);}
                mesh.setVertices(vertices,0,size);buffer.begin();begun=true;
                Gl.colorMask(true,true,true,true);Gl.depthMask(true);Gl.clearColor(0,0,0,0);Gl.clearDepthf(1);Gl.clear(Gl.colorBufferBit|Gl.depthBufferBit);
                Gl.enable(Gl.depthTest);Gl.depthFunc(Gl.lequal);Gl.disable(Gl.cullFace);shader.bind();shader.setUniformMatrix("u_proj",projection);shader.setUniformi("u_texture",0);
                int opaque=opaqueGroups<0?groups:opaqueGroups;
                Gl.disable(Gl.blend);Gl.colorMask(false,false,false,false);geometry(0,opaque);
                Gl.colorMask(true,true,true,true);Gl.depthMask(false);Gl.enable(Gl.blend);Gl.blendFuncSeparate(Gl.srcAlpha,Gl.oneMinusSrcAlpha,Gl.one,Gl.oneMinusSrcAlpha);geometry(0,opaque);
                // Transparent beams test the SAME body depth; never write depth or cut holes in the shell.
                Gl.blendFuncSeparate(Gl.srcAlpha,Gl.one,Gl.one,Gl.oneMinusSrcAlpha);geometry(opaque,groups);
                Gl.disable(Gl.depthTest);buffer.end();begun=false;
                Draw.proj(projection);Draw.shader();Draw.color();Draw.mixcol();Draw.blend(premultiplied);
                float w=2/projection.val[Mat.M00],h=2/projection.val[Mat.M11];
                // Composite only this model's screen rectangle, not a screen-sized transparent quad per unit.
                float u0=arc.math.Mathf.clamp((projection.val[Mat.M00]*minX+projection.val[Mat.M02]+1)/2-2f/width);
                float u1=arc.math.Mathf.clamp((projection.val[Mat.M00]*maxX+projection.val[Mat.M02]+1)/2+2f/width);
                float v0=arc.math.Mathf.clamp((projection.val[Mat.M11]*minY+projection.val[Mat.M12]+1)/2-2f/height);
                float v1=arc.math.Mathf.clamp((projection.val[Mat.M11]*maxY+projection.val[Mat.M12]+1)/2+2f/height);
                composite.set(buffer.getTexture());composite.u=u0;composite.u2=u1;composite.v=v1;composite.v2=v0;
                if(u1>u0&&v1>v0)Draw.rect(composite,(u0+u1-1-projection.val[Mat.M02])*w/2,(v0+v1-1-projection.val[Mat.M12])*h/2,(u1-u0)*w,(v1-v0)*h);
                Draw.flush();passes++;
            }finally{
                Gl.colorMask(true,true,true,true);Gl.depthMask(false);Gl.disable(Gl.depthTest);if(begun)buffer.end();
                Draw.proj(previous);Draw.shader(oldShader);Draw.blend(oldBlend);oldBlend.apply();Draw.color(color);Draw.mixcol(mix);
                free.add(this);
            }
        }
    }
}
