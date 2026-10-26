package net.minecraft.world; import java.util.List; import javax.annotation.Nullable; import com.google.common.base.Predicate; import net.minecraft.entity.Entity; import net.minecraft.util.math.AxisAlignedBB; import net.minecraft.world.storage.MapStorage;
public abstract class World { public final boolean isRemote = false; public final List<net.minecraft.entity.player.EntityPlayer> playerEntities = null;
 public long getWorldTime(){return 0;} public long getTotalWorldTime(){return 0;} public MapStorage getPerWorldStorage(){return null;}
 public <T extends Entity> List<T> getEntities(Class<? extends T> c, Predicate<? super T> f){return null;}
 public <T extends Entity> List<T> getEntitiesWithinAABB(Class<? extends T> c, AxisAlignedBB bb, @Nullable Predicate<? super T> f){return null;} }
