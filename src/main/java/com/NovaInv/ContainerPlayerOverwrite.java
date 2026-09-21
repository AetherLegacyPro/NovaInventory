package com.NovaInv;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCraftResult;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.inventory.SlotCrafting;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;

public class ContainerPlayerOverwrite extends Container {
    public InventoryCrafting craftMatrix = new InventoryCrafting(this, 2, 2);
    public IInventory craftResult = new InventoryCraftResult();

    public boolean isLocalWorld;

    private final EntityPlayer thePlayer;

     //Container slot layout:
     //--------------------------
     // 0       crafting output
     // 1-4     crafting grid
     // 5-8     armor
     // 9-35    inventory page 0
     // 36-44   hotbar
     // 45-71   inventory page 1

    private static final int CRAFT_RESULT_SLOT = 0;

    private static final int CRAFT_START = 1;
    private static final int CRAFT_END = 5;

    private static final int ARMOR_START = 5;
    private static final int ARMOR_END = 9;

    private static final int MAIN_PAGE_0_START = 9;
    private static final int MAIN_PAGE_0_END = 36;

    private static final int HOTBAR_START = 36;
    private static final int HOTBAR_SIZE = 9;
    private static final int HOTBAR_END = 45;

    private static final int MAIN_PAGE_1_START = 45;
    private static final int MAIN_PAGE_1_END = 72;

    private static final int PLAYER_MAIN_INVENTORY_SIZE = 63;
    private static final int PLAYER_ARMOR_SLOT_COUNT = 4;

    private static final int MAIN_INV_X = 8;
    private static final int MAIN_INV_Y = 84;

    private static final int HOTBAR_X = 8;
    private static final int HOTBAR_Y = 142;

    private static final int HIDDEN_SLOT_X = -10000;
    private static final int HIDDEN_SLOT_Y = -10000;

    public ContainerPlayerOverwrite(final InventoryPlayer playerInventory, boolean localWorld, EntityPlayer player) {
        this.isLocalWorld = localWorld;
        this.thePlayer = player;

        int i;
        int j;

        //Crafting Output
        NovaContainerBridge.addSlot(this, new SlotCrafting(this.thePlayer, this.craftMatrix, this.craftResult, 0, 144, 36));

        //2x2 crafting grid
        for (i = 0; i < 2; ++i) {
            for (j = 0; j < 2; ++j)
            {
                NovaContainerBridge.addSlot(this,new Slot(this.craftMatrix, j + i * 2, 88 + j * 18, 26 + i * 18));
            }
        }

        //Armor Slots: nothing is expanded or altered about them
        for (i = 0; i < PLAYER_ARMOR_SLOT_COUNT; ++i) {
            int armorInventoryIndex = PLAYER_MAIN_INVENTORY_SIZE + PLAYER_ARMOR_SLOT_COUNT - 1 - i;
            NovaContainerBridge.addSlot(this, new InventoryOverhaulArmorSlot(playerInventory, armorInventoryIndex, 8, 8 + i * 18, i, player));
        }

        //Inventory slots for page 1
        for (i = 0; i < 3; ++i) {
            for (j = 0; j < 9; ++j) {
                int inventoryIndex = 9 + i * 9 + j;
                NovaContainerBridge.addSlot(this, new Slot(playerInventory, inventoryIndex, MAIN_INV_X + j * 18, MAIN_INV_Y + i * 18));
            }
        }

        //Inventory slots for page 2
        for (i = 0; i < HOTBAR_SIZE; ++i) {
            NovaContainerBridge.addSlot(this, new Slot(playerInventory, i, HOTBAR_X + i * 18, HOTBAR_Y));
        }

        for (i = 0; i < 3; ++i) {
            for (j = 0; j < 9; ++j) {
                int inventoryIndex = 36 + i * 9 + j;
                NovaContainerBridge.addSlot(this, new Slot(playerInventory, inventoryIndex, HIDDEN_SLOT_X, HIDDEN_SLOT_Y));
            }
        }

        this.onCraftMatrixChanged(this.craftMatrix);
    }

    public void onCraftMatrixChanged(IInventory inventory) {
        ItemStack result = NovaContainerBridge.findMatchingRecipe(this.craftMatrix, this.thePlayer);
        NovaContainerBridge.setInventorySlotContents(this.craftResult, 0, result);
    }

    public void onContainerClosed(EntityPlayer player) {
        for (int i = 0; i < 4; ++i) {
            ItemStack stack = NovaContainerBridge.getStackInSlotOnClosing(this.craftMatrix, i);

            if (stack != null) {
                NovaContainerBridge.dropPlayerItem(player, stack, false);
            }
        }

        NovaContainerBridge.setInventorySlotContents(this.craftResult, 0, null);
    }

    public boolean canInteractWith(EntityPlayer player) {
        return true;
    }

