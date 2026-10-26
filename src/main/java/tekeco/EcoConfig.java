package tekeco;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.tangotek.tektopia.ProfessionType;
import tekeco.export.ItemSpec;

@Config(modid = TekEconomy.MODID)
public class EcoConfig {
    @Config.Comment({
        "Daily wage in emeralds for each TekTopia profession, before the skill bonus.",
        "A profession with a wage of 0 is not paid and does not pay living costs."
    })
    public static Map<String, Integer> wages = defaultWages();

    @Config.Comment("Extra wage at skill 100, as a fraction of the base wage. 0.5 means a fully skilled villager earns 1.5x.")
    @Config.RangeDouble(min = 0.0, max = 10.0)
    public static double skillBonus = 0.5;

    @Config.Comment("Emeralds each paid villager pays the treasury per day for housing.")
    @Config.RangeInt(min = 0)
    public static int rent = 1;

    @Config.Comment("Emeralds each paid villager pays the treasury per day for food.")
    @Config.RangeInt(min = 0)
    public static int foodCost = 1;

    @Config.Comment("Happiness lost when the treasury can't pay a villager's wage.")
    @Config.RangeInt(min = 0, max = 100)
    public static int unpaidPenalty = 10;

    @Config.Comment("Happiness lost when a villager can't afford their living costs.")
    @Config.RangeInt(min = 0, max = 100)
    public static int brokePenalty = 5;

    @Config.Comment("Savings (after living costs) at or above which a villager gets the comfort bonus.")
    @Config.RangeInt(min = 0)
    public static int comfortSavings = 20;

    @Config.Comment("Happiness gained each payday by a villager with comfortable savings.")
    @Config.RangeInt(min = 0, max = 100)
    public static int comfortBonus = 2;

    @Config.Comment("Time of day (0-23999) when payday happens. 0 is dawn.")
    @Config.RangeInt(min = 0, max = 23999)
    public static int paydayTime = 0;

    @Config.Comment("If true, a villager's savings go to the treasury when they die.")
    public static boolean estateToTreasury = true;

    @Config.Comment("If true, only operators can withdraw from a treasury.")
    public static boolean withdrawRequiresOp = false;

    @Config.Comment("If true, the Town Hall's Tradesman sells surplus village goods to the Merchant once a day, for the treasury.")
    public static boolean tradesmanSells = true;

    @Config.Comment({
        "Items the Tradesman may sell, as modid:name (any variant) or modid:name:meta (one variant), e.g. minecraft:dye:4 for lapis.",
        "* means anything the Merchant buys; an empty list means nothing. /tekeco sell can override this for one village."
    })
    public static String[] tradesmanItems = {"minecraft:diamond", "minecraft:redstone"};

    @Config.Comment("How many of an item the Tradesman leaves in Storage, by modid:name or modid:name:meta. Unlisted items use defaultReserve.")
    public static Map<String, Integer> tradesmanReserves = defaultReserves();

    @Config.Comment("How many of an unlisted item the Tradesman leaves in Storage.")
    @Config.RangeInt(min = 0)
    public static int defaultReserve = 64;

    @Config.Comment("If true, only operators can change a village's sell list with /tekeco sell.")
    public static boolean sellListRequiresOp = false;

    @Config.Comment("If true, players standing in a village see payday and Tradesman sale notices above the hotbar.")
    public static boolean actionBarNotices = true;

    @Config.Comment("Emeralds a villager on the payroll pays for an evening at the Tavern, once per day. 0 makes the Tavern free.")
    @Config.RangeInt(min = 0)
    public static int tavernTab = 1;

    @Config.Comment("Share of each Tavern tab that goes to a Bard performing in that Tavern; the rest goes to the treasury.")
    @Config.RangeDouble(min = 0.0, max = 1.0)
    public static double bardShare = 0.25;

    @Config.Comment({
        "If true, a villager who can't pay the Tavern tab stops visiting the Tavern until they can.",
        "If false, they lose tavernBrokePenalty happiness instead."
    })
    public static boolean tavernTurnsAwayBroke = true;

    @Config.Comment("Happiness lost by a villager who can't pay the Tavern tab, when tavernTurnsAwayBroke is false.")
    @Config.RangeInt(min = 0, max = 100)
    public static int tavernBrokePenalty = 5;

    /** How many of this exact item and variant the Tradesman must leave in Storage. */
    public static int reserve(ItemStack stack) {
        String spec = ItemSpec.of(stack);
        Integer reserve = tradesmanReserves.get(spec);
        if (reserve == null) {
            reserve = tradesmanReserves.get(spec.substring(0, spec.lastIndexOf(':')));
        }
        return reserve == null ? defaultReserve : Math.max(0, reserve);
    }

    public static int baseWage(ProfessionType profession) {
        if (profession == null) {
            return 0;
        }
        Integer wage = wages.get(profession.name);
        return wage == null ? 0 : Math.max(0, wage);
    }

    public static int wage(ProfessionType profession, int skill) {
        int base = baseWage(profession);
        return (int) Math.round(base * (1.0 + skillBonus * Math.max(0, Math.min(skill, 100)) / 100.0));
    }

    private static Map<String, Integer> defaultReserves() {
        Map<String, Integer> map = new HashMap<>();
        // The Blacksmith makes diamond tools and armor; villagers don't use redstone.
        map.put("minecraft:diamond", 4);
        map.put("minecraft:redstone", 0);
        return map;
    }

    private static Map<String, Integer> defaultWages() {
        Map<String, Integer> map = new HashMap<>();
        map.put("bard", 3);
        map.put("blacksmith", 5);
        map.put("butcher", 3);
        map.put("chef", 4);
        map.put("cleric", 4);
        map.put("druid", 4);
        map.put("enchanter", 5);
        map.put("farmer", 3);
        map.put("guard", 4);
        map.put("captain", 6);
        map.put("lumberjack", 3);
        map.put("miner", 4);
        map.put("rancher", 3);
        map.put("teacher", 4);
        map.put("child", 0);
        map.put("nitwit", 0);
        map.put("nomad", 0);
        return map;
    }

    @Mod.EventBusSubscriber(modid = TekEconomy.MODID)
    public static class Sync {
        @SubscribeEvent
        public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (event.getModID().equals(TekEconomy.MODID)) {
                ConfigManager.sync(TekEconomy.MODID, Config.Type.INSTANCE);
            }
        }
    }
}
