package net.minecraftforge.fml.common.gameevent; import net.minecraft.world.World; import net.minecraftforge.fml.common.eventhandler.Event;
public class TickEvent extends Event { public enum Phase { START, END } public Phase phase; public static class WorldTickEvent extends TickEvent { public final World world = null; } }
