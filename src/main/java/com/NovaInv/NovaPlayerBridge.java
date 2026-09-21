package com.NovaInv;

import java.lang.reflect.Field;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.PlayerCapabilities;
import net.minecraft.inventory.Container;

public final class NovaPlayerBridge {
    private static final Field INVENTORY_CONTAINER_FIELD = findField(EntityPlayer.class, "inventoryContainer", "field_71069_bz");

    private static final Field CAPABILITIES_FIELD = findField(EntityPlayer.class, "capabilities", "field_71075_bZ");

    private static final Field CREATIVE_MODE_FIELD = findField(PlayerCapabilities.class, "isCreativeMode", "field_75098_d");

    private NovaPlayerBridge() {
    }

    public static Container getInventoryContainer(EntityPlayer player) {
        if (player == null) {
            throw new IllegalArgumentException("[NovaInventory] Player cannot be null");
        }

        try {
            return (Container)INVENTORY_CONTAINER_FIELD.get(player);
        } catch (IllegalAccessException exception) {
            throw new RuntimeException("[NovaInventory] Could not access player inventory container", exception);
        }
    }

    public static boolean isCreativeMode(EntityPlayer player) {
        if (player == null) {
            return false;
        }

        try {
            Object capabilities = CAPABILITIES_FIELD.get(player);

            return capabilities != null && CREATIVE_MODE_FIELD.getBoolean(capabilities);
        } catch (IllegalAccessException exception) {
            throw new RuntimeException("[NovaInventory] Could not read creative-mode state", exception);
        }
    }

    private static Field findField(Class<?> owner, String... names) {
        Class<?> current = owner;
        while (current != null) {
            for (String name : names) {
                try {
                    Field field = current.getDeclaredField(name);
                    field.setAccessible(true);
                    return field;
                } catch (NoSuchFieldException ignored) {
                }
            }

            current = current.getSuperclass();
        }

        throw new RuntimeException("[NovaInventory] Could not locate required field in " + owner.getName());
    }
}