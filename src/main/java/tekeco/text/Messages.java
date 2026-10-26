package tekeco.text;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;

/** Builders for styled chat text. All text comes from the lang file, so it stays translatable. */
public final class Messages {
    public static final TextFormatting LABEL = TextFormatting.GRAY;
    public static final TextFormatting GOOD = TextFormatting.GREEN;
    public static final TextFormatting BAD = TextFormatting.RED;
    public static final TextFormatting NEUTRAL = TextFormatting.WHITE;

    private Messages() {
    }

    /** A gray line of text whose arguments keep their own colors, with an optional hover explanation. */
    public static ITextComponent line(String key, String hoverKey, Object... args) {
        ITextComponent line = new TextComponentTranslation(key, args);
        line.getStyle().setColor(LABEL);
        if (hoverKey != null) {
            line.getStyle().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentTranslation(hoverKey)));
        }
        return line;
    }

    public static ITextComponent value(Object value, TextFormatting color) {
        ITextComponent text = new TextComponentString(String.valueOf(value));
        text.getStyle().setColor(color);
        return text;
    }

    /** Green when there's money in or nobody missed out, white for zero income. */
    public static ITextComponent income(int amount) {
        return value(amount, amount > 0 ? GOOD : NEUTRAL);
    }

    /** Red when anything went wrong, green when the count is zero. */
    public static ITextComponent problems(int count) {
        return value(count, count > 0 ? BAD : GOOD);
    }

    /** "today", "yesterday" or "N days ago". */
    public static ITextComponent when(long daysAgo) {
        ITextComponent text;
        if (daysAgo <= 0) {
            text = new TextComponentTranslation("tekeco.when.today");
        } else if (daysAgo == 1) {
            text = new TextComponentTranslation("tekeco.when.yesterday");
        } else {
            text = new TextComponentTranslation("tekeco.when.daysago", daysAgo);
        }
        text.getStyle().setColor(NEUTRAL);
        return text;
    }

    /** A clickable [label] that runs or fills in a command. */
    public static ITextComponent button(String labelKey, String hoverKey, ClickEvent.Action action, String command) {
        ITextComponent button = new TextComponentTranslation(labelKey);
        button.getStyle().setColor(TextFormatting.AQUA);
        button.getStyle().setClickEvent(new ClickEvent(action, command));
        button.getStyle().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentTranslation(hoverKey)));
        return button;
    }
}
