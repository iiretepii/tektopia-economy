package net.minecraftforge.event.entity.player; import net.minecraft.entity.Entity; import net.minecraft.util.EnumActionResult; import net.minecraft.util.EnumHand; import net.minecraft.world.World;
public class PlayerInteractEvent extends PlayerEvent { public EnumHand getHand(){return null;} public World getWorld(){return null;} public void setCancellationResult(EnumActionResult r){}
 public static class EntityInteract extends PlayerInteractEvent { public Entity getTarget(){return null;} } }
