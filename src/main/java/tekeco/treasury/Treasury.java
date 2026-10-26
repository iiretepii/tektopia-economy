package tekeco.treasury;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraftforge.common.util.Constants;
import tekeco.EcoConfig;

/**
 * One village's emerald balance, its Tradesman's sell list, and the totals from its most
 * recent payday, Tradesman sale and Tavern night.
 */
public class Treasury {
    private int balance;

    private long statsDay = -1L;
    private int wagesPaid;
    private int upkeepCollected;
    private int villagersPaid;
    private int villagersUnpaid;
    private int villagersBroke;

    /** This village's own sell list, or null to use the config's. */
    @Nullable
    private List<String> sellList;

    private long exportDay = -1L;
    private int exportIncome;
    private int exportTrades;

    private long tavernDay = -1L;
    private int tavernTabs;
    private int tavernIncome;
    private int tavernBardPaid;
    private int tavernTurnedAway;
    /** Bard's share of tabs not yet paid out, since tabs are whole emeralds but the share is a fraction. */
    private double bardCredit;

    public int getBalance() {
        return balance;
    }

    public void deposit(int amount) {
        if (amount > 0) {
            balance += amount;
        }
    }

    /** Removes {@code amount} if the treasury holds that much; otherwise leaves it unchanged. */
    public boolean withdraw(int amount) {
        if (amount <= 0) {
            return true;
        }
        if (balance < amount) {
            return false;
        }
        balance -= amount;
        return true;
    }

    /** Starts a fresh set of payday totals the first time a villager is settled on {@code day}. */
    public void beginDay(long day) {
        if (statsDay != day) {
            statsDay = day;
            wagesPaid = 0;
            upkeepCollected = 0;
            villagersPaid = 0;
            villagersUnpaid = 0;
            villagersBroke = 0;
        }
    }

    public void recordPaid(int wage) {
        wagesPaid += wage;
        villagersPaid++;
    }

    public void recordUnpaid() {
        villagersUnpaid++;
    }

    public void recordUpkeep(int amount) {
        upkeepCollected += amount;
    }

    public void recordBroke() {
        villagersBroke++;
    }

    public long getStatsDay() {
        return statsDay;
    }

    public int getWagesPaid() {
        return wagesPaid;
    }

    public int getUpkeepCollected() {
        return upkeepCollected;
    }

    public int getVillagersPaid() {
        return villagersPaid;
    }

    public int getVillagersUnpaid() {
        return villagersUnpaid;
    }

    public int getVillagersBroke() {
        return villagersBroke;
    }

    /** The items this village's Tradesman may sell: its own list if set, else the config's. */
    public List<String> getSellList() {
        return sellList != null ? Collections.unmodifiableList(sellList) : Arrays.asList(EcoConfig.tradesmanItems);
    }

    public boolean hasOwnSellList() {
        return sellList != null;
    }

    /** Gives this village its own sell list, starting from the config's if it had none. */
    public List<String> editSellList() {
        if (sellList == null) {
            sellList = new ArrayList<>(Arrays.asList(EcoConfig.tradesmanItems));
        }
        return sellList;
    }

    public void resetSellList() {
        sellList = null;
    }

    public long getExportDay() {
        return exportDay;
    }

    /** Records that the Tradesman sold to the Merchant on {@code day}. */
    public void recordExport(long day, int trades, int income) {
        exportDay = day;
        exportTrades = trades;
        exportIncome = income;
    }

    public int getExportTrades() {
        return exportTrades;
    }

    public int getExportIncome() {
        return exportIncome;
    }

    /** Starts fresh Tavern totals the first time a tab is charged on {@code day}. */
    public void beginTavernDay(long day) {
        if (tavernDay != day) {
            tavernDay = day;
            tavernTabs = 0;
            tavernIncome = 0;
            tavernBardPaid = 0;
            tavernTurnedAway = 0;
        }
    }

    public void recordTab(int amount) {
        tavernTabs++;
        tavernIncome += amount;
    }

    public void recordTurnedAway() {
        tavernTurnedAway++;
    }

    /** Adds to the Bard's credit and returns the whole emeralds now owed, paid from the treasury. */
    public int payBard(double share) {
        bardCredit += share;
        int owed = (int) Math.floor(bardCredit);
        if (owed > 0 && withdraw(owed)) {
            bardCredit -= owed;
            tavernIncome -= owed;
            tavernBardPaid += owed;
            return owed;
        }
        return 0;
    }

    public long getTavernDay() {
        return tavernDay;
    }

    public int getTavernTabs() {
        return tavernTabs;
    }

    /** Tab money the treasury kept after the Bard's share. */
    public int getTavernIncome() {
        return tavernIncome;
    }

    public int getTavernBardPaid() {
        return tavernBardPaid;
    }

    public int getTavernTurnedAway() {
        return tavernTurnedAway;
    }

    void readFromNBT(NBTTagCompound tag) {
        balance = tag.getInteger("balance");
        statsDay = tag.hasKey("statsDay") ? tag.getLong("statsDay") : -1L;
        wagesPaid = tag.getInteger("wagesPaid");
        upkeepCollected = tag.getInteger("upkeepCollected");
        villagersPaid = tag.getInteger("villagersPaid");
        villagersUnpaid = tag.getInteger("villagersUnpaid");
        villagersBroke = tag.getInteger("villagersBroke");
        if (tag.hasKey("sellList")) {
            sellList = new ArrayList<>();
            NBTTagList list = tag.getTagList("sellList", Constants.NBT.TAG_STRING);
            for (int i = 0; i < list.tagCount(); i++) {
                sellList.add(list.getStringTagAt(i));
            }
        } else {
            sellList = null;
        }
        exportDay = tag.hasKey("exportDay") ? tag.getLong("exportDay") : -1L;
        exportTrades = tag.getInteger("exportTrades");
        exportIncome = tag.getInteger("exportIncome");
        tavernDay = tag.hasKey("tavernDay") ? tag.getLong("tavernDay") : -1L;
        tavernTabs = tag.getInteger("tavernTabs");
        tavernIncome = tag.getInteger("tavernIncome");
        tavernBardPaid = tag.getInteger("tavernBardPaid");
        tavernTurnedAway = tag.getInteger("tavernTurnedAway");
        bardCredit = tag.getDouble("bardCredit");
    }

    void writeToNBT(NBTTagCompound tag) {
        tag.setInteger("balance", balance);
        tag.setLong("statsDay", statsDay);
        tag.setInteger("wagesPaid", wagesPaid);
        tag.setInteger("upkeepCollected", upkeepCollected);
        tag.setInteger("villagersPaid", villagersPaid);
        tag.setInteger("villagersUnpaid", villagersUnpaid);
        tag.setInteger("villagersBroke", villagersBroke);
        if (sellList != null) {
            NBTTagList list = new NBTTagList();
            for (String spec : sellList) {
                list.appendTag(new NBTTagString(spec));
            }
            tag.setTag("sellList", list);
        }
        tag.setLong("exportDay", exportDay);
        tag.setInteger("exportTrades", exportTrades);
        tag.setInteger("exportIncome", exportIncome);
        tag.setLong("tavernDay", tavernDay);
        tag.setInteger("tavernTabs", tavernTabs);
        tag.setInteger("tavernIncome", tavernIncome);
        tag.setInteger("tavernBardPaid", tavernBardPaid);
        tag.setInteger("tavernTurnedAway", tavernTurnedAway);
        tag.setDouble("bardCredit", bardCredit);
    }
}
