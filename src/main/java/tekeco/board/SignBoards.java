package tekeco.board;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.tangotek.tektopia.ProfessionType;
import net.tangotek.tektopia.Village;
import tekeco.payroll.Payroll;
import tekeco.payroll.Villages;
import tekeco.treasury.Treasury;

/**
 * Keeps economy signs up to date. A sign in a village whose first line is [Treasury], [Payday],
 * [Tradesman], [Tavern], [Morale], [Hunger] or [Skills] gets its other lines filled in with that
 * village's numbers. On a [Skills] sign, line 2 can name a profession; it's kept as written.
 *
 * Every line fits a 1.12 sign at its widest value; see README.md for the layouts. Colors are the
 * dark ones, because signs are light wood.
 */
public final class SignBoards {
    private static final TextFormatting HEADER = TextFormatting.GOLD;
    private static final TextFormatting GOOD = TextFormatting.DARK_GREEN;
    private static final TextFormatting BAD = TextFormatting.DARK_RED;
    private static final TextFormatting PLAIN = TextFormatting.BLACK;
    private static final String ALL_JOBS = "All jobs";

    private enum Board {
        TREASURY("[Treasury]", true), PAYDAY("[Payday]", true), TRADESMAN("[Tradesman]", true), TAVERN("[Tavern]", true),
        MORALE("[Morale]", false), HUNGER("[Hunger]", false), SKILLS("[Skills]", false);

        final String keyword;
        /** Whether the board shows treasury numbers, and so needs a Town Hall. */
        final boolean needsTreasury;

        Board(String keyword, boolean needsTreasury) {
            this.keyword = keyword;
            this.needsTreasury = needsTreasury;
        }

        static Board of(String firstLine) {
            String text = firstLine.trim().toLowerCase(Locale.ROOT);
            for (Board board : values()) {
                if (board.keyword.toLowerCase(Locale.ROOT).equals(text)) {
                    return board;
                }
            }
            return null;
        }
    }

    private SignBoards() {
    }

    public static void run(World world) {
        long today = Payroll.payday(world);
        Map<Village, VillageStats> stats = new IdentityHashMap<>();
        List<TileEntity> tiles = new ArrayList<>(world.loadedTileEntityList);
        for (TileEntity tile : tiles) {
            if (!(tile instanceof TileEntitySign)) {
                continue;
            }
            TileEntitySign sign = (TileEntitySign) tile;
            Board board = Board.of(sign.signText[0].getUnformattedText());
            if (board == null) {
                continue;
            }
            Village village = Villages.at(world, sign.getPos());
            Treasury treasury = Villages.treasury(world, village);
            ITextComponent[] lines = new ITextComponent[4];
            lines[0] = styled(new TextComponentString(board.keyword), HEADER);
            String problem = village == null ? "tekeco.sign.novillage" : board.needsTreasury && treasury == null ? "tekeco.sign.notownhall" : null;
            if (problem != null) {
                // A Skills sign keeps the profession written on line 2.
                int at = board == Board.SKILLS ? 2 : 1;
                for (int i = 1; i < 4; i++) {
                    lines[i] = i == at ? new TextComponentTranslation(problem) : new TextComponentString("");
                }
                if (board == Board.SKILLS) {
                    lines[1] = sign.signText[1];
                }
            } else {
                VillageStats villageStats = board.needsTreasury && board != Board.TREASURY ? null
                    : stats.computeIfAbsent(village, v -> VillageStats.of(world, v));
                fill(board, lines, sign, treasury, villageStats, today);
            }
            update(world, sign, lines);
        }
    }

