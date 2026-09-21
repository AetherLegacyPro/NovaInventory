package com.NovaInv;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.world.World;

//Pain I had to add due to ArchaicFix...
//Fixes the development and production environments naming differences
public final class NovaContainerBridge {

    private static final Field INVENTORY_ITEM_STACKS_FIELD = findField(Container.class, "inventoryItemStacks", "field_75153_a");

    private static final Field INVENTORY_SLOTS_FIELD = findField(Container.class, "inventorySlots", "field_75151_b");

    private static final Field SLOT_NUMBER_FIELD = findField(Slot.class, "slotNumber", "field_75222_d");

    private static final Field WORLD_FIELD = findField(Entity.class, "worldObj", "field_70170_p");

    private static Method craftingManagerGetter;
    private static Method recipeLookupMethod;
    private static Method setInventorySlotMethod;
    private static Method getStackOnClosingMethod;
    private static Method dropPlayerItemMethod;

    private NovaContainerBridge() {
    }

    public static Slot addSlot(Container container, Slot slot) {
        if (container == null) {
            throw new IllegalArgumentException("[NovaInventory] Cannot add a slot to a null container");
        }

        if (slot == null) {
            throw new IllegalArgumentException("[NovaInventory] Cannot add a null slot");
        }

        List slots = getInventorySlots(container);
        List trackedStacks = getInventoryItemStacks(container);

        setSlotNumber(slot, slots.size());

        slots.add(slot);
        trackedStacks.add(null);

        return slot;
    }

    public static Slot getSlot(Container container, int index) {
        if (container == null) {
            return null;
        }

        return getSlotFromList(getInventorySlots(container), index);
    }

    public static List getInventorySlots(Container container) {
        if (container == null) {
            throw new IllegalArgumentException("[NovaInventory] Cannot read slots from a null container");
        }

        try {
            return (List)INVENTORY_SLOTS_FIELD.get(container);
        }
        catch (IllegalAccessException exception) {
            throw new RuntimeException("[NovaInventory] Could not access Container.inventorySlots", exception);
        }
    }

    public static List getInventoryItemStacks(Container container) {
        if (container == null) {
            throw new IllegalArgumentException("[NovaInventory] Cannot read tracked stacks from a null container");
        }

        try {
            return (List)INVENTORY_ITEM_STACKS_FIELD.get(container);
        } catch (IllegalAccessException exception) {
            throw new RuntimeException("[NovaInventory] Could not access Container.inventoryItemStacks", exception);
        }
    }

    public static void setSlotNumber(Slot slot, int slotNumber) {
        if (slot == null) {
            throw new IllegalArgumentException("[NovaInventory] Cannot assign an ID to a null slot");
        }

        try {
            SLOT_NUMBER_FIELD.setInt(slot, slotNumber);
        } catch (IllegalAccessException exception) {
            throw new RuntimeException("[NovaInventory] Could not set Slot.slotNumber", exception);
        }
    }

    public static boolean mergeItemStack(Container container, ItemStack stack, int startIndex, int endIndex, boolean reverseDirection) {
        if (container == null || stack == null) {
            return false;
        }

        List slots = getInventorySlots(container);

        int safeStart = Math.max(0, startIndex);
        int safeEnd = Math.min(endIndex, slots.size());

        if (safeStart >= safeEnd) {
            return false;
        }

        boolean changed = false;
        int index = reverseDirection ? safeEnd - 1 : safeStart;

        if (stack.isStackable()) {
            while (stack.stackSize > 0 && isIndexInRange(index, safeStart, safeEnd, reverseDirection)) {
                Slot slot = getSlotFromList(slots, index);

                if (slot != null) {
                    ItemStack existing = slot.getStack();

                    if (canStacksMerge(stack, existing)) {
                        int maximumSize = Math.min(stack.getMaxStackSize(), slot.getSlotStackLimit());

                        int availableSpace = maximumSize - existing.stackSize;

                        if (availableSpace > 0) {
                            int amountToMove = Math.min(availableSpace, stack.stackSize);

                            existing.stackSize += amountToMove;
                            stack.stackSize -= amountToMove;

                            slot.onSlotChanged();
                            changed = true;
                        }
                    }
                }

                index += reverseDirection ? -1 : 1;
            }
        }

        if (stack.stackSize > 0) {
            index = reverseDirection ? safeEnd - 1 : safeStart;

            while (stack.stackSize > 0 && isIndexInRange(index, safeStart, safeEnd, reverseDirection)) {
                Slot slot = getSlotFromList(slots, index);

                if (slot != null && !slot.getHasStack() && slot.isItemValid(stack)) {
                    int amountToMove = Math.min(stack.stackSize, Math.min(stack.getMaxStackSize(), slot.getSlotStackLimit()));

                    if (amountToMove > 0) {
                        ItemStack placed = stack.copy();
                        placed.stackSize = amountToMove;

                        slot.putStack(placed);
                        slot.onSlotChanged();

                        stack.stackSize -= amountToMove;
                        changed = true;
                    }
                }

                index += reverseDirection ? -1 : 1;
            }
        }

        return changed;
    }

