package tekeco.treasury;

import java.util.TreeMap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

/** How many times something happened on each of the last few days. Older days are dropped. */
public class DayCounts {
    static final int KEEP_DAYS = 3;

    private final TreeMap<Long, Integer> counts = new TreeMap<>();

    public void add(long day) {
        counts.merge(day, 1, Integer::sum);
        counts.headMap(day - KEEP_DAYS + 1).clear();
    }

    /** The total from {@code fromDay} through {@code toDay}. */
    public int between(long fromDay, long toDay) {
        int total = 0;
        for (int n : counts.subMap(fromDay, true, toDay, true).values()) {
            total += n;
        }
        return total;
    }

    NBTTagList writeToNBT() {
        NBTTagList list = new NBTTagList();
        counts.forEach((day, n) -> {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setLong("day", day);
            tag.setInteger("count", n);
            list.appendTag(tag);
        });
        return list;
    }

    void readFromNBT(NBTTagCompound parent, String key) {
        counts.clear();
        NBTTagList list = parent.getTagList(key, Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            counts.put(tag.getLong("day"), tag.getInteger("count"));
        }
    }
}
