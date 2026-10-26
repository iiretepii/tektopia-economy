package net.minecraftforge.registries; import javax.annotation.Nullable; import net.minecraft.util.ResourceLocation;
public interface IForgeRegistryEntry<V> { @Nullable ResourceLocation getRegistryName();
 class Impl<T extends IForgeRegistryEntry<T>> implements IForgeRegistryEntry<T> { @Nullable public final ResourceLocation getRegistryName(){return null;} } }
