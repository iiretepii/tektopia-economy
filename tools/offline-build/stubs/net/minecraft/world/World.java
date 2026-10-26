package net.minecraft.world; import java.util.List; import javax.annotation.Nullable; import com.google.common.base.Predicate; import net.minecraft.entity.Entity; import net.minecraft.util.math.AxisAlignedBB; import net.minecraft.world.storage.MapStorage;
public abstract class World { public final boolean isRemote = false; public final List<net.minecraft.entity.player.EntityPlayer> playerEntities = null; public final List<net.minecraft.tileentity.TileEntity> loadedTileEntityList = null;
 public net.minecraft.block.state.IBlockState getBlockState(net.minecraft.util.math.BlockPos pos){return null;} public void notifyBlockUpdate(net.minecraft.util.math.BlockPos pos, net.minecraft.block.state.IBlockState o, net.minecraft.block.state.IBlockState n, int flags){}
 public long getWorldTime(){return 0;} public long getTotalWorldTime(){return 0;} public MapStorage getPerWorldStorage(){return null;}
 public <T extends Entity> List<T> getEntities(Class<? extends T> c, Predicate<? super T> f){return null;}
 public <T extends Entity> List<T> getEntitiesWithinAABB(Class<? extends T> c, AxisAlignedBB bb, @Nullable Predicate<? super T> f){return null;} }
