package net.minecraft.nbt; import java.util.UUID; public class NBTTagCompound extends NBTBase {
 public void setInteger(String k,int v){} public int getInteger(String k){return 0;}
 public void setLong(String k,long v){} public long getLong(String k){return 0;}
 public boolean hasKey(String k){return false;} public void setTag(String k, NBTBase v){}
 public NBTTagList getTagList(String k,int t){return null;}
 public void setUniqueId(String k, UUID v){} public UUID getUniqueId(String k){return null;}
 public void setBoolean(String k, boolean v){} public boolean getBoolean(String k){return false;}
 public void setDouble(String k, double v){} public double getDouble(String k){return 0;} }