    private static void fill(Board board, ITextComponent[] lines, TileEntitySign sign, Treasury treasury,
                             VillageStats stats, long today) {
        switch (board) {
            case TREASURY: {
                int balance = treasury.getBalance();
                int in = (treasury.getStatsDay() == today ? treasury.getUpkeepCollected() : 0)
                    + (treasury.getExportDay() == today ? treasury.getExportIncome() : 0)
                    + (treasury.getTavernDay() == today ? treasury.getTavernIncome() : 0);
                int out = treasury.getStatsDay() == today ? treasury.getWagesPaid() : 0;
                int saved = stats.savings;
                lines[1] = line("tekeco.sign.treasury.balance", value(SignText.number(balance, 99_999L), balance > 0 ? GOOD : BAD));
                lines[2] = line("tekeco.sign.treasury.flow", value(SignText.number(in), in > 0 ? GOOD : PLAIN),
                    value(SignText.number(out), PLAIN));
                lines[3] = line("tekeco.sign.treasury.savings", value(SignText.number(saved, 99_999L), PLAIN));
                break;
            }
            case PAYDAY: {
                if (treasury.getStatsDay() < 0) {
                    noneYet(lines, "tekeco.sign.payday.none");
                    break;
                }
                int paid = treasury.getVillagersPaid();
                int onPayroll = paid + treasury.getVillagersUnpaid();
                int broke = treasury.getVillagersBroke();
                lines[1] = days(today - treasury.getStatsDay());
                lines[2] = line("tekeco.sign.payday.paid", value(SignText.number(paid), paid < onPayroll ? BAD : GOOD),
                    value(SignText.number(onPayroll), PLAIN));
                lines[3] = line("tekeco.sign.payday.broke", value(SignText.number(broke), broke > 0 ? BAD : GOOD));
                break;
            }
            case TRADESMAN: {
                if (treasury.getExportDay() < 0) {
                    noneYet(lines, "tekeco.sign.tradesman.none");
                    break;
                }
                int trades = treasury.getExportTrades();
                int income = treasury.getExportIncome();
                lines[1] = days(today - treasury.getExportDay());
                lines[2] = line(trades == 1 ? "tekeco.sign.tradesman.trade" : "tekeco.sign.tradesman.trades", value(SignText.number(trades), PLAIN));
                lines[3] = line("tekeco.sign.tradesman.income", value(SignText.number(income), income > 0 ? GOOD : PLAIN));
                break;
            }
            case TAVERN: {
                if (treasury.getTavernDay() < 0) {
                    noneYet(lines, "tekeco.sign.tavern.none");
                    break;
                }
                String tabs = SignText.number(treasury.getTavernTabs());
                String bard = SignText.number(treasury.getTavernBardPaid());
                int away = treasury.getTavernTurnedAway();
                lines[1] = nights(today - treasury.getTavernDay());
                // "Tabs 99 Bard +99" just fits; with more tabs, leave the Bard's cut to /tekeco report.
                if (treasury.getTavernBardPaid() > 0 && SignText.fits("Tabs " + tabs + " Bard +" + bard)) {
                    lines[2] = line("tekeco.sign.tavern.tabsbard", value(tabs, PLAIN), value(bard, GOOD));
                } else {
                    lines[2] = line("tekeco.sign.tavern.tabs", value(tabs, PLAIN));
                }
                lines[3] = line("tekeco.sign.tavern.away", value(SignText.number(away), away > 0 ? BAD : GOOD));
                break;
            }
            case MORALE: {
                if (stats.residents == 0) {
                    noneYet(lines, "tekeco.sign.noresidents");
                    break;
                }
                lines[1] = line("tekeco.sign.morale.average", band(stats.happyAverage()));
                lines[2] = line("tekeco.sign.morale.sulking", value(SignText.number(stats.sulking, 999), stats.sulking > 0 ? BAD : GOOD),
                    value(SignText.number(stats.residents, 999), PLAIN));
                lines[3] = line("tekeco.sign.morale.lowest", band(stats.happyLowest));
                break;
            }
            case HUNGER: {
                if (stats.residents == 0) {
                    noneYet(lines, "tekeco.sign.noresidents");
                    break;
                }
                lines[1] = line("tekeco.sign.hunger.average", band(stats.hungerAverage()));
                lines[2] = line("tekeco.sign.hunger.hungry", value(SignText.number(stats.hungry, 999), stats.hungry > 0 ? BAD : GOOD),
                    value(SignText.number(stats.residents, 999), PLAIN));
                lines[3] = line("tekeco.sign.hunger.starving", value(SignText.number(stats.starving), stats.starving > 0 ? BAD : GOOD));
                break;
            }
            case SKILLS: {
                String written = sign.signText[1].getUnformattedText().trim();
                String key = written.toLowerCase(Locale.ROOT);
                if (key.isEmpty() || key.equals("all") || key.equals("all jobs")) {
                    // Plain text, not a translation: line 2 is read back on the next update.
                    lines[1] = new TextComponentString(ALL_JOBS);
                    if (stats.workers == 0) {
                        lines[2] = new TextComponentTranslation("tekeco.sign.skills.noworkers");
                        lines[3] = new TextComponentString("");
                        break;
                    }
                    lines[2] = line("tekeco.sign.skills.average", band(stats.skillAverage()));
                    lines[3] = line("tekeco.sign.skills.intelligence", band(stats.intelligenceAverage()));
                    break;
                }
                lines[1] = sign.signText[1];
                ProfessionType profession = profession(key);
                if (profession == null) {
                    lines[2] = new TextComponentTranslation("tekeco.sign.skills.unknown");
                    lines[3] = new TextComponentString("");
                    break;
                }
                VillageStats.Job job = stats.jobs.get(profession);
                int count = job == null ? 0 : job.count;
                lines[2] = count == 0 ? new TextComponentTranslation("tekeco.sign.skills.nobody")
                    : line("tekeco.sign.skills.job", band(job.average()), band(job.skillTop));
                lines[3] = line("tekeco.sign.skills.count", value(SignText.number(count), PLAIN));
                break;
            }
        }
    }

