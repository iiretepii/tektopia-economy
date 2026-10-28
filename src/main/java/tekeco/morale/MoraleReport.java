package tekeco.morale;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.item.Item;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.world.World;
import net.tangotek.tektopia.ProfessionType;
import net.tangotek.tektopia.Village;
import net.tangotek.tektopia.entities.EntityVillagerTek;
import net.tangotek.tektopia.structures.VillageStructure;
import net.tangotek.tektopia.structures.VillageStructureType;
import tekeco.EcoConfig;
import tekeco.board.VillageStats;
import tekeco.payroll.Payroll;
import tekeco.text.Messages;
import tekeco.treasury.Treasury;
import tekeco.wallet.IWallet;
import tekeco.wallet.WalletCapability;

/**
 * /tekeco morale: what is likely making a village unhappy, worked out from TekTopia's own rules.
 *
 * TekTopia doesn't record why happiness changes, so causes are detected from each villager's
 * current state (no bed, hungry, nitwit, no tool, leashed, unpaid...), the village's buildings
 * (crowding, missing Tavern or Library), and deaths and injuries this mod records. Causes are
 * sorted by a rough estimate of how much happiness each costs: villagers affected times TekTopia's
 * usual penalty. Everyday work, which slowly lowers happiness for everyone, can't be pinned on
 * anything and isn't listed.
 */
public final class MoraleReport {
    private static final int UNHAPPIEST_SHOWN = 5;

    private MoraleReport() {
    }

    /** One villager and what's wrong for them. */
    private static final class Resident {
        final EntityVillagerTek villager;
        final List<ITextComponent> issues = new ArrayList<>();

        Resident(EntityVillagerTek villager) {
            this.villager = villager;
        }
    }

    /** One likely cause, with a rough happiness cost used for sorting. */
    private static final class Cause {
        final ITextComponent text;
        final String hoverKey;
        final int estimate;

        Cause(ITextComponent text, String hoverKey, int estimate) {
            this.text = text;
            this.hoverKey = hoverKey;
            this.estimate = estimate;
        }
    }

