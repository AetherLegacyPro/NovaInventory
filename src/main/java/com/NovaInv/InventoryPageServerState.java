package com.NovaInv;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.entity.player.EntityPlayer;

public final class InventoryPageServerState {
    private static final Map<String, Integer> PLAYER_PAGES = new ConcurrentHashMap<String, Integer>();

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

        if (page < 0) {
            return 0;
        }

        if (page > 1) {
            return 1;
        }

        return page;
    }

    public static void setPage(EntityPlayer player, int page) {
        if (player == null) {
            return;
        }

        if (page < 0) {
            page = 0;
        }

        if (page > 1) {
            page = 1;
        }

        PLAYER_PAGES.put(player.getCommandSenderName(), page);
    }
}
