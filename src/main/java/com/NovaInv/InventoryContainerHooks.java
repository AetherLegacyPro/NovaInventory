package com.NovaInv;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;

public final class InventoryContainerHooks {
    private static final Map<Container, Boolean> PATCHED_CONTAINERS = Collections.synchronizedMap(new WeakHashMap<Container, Boolean>());

    private static final int EXTRA_START = 36;
    private static final int EXTRA_END = 63;
    private static final int HIDDEN_X = -10000;
    private static final int HIDDEN_Y = -10000;

    private InventoryContainerHooks() {
    }

    public static void onSlotAdded(Container container, Slot addedSlot) {
        if (container == null || addedSlot == null) {
            return;
        }

        if (addedSlot != null && addedSlot.inventory instanceof InventoryPlayer) {
            System.out.println("[NovaInventory] Player slot added to " + container.getClass().getName() + " invSlot=" + addedSlot.getSlotIndex() + " containerSlot=" + addedSlot.slotNumber);
        }

        System.out.println("[NovaInventory] Added hidden expanded inventory slots to " + container.getClass().getName() + " totalSlots=" + container.inventorySlots.size());
        String className = container.getClass().getName();

        //Already patched the inventory, do not want to do it twice
        if ("net.minecraft.inventory.ContainerPlayer".equals(className) || "net.minecraft.client.gui.inventory.GuiContainerCreative$ContainerCreative".equals(className)) {
            return;
        }

        if (!(addedSlot.inventory instanceof InventoryPlayer)) {
            return;
        }

        InventoryPlayer playerInventory = (InventoryPlayer)addedSlot.inventory;

        //If container does not have a hotbar slot return
        if (addedSlot.getSlotIndex() != 8)
        {
            return;
        }

        if (PATCHED_CONTAINERS.containsKey(container))
        {
            return;
        }

        //If the container style is not that of vanilla containers, return
        if (!hasPlayerSlotRange(container, playerInventory, 9, 36)) {
            return;
        }

        if (!hasPlayerSlotRange(container, playerInventory, 0, 9)) {
            return;
        }

        //Do not add extra slots if they are already there(stops it from looping infinitely
        if (hasPlayerSlotRange(container, playerInventory, EXTRA_START, EXTRA_END)) {
            PATCHED_CONTAINERS.put(container, Boolean.TRUE);
            return;
        }
        
        //Add extra inventory slots, they are hidden unless the other inventory is accessed 
        for (int i = EXTRA_START; i < EXTRA_END; i++) {
            Slot slot = new Slot(playerInventory, i, HIDDEN_X, HIDDEN_Y);

            slot.slotNumber = container.inventorySlots.size();
            container.inventorySlots.add(slot);
            container.inventoryItemStacks.add(null);
        }

        PATCHED_CONTAINERS.put(container, Boolean.TRUE);
        System.out.println("[NovaInventory] Added hidden expanded inventory slots to " + container.getClass().getName());
    }

    private static boolean hasPlayerSlotRange(Container container, InventoryPlayer inventory, int startInclusive, int endExclusive) {
        for (int index = startInclusive; index < endExclusive; index++)
        {
            boolean found = false;

            for (int i = 0; i < container.inventorySlots.size(); i++)
            {
                Slot slot = (Slot)container.inventorySlots.get(i);

                if (slot.inventory == inventory && slot.getSlotIndex() == index)
                {
                    found = true;
                    break;
                }
            }

            if (!found)
            {
                return false;
            }
        }

        return true;
    }
}
