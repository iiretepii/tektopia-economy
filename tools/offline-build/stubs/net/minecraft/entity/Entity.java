package net.minecraft.entity; import javax.annotation.Nullable; import net.minecraft.world.World; import net.minecraft.util.math.*; import net.minecraft.util.text.ITextComponent; import net.minecraft.util.EnumFacing; import net.minecraft.command.ICommandSender; import net.minecraftforge.common.capabilities.*;
public abstract class Entity implements ICommandSender, ICapabilityProvider {
 public World world; public boolean isDead;
 public boolean isEntityAlive(){return true;} public boolean hasCustomName(){return false;} public String getCustomNameTag(){return "";}
 public AxisAlignedBB getEntityBoundingBox(){return null;} public ITextComponent getDisplayName(){return null;} public BlockPos getPosition(){return null;}
 public boolean isSneaking(){return false;}
 public boolean canUseCommand(int level, String name){return false;}
 public boolean hasCapability(Capability<?> c, @Nullable EnumFacing f){return false;}
 @Nullable public <T> T getCapability(Capability<T> c, @Nullable EnumFacing f){return null;} }