    public static ItemStack findMatchingRecipe(InventoryCrafting craftingMatrix, EntityPlayer player) {
        if (craftingMatrix == null || player == null) {
            return null;
        }

        try {
            World world = (World)WORLD_FIELD.get(player);

            if (craftingManagerGetter == null) {
                craftingManagerGetter = findCraftingManagerGetter();
                craftingManagerGetter.setAccessible(true);
            }

            Object manager = craftingManagerGetter.invoke(null);
            if (manager == null) {
                return null;
            }

            if (recipeLookupMethod == null) {
                recipeLookupMethod = findRecipeLookupMethod(manager.getClass());
                recipeLookupMethod.setAccessible(true);
            }

            return (ItemStack)recipeLookupMethod.invoke(manager, craftingMatrix, world);
        }
        catch (Throwable throwable) {
            throw new RuntimeException("[NovaInventory] Could not perform crafting lookup", throwable);
        }
    }

    public static void setInventorySlotContents(IInventory inventory, int slotIndex, ItemStack stack) {
        if (inventory == null) {
            return;
        }

        try {
            if (setInventorySlotMethod == null) {
                setInventorySlotMethod = findSetInventorySlotContentsMethod();
                setInventorySlotMethod.setAccessible(true);
            }

            setInventorySlotMethod.invoke(inventory, Integer.valueOf(slotIndex), stack);
        } catch (Throwable throwable) {
            throw new RuntimeException("[NovaInventory] Could not set inventory slot contents", throwable);
        }
    }

    public static ItemStack getStackInSlotOnClosing(IInventory inventory, int slotIndex) {
        if (inventory == null) {
            return null;
        }

        try {
            if (getStackOnClosingMethod == null) {
                getStackOnClosingMethod = findGetStackOnClosingMethod();
                getStackOnClosingMethod.setAccessible(true);
            }

            return (ItemStack)getStackOnClosingMethod.invoke(inventory, Integer.valueOf(slotIndex));
        }
        catch (Throwable throwable) {
            throw new RuntimeException("[NovaInventory] Could not call " + "IInventory.getStackInSlotOnClosing", throwable);
        }
    }

    public static void dropPlayerItem(EntityPlayer player, ItemStack stack, boolean randomChoice) {
        if (player == null || stack == null) {
            return;
        }

        try {
            if (dropPlayerItemMethod == null) {
                dropPlayerItemMethod = findDropPlayerItemMethod();
                dropPlayerItemMethod.setAccessible(true);
            }

            dropPlayerItemMethod.invoke(player, stack, Boolean.valueOf(randomChoice));
        }
        catch (Throwable throwable) {
            throw new RuntimeException("[NovaInventory] Could not call " + "EntityPlayer.dropPlayerItemWithRandomChoice", throwable);
        }
    }

    private static Method findCraftingManagerGetter() {
        Method[] methods = CraftingManager.class.getDeclaredMethods();

        for (Method method : methods) {
            String name = method.getName();

            if (("getInstance".equals(name) || "func_77594_a".equals(name)) && isCraftingManagerGetter(method)) {
                return method;
            }
        }

        for (Method method : methods) {
            if (isCraftingManagerGetter(method)) {
                return method;
            }
        }

        throw new RuntimeException("[NovaInventory] Could not find " + "CraftingManager singleton getter");
    }

    private static boolean isCraftingManagerGetter(Method method) {
        return Modifier.isStatic(method.getModifiers()) && method.getParameterTypes().length == 0 && CraftingManager.class.isAssignableFrom(method.getReturnType());
    }

