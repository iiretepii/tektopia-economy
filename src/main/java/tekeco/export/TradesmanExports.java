package tekeco.export;

import java.util.List;
import net.minecraft.entity.IMerchant;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import net.minecraft.world.World;
import net.tangotek.tektopia.ItemTagType;
import net.tangotek.tektopia.ModItems;
import net.tangotek.tektopia.Village;
import net.tangotek.tektopia.entities.EntityMerchant;
import net.tangotek.tektopia.entities.EntityTradesman;
import tekeco.EcoConfig;
import tekeco.payroll.Payroll;
import tekeco.payroll.Villages;
import tekeco.treasury.Treasury;
import tekeco.treasury.TreasuryData;

/**
 * Once a day, while TekTopia's wandering Merchant is in town, each village's Tradesman sells
 * surplus goods from Storage to the Merchant and puts the emeralds in the treasury.
 *
 * It takes the Merchant's current offers, the same ones a player sees, and only sells items on
 * the village's sell list. It sells only villager-made (green) items, as a player would have to,
 * and always leaves the configured reserve in Storage. Each sale is recorded in TekTopia's
 * market the same way a player's is, so prices drop as the Tradesman sells.
 */
public final class TradesmanExports {
    private TradesmanExports() {
    }

    public static void run(World world) {
        if (!EcoConfig.tradesmanSells) {
            return;
        }
        long today = Payroll.payday(world);
        boolean changed = false;
        for (EntityTradesman tradesman : world.getEntities(EntityTradesman.class, t -> t != null && t.isEntityAlive() && t.hasVillage())) {
            changed |= sell(world, tradesman.getVillage(), today);
        }
        if (changed) {
            TreasuryData.get(world).markDirty();
        }
    }

    /** Returns true if the treasury changed. */
    static boolean sell(World world, Village village, long today) {
        Treasury treasury = Villages.treasury(world, village);
        if (treasury == null || treasury.getExportDay() == today) {
            return false;
        }
        List<EntityMerchant> merchants = world.getEntities(EntityMerchant.class, m -> m != null && m.isEntityAlive() && m.getVillage() == village);
        if (merchants.isEmpty()) {
            return false;
        }
        IMerchant merchant = merchants.get(0);
        MerchantRecipeList offers = merchant.getRecipes(null);
        if (offers == null) {
            return false;
        }

        List<String> sellList = treasury.getSellList();
        int trades = 0;
        int income = 0;
        for (MerchantRecipe offer : offers) {
            ItemStack wanted = offer.getItemToBuy();
            ItemStack paid = offer.getItemToSell();
            // Only "goods for emeralds" offers; skip the Merchant's food sales and special orders.
            // (TekTopia's offers never take a second item.)
            if (wanted.isEmpty() || wanted.getItem() == Items.EMERALD || paid.getItem() != Items.EMERALD) {
                continue;
            }
            if (sellList.stream().noneMatch(spec -> ItemSpec.matches(spec, wanted))) {
                continue;
            }
            int reserve = EcoConfig.reserve(wanted);
            while (!offer.isRecipeDisabled() && canSell(village, wanted, reserve)) {
                if (!take(village, wanted)) {
                    break;
                }
                offer.incrementToolUses();
                village.getTownData().getEconomy().sellItem(offer, village);
                treasury.deposit(paid.getCount());
                trades++;
                income += paid.getCount();
            }
        }
        treasury.recordExport(today, trades, income);
        return true;
    }

    /** True if Storage has enough villager-made items for one trade and still keeps the reserve. */
    private static boolean canSell(Village village, ItemStack wanted, int reserve) {
        int all = village.getStorageCount(s -> sameItem(s, wanted));
        int sellable = village.getStorageCount(s -> sellable(s, wanted));
        return sellable >= wanted.getCount() && all - wanted.getCount() >= reserve;
    }

    /** Removes one trade's worth of villager-made items from Storage. */
    private static boolean take(Village village, ItemStack wanted) {
        int needed = wanted.getCount();
        while (needed > 0) {
            TileEntityChest chest = village.getStorageChestWithItem(s -> sellable(s, wanted) ? s.getCount() : 0);
            if (chest == null) {
                return false;
            }
            for (int slot = 0; slot < chest.getSizeInventory() && needed > 0; slot++) {
                ItemStack stack = chest.getStackInSlot(slot);
                if (sellable(stack, wanted)) {
                    int n = Math.min(needed, stack.getCount());
                    stack.shrink(n);
                    needed -= n;
                    chest.markDirty();
                    village.onStorageChange(chest, slot, chest.getStackInSlot(slot));
                }
            }
        }
        return true;
    }

    private static boolean sameItem(ItemStack stack, ItemStack wanted) {
        return !stack.isEmpty() && stack.getItem() == wanted.getItem() && stack.getMetadata() == wanted.getMetadata();
    }

    private static boolean sellable(ItemStack stack, ItemStack wanted) {
        return sameItem(stack, wanted) && ModItems.isTaggedItem(stack, ItemTagType.VILLAGER);
    }
}
