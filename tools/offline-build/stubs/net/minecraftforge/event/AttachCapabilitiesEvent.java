package net.minecraftforge.event; import net.minecraft.util.ResourceLocation; import net.minecraftforge.common.capabilities.ICapabilityProvider; import net.minecraftforge.fml.common.eventhandler.Event;
public class AttachCapabilitiesEvent<T> extends Event { public T getObject(){return null;} public void addCapability(ResourceLocation k, ICapabilityProvider p){} }
