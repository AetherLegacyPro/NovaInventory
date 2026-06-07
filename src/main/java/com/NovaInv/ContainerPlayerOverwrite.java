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
import net.minecraft.item.crafting.CraftingManager;

public class ContainerPlayerOverwrite extends Container
{
    public InventoryCrafting craftMatrix = new InventoryCrafting(this, 2, 2);
    public IInventory craftResult = new InventoryCraftResult();

    public boolean isLocalWorld;

    private final EntityPlayer thePlayer;

     //Container slot layout:
     //--------------------------
     //0        crafting output
     //1 - 4    crafting grid
     //5 - 8    armor
     //9 - 62   expanded main inventory
     //63 - 71  hotbar

    private static final int CRAFT_RESULT_SLOT = 0;

    private static final int CRAFT_START = 1;
    private static final int CRAFT_END = 5;

    private static final int ARMOR_START = 5;
    private static final int ARMOR_END = 9;

    private static final int MAIN_INV_START = 9;
    private static final int MAIN_INV_ROWS = 6;
    private static final int MAIN_INV_COLUMNS = 9;
    private static final int MAIN_INV_SIZE = MAIN_INV_ROWS * MAIN_INV_COLUMNS;
    private static final int MAIN_INV_END = MAIN_INV_START + MAIN_INV_SIZE;

    private static final int HOTBAR_START = MAIN_INV_END;
    private static final int HOTBAR_SIZE = 9;
    private static final int HOTBAR_END = HOTBAR_START + HOTBAR_SIZE;

    private static final int PLAYER_INV_START = MAIN_INV_START;
    private static final int PLAYER_INV_END = HOTBAR_END;

    private static final int MAIN_INV_X = 8;
    private static final int MAIN_INV_Y = 84;

    private static final int HOTBAR_X = 8;
    private static final int HOTBAR_Y = MAIN_INV_Y + MAIN_INV_ROWS * 18 + 4;

    private static final int MAIN_INV_PAGE_SIZE = 27;

    private static final int MAIN_PAGE_0_START = MAIN_INV_START;
    private static final int MAIN_PAGE_0_END = MAIN_PAGE_0_START + 27;

    private static final int MAIN_PAGE_1_START = MAIN_PAGE_0_END;
    private static final int MAIN_PAGE_1_END = MAIN_INV_END;

    public ContainerPlayerOverwrite(final InventoryPlayer playerInventory, boolean localWorld, EntityPlayer player)
    {
        this.isLocalWorld = localWorld;
        this.thePlayer = player;

        int i;
        int j;

        //Crafting Output
        this.addSlotToContainer(new SlotCrafting(playerInventory.player, this.craftMatrix, this.craftResult, 0, 144, 36));

        //2x2 crafting grid
        for (i = 0; i < 2; ++i) {
            for (j = 0; j < 2; ++j)
            {
                this.addSlotToContainer(new Slot(this.craftMatrix, j + i * 2, 88 + j * 18, 26 + i * 18));
            }
        }

        //Armor Slots: nothing is expanded or altered about them
        for (i = 0; i < 4; ++i) {
            this.addSlotToContainer(new InventoryOverhaulArmorSlot(playerInventory, playerInventory.getSizeInventory() - 1 - i, 8, 8 + i * 18, i, player));
        }

        //Inventory: slots[9 - 62]
        for (i = 0; i < MAIN_INV_ROWS; ++i) {
            for (j = 0; j < MAIN_INV_COLUMNS; ++j) {
                this.addSlotToContainer(new Slot(playerInventory, j + (i + 1) * 9, MAIN_INV_X + j * 18, MAIN_INV_Y + i * 18));
            }
        }

         //Hotbar: slots[0 - 8]
        for (i = 0; i < HOTBAR_SIZE; ++i) {
            this.addSlotToContainer(new Slot(playerInventory, i, HOTBAR_X + i * 18, HOTBAR_Y));
        }

        this.onCraftMatrixChanged(this.craftMatrix);
    }

    public void onCraftMatrixChanged(IInventory inventory) {
        this.craftResult.setInventorySlotContents(0, CraftingManager.getInstance().findMatchingRecipe(this.craftMatrix, this.thePlayer.worldObj));
    }

    public void onContainerClosed(EntityPlayer player)
    {
        super.onContainerClosed(player);

        for (int i = 0; i < 4; ++i)
        {
            ItemStack itemstack = this.craftMatrix.getStackInSlotOnClosing(i);

            if (itemstack != null)
            {
                player.dropPlayerItemWithRandomChoice(itemstack, false);
            }
        }

        this.craftResult.setInventorySlotContents(0, null);
    }

    public boolean canInteractWith(EntityPlayer player)
    {
        return true;
    }

