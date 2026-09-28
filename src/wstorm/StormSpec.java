package wstorm;

import arc.files.Fi;
import arc.util.serialization.*;

/** Data-only morphology/gameplay parser. No reflection, scripting or executable expressions. */
public final class StormSpec{
    public float initialMatter=90, matterPerVolume=18, initialHealth=15000, healthPerMatter=14, initialCore=2600;
    public float reach=190, extractionTicks=150, powerPerTick=18, summonTicks=600, musicGain=.55f;
    public int voxelDetail=8,maxActive=6;
    public static StormSpec parse(String text){
        if(text.length()>16384)throw new IllegalArgumentException("Blueprint exceeds 16 KiB");
        JsonValue j=new JsonReader().parse(text);
        if(j.getInt("version",0)!=1)throw new IllegalArgumentException("Unsupported storm blueprint version");
        StormSpec s=new StormSpec();
        s.initialMatter=number(j,"initialMatter",90,1,10000);
        s.matterPerVolume=number(j,"matterPerVolume",18,1,1000);
        s.initialHealth=number(j,"initialHealth",15000,1000,1000000);
        s.healthPerMatter=number(j,"healthPerMatter",14,0,1000);
        s.initialCore=number(j,"initialCore",2600,100,1000000);
        s.reach=number(j,"reach",190,40,1000);
        s.extractionTicks=number(j,"extractionTicks",150,30,1200);
        s.powerPerTick=number(j,"powerPerTick",18,1,1000);
        s.summonTicks=number(j,"summonTicks",600,60,10000);
        s.musicGain=number(j,"musicGain",.55f,0,1);
        s.voxelDetail=(int)number(j,"voxelDetail",8,4,10);
        s.maxActive=(int)number(j,"maxActive",6,1,12);
        return s;
    }
    private static float number(JsonValue j,String key,float fallback,float lo,float hi){
        float x=j.getFloat(key,fallback);
        if(!Float.isFinite(x)||x<lo||x>hi)throw new IllegalArgumentException("Invalid "+key+"; expected "+lo+".."+hi);
        return x;
    }
    /** No discrete evolution stages. Characteristic length follows absorbed volume. */
    public float growth(float matter){return .52f+(float)Math.cbrt(Math.max(0,matter)/matterPerVolume)*.26f;}
}
