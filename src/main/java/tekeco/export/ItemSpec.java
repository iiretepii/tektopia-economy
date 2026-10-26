package tekeco.export;

import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

/**
 * An item written as {@code modid:name} (any variant) or {@code modid:name:meta} (one variant),
 * as used in the config and the /tekeco sell command. A bare {@code name} means {@code minecraft:name},
 * and {@code *} means any item.
 */
public final class ItemSpec {
    public static final String ANY = "*";

    private ItemSpec() {
    }

    /** Lower-cases the spec and adds the minecraft: namespace if it's missing. */
    public static String normalize(String spec) {
        String s = spec.trim().toLowerCase(Locale.ROOT);
        return s.equals(ANY) || s.indexOf(':') >= 0 ? s : "minecraft:" + s;
    }

    /** True if the spec names a registered item and, if given, a numeric variant. */
    public static boolean isValid(String spec) {
        if (normalize(spec).equals(ANY)) {
            return true;
        }
        String[] parts = normalize(spec).split(":");
        if (parts.length < 2 || parts.length > 3) {
            return false;
        }
        if (parts.length == 3 && metaOf(parts[2]) == null) {
            return false;
        }
        return Item.getByNameOrId(parts[0] + ":" + parts[1]) != null;
    }

    public static boolean matches(String spec, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (normalize(spec).equals(ANY)) {
            return true;
        }
        String[] parts = normalize(spec).split(":");
        if (parts.length < 2 || !(parts[0] + ":" + parts[1]).equals(idOf(stack))) {
            return false;
        }
        if (parts.length == 2) {
            return true;
        }
        Integer meta = metaOf(parts[2]);
        return meta != null && meta == stack.getMetadata();
    }

    /** The spec for exactly this item and variant, e.g. minecraft:dye:4. */
    public static String of(ItemStack stack) {
        return idOf(stack) + ":" + stack.getMetadata();
    }

    private static String idOf(ItemStack stack) {
        ResourceLocation id = stack.getItem().getRegistryName();
        return id == null ? "" : id.toString();
    }

    @Nullable
    private static Integer metaOf(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
