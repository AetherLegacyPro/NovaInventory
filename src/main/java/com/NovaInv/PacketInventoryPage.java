package com.NovaInv;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;

public class PacketInventoryPage implements IMessage {
    private int page;

    public PacketInventoryPage() {
    }

    public PacketInventoryPage(int page) {
        if (page < 0)
        {
            page = 0;
        }

        if (page > 1)
        {
            page = 1;
        }

        this.page = page;
    }

    public void fromBytes(ByteBuf buf) {
        this.page = buf.readByte();

        if (this.page < 0)
        {
            this.page = 0;
        }

        if (this.page > 1)
        {
            this.page = 1;
        }
    }

    public void toBytes(ByteBuf buf) {
        buf.writeByte(this.page);
    }

    public static class Handler implements IMessageHandler<PacketInventoryPage, IMessage> {
        public IMessage onMessage(PacketInventoryPage message, MessageContext ctx)
        {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;

            InventoryPageServerState.setPage(player, message.page);

            return null;
        }
    }
}

