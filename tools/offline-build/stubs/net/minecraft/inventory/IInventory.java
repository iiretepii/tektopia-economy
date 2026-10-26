package net.minecraft.inventory; import net.minecraft.item.ItemStack; public interface IInventory { int getSizeInventory(); ItemStack getStackInSlot(int index); void markDirty(); }