    public static List<ITextComponent> build(World world, Village village, @Nullable Treasury treasury) {
        long today = Payroll.payday(world);
        List<Resident> residents = new ArrayList<>();
        for (EntityVillagerTek v : world.getEntities(EntityVillagerTek.class, v -> v != null && v.isEntityAlive() && v.getVillage() == village)) {
            ProfessionType profession = v.getProfessionType();
            if (profession != null && profession != ProfessionType.NOMAD) {
                residents.add(new Resident(v));
            }
        }

        List<ITextComponent> out = new ArrayList<>();
        ITextComponent header = new TextComponentTranslation("commands.tekeco.morale.header", Messages.value(village.getName(), TextFormatting.WHITE));
        header.getStyle().setColor(TextFormatting.GOLD);
        header.getStyle().setBold(true);
        out.add(header);
        if (residents.isEmpty()) {
            out.add(Messages.line("commands.tekeco.morale.noresidents", null));
            return out;
        }

        // Per-villager checks.
        int nitwits = 0, noBed = 0, hungry = 0, starving = 0, noTool = 0, leashed = 0, unpaid = 0, cantAfford = 0,
            turnedAway = 0, children = 0, bedOwners = 0, bards = 0, happySum = 0, sulking = 0;
        for (Resident r : residents) {
            EntityVillagerTek v = r.villager;
            ProfessionType profession = v.getProfessionType();
            happySum += v.getHappy();
            if (v.getHappy() < VillageStats.SULKING_BELOW) {
                sulking++;
            }
            if (profession == ProfessionType.BARD) {
                bards++;
            }
            if (profession == ProfessionType.CHILD) {
                children++;
            }
            if (profession == ProfessionType.NITWIT) {
                nitwits++;
                r.issues.add(issue("nitwit"));
            }
            if (v.getBedPos() == null) {
                noBed++;
                r.issues.add(issue("nobed"));
            } else {
                bedOwners++;
            }
            if (v.getHunger() <= 0) {
                starving++;
                r.issues.add(issue("starving"));
            } else if (v.getHunger() < VillageStats.HUNGRY_BELOW) {
                hungry++;
                r.issues.add(issue("hungry"));
            }
            String tool = missingTool(v, profession);
            if (tool != null) {
                noTool++;
                r.issues.add(new TextComponentTranslation("commands.tekeco.morale.issue.notool", new TextComponentTranslation("commands.tekeco.morale.tool." + tool)));
            }
            if (v.getLeashed()) {
                leashed++;
                r.issues.add(issue("leashed"));
            }
            IWallet wallet = WalletCapability.get(v);
            if (wallet != null && EcoConfig.baseWage(profession) > 0) {
                if (wallet.wasUnpaid()) {
                    unpaid++;
                    r.issues.add(issue("unpaid"));
                }
                if (wallet.wasBroke()) {
                    cantAfford++;
                    r.issues.add(issue("cantafford"));
                }
                if (wallet.isTavernBlocked()) {
                    turnedAway++;
                    r.issues.add(issue("turnedaway"));
                }
            }
        }

        // Overview, with the change since the last payday's snapshot.
        int average = Math.round((float) happySum / residents.size());
        ITextComponent trend;
        if (treasury == null || treasury.getMoraleDay() < 0) {
            trend = new TextComponentString("");
        } else {
            int change = average - treasury.getMoraleAverage();
            String key = change < 0 ? "commands.tekeco.morale.trend.down" : change > 0 ? "commands.tekeco.morale.trend.up" : "commands.tekeco.morale.trend.same";
            trend = new TextComponentTranslation(key, Messages.value(Math.abs(change), change < 0 ? Messages.BAD : change > 0 ? Messages.GOOD : Messages.NEUTRAL),
                since(today - treasury.getMoraleDay()));
        }
        out.add(Messages.line("commands.tekeco.morale.overview", "commands.tekeco.morale.overview.hover",
            Messages.value(average, average >= 60 ? Messages.GOOD : average >= 30 ? Messages.NEUTRAL : Messages.BAD), trend,
            Messages.problems(sulking), Messages.value(residents.size(), Messages.NEUTRAL)));

        // Causes, biggest estimated cost first.
        List<Cause> causes = new ArrayList<>();
        if (treasury != null) {
            int deaths = treasury.deaths.between(today - 1, today);
            int childDeaths = treasury.childDeaths.between(today - 1, today);
            add(causes, deaths, "deaths", deaths * bedOwners * 32);
            add(causes, childDeaths, "childdeaths", childDeaths * bedOwners * 57);
            int hurt = treasury.injuries.between(today, today);
            add(causes, hurt, "injuries", hurt * 8);
        }
        add(causes, nitwits, "nitwits", nitwits * 15);
        add(causes, noBed, "nobed", noBed * 20);
        add(causes, starving, "starving", starving * 20);
        add(causes, hungry, "hungry", hungry * 10);
        add(causes, noTool, "notool", noTool * 5);
        add(causes, leashed, "leashed", leashed * 10);
        add(causes, unpaid, "unpaid", unpaid * EcoConfig.unpaidPenalty);
        add(causes, cantAfford, "cantafford", cantAfford * EcoConfig.brokePenalty);
        add(causes, turnedAway, "turnedaway", turnedAway * 5);
        if (children > 0 && village.hasStructure(VillageStructureType.SCHOOL)) {
            add(causes, children, "school", children * 2);
        }
        for (VillageStructure structure : village.getStructures(VillageStructureType.values())) {
            int inside = structure.getEntitiesInside(EntityVillagerTek.class).size();
            // TekTopia's crowding check divides by the number of villagers inside, so only ask with someone there.
            float crowded = inside > 0 ? structure.getCrowdedFactor() : 0F;
            if (crowded > 0F) {
                BlockPos door = structure.getDoor();
                causes.add(new Cause(new TextComponentTranslation("commands.tekeco.morale.cause.crowded",
                    Messages.value(capitalized(structure.type.name()), Messages.NEUTRAL),
                    Messages.value(door.getX() + ", " + door.getY() + ", " + door.getZ(), Messages.NEUTRAL)),
                    "commands.tekeco.morale.cause.crowded.hover", Math.round(5 * crowded * inside) + 1));
            }
        }
        if (!village.hasStructure(VillageStructureType.TAVERN)) {
            causes.add(new Cause(new TextComponentTranslation("commands.tekeco.morale.cause.notavern"), "commands.tekeco.morale.cause.notavern.hover", residents.size() * 5));
        } else if (bards == 0) {
            causes.add(new Cause(new TextComponentTranslation("commands.tekeco.morale.cause.nobard"), "commands.tekeco.morale.cause.nobard.hover", residents.size() * 3));
        }
        if (!village.hasStructure(VillageStructureType.LIBRARY)) {
            causes.add(new Cause(new TextComponentTranslation("commands.tekeco.morale.cause.nolibrary"), "commands.tekeco.morale.cause.nolibrary.hover", residents.size() * 2));
        }
        causes.sort(Comparator.comparingInt((Cause c) -> c.estimate).reversed());

        if (causes.isEmpty()) {
            out.add(Messages.line("commands.tekeco.morale.nocauses", "commands.tekeco.morale.nocauses.hover"));
        } else {
            out.add(Messages.line("commands.tekeco.morale.causes", "commands.tekeco.morale.causes.hover"));
            for (Cause cause : causes) {
                ITextComponent line = new TextComponentString(" - ");
                line.getStyle().setColor(Messages.LABEL);
                line.getStyle().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentTranslation(cause.hoverKey)));
                line.appendSibling(cause.text);
                out.add(line);
            }
        }

        // The unhappiest villagers and what's wrong for each.
        residents.sort(Comparator.comparingInt(r -> r.villager.getHappy()));
        out.add(Messages.line("commands.tekeco.morale.unhappiest", null));
        for (Resident r : residents.subList(0, Math.min(UNHAPPIEST_SHOWN, residents.size()))) {
            EntityVillagerTek v = r.villager;
            ITextComponent issues;
            if (r.issues.isEmpty()) {
                issues = new TextComponentTranslation("commands.tekeco.morale.issue.none");
            } else {
                issues = new TextComponentString("");
                for (int i = 0; i < r.issues.size(); i++) {
                    if (i > 0) {
                        issues.appendText(", ");
                    }
                    issues.appendSibling(r.issues.get(i));
                }
            }
            BlockPos pos = v.getPosition();
            ITextComponent line = new TextComponentTranslation("commands.tekeco.morale.villager", v.getDisplayName(),
                professionName(v.getProfessionType()), Messages.value(v.getHappy(), v.getHappy() < VillageStats.SULKING_BELOW ? Messages.BAD : Messages.NEUTRAL), issues);
            line.getStyle().setColor(Messages.LABEL);
            line.getStyle().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new TextComponentTranslation("commands.tekeco.morale.villager.hover", pos.getX(), pos.getY(), pos.getZ())));
            out.add(line);
        }
        return out;
    }

    private static void add(List<Cause> causes, int count, String key, int estimate) {
        if (count > 0) {
            causes.add(new Cause(new TextComponentTranslation("commands.tekeco.morale.cause." + key, Messages.problems(count)),
                "commands.tekeco.morale.cause." + key + ".hover", estimate));
        }
    }

    private static ITextComponent issue(String key) {
        return new TextComponentTranslation("commands.tekeco.morale.issue." + key);
    }

    private static ITextComponent since(long daysAgo) {
        if (daysAgo <= 0) {
            return new TextComponentTranslation("commands.tekeco.morale.since.today");
        }
        if (daysAgo == 1) {
            return new TextComponentTranslation("commands.tekeco.morale.since.yesterday");
        }
        return new TextComponentTranslation("commands.tekeco.morale.since.daysago", daysAgo);
    }

    private static String professionName(ProfessionType profession) {
        return capitalized(profession.name);
    }

    /** HOME4 or home4 to Home4. */
    private static String capitalized(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    /** The tool TekTopia makes this worker craft (losing 5 happiness) when they don't have one, if it's missing. */
    @Nullable
    private static String missingTool(EntityVillagerTek v, @Nullable ProfessionType profession) {
        if (profession == null) {
            return null;
        }
        switch (profession) {
            case FARMER:
                return has(v, ItemHoe.class) ? null : "hoe";
            case LUMBERJACK:
            case BUTCHER:
                return has(v, ItemAxe.class) ? null : "axe";
            case MINER:
                return has(v, ItemPickaxe.class) ? null : "pickaxe";
            case GUARD:
                return has(v, ItemSword.class) || has(v, ItemAxe.class) ? null : "weapon";
            default:
                return null;
        }
    }

    private static boolean has(EntityVillagerTek v, Class<? extends Item> type) {
        Predicate<ItemStack> isTool = s -> !s.isEmpty() && type.isInstance(s.getItem());
        return v.getInventory().getItemCount(isTool) > 0;
    }
}
