package com.NovaInv;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.entity.player.EntityPlayer;

public final class InventoryPageServerState {
    private static final Map<String, Integer> PLAYER_PAGES = new ConcurrentHashMap<String, Integer>();
    private static final Map<String, Boolean> CREATIVE_SECOND_PAGE = new ConcurrentHashMap<String, Boolean>();

    private InventoryPageServerState() {
    }

    public static int getPage(EntityPlayer player) {
        if (player == null) {
            return 0;
        }

        Integer page = PLAYER_PAGES.get(player.getCommandSenderName());

        if (page == null) {
            return 0;
        }

        return page.intValue() == 1 ? 1 : 0;
    }

    public static void setPage(EntityPlayer player, int page) {
        if (player == null) {
            return;
        }

        PLAYER_PAGES.put(player.getCommandSenderName(), Integer.valueOf(page == 1 ? 1 : 0));
    }

    public static boolean isCreativeSecondPageEnabled(EntityPlayer player) {
        if (player == null) {
            return false;
        }

        Boolean enabled = CREATIVE_SECOND_PAGE.get(player.getCommandSenderName());

        return enabled != null && enabled.booleanValue();
    }

    public static void setCreativeSecondPageEnabled(EntityPlayer player, boolean enabled) {
        if (player == null) {
            return;
        }

        CREATIVE_SECOND_PAGE.put(player.getCommandSenderName(), Boolean.valueOf(enabled));
    }

    public static void removePlayer(EntityPlayer player) {
        if (player == null) {
            return;
        }

        String name = player.getCommandSenderName();

        PLAYER_PAGES.remove(name);
        CREATIVE_SECOND_PAGE.remove(name);
    }
}
