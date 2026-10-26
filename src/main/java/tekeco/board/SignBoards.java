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
import net.tangotek.tektopia.Village;
import net.tangotek.tektopia.entities.EntityVillagerTek;
import tekeco.payroll.Payroll;
import tekeco.payroll.Villages;
import tekeco.treasury.Treasury;
import tekeco.wallet.IWallet;
import tekeco.wallet.WalletCapability;

/**
 * Keeps economy signs up to date. A sign in a village whose first line is [Treasury], [Payday],
 * [Tradesman] or [Tavern] gets its other three lines filled in with that village's numbers.
 *
 * Every line fits a 1.12 sign at its widest value; see README.md for the layouts. Colors are the
 * dark ones, because signs are light wood.
 */
public final class SignBoards {
    private static final TextFormatting HEADER = TextFormatting.GOLD;
    private static final TextFormatting GOOD = TextFormatting.DARK_GREEN;
    private static final TextFormatting BAD = TextFormatting.DARK_RED;
    private static final TextFormatting PLAIN = TextFormatting.BLACK;

    private enum Board {
        TREASURY("[Treasury]"), PAYDAY("[Payday]"), TRADESMAN("[Tradesman]"), TAVERN("[Tavern]");

        final String keyword;

        Board(String keyword) {
            this.keyword = keyword;
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
        Map<Village, int[]> savings = new IdentityHashMap<>();
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
            if (treasury == null) {
                lines[1] = new TextComponentTranslation(village == null ? "tekeco.sign.novillage" : "tekeco.sign.notownhall");
                lines[2] = new TextComponentString("");
                lines[3] = new TextComponentString("");
            } else {
                fill(board, lines, world, village, treasury, today, savings);
            }
            update(world, sign, lines);
        }
    }

    private static void fill(Board board, ITextComponent[] lines, World world, Village village, Treasury treasury,
                             long today, Map<Village, int[]> savings) {
        switch (board) {
            case TREASURY: {
                int balance = treasury.getBalance();
                int in = (treasury.getStatsDay() == today ? treasury.getUpkeepCollected() : 0)
                    + (treasury.getExportDay() == today ? treasury.getExportIncome() : 0)
                    + (treasury.getTavernDay() == today ? treasury.getTavernIncome() : 0);
                int out = treasury.getStatsDay() == today ? treasury.getWagesPaid() : 0;
                int saved = savings.computeIfAbsent(village, v -> new int[] {savingsOf(world, v)})[0];
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
        }
    }

    private static void noneYet(ITextComponent[] lines, String key) {
        lines[1] = new TextComponentTranslation(key);
        lines[2] = new TextComponentString("");
        lines[3] = new TextComponentString("");
    }

    private static int savingsOf(World world, Village village) {
        int total = 0;
        for (EntityVillagerTek v : world.getEntities(EntityVillagerTek.class, v -> v != null && v.isEntityAlive() && v.getVillage() == village)) {
            IWallet wallet = WalletCapability.get(v);
            if (wallet != null) {
                total += wallet.getCoins();
            }
        }
        return total;
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
