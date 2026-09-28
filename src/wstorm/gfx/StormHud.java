package wstorm.gfx;
import arc.*;import arc.scene.event.*;import arc.math.geom.*;import arc.struct.*;import arc.graphics.*;import arc.graphics.g2d.*;import arc.math.*;import arc.scene.*;import arc.scene.ui.layout.*;import arc.util.*;import mindustry.*;import mindustry.game.*;import mindustry.gen.*;import mindustry.graphics.*;import mindustry.ui.*;import wstorm.model.*;import wstorm.world.*;
/** Team aggregates, not an arbitrary first entity. Pure collection is also headless-testable. */
public final class StormHud{
    public static final class Total{
        public int count,flights;public double mass,hp,maxHp,core;public long units,buildings;
        void add(StormUnit s){count++;mass+=s.matter;hp+=Math.max(0,s.health);maxHp+=s.maxHealth;core+=s.coreIntegrity;units+=s.eatenUnits;buildings+=s.eatenBuildings;flights+=s.buildingsInFlight();}
        public float fraction(){return maxHp<=0?0:(float)Mathf.clamp((float)(hp/maxHp));}
    }
    public static Total[] collect(Team team){Total[] out={new Total(),new Total()};for(Unit u:Groups.unit)if(u instanceof StormUnit s&&!s.dead&&s.team!=Team.derelict)out[s.team==team?0:1].add(s);return out;}
    private static Total[] totals={new Total(),new Total()};private static float timer;
    public static String number(double x){if(x>=1e12)return Strings.fixed((float)(x/1e12),2)+"T";if(x>=1e9)return Strings.fixed((float)(x/1e9),2)+"G";if(x>=1e6)return Strings.fixed((float)(x/1e6),2)+"M";if(x>=10000)return Strings.fixed((float)(x/1000),1)+"k";return Long.toString(Math.round(x));}
    public static final Table[] panels=new Table[2];
    private static final Table[] contents=new Table[2];private static final boolean[] compact=new boolean[2];
    private static final Seq<Rect> obstacles=new Seq<>();private static final Vec2 pos=new Vec2();private static final Rect placed=new Rect();
    private static float layoutTimer;private static int dragging=-1;
    public static void resetPositions(){for(int i=0;i<2;i++){Core.settings.remove("ws-panel-"+i+"-x");Core.settings.remove("ws-panel-"+i+"-y");}layoutTimer=0;}
    private static void nativeObstacles(Element e){
        if(!e.visible||e.color.a<.01f)return;
        boolean surface=e instanceof Table t&&t.getBackground()!=null;
        if((surface||!(e instanceof Group))&&e.getWidth()>3&&e.getHeight()>3&&e.getWidth()<Core.scene.getWidth()*.9f&&e.getHeight()<Core.scene.getHeight()*.9f){
            e.localToStageCoordinates(pos.setZero());obstacles.add(new Rect(pos.x,pos.y,e.getWidth(),e.getHeight()));if(surface)return;
        }
        if(e instanceof Group g)for(Element child:g.getChildren())nativeObstacles(child);
    }
    public static void layoutNow(){
        float sw=Core.scene.getWidth(),sh=Core.scene.getHeight();obstacles.clear();nativeObstacles(Vars.ui.hudGroup);
        for(int i=0;i<2;i++){
            Table box=panels[i];boolean shown=Core.settings.getBool(i==0?"ws-hud-allies":"ws-hud-enemies",true);box.visible=shown;if(!shown)continue;
            if(dragging==i){obstacles.add(new Rect(box.x,box.y,box.getWidth(),box.getHeight()));continue;}
            float width=Math.min(Scl.scl(Core.settings.getInt("ws-hud-width",360)),sw-16);Table content=contents[i];content.setWidth(width);content.invalidateHierarchy();content.validate();float height=content.getPrefHeight();
            float x=Core.settings.getInt("ws-panel-"+i+"-x",-1),y=Core.settings.getInt("ws-panel-"+i+"-y",-1);
            float wantX=x<0?8:x/10000f*Math.max(0,sw-width),wantY=y<0?sh-height-90-i*(height+12):y/10000f*Math.max(0,sh-height);
            boolean fit=HudLayout.place(placed,wantX,wantY,width,height,sw,sh,obstacles);
            if(!fit){width=Math.min(width,Math.max(170,sw*.32f));content.setWidth(width);content.invalidateHierarchy();content.validate();height=content.getPrefHeight();fit=HudLayout.place(placed,wantX,wantY,width,height,sw,sh,obstacles);}
            // On tiny screens reserve a compact title strip instead of covering native controls.
            boolean collapse=!fit;
            if(!fit){height=Scl.scl(32);fit=HudLayout.place(placed,wantX,wantY,width,height,sw,sh,obstacles);}
            box.visible=fit;if(fit){
                if(box.getChildren().isEmpty()||compact[i]!=collapse){box.clearChildren();compact[i]=collapse;final int side=i;
                    if(collapse)box.label(()->(side==0?"友方":"敌方")+"风暴 ×"+totals[side].count+"（收起）").minHeight(26).growX().padLeft(6).left();
                    else box.add(content).growX().minWidth(0);
                }
                box.setBounds(placed.x,placed.y,placed.width,placed.height);obstacles.add(new Rect(placed));}
        }
    }
    public static void install(Table table){
        table.touchable=Touchable.disabled;
        table.visible(()->Vars.state.isGame()&&!Vars.disableUI&&Core.settings.getBool("ws-hud",true));
        panel(table,0);panel(table,1);
        table.update(()->{
            boolean edit=Core.settings.getBool("ws-hud-edit",false);table.touchable=edit?Touchable.childrenOnly:Touchable.disabled;
            for(Table box:panels)box.touchable=edit?Touchable.enabled:Touchable.disabled;
            if((timer-=Time.delta)<=0){timer=10;totals=collect(Vars.player==null?Team.sharded:Vars.player.team());}
            if((layoutTimer-=Time.delta)<=0){layoutTimer=10;layoutNow();}
            table.color.a=Core.settings.getInt("ws-hud-opacity",90)/100f;
        });
    }
    private static void panel(Table root,int side){
        Table content=new Table(Tex.pane);content.touchable=arc.scene.event.Touchable.disabled;content.defaults().pad(3);content.top();contents[side]=content;
        content.label(()->(side==0?"友方凋零风暴":"敌方凋零风暴")+"  ×"+totals[side].count+(Core.settings.getBool("ws-hud-edit",false)?"  ↔ 拖动":"")).wrap().growX().minHeight(26).left().row();
        Bar bar=new Bar(()->"HP  "+number(totals[side].hp)+" / "+number(totals[side].maxHp),()->side==0?Color.valueOf("a77dff"):Color.valueOf("ed727a"),()->totals[side].fraction());
        content.add(bar).growX().height(20).row();
        content.label(()->"总物质量  "+number(totals[side].mass)+"   |   吞噬单位  "+totals[side].units).wrap().growX().left().row();
        content.label(()->"吞噬建筑  "+totals[side].buildings+"   |   吸收中  "+totals[side].flights).wrap().growX().left().row();
        var detail=content.label(()->"核心完整度  "+number(totals[side].core)).left();
        detail.get().visible(()->Core.settings.getBool("ws-hud-details",true));
        Table box=new Table(Tex.pane);box.margin(0);box.top();box.setClip(true);panels[side]=box;
        root.addChild(box);
        box.addListener(new InputListener(){float startX,startY,pressX,pressY;
            @Override public boolean touchDown(InputEvent event,float x,float y,int pointer,arc.input.KeyCode button){
                if(!Core.settings.getBool("ws-hud-edit",false))return false;
                dragging=side;startX=box.x;startY=box.y;pressX=event.stageX;pressY=event.stageY;return true;
            }
            @Override public void touchDragged(InputEvent event,float x,float y,int pointer){
                box.setPosition(Mathf.clamp(startX+event.stageX-pressX,8,Math.max(8,Core.scene.getWidth()-box.getWidth()-8)),Mathf.clamp(startY+event.stageY-pressY,8,Math.max(8,Core.scene.getHeight()-box.getHeight()-8)));
            }
            @Override public void touchUp(InputEvent event,float x,float y,int pointer,arc.input.KeyCode button){
                Core.settings.put("ws-panel-"+side+"-x",Math.round(box.x/Math.max(1,Core.scene.getWidth()-box.getWidth())*10000));
                Core.settings.put("ws-panel-"+side+"-y",Math.round(box.y/Math.max(1,Core.scene.getHeight()-box.getHeight())*10000));dragging=-1;layoutTimer=0;
            }
        });
    }

    public static void drawHealth(StormUnit s,StormModel m){
        if(!Core.settings.getBool("ws-health-bars",true))return;
        float pixel=Core.camera.width/Math.max(1,Core.graphics.getWidth());
        float y=s.y+m.renderer.screenY(0,132*m.scale)+18*pixel,w=112*pixel,f=s.healthf();
        Draw.z(Layer.overlayUI);Draw.color(Color.black,.8f);Fill.rect(s.x,y,w+3*pixel,6*pixel);
        Draw.color(s.team.color);Fill.rect(s.x-w/2+w*f/2,y,w*f,3.5f*pixel);Draw.reset();
        if(Core.settings.getBool("ws-health-text",true))Fonts.outline.draw(number(s.health)+" / "+number(s.maxHealth),s.x,y+13*pixel,Color.white,.55f*pixel,false,Align.center);
    }
}
