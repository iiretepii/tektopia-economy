package tekeco.command;

import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.world.World;
import net.minecraftforge.items.ItemHandlerHelper;
import net.tangotek.tektopia.Village;
import net.tangotek.tektopia.entities.EntityVillagerTek;
import tekeco.EcoConfig;
import tekeco.export.ItemSpec;
import tekeco.payroll.Payroll;
import tekeco.payroll.Villages;
import tekeco.text.Messages;
import tekeco.treasury.Treasury;
import tekeco.treasury.TreasuryData;
import tekeco.wallet.IWallet;
import tekeco.wallet.WalletCapability;

/**
 * /tekeco balance | deposit <amount|all> | withdraw <amount> | report | sell [list|add|remove|reset],
 * for the village you're standing in.
 */
public class CommandTekEco extends CommandBase {

    @Override
    public String getName() {
        return "tekeco";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "commands.tekeco.usage";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 1) {
            throw new WrongUsageException(getUsage(sender));
        }
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        World world = player.world;
        Village village = Villages.at(world, player.getPosition());
        Treasury treasury = Villages.treasury(world, village);
        if (treasury == null) {
            throw new CommandException(village == null ? "commands.tekeco.novillage" : "commands.tekeco.notownhall");
        }

        switch (args[0]) {
            case "balance":
                sender.sendMessage(new TextComponentTranslation("commands.tekeco.balance", village.getName(), treasury.getBalance()));
                break;
            case "deposit":
                deposit(player, village, treasury, args);
                break;
            case "withdraw":
                withdraw(server, player, village, treasury, args);
                break;
            case "report":
                report(player, village, treasury);
                break;
            case "sell":
                sell(player, village, treasury, args);
                break;
            default:
                throw new WrongUsageException(getUsage(sender));
        }
    }

    private void deposit(EntityPlayerMP player, Village village, Treasury treasury, String[] args) throws CommandException {
        if (args.length < 2) {
            throw new WrongUsageException("commands.tekeco.deposit.usage");
        }
        int held = player.inventory.clearMatchingItems(Items.EMERALD, -1, 0, null);
        int amount = "all".equals(args[1]) ? held : parseInt(args[1], 1);
        if (amount <= 0 || amount > held) {
            throw new CommandException("commands.tekeco.deposit.notenough", held);
        }
        int removed = player.inventory.clearMatchingItems(Items.EMERALD, -1, amount, null);
        player.inventoryContainer.detectAndSendChanges();
        treasury.deposit(removed);
        TreasuryData.get(player.world).markDirty();
        player.sendMessage(new TextComponentTranslation("commands.tekeco.deposit.done", removed, village.getName(), treasury.getBalance()));
    }

    private void withdraw(MinecraftServer server, EntityPlayerMP player, Village village, Treasury treasury, String[] args) throws CommandException {
        if (args.length < 2) {
            throw new WrongUsageException("commands.tekeco.withdraw.usage");
        }
        if (EcoConfig.withdrawRequiresOp && !player.canUseCommand(2, getName())) {
            throw new CommandException("commands.tekeco.withdraw.oponly");
        }
        int amount = parseInt(args[1], 1);
        if (!treasury.withdraw(amount)) {
            throw new CommandException("commands.tekeco.withdraw.notenough", treasury.getBalance());
        }
        TreasuryData.get(player.world).markDirty();
        int maxStack = new ItemStack(Items.EMERALD, 1).getMaxStackSize();
        for (int left = amount; left > 0; left -= maxStack) {
            ItemHandlerHelper.giveItemToPlayer(player, new ItemStack(Items.EMERALD, Math.min(left, maxStack)));
        }
        player.sendMessage(new TextComponentTranslation("commands.tekeco.withdraw.done", amount, village.getName(), treasury.getBalance()));
    }

    private void report(EntityPlayerMP player, Village village, Treasury treasury) {
        World world = player.world;
        int residents = 0;
        int savings = 0;
        for (EntityVillagerTek v : world.getEntities(EntityVillagerTek.class, v -> v != null && v.isEntityAlive() && v.getVillage() == village)) {
            IWallet wallet = WalletCapability.get(v);
            if (wallet != null) {
                residents++;
                savings += wallet.getCoins();
            }
        }
        long today = Payroll.payday(world);
        String hover = "commands.tekeco.report.hover.";

        ITextComponent header = new TextComponentTranslation("commands.tekeco.report.header", Messages.value(village.getName(), TextFormatting.WHITE));
        header.getStyle().setColor(TextFormatting.GOLD);
        header.getStyle().setBold(true);
        player.sendMessage(header);

        int balance = treasury.getBalance();
        player.sendMessage(Messages.line("commands.tekeco.report.balance", hover + "balance",
            Messages.value(balance, balance > 0 ? Messages.GOOD : Messages.BAD)));
        player.sendMessage(Messages.line("commands.tekeco.report.savings", hover + "savings",
            Messages.value(savings, Messages.NEUTRAL), Messages.value(residents, Messages.NEUTRAL)));

        if (treasury.getStatsDay() < 0) {
            player.sendMessage(Messages.line("commands.tekeco.report.nopayday", hover + "payday"));
        } else {
            player.sendMessage(Messages.line("commands.tekeco.report.payday", hover + "payday",
                Messages.when(today - treasury.getStatsDay()),
                Messages.value(treasury.getWagesPaid(), Messages.NEUTRAL), Messages.value(treasury.getVillagersPaid(), Messages.NEUTRAL),
                Messages.income(treasury.getUpkeepCollected()),
                Messages.problems(treasury.getVillagersUnpaid()), Messages.problems(treasury.getVillagersBroke())));
        }
        if (treasury.getExportDay() < 0) {
            player.sendMessage(Messages.line("commands.tekeco.report.nosales", hover + "sales"));
        } else {
            player.sendMessage(Messages.line("commands.tekeco.report.sales", hover + "sales",
                Messages.when(today - treasury.getExportDay()),
                Messages.value(treasury.getExportTrades(), Messages.NEUTRAL), Messages.income(treasury.getExportIncome())));
        }
        if (treasury.getTavernDay() < 0) {
            player.sendMessage(Messages.line("commands.tekeco.report.notavern", hover + "tavern"));
        } else {
            player.sendMessage(Messages.line("commands.tekeco.report.tavern", hover + "tavern",
                Messages.when(today - treasury.getTavernDay()),
                Messages.value(treasury.getTavernTabs(), Messages.NEUTRAL), Messages.income(treasury.getTavernIncome()),
                Messages.income(treasury.getTavernBardPaid()), Messages.problems(treasury.getTavernTurnedAway())));
        }

        String button = "commands.tekeco.button.";
        // Start from an unstyled root so the spaces between buttons aren't clickable.
        ITextComponent buttons = new TextComponentString("");
        buttons.appendSibling(Messages.button(button + "deposit", button + "deposit.hover", ClickEvent.Action.SUGGEST_COMMAND, "/tekeco deposit "));
        buttons.appendText(" ");
        buttons.appendSibling(Messages.button(button + "sell", button + "sell.hover", ClickEvent.Action.RUN_COMMAND, "/tekeco sell"));
        buttons.appendText(" ");
        buttons.appendSibling(Messages.button(button + "refresh", button + "refresh.hover", ClickEvent.Action.RUN_COMMAND, "/tekeco report"));
        player.sendMessage(buttons);
    }

    private void sell(EntityPlayerMP player, Village village, Treasury treasury, String[] args) throws CommandException {
        String action = args.length < 2 ? "list" : args[1];
        if (!"list".equals(action) && EcoConfig.sellListRequiresOp && !player.canUseCommand(2, getName())) {
            throw new CommandException("commands.tekeco.sell.oponly");
        }
        switch (action) {
            case "list":
                break;
            case "add": {
                String spec = sellArgument(args);
                if (!ItemSpec.isValid(spec)) {
                    throw new CommandException("commands.tekeco.sell.unknown", spec);
                }
                if (treasury.getSellList().stream().noneMatch(e -> ItemSpec.normalize(e).equals(spec))) {
                    treasury.editSellList().add(spec);
                }
                break;
            }
            case "remove": {
                String spec = sellArgument(args);
                if (treasury.getSellList().stream().noneMatch(e -> ItemSpec.normalize(e).equals(spec))) {
                    throw new CommandException("commands.tekeco.sell.notlisted", spec);
                }
                treasury.editSellList().removeIf(e -> ItemSpec.normalize(e).equals(spec));
                break;
            }
            case "reset":
                treasury.resetSellList();
                break;
            default:
                throw new WrongUsageException("commands.tekeco.sell.usage");
        }
        if (!"list".equals(action)) {
            TreasuryData.get(player.world).markDirty();
        }

        List<String> list = treasury.getSellList();
        if (list.isEmpty()) {
            player.sendMessage(new TextComponentTranslation("commands.tekeco.sell.nothing", village.getName()));
        } else if (list.contains(ItemSpec.ANY)) {
            player.sendMessage(new TextComponentTranslation("commands.tekeco.sell.anything", village.getName()));
        } else {
            player.sendMessage(new TextComponentTranslation("commands.tekeco.sell.list", village.getName(), String.join(", ", list)));
        }
        player.sendMessage(new TextComponentTranslation(treasury.hasOwnSellList() ? "commands.tekeco.sell.own" : "commands.tekeco.sell.default"));
        if (!EcoConfig.tradesmanSells) {
            player.sendMessage(new TextComponentTranslation("commands.tekeco.sell.disabled"));
        }
    }

    private static String sellArgument(String[] args) throws WrongUsageException {
        if (args.length < 3) {
            throw new WrongUsageException("commands.tekeco.sell.usage");
        }
        return ItemSpec.normalize(args[2]);
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "balance", "deposit", "withdraw", "report", "sell");
        }
        if (args.length == 2 && "deposit".equals(args[0])) {
            return getListOfStringsMatchingLastWord(args, "all");
        }
        if (args.length == 2 && "sell".equals(args[0])) {
            return getListOfStringsMatchingLastWord(args, "list", "add", "remove", "reset");
        }
        if (args.length == 3 && "sell".equals(args[0]) && "remove".equals(args[1]) && sender instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) sender;
            Treasury treasury = Villages.treasury(player.world, Villages.at(player.world, player.getPosition()));
            if (treasury != null) {
                return getListOfStringsMatchingLastWord(args, treasury.getSellList());
            }
        }
        return Collections.emptyList();
    }
}
