package tekeco.tavern;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import net.minecraft.world.World;
import net.tangotek.tektopia.Village;
import net.tangotek.tektopia.entities.EntityBard;
import net.tangotek.tektopia.entities.EntityVillagerTek;
import net.tangotek.tektopia.structures.VillageStructure;
import net.tangotek.tektopia.structures.VillageStructureType;
import tekeco.EcoConfig;
import tekeco.payroll.Payroll;
import tekeco.payroll.Villages;
import tekeco.treasury.Treasury;
import tekeco.treasury.TreasuryData;
import tekeco.wallet.IWallet;
import tekeco.wallet.WalletCapability;

/**
 * Charges villagers on the payroll a tab, once a day, when they spend an evening in a Tavern.
 * The tab goes to the treasury, less a share for a Bard performing in the same Tavern.
 *
 * A villager who can't pay has TekTopia's own "visit_tavern" AI filter switched off, the same
 * setting a player can toggle in the villager's screen, and it is switched back on once they can
 * pay. Only villagers this mod switched off are switched back on.
 */
public final class TavernTabs {
    /** TekTopia's AI filter for Tavern visits. */
    public static final String VISIT_TAVERN = "visit_tavern";

    private TavernTabs() {
    }

    public static void run(World world) {
        long today = Payroll.payday(world);
        Set<Village> villages = Collections.newSetFromMap(new IdentityHashMap<>());
        for (EntityVillagerTek villager : world.getEntities(EntityVillagerTek.class, v -> v != null && v.isEntityAlive())) {
            letBackIn(villager);
            if (villager.hasVillage()) {
                villages.add(villager.getVillage());
            }
        }
        if (EcoConfig.tavernTab <= 0) {
            return;
        }
        boolean changed = false;
        for (Village village : villages) {
            changed |= charge(world, village, today);
        }
        if (changed) {
            TreasuryData.get(world).markDirty();
        }
    }

    /** Switches Tavern visits back on for a villager this mod turned away, once they can pay. */
    private static void letBackIn(EntityVillagerTek villager) {
        IWallet wallet = WalletCapability.get(villager);
        if (wallet == null || !wallet.isTavernBlocked()) {
            return;
        }
        if (EcoConfig.tavernTab <= 0 || !EcoConfig.tavernTurnsAwayBroke || wallet.getCoins() >= EcoConfig.tavernTab) {
            if (villager.getAIFilters().contains(VISIT_TAVERN)) {
                villager.setAIFilter(VISIT_TAVERN, true);
            }
            wallet.setTavernBlocked(false);
        }
    }

    /** Returns true if the treasury changed. */
    static boolean charge(World world, Village village, long today) {
        Treasury treasury = Villages.treasury(world, village);
        if (treasury == null) {
            return false;
        }
        boolean changed = false;
        for (VillageStructure tavern : village.getStructures(VillageStructureType.TAVERN)) {
            List<EntityVillagerTek> inside = tavern.getEntitiesInside(EntityVillagerTek.class);
            EntityBard bard = null;
            for (EntityVillagerTek v : inside) {
                if (v instanceof EntityBard && v.isEntityAlive()) {
                    bard = (EntityBard) v;
                    break;
                }
            }
            for (EntityVillagerTek villager : inside) {
                // Only off-duty villagers on the payroll pay; someone passing through at work doesn't,
                // and neither does the Bard playing there.
                if (!villager.isEntityAlive() || villager instanceof EntityBard || villager.isWorkTime() || villager.getVillage() != village
                        || EcoConfig.baseWage(villager.getProfessionType()) <= 0) {
                    continue;
                }
                IWallet wallet = WalletCapability.get(villager);
                if (wallet == null || wallet.getTavernDay() == today) {
                    continue;
                }
                wallet.setTavernDay(today);
                treasury.beginTavernDay(today);
                changed = true;
                if (wallet.take(EcoConfig.tavernTab)) {
                    treasury.deposit(EcoConfig.tavernTab);
                    treasury.recordTab(EcoConfig.tavernTab);
                    IWallet bardWallet = bard != null ? WalletCapability.get(bard) : null;
                    if (bardWallet != null) {
                        bardWallet.add(treasury.payBard(EcoConfig.tavernTab * EcoConfig.bardShare));
                    }
                } else {
                    treasury.recordTurnedAway();
                    turnAway(villager, wallet);
                }
            }
        }
        return changed;
    }

    private static void turnAway(EntityVillagerTek villager, IWallet wallet) {
        if (!EcoConfig.tavernTurnsAwayBroke) {
            villager.modifyHappy(-EcoConfig.tavernBrokePenalty);
        } else if (villager.getAIFilters().contains(VISIT_TAVERN) && villager.isAIFilterEnabled(VISIT_TAVERN)) {
            // They finish this evening, but won't head to the Tavern again until they can pay.
            villager.setAIFilter(VISIT_TAVERN, false);
            wallet.setTavernBlocked(true);
        }
    }
}
