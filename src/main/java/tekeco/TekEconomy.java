package tekeco;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import tekeco.command.CommandTekEco;
import tekeco.event.EconomyEvents;
import tekeco.wallet.WalletCapability;

@Mod(
    modid = TekEconomy.MODID,
    name = TekEconomy.NAME,
    version = TekEconomy.VERSION,
    dependencies = "required-after:tektopia",
    acceptedMinecraftVersions = "[1.12.2]"
)
public class TekEconomy {
    public static final String MODID = "tekeco";
    public static final String NAME = "TekTopia Economy";
    public static final String VERSION = "0.2.0";

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        WalletCapability.register();
        MinecraftForge.EVENT_BUS.register(new EconomyEvents());
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandTekEco());
    }
}