    private static Method findRecipeLookupMethod(Class<?> managerClass) {
        Method[] methods = managerClass.getDeclaredMethods();

        for (Method method : methods) {
            String name = method.getName();

            if (("findMatchingRecipe".equals(name) || "func_82787_a".equals(name)) && isRecipeLookupMethod(method)) {
                return method;
            }
        }

        for (Method method : methods) {
            if (isRecipeLookupMethod(method)) {
                return method;
            }
        }

        throw new RuntimeException("[NovaInventory] Could not find " + "CraftingManager recipe lookup");
    }

    private static boolean isRecipeLookupMethod(Method method) {
        Class<?>[] parameters = method.getParameterTypes();

        return !Modifier.isStatic(method.getModifiers()) && parameters.length == 2 && InventoryCrafting.class.isAssignableFrom(parameters[0]) && World.class.isAssignableFrom(parameters[1]) && ItemStack.class.isAssignableFrom(method.getReturnType());
    }

    private static Method findSetInventorySlotContentsMethod() {
        Method[] methods = IInventory.class.getMethods();

        for (Method method : methods) {
            String name = method.getName();

            if (("setInventorySlotContents".equals(name) || "func_70299_a".equals(name)) && isSetInventorySlotContentsMethod(method)) {
                return method;
            }
        }

        for (Method method : methods) {
            if (isSetInventorySlotContentsMethod(method)) {
                return method;
            }
        }

        throw new RuntimeException("[NovaInventory] Could not find " + "IInventory.setInventorySlotContents");
    }

    private static boolean isSetInventorySlotContentsMethod(Method method) {
        Class<?>[] parameters = method.getParameterTypes();

        return method.getReturnType() == void.class && parameters.length == 2 && parameters[0] == int.class && ItemStack.class.isAssignableFrom(parameters[1]);
    }

    private static Method findGetStackOnClosingMethod() {
        Method[] methods = IInventory.class.getMethods();

        for (Method method : methods) {
            String name = method.getName();

            if (("getStackInSlotOnClosing".equals(name) || "func_70304_b".equals(name)) && isGetStackOnClosingMethod(method)) {
                return method;
            }
        }

        for (Method method : methods) {
            if (isGetStackOnClosingMethod(method)) {
                return method;
            }
        }

        throw new RuntimeException("[NovaInventory] Could not find " + "IInventory.getStackInSlotOnClosing");
    }

    private static boolean isGetStackOnClosingMethod(Method method) {
        Class<?>[] parameters = method.getParameterTypes();

        return parameters.length == 1 && parameters[0] == int.class && ItemStack.class.isAssignableFrom(method.getReturnType());
    }

    private static Method findDropPlayerItemMethod() {
        Method[] methods = EntityPlayer.class.getMethods();

        for (Method method : methods) {
            String name = method.getName();

            if (("dropPlayerItemWithRandomChoice".equals(name) || "func_146097_a".equals(name)) && isDropPlayerItemMethod(method)) {
                return method;
            }
        }

        for (Method method : methods) {
            if (isDropPlayerItemMethod(method)) {
                return method;
            }
        }

        throw new RuntimeException("[NovaInventory] Could not find " + "EntityPlayer.dropPlayerItemWithRandomChoice");
    }

    private static boolean isDropPlayerItemMethod(Method method) {
        Class<?>[] parameters = method.getParameterTypes();

        return !Modifier.isStatic(method.getModifiers()) && parameters.length == 2 && ItemStack.class.isAssignableFrom(parameters[0]) && parameters[1] == boolean.class;
    }

    private static Slot getSlotFromList(List slots, int index) {
        if (slots == null || index < 0 || index >= slots.size()) {
            return null;
        }

        Object value = slots.get(index);

        return value instanceof Slot ? (Slot)value : null;
    }

    private static boolean isIndexInRange(int index, int startIndex, int endIndex, boolean reverseDirection) {
        return reverseDirection ? index >= startIndex : index < endIndex;
    }

    private static boolean canStacksMerge(ItemStack source, ItemStack destination) {
        return source != null && destination != null && source.getItem() == destination.getItem() && (!source.getHasSubtypes() || source.getItemDamage() == destination.getItemDamage()) && ItemStack.areItemStackTagsEqual(source, destination);
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

        throw new RuntimeException("[NovaInventory] Could not find fields [" + joinNames(names) + "] in " + owner.getName());
    }