    public ItemStack transferStackInSlot(EntityPlayer player, int slotIndex)
    {
        ItemStack copiedStack = null;
        Slot slot = (Slot)this.inventorySlots.get(slotIndex);

        if (slot != null && slot.getHasStack())
        {
            ItemStack stackInSlot = slot.getStack();
            copiedStack = stackInSlot.copy();

            if (slotIndex == CRAFT_RESULT_SLOT)
            {
                if (!this.mergeIntoPreferredMainPageThenHotbar(stackInSlot, player))
                {
                    return null;
                }

                slot.onSlotChange(stackInSlot, copiedStack);
            }
            else if (slotIndex >= CRAFT_START && slotIndex < CRAFT_END)
            {
                if (!this.mergeIntoPreferredMainPageThenHotbar(stackInSlot, player))
                {
                    return null;
                }
            }
            else if (slotIndex >= ARMOR_START && slotIndex < ARMOR_END)
            {
                if (!this.mergeIntoPreferredMainPageThenHotbar(stackInSlot, player))
                {
                    return null;
                }
            }
            else if (stackInSlot.getItem() instanceof ItemArmor)
            {
                ItemArmor armor = (ItemArmor)stackInSlot.getItem();
                int targetArmorSlot = ARMOR_START + armor.armorType;

                if (!((Slot)this.inventorySlots.get(targetArmorSlot)).getHasStack())
                {
                    if (!this.mergeItemStack(stackInSlot, targetArmorSlot, targetArmorSlot + 1, false))
                    {
                        return null;
                    }
                }
                else if (slotIndex >= MAIN_INV_START && slotIndex < MAIN_INV_END)
                {
                    if (!this.mergeItemStack(stackInSlot, HOTBAR_START, HOTBAR_END, false))
                    {
                        return null;
                    }
                }
                else if (slotIndex >= HOTBAR_START && slotIndex < HOTBAR_END)
                {
                    if (!this.mergeIntoPreferredMainPage(stackInSlot, player))
                    {
                        return null;
                    }
                }
                else if (!this.mergeIntoPreferredMainPageThenHotbar(stackInSlot, player))
                {
                    return null;
                }
            }
            else if (slotIndex >= MAIN_INV_START && slotIndex < MAIN_INV_END)
            {
                if (!this.mergeItemStack(stackInSlot, HOTBAR_START, HOTBAR_END, false))
                {
                    return null;
                }
            }
            else if (slotIndex >= HOTBAR_START && slotIndex < HOTBAR_END)
            {
                //When shift clicking it tries to move the item/block into the page open first, if no slots are open try alternate page
                if (!this.mergeIntoPreferredMainPage(stackInSlot, player))
                {
                    return null;
                }
            }
            else if (!this.mergeItemStack(stackInSlot, PLAYER_INV_START, PLAYER_INV_END, false))
            {
                return null;
            }

            if (stackInSlot.stackSize == 0)
            {
                slot.putStack(null);
            }
            else
            {
                slot.onSlotChanged();
            }

            if (stackInSlot.stackSize == copiedStack.stackSize)
            {
                return null;
            }

            slot.onPickupFromSlot(player, stackInSlot);
        }

        return copiedStack;
    }

    public boolean func_94530_a(ItemStack stack, Slot slot)
    {
        return slot.inventory != this.craftResult && super.func_94530_a(stack, slot);
    }

    private boolean mergeIntoPreferredMainPage(ItemStack stack, EntityPlayer player)
    {
        boolean changed = false;

        int page = InventoryPageServerState.getPage(player);

        if (page <= 0)
        {
            //Try Page Open first otherwise try alternate one
            changed |= this.mergeItemStack(stack, MAIN_PAGE_0_START, MAIN_PAGE_0_END, false);

            if (stack.stackSize > 0)
            {
                changed |= this.mergeItemStack(stack, MAIN_PAGE_1_START, MAIN_PAGE_1_END, false);
            }
        }
        else
        {
            //Try Page Not Open first otherwise try alternate one
            changed |= this.mergeItemStack(stack, MAIN_PAGE_1_START, MAIN_PAGE_1_END, false);

            if (stack.stackSize > 0)
            {
                changed |= this.mergeItemStack(stack, MAIN_PAGE_0_START, MAIN_PAGE_0_END, false);
            }
        }

        return changed;
    }

    private boolean mergeIntoPreferredMainPageThenHotbar(ItemStack stack, EntityPlayer player)
    {
        boolean changed = false;

        changed |= this.mergeIntoPreferredMainPage(stack, player);

        if (stack.stackSize > 0)
        {
            changed |= this.mergeItemStack(stack, HOTBAR_START, HOTBAR_END, false);
        }

        return changed;
    }
}

