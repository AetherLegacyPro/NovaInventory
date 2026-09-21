package com.NovaInv;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraftforge.client.event.GuiOpenEvent;

@SideOnly(Side.CLIENT)
public final class NovaInventoryGuiHandler {

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        if (event.gui == null) {
            return;
        }

        EntityClientPlayerMP player = FMLClientHandler.instance().getClientPlayerEntity();
        if (player == null) {
            return;
        }

        if (event.gui instanceof GuiContainerCreative) {
            InventoryPageState.setPage(0);
            NovaInventory.NETWORK.sendToServer(new PacketInventoryPage(0, false));

            return;
        }

        if (event.gui.getClass() != GuiInventory.class) {
            return;
        }

        if (NovaPlayerBridge.isCreativeMode(player)) {
            NovaInventory.NETWORK.sendToServer(new PacketInventoryPage(0, false));

            return;
        }

        event.gui = new GuiInventoryOverwrite(player);
    }
}