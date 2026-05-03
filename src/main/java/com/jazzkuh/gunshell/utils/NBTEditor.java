package com.jazzkuh.gunshell.utils;

import com.saicone.rtag.RtagItem;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class NBTEditor {

    public static ItemStack set(ItemStack object, Object value, Object... keys) {
        if (!isUsableItem(object)) return object;
        try {
            RtagItem tag = new RtagItem(object);
            tag.set(value, keys);
            return tag.loadCopy();
        } catch (RuntimeException ignored) {
            return object;
        }
    }

    public static boolean contains(ItemStack object, Object... keys) {
        if (!isUsableItem(object)) return false;
        try {
            RtagItem tag = new RtagItem(object);
            return !tag.getOptional(keys).isEmpty();
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    public static String getString(ItemStack object, Object... keys) {
        return get(object, keys);
    }

    public static Integer getInt(ItemStack object, Object... keys) {
        return get(object, keys);
    }

    public static Double getDouble(ItemStack object, Object... keys) {
        return get(object, keys);
    }

    public static Byte getByte(ItemStack object, Object... keys) {
        return get(object, keys);
    }

    public static Long getLong(ItemStack object, Object... keys) {
        return get(object, keys);
    }

    private static boolean isUsableItem(ItemStack itemStack) {
        return itemStack != null && itemStack.getType() != Material.AIR;
    }

    private static <T> T get(ItemStack object, Object... keys) {
        if (!isUsableItem(object)) return null;
        try {
            RtagItem tag = new RtagItem(object);
            return tag.get(keys);
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}
