package tekeco.board;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.world.World;
import net.tangotek.tektopia.ProfessionType;
import net.tangotek.tektopia.Village;
import net.tangotek.tektopia.entities.EntityVillagerTek;
import tekeco.wallet.IWallet;
import tekeco.wallet.WalletCapability;

/**
 * One village's numbers for the sign boards, worked out once per update from its loaded villagers.
 *
 * Residents are the villagers TekTopia counts as living there: workers, children and nitwits.
 * Vendors (no profession) and visiting nomads are left out. Skills only count villagers with a job.
 */
public final class VillageStats {
    /** TekTopia's own thresholds: hungry villagers look for food, sulking ones walk slowly and barely work. */
    public static final int HUNGRY_BELOW = 30;
    public static final int SULKING_BELOW = 20;

    public int residents;
    public int savings;

    public int happySum;
    public int happyLowest = Integer.MAX_VALUE;
    public int sulking;

    public int hungerSum;
    public int hungry;
    public int starving;

    public int intelligenceSum;

    public int workers;
    public int skillSum;
    public final Map<ProfessionType, Job> jobs = new EnumMap<>(ProfessionType.class);

    public static final class Job {
        public int count;
        public int skillSum;
        public int skillTop;

        public int average() {
            return count == 0 ? 0 : Math.round((float) skillSum / count);
        }
    }

    public static VillageStats of(World world, Village village) {
        VillageStats stats = new VillageStats();
        for (EntityVillagerTek v : world.getEntities(EntityVillagerTek.class, v -> v != null && v.isEntityAlive() && v.getVillage() == village)) {
            IWallet wallet = WalletCapability.get(v);
            if (wallet != null) {
                stats.savings += wallet.getCoins();
            }
            ProfessionType profession = v.getProfessionType();
            if (profession == null || profession == ProfessionType.NOMAD) {
                continue;
            }
            stats.residents++;
            int happy = v.getHappy();
            stats.happySum += happy;
            stats.happyLowest = Math.min(stats.happyLowest, happy);
            if (happy < SULKING_BELOW) {
                stats.sulking++;
            }
            int hunger = v.getHunger();
            stats.hungerSum += hunger;
            if (hunger < HUNGRY_BELOW) {
                stats.hungry++;
            }
            if (hunger <= 0) {
                stats.starving++;
            }
            stats.intelligenceSum += v.getIntelligence();

            if (hasJob(profession)) {
                int skill = v.getSkill(profession);
                stats.workers++;
                stats.skillSum += skill;
                Job job = stats.jobs.computeIfAbsent(profession, p -> new Job());
                job.count++;
                job.skillSum += skill;
                job.skillTop = Math.max(job.skillTop, skill);
            }
        }
        return stats;
    }

    public static boolean hasJob(ProfessionType profession) {
        return profession != null && profession != ProfessionType.NOMAD && profession != ProfessionType.CHILD
            && profession != ProfessionType.NITWIT;
    }

    public int happyAverage() {
        return residents == 0 ? 0 : Math.round((float) happySum / residents);
    }

    public int hungerAverage() {
        return residents == 0 ? 0 : Math.round((float) hungerSum / residents);
    }

    public int intelligenceAverage() {
        return residents == 0 ? 0 : Math.round((float) intelligenceSum / residents);
    }

    public int skillAverage() {
        return workers == 0 ? 0 : Math.round((float) skillSum / workers);
    }
}
