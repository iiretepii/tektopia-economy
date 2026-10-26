package tekeco.board;

/**
 * Sizes and short number forms for sign text. A 1.12 sign line is 90 pixels wide, and Minecraft
 * cuts off anything wider at the last word that fits.
 */
public final class SignText {
    public static final int LINE_WIDTH = 90;

    private SignText() {
    }

    /** Width in pixels in Minecraft's default font, including the 1px gap after each character. */
    public static int width(String text) {
        int width = 0;
        for (char c : text.toCharArray()) {
            width += charWidth(c);
        }
        return width;
    }

    public static boolean fits(String text) {
        return width(text) <= LINE_WIDTH;
    }

    private static int charWidth(char c) {
        switch (c) {
            case '!': case '\'': case ',': case '.': case ':': case ';': case 'i': case '|':
                return 2;
            case '`': case 'l':
                return 3;
            case ' ': case '"': case 'I': case '[': case ']': case 't':
                return 4;
            case '(': case ')': case '*': case '<': case '>': case '{': case '}': case 'f': case 'k':
                return 5;
            case '@': case '~':
                return 7;
            default:
                return 6;
        }
    }

    /** The number as-is up to {@code fullUpTo}, then like 12k or 1.2M. */
    public static String number(long n, long fullUpTo) {
        long abs = Math.abs(n);
        String sign = n < 0 ? "-" : "";
        if (abs <= fullUpTo) {
            return Long.toString(n);
        }
        if (abs < 1_000_000L) {
            return sign + abs / 1000 + "k";
        }
        return sign + abs / 1_000_000L + "." + (abs % 1_000_000L) / 100_000L + "M";
    }

    /** Numbers on most lines: as-is up to 9,999. */
    public static String number(long n) {
        return number(n, 9_999L);
    }
}
