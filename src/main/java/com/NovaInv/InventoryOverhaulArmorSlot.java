package com.NovaInv;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;

public class InventoryOverhaulArmorSlot extends Slot
{
    private final int armorSlot;
    private final EntityPlayer player;

    public InventoryOverhaulArmorSlot(InventoryPlayer inventory, int slotIndex, int x, int y, int armorSlot, EntityPlayer player) {
        super(inventory, slotIndex, x, y);
        this.armorSlot = armorSlot;
        this.player = player;
    }

    public int getSlotStackLimit() {
        return 1;
    }

    public boolean isItemValid(ItemStack stack) {
        if (stack == null)
        {
            return false;
        }

        return stack.getItem().isValidArmor(stack, this.armorSlot, this.player);
    }

    @SideOnly(Side.CLIENT)
    public IIcon getBackgroundIconIndex() {
        return ItemArmor.func_94602_b(this.armorSlot);
    }
}
