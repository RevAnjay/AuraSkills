package dev.aurelium.auraskills.bukkit.util;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

public class PdcUtils {

    private static Method getPdcMethod;

    static {
        try {
            getPdcMethod = ItemStack.class.getMethod("getPersistentDataContainer");
        } catch (NoSuchMethodException e) {
            getPdcMethod = null;
        }
    }

    public static boolean isPaperPdcSupported() {
        return getPdcMethod != null;
    }

    @Nullable
    public static PersistentDataContainer getPdc(ItemStack item) {
        if (item == null) return null;
        if (getPdcMethod != null) {
            try {
                return (PersistentDataContainer) getPdcMethod.invoke(item);
            } catch (Exception ignored) {
            }
        }
        return item.hasItemMeta() ? item.getItemMeta().getPersistentDataContainer() : null;
    }

    public static <T, Z> Z getOrDefault(ItemStack item, NamespacedKey key, PersistentDataType<T, Z> type, Z defaultValue) {
        if (item == null) return defaultValue;
        PersistentDataContainer pdc = getPdc(item);
        if (pdc == null) return defaultValue;
        return pdc.getOrDefault(key, type, defaultValue);
    }

    public static <T, Z> Z get(ItemStack item, NamespacedKey key, PersistentDataType<T, Z> type) {
        if (item == null) return null;
        PersistentDataContainer pdc = getPdc(item);
        if (pdc == null) return null;
        return pdc.get(key, type);
    }

    public static boolean has(ItemStack item, NamespacedKey key, PersistentDataType<?, ?> type) {
        if (item == null) return false;
        PersistentDataContainer pdc = getPdc(item);
        if (pdc == null) return false;
        return pdc.has(key, type);
    }
}
