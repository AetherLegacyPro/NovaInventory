package com.NovaInv;

public final class InventoryPageState {
    private static int selectedPage = 0;

    private InventoryPageState() {
    }

    public static int getPage() {
        return selectedPage;
    }

    public static void setPage(int page) {
        if (page < 0) {
            page = 0;
        }

        if (page > 1) {
            page = 1;
        }

        selectedPage = page;
    }
}
