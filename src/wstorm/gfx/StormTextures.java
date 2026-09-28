package wstorm.gfx;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.files.Fi;

/** Original, deterministic 16x16 texels. Every monster surface remains real 3D geometry. */
public final class StormTextures{
    public static final int obsidian=32, skull=40, tooth=44, command=46, rib=48,orbitBlack=56,orbitViolet=57;
    private static Texture texture,commandTexture;
    private static TextureRegion commandRegion;
    private static final TextureRegion[] regions=new TextureRegion[32];
    public static long texturedFaces;
    private static int hash(int a){a^=a>>>16;a*=0x7feb352d;a^=a>>>15;a*=0x846ca68b;return a^(a>>>16);}
    private static int rgba(float r,float g,float b){return Color.rgba8888(Math.max(0,Math.min(r,1)),Math.max(0,Math.min(g,1)),Math.max(0,Math.min(b,1)),1);}
    private static Pixmap procedural(){
        Pixmap p=new Pixmap(128,64);
        for(int tile=0;tile<32;tile++)for(int y=0;y<16;y++)for(int x=0;x<16;x++){
            int id=tile+32,h=hash(tile*8191+(y/3)*53+(x/3)*967);float n=(h&255)/255f;
            float r,g,b;
            if(id==orbitBlack){float dark=.025f+n*.025f;r=g=b=dark;}
            else if(id==orbitViolet){
                // Charcoal with clustered muted purple obsidian veins, not a neon purple solid cube.
                int vein=hash((x/3)*91+(y/3)*773+41)&255;float grain=(hash(x*31+y*197)&63)/255f;
                if(vein>153){r=.13f+grain*.23f;g=.055f+grain*.12f;b=.22f+grain*.32f;}
                else{r=.036f+n*.033f;g=.028f+n*.020f;b=.060f+n*.045f;}
            }
            else if(id>=tooth&&id<command){r=.71f+n*.22f;g=.70f+n*.22f;b=.78f+n*.19f;if(x==0||y==15){r*=.67f;g*=.67f;b*=.67f;}}
            else if(id>=command&&id<rib){
                boolean panel=x>=3&&x<=12&&y>=3&&y<=12;
                boolean rim=x==2||x==13||y==2||y==13;
                r=panel?.57f:rim?.72f:.57f+n*.19f;
                g=panel?.59f:rim?.47f:.27f+n*.16f;
                b=panel?.56f:rim?.29f:.12f+n*.12f;
                if((x==0||y==15)&&!panel){r*=.75f;g*=.7f;b*=.7f;}

            }else{
                boolean stone=id<skull;
                float tone=(stone?.135f:.21f)+n*(stone?.052f:.095f);
                r=tone*(stone?.76f:.72f);g=tone*(stone?.76f:.88f);b=tone*(stone?1.04f:.86f);
                if((x+(tile%3)*3)%7==0&&y%4<2){r*=.62f;g*=.64f;b*=.72f;}
                if(!stone&&(y==0||x==0)){r+=.012f;g+=.012f;b+=.012f;}
                // Veins are deliberately sparse: dark mass, not an all-purple balloon.
                if(stone&&((x+y/3+tile*3)%19==5&&(y+tile)%9<3)){r=.12f;g=.11f;b=.18f;}
                if(id>=rib){r=tone*.54f;g=tone*.59f;b=tone*.62f;}
            }
            p.set((tile%8)*16+x,(tile/8)*16+y,rgba(r,g,b));
        }
        return p;
    }
    public static Pixmap atlas(){
        Pixmap p=procedural();
        Pixmap source=new Pixmap(wstorm.WitherStormMod.file("textures","supplied-material-atlas.png"));
        if(source.width!=128||source.height!=64)throw new IllegalArgumentException("Expected supplied 8x4 atlas of 16px tiles");
        for(int tile=0;tile<32;tile++){
            int from=tile==14||tile==15||tile==orbitBlack-32?8:tile==orbitViolet-32?0:tile;
            for(int y=0;y<16;y++)for(int x=0;x<16;x++)p.set((tile%8)*16+x,(tile/8)*16+y,source.get((from%8)*16+x,(from/8)*16+y));
        }
        source.dispose();return p;
    }
    public static void load(){
        if(texture!=null)return;
        Pixmap p=atlas();texture=new Texture(p);texture.setFilter(Texture.TextureFilter.nearest);
        for(int i=0;i<regions.length;i++)regions[i]=new TextureRegion(texture,(i%8)*16,(i/8)*16,16,16);
        p.dispose();
        // The nine-dot command block is independent; excluded input tiles are NOT used here.
        Pixmap original=procedural(),commandPixels=new Pixmap(16,16);
        for(int y=0;y<16;y++)for(int x=0;x<16;x++)commandPixels.set(x,y,original.get(96+x,16+y));
        original.dispose();commandTexture=new Texture(commandPixels);commandTexture.setFilter(Texture.TextureFilter.nearest);commandPixels.dispose();commandRegion=new TextureRegion(commandTexture);
    }
    public static TextureRegion region(int material,int face){
        if(material>=20000){mindustry.world.Block b=mindustry.Vars.content.block(material-20000);return b==null?null:b.fullIcon;}
        if(material>=1000){texturedFaces++;return wstorm.model.ImportedStormHead.region(material,face);}
        if(texture==null||material<32)return null;
        if(material==command||material==command+1){texturedFaces++;return commandRegion;}
        int variant=material==obsidian?(face*3)%8:material==skull?face%4:material==tooth?face%2:0;
        texturedFaces++;return regions[Math.min(31,material-32+variant)];
    }
    public static void export(Fi file){Pixmap p=atlas();PixmapIO.writePng(file,p);p.dispose();}
    public static void dispose(){if(texture!=null)texture.dispose();texture=null;if(commandTexture!=null)commandTexture.dispose();commandTexture=null;commandRegion=null;}
}
