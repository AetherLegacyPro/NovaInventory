package com.NovaInv;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;

public class PacketInventoryPage implements IMessage {

    private int page;
    private boolean creativeSecondPageEnabled;

    public PacketInventoryPage() {
    }

    public PacketInventoryPage(int page) {
        this(page, false);
    }

    public PacketInventoryPage(int page, boolean creativeSecondPageEnabled) {
        this.page = page == 1 ? 1 : 0;
        this.creativeSecondPageEnabled = creativeSecondPageEnabled;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        this.page = buffer.readUnsignedByte() == 1 ? 1 : 0;
        this.creativeSecondPageEnabled = buffer.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeByte(this.page);
        buffer.writeBoolean(this.creativeSecondPageEnabled);
    }

    public static class Handler implements IMessageHandler<PacketInventoryPage, IMessage> {

        @Override
        public IMessage onMessage(PacketInventoryPage message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().playerEntity;

            InventoryPageServerState.setPage(player, message.page);
            InventoryPageServerState.setCreativeSecondPageEnabled(player, message.creativeSecondPageEnabled);

            return null;
        }
    }
}