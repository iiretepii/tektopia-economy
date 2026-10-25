package net.minecraftforge.common.capabilities; import javax.annotation.Nullable; import net.minecraft.nbt.NBTBase; import net.minecraft.util.EnumFacing;
public class Capability<T> { public interface IStorage<T> { @Nullable NBTBase writeNBT(Capability<T> c, T i, EnumFacing s); void readNBT(Capability<T> c, T i, EnumFacing s, NBTBase n); }
 public IStorage<T> getStorage(){return null;} public <R> R cast(T i){return null;} }