    private static String joinNames(String[] names) {
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < names.length; ++i) {
            if (i > 0) {
                result.append(", ");
            }

            result.append(names[i]);
        }

        return result.toString();
    }

    public static boolean mergePlayerInventoryHotbarFirst(Container container, ItemStack stack, int startIndex, int endIndex, boolean allowSecondPage) {
        if (container == null || stack == null) {
            return false;
        }

        List containerSlots = getInventorySlots(container);

        int safeStart = Math.max(0, startIndex);
        int safeEnd = Math.min(endIndex, containerSlots.size());

        if (safeStart >= safeEnd) {
            return false;
        }

        List<Slot> orderedSlots = new ArrayList<Slot>();

        addPlayerSlotsByInventoryIndex(containerSlots, orderedSlots, safeStart, safeEnd, 0, 9);
        addPlayerSlotsByInventoryIndex(containerSlots, orderedSlots, safeStart, safeEnd, 9,36);

        if (allowSecondPage) {
            addPlayerSlotsByInventoryIndex(containerSlots, orderedSlots, safeStart, safeEnd, 36, 63);
        }

        if (orderedSlots.isEmpty()) {
            return false;
        }

        return mergeIntoOrderedSlots(stack, orderedSlots);
    }

    public static int tryMergePlayerInventoryRange(Container container, ItemStack stack, int startIndex, int endIndex, boolean reverseDirection) {
        if (container == null || stack == null) {
            return -1;
        }

        List slots = getInventorySlots(container);

        int safeStart = Math.max(0, startIndex);
        int safeEnd = Math.min(endIndex, slots.size());

        if (safeStart >= safeEnd) {
            return -1;
        }

        boolean foundPlayerSlot = false;

        for (int index = safeStart; index < safeEnd; ++index) {
            Object object = slots.get(index);

            if (!(object instanceof Slot)) {
                return -1;
            }

            Slot slot = (Slot)object;
            if (!(slot.inventory instanceof InventoryPlayer)) {
                return -1;
            }

            foundPlayerSlot = true;
        }

        if (!foundPlayerSlot) {
            return -1;
        }

        boolean moved = mergePlayerInventoryHotbarFirst(container, stack, safeStart, safeEnd, true);

        return moved ? 1 : 0;
    }

    private static void addPlayerSlotsByInventoryIndex(List containerSlots, List<Slot> output, int containerStart, int containerEnd, int inventoryStart, int inventoryEnd) {
        for (int wantedIndex = inventoryStart; wantedIndex < inventoryEnd; ++wantedIndex) {
            for (int containerIndex = containerStart; containerIndex < containerEnd; ++containerIndex) {
                Object object = containerSlots.get(containerIndex);

                if (!(object instanceof Slot)) {
                    continue;
                }

                Slot slot = (Slot)object;
                if (slot.inventory instanceof InventoryPlayer && slot.getSlotIndex() == wantedIndex) {
                    output.add(slot);
                    break;
                }
            }
        }
    }

    private static boolean mergeIntoOrderedSlots(ItemStack stack, List<Slot> orderedSlots) {
        boolean changed = false;

        if (stack.isStackable()) {
            for (Slot slot : orderedSlots) {
                if (stack.stackSize <= 0) {
                    break;
                }

                ItemStack existing = slot.getStack();
                if (!canStacksMerge(stack, existing)) {
                    continue;
                }

                int maximumSize = Math.min(stack.getMaxStackSize(), slot.getSlotStackLimit());

                int availableSpace = maximumSize - existing.stackSize;
                if (availableSpace <= 0) {
                    continue;
                }

                int amountToMove = Math.min(availableSpace, stack.stackSize);

                existing.stackSize += amountToMove;
                stack.stackSize -= amountToMove;

                slot.onSlotChanged();
                changed = true;
            }
        }

        for (Slot slot : orderedSlots) {
            if (stack.stackSize <= 0) {
                break;
            }

            if (slot.getHasStack() || !slot.isItemValid(stack)) {
                continue;
            }

            int amountToMove = Math.min(stack.stackSize, Math.min(stack.getMaxStackSize(), slot.getSlotStackLimit()));
            if (amountToMove <= 0) {
                continue;
            }

            ItemStack placed = stack.copy();
            placed.stackSize = amountToMove;

            slot.putStack(placed);
            slot.onSlotChanged();

            stack.stackSize -= amountToMove;
            changed = true;
        }

        return changed;
    }

}