    /** A profession by its TekTopia name (farmer, miner, ...), if it's one with a skill. */
    private static ProfessionType profession(String name) {
        for (ProfessionType profession : ProfessionType.values()) {
            if (profession.name.equals(name) && VillageStats.hasJob(profession)) {
                return profession;
            }
        }
        return null;
    }

    /** A 0-100 value colored by how good it is: dark green from 60, black from 30, dark red below. */
    private static ITextComponent band(int value) {
        return value(SignText.number(value), value >= 60 ? GOOD : value >= 30 ? PLAIN : BAD);
    }

    private static void noneYet(ITextComponent[] lines, String key) {
        lines[1] = new TextComponentTranslation(key);
        lines[2] = new TextComponentString("");
        lines[3] = new TextComponentString("");
    }

    private static ITextComponent days(long daysAgo) {
        if (daysAgo <= 0) {
            return new TextComponentTranslation("tekeco.when.today");
        }
        if (daysAgo == 1) {
            return new TextComponentTranslation("tekeco.when.yesterday");
        }
        return new TextComponentTranslation("tekeco.when.daysago", SignText.number(daysAgo));
    }

    private static ITextComponent nights(long daysAgo) {
        if (daysAgo <= 0) {
            return new TextComponentTranslation("tekeco.sign.tonight");
        }
        if (daysAgo == 1) {
            return new TextComponentTranslation("tekeco.sign.lastnight");
        }
        return new TextComponentTranslation("tekeco.sign.nightsago", SignText.number(daysAgo));
    }

    private static ITextComponent line(String key, Object... args) {
        return new TextComponentTranslation(key, args);
    }

    private static ITextComponent value(String text, TextFormatting color) {
        return styled(new TextComponentString(text), color);
    }

    private static ITextComponent styled(ITextComponent text, TextFormatting color) {
        text.getStyle().setColor(color);
        return text;
    }

    /** Writes the lines and sends them to players, only if something changed. */
    private static void update(World world, TileEntitySign sign, ITextComponent[] lines) {
        boolean changed = false;
        for (int i = 0; i < 4; i++) {
            if (!lines[i].equals(sign.signText[i])) {
                sign.signText[i] = lines[i];
                changed = true;
            }
        }
        if (changed) {
            sign.markDirty();
            IBlockState state = world.getBlockState(sign.getPos());
            world.notifyBlockUpdate(sign.getPos(), state, state, 3);
        }
    }
}
