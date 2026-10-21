package dev.aurelium.auraskills.bukkit.util;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;

public class AttributeCompat {

    public static Attribute maxHealth;
    public static Attribute movementSpeed;
    public static Attribute attackDamage;
    public static Attribute attackSpeed;
    public static Attribute luck;

    static {
        try {
            if (VersionUtils.isAtLeastVersion(21, 2)) {
                maxHealth = fromRegistry("max_health", "generic.max_health");
                movementSpeed = fromRegistry("movement_speed", "generic.movement_speed");
                attackDamage = fromRegistry("attack_damage", "generic.attack_damage");
                attackSpeed = fromRegistry("attack_speed", "generic.attack_speed");
                luck = fromRegistry("luck", "generic.luck");
            } else {
                maxHealth = fromRegistry("generic.max_health", "max_health");
                movementSpeed = fromRegistry("generic.movement_speed", "movement_speed");
                attackDamage = fromRegistry("generic.attack_damage", "attack_damage");
                attackSpeed = fromRegistry("generic.attack_speed", "attack_speed");
                luck = fromRegistry("generic.luck", "luck");
            }
        } catch (Throwable ignored) {
        }
    }

    private static Attribute fromRegistry(String key, String fallbackKey) {
        Attribute attribute = null;
        try {
            attribute = Registry.ATTRIBUTE.get(NamespacedKey.minecraft(key));
        } catch (Throwable ignored) {
        }
        if (attribute == null && fallbackKey != null) {
            try {
                attribute = Registry.ATTRIBUTE.get(NamespacedKey.minecraft(fallbackKey));
            } catch (Throwable ignored) {
            }
        }
        return attribute;
    }

}