    public ItemStack transferStackInSlot(EntityPlayer player, int slotIndex) {
        ItemStack copiedStack = null;
        Slot slot = NovaContainerBridge.getSlot(this, slotIndex);

        if (slot != null && slot.getHasStack()) {
            ItemStack stackInSlot = slot.getStack();
            copiedStack = stackInSlot.copy();

            if (slotIndex == CRAFT_RESULT_SLOT) {
                if (!this.mergeIntoPreferredMainPageThenHotbar(stackInSlot, player)) {
                    return null;
                }

                slot.onSlotChange(stackInSlot, copiedStack);
            }
            else if (slotIndex >= CRAFT_START && slotIndex < CRAFT_END) {
                if (!this.mergeIntoPreferredMainPageThenHotbar(stackInSlot, player)) {
                    return null;
                }
            }
            else if (slotIndex >= ARMOR_START && slotIndex < ARMOR_END) {
                if (!this.mergeIntoPreferredMainPageThenHotbar(stackInSlot, player)) {
                    return null;
                }
            }
            else if (stackInSlot.getItem() instanceof ItemArmor) {
                ItemArmor armor = (ItemArmor)stackInSlot.getItem();
                int targetArmorSlot = ARMOR_START + armor.armorType;
                Slot armorSlot = NovaContainerBridge.getSlot(this, targetArmorSlot);

                if (armorSlot != null && !armorSlot.getHasStack()) {
                    if (!NovaContainerBridge.mergeItemStack(this,stackInSlot, targetArmorSlot, targetArmorSlot + 1, false)) {
                        return null;
                    }
                }
                else if (this.isMainInventorySlot(slotIndex)) {
                    if (!NovaContainerBridge.mergeItemStack(this,stackInSlot, HOTBAR_START, HOTBAR_END, false)) {
                        return null;
                    }
                }
                else if (this.isHotbarSlot(slotIndex)) {
                    if (!this.mergeIntoPreferredMainPage(stackInSlot, player)) {
                        return null;
                    }
                }
                else if (!this.mergeIntoPreferredMainPageThenHotbar(stackInSlot, player)) {
                    return null;
                }
            }
            else if (this.isMainInventorySlot(slotIndex)) {
                if (!NovaContainerBridge.mergeItemStack(this,stackInSlot, HOTBAR_START, HOTBAR_END, false)) {
                    return null;
                }
            }
            else if (this.isHotbarSlot(slotIndex)) {
                //When shift clicking it tries to move the item/block into the page open first, if no slots are open try alternate page
                if (!this.mergeIntoPreferredMainPage(stackInSlot, player)) {
                    return null;
                }
            }

            else if (!this.mergeIntoPreferredMainPageThenHotbar(stackInSlot, player)) {
                return null;
            }

            if (stackInSlot.stackSize == 0) {
                slot.putStack(null);
            } else {
                slot.onSlotChanged();
            }

            if (stackInSlot.stackSize == copiedStack.stackSize) {
                return null;
            }

            slot.onPickupFromSlot(player, stackInSlot);
        }

        return copiedStack;
    }

    public boolean func_94530_a(ItemStack stack, Slot slot) {
        return slot.inventory != this.craftResult;
    }

    private boolean mergeIntoPreferredMainPage(ItemStack stack, EntityPlayer player) {
        int page = InventoryPageServerState.getPage(player);

        /*
         * In creative, never silently move an item into the hidden page.
         *
         * If page 0 is visible, use page 0 only.
         * If Nova's page 1 is visibly open, use page 1 only.
         *
         * With vanilla/ArchaicFix creative, the page-state packet should set
         * page to 0, so only page 0 can receive shift-clicked hotbar items.
         */
        //Stops moving items/blocks into a hidden page
        if (NovaPlayerBridge.isCreativeMode(player)) {
            if (page == 1 && InventoryPageServerState.isCreativeSecondPageEnabled(player)) {
                return NovaContainerBridge.mergeItemStack(this, stack, MAIN_PAGE_1_START, MAIN_PAGE_1_END, false);
            }

            return NovaContainerBridge.mergeItemStack(this, stack, MAIN_PAGE_0_START, MAIN_PAGE_0_END, false);
        }
        boolean changed = false;

        if (page <= 0) {
            changed |= NovaContainerBridge.mergeItemStack(this, stack, MAIN_PAGE_0_START, MAIN_PAGE_0_END, false);

            if (stack.stackSize > 0) {
                changed |= NovaContainerBridge.mergeItemStack(this, stack, MAIN_PAGE_1_START, MAIN_PAGE_1_END, false);
            }
        } else {
            changed |= NovaContainerBridge.mergeItemStack(this, stack, MAIN_PAGE_1_START, MAIN_PAGE_1_END, false);
            if (stack.stackSize > 0) {
                changed |= NovaContainerBridge.mergeItemStack(this, stack, MAIN_PAGE_0_START, MAIN_PAGE_0_END, false);
            }
        }

        return changed;
    }

    private boolean isMainInventorySlot(int slotIndex) {
        return (slotIndex >= MAIN_PAGE_0_START && slotIndex < MAIN_PAGE_0_END) || (slotIndex >= MAIN_PAGE_1_START && slotIndex < MAIN_PAGE_1_END);
    }

    private boolean isHotbarSlot(int slotIndex) {
        return slotIndex >= HOTBAR_START && slotIndex < HOTBAR_END;
    }

    private boolean mergeIntoPreferredMainPageThenHotbar(ItemStack stack, EntityPlayer player) {
        boolean changed = this.mergeIntoPreferredMainPage(stack, player);

        if (stack.stackSize > 0) {
            changed |= NovaContainerBridge.mergeItemStack(this, stack, HOTBAR_START, HOTBAR_END, false);
        }

        return changed;
    }
}

