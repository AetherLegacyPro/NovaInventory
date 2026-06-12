package com.NovaInv;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
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

        //We already override this class in ContainerPlayerOverwrite so no need to do it twice
        if (isClassOrSuper(container.getClass(), "net.minecraft.inventory.ContainerPlayer")) {
            return;
        }

        //We already override this class in GuiContainerCreativeOverwrite so no need to do it twice
        if (isClassOrSuper(container.getClass(), "net.minecraft.client.gui.inventory.GuiContainerCreative$ContainerCreative")) {
            return;
        }

        if (!(addedSlot.inventory instanceof InventoryPlayer)) {
            return;
        }

        InventoryPlayer playerInventory = (InventoryPlayer)addedSlot.inventory;

        //Modify the inventory once the hotbar is added
        if (addedSlot.getSlotIndex() != 8) {
            return;
        }

        if (PATCHED_CONTAINERS.containsKey(container)) {
            return;
        }

        //Patch the containers that have the vanilla player inventory and hotbar
        if (!hasPlayerSlotRange(container, playerInventory, 9, 36)) {
            return;
        }

        if (!hasPlayerSlotRange(container, playerInventory, 0, 9)) {
            return;
        }

        //It just works
        if (hasPlayerSlotRange(container, playerInventory, EXTRA_START, EXTRA_END)) {
            PATCHED_CONTAINERS.put(container, Boolean.TRUE);
            return;
        }

        PATCHED_CONTAINERS.put(container, Boolean.TRUE);


        //AE2 container compat added to AppEngSlot
        //Why does AE2 do this????
        if (isAE2Container(container)) {
            boolean success = appendAE2ExtraPlayerSlots(container, playerInventory);

            if (success) {
                System.out.println("[NovaInventory] Added AE2-compatible hidden expanded inventory slots to " + container.getClass().getName() + " totalSlots=" + container.inventorySlots.size());
            }
            else {
                System.err.println("[NovaInventory] WARNING: Could not add AE2-compatible slots to " + container.getClass().getName() + ". Skipping extra inventory slots for this container.");
            }

            return;
        }

        appendVanillaExtraPlayerSlots(container, playerInventory);
        System.out.println("[NovaInventory] Added hidden expanded inventory slots to " + container.getClass().getName() + " totalSlots=" + container.inventorySlots.size());
    }

    private static void appendVanillaExtraPlayerSlots(Container container, InventoryPlayer playerInventory) {
        for (int i = EXTRA_START; i < EXTRA_END; i++) {
            Slot slot = new Slot(playerInventory, i, HIDDEN_X, HIDDEN_Y);
            slot.slotNumber = container.inventorySlots.size();
            container.inventorySlots.add(slot);
            container.inventoryItemStacks.add(null);
        }
    }

    private static boolean appendAE2ExtraPlayerSlots(Container container, InventoryPlayer playerInventory) {
        try {
            for (int i = EXTRA_START; i < EXTRA_END; i++) {
                Slot slot = createAE2PlayerSlot(container, playerInventory, i, HIDDEN_X, HIDDEN_Y);

                if (slot == null) {
                    return false;
                }

                //Doing a refection within the ASM to bypass addSlotToContainer to avoid the crash by calling the setContainer with the reflection
                callAE2SetContainer(slot, container);
                slot.slotNumber = container.inventorySlots.size();
                container.inventorySlots.add(slot);
                container.inventoryItemStacks.add(null);
            }

            return true;
        }
        catch (Throwable throwable) {
            System.err.println("[NovaInventory] Failed adding AE2 hidden slots");
            throwable.printStackTrace();
            return false;
        }
    }

    //Tries a few different things so the new inventory slots are recognized
    private static Slot createAE2PlayerSlot(Container container, InventoryPlayer playerInventory, int slotIndex, int x, int y) {
        try {
            String className = isAE2SlotLocked(container, slotIndex) ? "appeng.container.slot.SlotDisabled" : "appeng.container.slot.SlotPlayerInv";
            Class<?> slotClass = Class.forName(className);
            Constructor<?> constructor = findAE2SlotConstructor(slotClass, playerInventory);

            if (constructor == null) {
                System.err.println("[NovaInventory] Could not find AE2 slot constructor for " + className);
                return null;
            }

            constructor.setAccessible(true);
            Object created = constructor.newInstance(playerInventory, slotIndex, x, y);

            if (!(created instanceof Slot)) {
                System.err.println("[NovaInventory] AE2 slot was not a net.minecraft.inventory.Slot: " + created);
                return null;
            }

            return (Slot)created;
        }
        catch (Throwable throwable) {
            System.err.println("[NovaInventory] Failed creating AE2 player slot for inventory slot " + slotIndex);
            throwable.printStackTrace();
            return null;
        }
    }

    private static Constructor<?> findAE2SlotConstructor(Class<?> slotClass, InventoryPlayer playerInventory) {
        try {
            return slotClass.getConstructor(IInventory.class, int.class, int.class, int.class);
        }
        catch (Throwable ignored) {
        }

        try {
            return slotClass.getConstructor(InventoryPlayer.class, int.class, int.class, int.class);
        }
        catch (Throwable ignored) {
        }

        Constructor<?>[] constructors = slotClass.getDeclaredConstructors();

        for (int i = 0; i < constructors.length; i++) {
            Constructor<?> constructor = constructors[i];
            Class<?>[] params = constructor.getParameterTypes();

            if (params.length != 4) {
                continue;
            }

            if (!params[1].equals(int.class) || !params[2].equals(int.class) || !params[3].equals(int.class)) {
                continue;
            }

            if (params[0].isAssignableFrom(playerInventory.getClass()) || params[0].isAssignableFrom(IInventory.class) || IInventory.class.isAssignableFrom(params[0])) {
                return constructor;
            }
        }

        return null;
    }

    private static void callAE2SetContainer(Slot slot, Container container) {
        try {
            Class<?> current = slot.getClass();

            while (current != null) {
                Method[] methods = current.getDeclaredMethods();

                for (int i = 0; i < methods.length; i++) {
                    Method method = methods[i];

                    if (!"setContainer".equals(method.getName())) {
                        continue;
                    }

                    Class<?>[] params = method.getParameterTypes();

                    if (params.length != 1) {
                        continue;
                    }

                    if (!params[0].isAssignableFrom(container.getClass()) && !params[0].isInstance(container)) {
                        continue;
                    }

                    method.setAccessible(true);
                    method.invoke(slot, container);
                    return;
                }

                current = current.getSuperclass();
            }

            System.err.println("[NovaInventory] WARNING: Could not find AppEngSlot#setContainer for " + slot.getClass().getName());
        }
        catch (Throwable throwable) {
            System.err.println("[NovaInventory] Failed calling AE2 AppEngSlot#setContainer");
            throwable.printStackTrace();
        }
    }

    private static boolean isAE2SlotLocked(Container container, int slotIndex) {
        try {
            Field lockedField = findFieldInHierarchy(container.getClass(), "locked");

            if (lockedField == null) {
                return false;
            }

            lockedField.setAccessible(true);

            Object value = lockedField.get(container);

            if (value instanceof HashSet) {
                return ((HashSet)value).contains(Integer.valueOf(slotIndex));
            }

            if (value instanceof java.util.Set) {
                return ((java.util.Set)value).contains(Integer.valueOf(slotIndex));
            }
        }

        catch (Throwable ignored) {
        }

        return false;
    }

    private static Field findFieldInHierarchy(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;

        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            }
            catch (NoSuchFieldException ignored) {
            }
            current = current.getSuperclass();
        }

        return null;
    }

    private static boolean hasPlayerSlotRange(Container container, InventoryPlayer inventory, int startInclusive, int endExclusive) {
        for (int index = startInclusive; index < endExclusive; index++) {
            boolean found = false;

            for (int i = 0; i < container.inventorySlots.size(); i++) {
                Slot slot = (Slot)container.inventorySlots.get(i);

                if (slot.inventory == inventory && slot.getSlotIndex() == index) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                return false;
            }
        }

        return true;
    }

    private static boolean isAE2Container(Container container) {
        return isClassOrSuper(container.getClass(), "appeng.container.AEBaseContainer");
    }

    private static boolean isClassOrSuper(Class<?> clazz, String wantedName) {
        Class<?> current = clazz;

        while (current != null) {
            if (wantedName.equals(current.getName())) {
                return true;
            }
            current = current.getSuperclass();
        }

        return false;
    }
}