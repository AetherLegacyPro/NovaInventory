package com.NovaInv;

import java.lang.reflect.Field;

import net.minecraft.client.gui.GuiScreen;

//More Pain I had to add due to ArchaicFix...
public final class NovaGuiBridge {
    private static final Field ALLOW_USER_INPUT_FIELD = findField(GuiScreen.class, "allowUserInput", "field_146291_p");

    private NovaGuiBridge() {
    }

    public static void setAllowUserInput(GuiScreen screen, boolean value) {
        try {
            ALLOW_USER_INPUT_FIELD.setBoolean(screen, value);
        } catch (IllegalAccessException exception) {
            throw new RuntimeException("[NovaInventory] Could not set allowUserInput", exception);
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

        throw new RuntimeException("[NovaInventory] Could not locate GuiScreen.allowUserInput");
    }
}