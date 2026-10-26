package tekeco.text;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.tangotek.tektopia.Village;
import net.tangotek.tektopia.VillageManager;
import tekeco.EcoConfig;
import tekeco.treasury.Treasury;

/** Short messages shown above the hotbar (the action bar) to players standing in a village. */
public final class Notices {
    private Notices() {
    }

    public static void payday(World world, Village village, Treasury treasury) {
        ITextComponent balance = Messages.value(treasury.getBalance(), treasury.getBalance() > 0 ? Messages.GOOD : Messages.BAD);
        ITextComponent message;
        if (treasury.getVillagersUnpaid() > 0) {
            message = new TextComponentTranslation("tekeco.notice.payday.unpaid",
                Messages.problems(treasury.getVillagersUnpaid()), balance);
            message.getStyle().setColor(Messages.BAD);
        } else {
            message = new TextComponentTranslation("tekeco.notice.payday",
                Messages.value(treasury.getWagesPaid(), Messages.NEUTRAL), Messages.value(treasury.getVillagersPaid(), Messages.NEUTRAL), balance);
            message.getStyle().setColor(Messages.LABEL);
        }
        toVillage(world, village, message);
    }

    public static void sale(World world, Village village, Treasury treasury) {
        ITextComponent message = new TextComponentTranslation("tekeco.notice.sale",
            Messages.income(treasury.getExportIncome()), Messages.value(treasury.getBalance(), Messages.GOOD));
        message.getStyle().setColor(Messages.LABEL);
        toVillage(world, village, message);
    }

    private static void toVillage(World world, Village village, ITextComponent message) {
        if (!EcoConfig.actionBarNotices) {
            return;
        }
        VillageManager villages = VillageManager.get(world);
        for (EntityPlayer player : world.playerEntities) {
            if (villages.getVillageAt(player.getPosition()) == village) {
                player.sendStatusMessage(message, true);
            }
        }
    }
}
