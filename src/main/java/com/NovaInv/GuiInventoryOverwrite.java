package com.NovaInv;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.achievement.GuiAchievements;
import net.minecraft.client.gui.achievement.GuiStats;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.renderer.InventoryEffectRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

@SideOnly(Side.CLIENT)
public class GuiInventoryOverwrite extends InventoryEffectRenderer
{
    private static final ResourceLocation VANILLA_INVENTORY_TEXTURE = new ResourceLocation("textures/gui/container/inventory.png");

    private float xSizeFloat;
    private float ySizeFloat;

    //Vanilla inventory size.
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;

    //Define Container slot IDs.
    private static final int MAIN_INV_CONTAINER_START = 9;
    private static final int MAIN_INV_CONTAINER_SIZE = 54;
    private static final int HOTBAR_CONTAINER_START = 63;
    private static final int HOTBAR_CONTAINER_SIZE = 9;

    //Only 27 main inventory slots are visible at once as to avoid compatibility issues with every other GUI in existence.
    private static final int VISIBLE_MAIN_SLOTS = 27;
    private static final int PAGE_COUNT = 2;

    //Slot positions
    private static final int MAIN_INV_X = 8;
    private static final int MAIN_INV_Y = 84;

    private static final int HOTBAR_X = 8;
    private static final int HOTBAR_Y = 142;

    private static final int HIDDEN_SLOT_X = -10000;
    private static final int HIDDEN_SLOT_Y = -10000;

    //Thin Scrollbar, well it is more of a button
    private static final int SCROLLBAR_X = 7;
    private static final int SCROLLBAR_Y = 138;
    private static final int SCROLLBAR_WIDTH = 162;
    private static final int SCROLLBAR_HEIGHT = 3;

    private static final int SCROLLBAR_CLICK_Y_PADDING = 3;

    private int inventoryPage = 0;
    private boolean draggingScrollbar = false;

    public GuiInventoryOverwrite(EntityPlayer player) {
        super(player.inventoryContainer);

        this.allowUserInput = true;

        this.xSize = GUI_WIDTH;
        this.ySize = GUI_HEIGHT;

        //Save the last selected inventory page
        this.inventoryPage = InventoryPageState.getPage();
    }

    public void updateScreen()
    {
        if (this.mc.playerController.isInCreativeMode())
        {
            this.mc.displayGuiScreen(new GuiContainerCreative(this.mc.thePlayer));
        }
    }

    public void initGui()
    {
        this.inventoryPage = InventoryPageState.getPage();
        NovaInventory.NETWORK.sendToServer(new PacketInventoryPage(this.inventoryPage));
        this.updateScrolledInventorySlots();

        this.buttonList.clear();

        if (this.mc.playerController.isInCreativeMode())
        {
            this.mc.displayGuiScreen(new GuiContainerCreative(this.mc.thePlayer));
        }
        else
        {
            super.initGui();

            this.inventoryPage = InventoryPageState.getPage();

            this.updateScrolledInventorySlots();
        }
    }

    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        this.fontRendererObj.drawString(I18n.format("container.crafting", new Object[0]), 86, 16, 4210752);
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.updateScrolledInventorySlots();

        super.drawScreen(mouseX, mouseY, partialTicks);

        this.xSizeFloat = (float)mouseX;
        this.ySizeFloat = (float)mouseY;
    }

    //Vanilla class
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY)
    {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        this.mc.getTextureManager().bindTexture(VANILLA_INVENTORY_TEXTURE);

        int left = this.guiLeft;
        int top = this.guiTop;

        this.drawTexturedModalRect(left, top, 0, 0, this.xSize, this.ySize);
        this.drawInventoryScrollbar(left, top);

        func_147046_a(left + 51, top + 75, 30, (float)(left + 51) - this.xSizeFloat, (float)(top + 75 - 50) - this.ySizeFloat, this.mc.thePlayer);
    }

    private void updateScrolledInventorySlots() {
        //Which inventory slots should be visible?
        int visibleStart = MAIN_INV_CONTAINER_START + this.inventoryPage * VISIBLE_MAIN_SLOTS;
        int visibleEnd = visibleStart + VISIBLE_MAIN_SLOTS;

        for (int containerSlot = MAIN_INV_CONTAINER_START;
             containerSlot < MAIN_INV_CONTAINER_START + MAIN_INV_CONTAINER_SIZE;
             containerSlot++)
        {
            Slot slot = (Slot)this.inventorySlots.inventorySlots.get(containerSlot);

            if (containerSlot >= visibleStart && containerSlot < visibleEnd)
            {
                int visibleIndex = containerSlot - visibleStart;
                int col = visibleIndex % 9;
                int row = visibleIndex / 9;

                slot.xDisplayPosition = MAIN_INV_X + col * 18;
                slot.yDisplayPosition = MAIN_INV_Y + row * 18;
            }
            else
            {
                slot.xDisplayPosition = HIDDEN_SLOT_X;
                slot.yDisplayPosition = HIDDEN_SLOT_Y;
            }
        }

        //Hotbar's position is never altered
        for (int i = 0; i < HOTBAR_CONTAINER_SIZE; i++)
        {
            Slot slot = (Slot)this.inventorySlots.inventorySlots.get(HOTBAR_CONTAINER_START + i);
            slot.xDisplayPosition = HOTBAR_X + i * 18;
            slot.yDisplayPosition = HOTBAR_Y;
        }
    }

    private void drawInventoryScrollbar(int left, int top) {
        int trackX = left + SCROLLBAR_X;
        int trackY = top + SCROLLBAR_Y;

        drawRect(trackX, trackY, trackX + SCROLLBAR_WIDTH, trackY + SCROLLBAR_HEIGHT, 0xFF202020);

        int thumbWidth = SCROLLBAR_WIDTH / PAGE_COUNT;
        int maxThumbTravel = SCROLLBAR_WIDTH - thumbWidth;

        int thumbX = trackX;

        if (PAGE_COUNT > 1)
        {
            thumbX = trackX + (maxThumbTravel * this.inventoryPage) / (PAGE_COUNT - 1);
        }

        drawRect(thumbX, trackY - 1, thumbX + thumbWidth, trackY + SCROLLBAR_HEIGHT + 1, 0xFFAAAAAA);
        drawRect(thumbX + 1, trackY, thumbX + thumbWidth - 1, trackY + SCROLLBAR_HEIGHT, 0xFFFFFFFF);
    }

    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0 && this.isMouseOverScrollbar(mouseX, mouseY))
        {
            this.draggingScrollbar = true;
            this.setPageFromMouse(mouseX);
            this.updateScrolledInventorySlots();
            return;
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick)
    {
        if (this.draggingScrollbar)
        {
            this.setPageFromMouse(mouseX);
            this.updateScrolledInventorySlots();
            return;
        }

        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    protected void mouseMovedOrUp(int mouseX, int mouseY, int state)
    {
        if (state == 0)
        {
            this.draggingScrollbar = false;
        }

        super.mouseMovedOrUp(mouseX, mouseY, state);
    }

    public void handleMouseInput()
    {
        super.handleMouseInput();

        int wheel = Mouse.getEventDWheel();

        if (wheel != 0)
        {
            int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
            int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;

            if (this.isMouseInsideInventory(mouseX, mouseY))
            {
                if (wheel < 0)
                {
                    this.setInventoryPage(this.inventoryPage + 1);
                }
                else
                {
                    this.setInventoryPage(this.inventoryPage - 1);
                }

                this.updateScrolledInventorySlots();
            }
        }
    }

    private boolean isMouseInsideInventory(int mouseX, int mouseY)
    {
        int left = this.guiLeft;
        int top = this.guiTop;

        return mouseX >= left && mouseX < left + this.xSize && mouseY >= top && mouseY < top + this.ySize;
    }

    private boolean isMouseOverScrollbar(int mouseX, int mouseY)
    {
        int left = this.guiLeft;
        int top = this.guiTop;

        int trackX = left + SCROLLBAR_X;
        int trackY = top + SCROLLBAR_Y;

        return mouseX >= trackX && mouseX < trackX + SCROLLBAR_WIDTH && mouseY >= trackY - SCROLLBAR_CLICK_Y_PADDING && mouseY < trackY + SCROLLBAR_HEIGHT + SCROLLBAR_CLICK_Y_PADDING;
    }

    private void setPageFromMouse(int mouseX) {
        int left = this.guiLeft;
        int trackX = left + SCROLLBAR_X;

        int relativeX = mouseX - trackX;

        if (relativeX < 0)
        {
            relativeX = 0;
        }

        if (relativeX > SCROLLBAR_WIDTH)
        {
            relativeX = SCROLLBAR_WIDTH;
        }

        int page;

        if (relativeX < SCROLLBAR_WIDTH / 2)
        {
            page = 0;
        }
        else
        {
            page = 1;
        }

        this.setInventoryPage(page);
    }

    private void setInventoryPage(int page) {
        if (page < 0)
        {
            page = 0;
        }

        if (page >= PAGE_COUNT)
        {
            page = PAGE_COUNT - 1;
        }

        this.inventoryPage = page;

        InventoryPageState.setPage(page);

        //Sync the page last opening so shift clicking is possible
        NovaInventory.NETWORK.sendToServer(new PacketInventoryPage(page));
    }

    public void onGuiClosed() {
        InventoryPageState.setPage(this.inventoryPage);
        super.onGuiClosed();
    }

    public static void func_147046_a(int p_147046_0_, int p_147046_1_, int p_147046_2_, float p_147046_3_, float p_147046_4_, EntityLivingBase p_147046_5_) {
        GL11.glEnable(GL11.GL_COLOR_MATERIAL);
        GL11.glPushMatrix();
        GL11.glTranslatef((float)p_147046_0_, (float)p_147046_1_, 50.0F);
        GL11.glScalef((float)(-p_147046_2_), (float)p_147046_2_, (float)p_147046_2_);
        GL11.glRotatef(180.0F, 0.0F, 0.0F, 1.0F);

        float f2 = p_147046_5_.renderYawOffset;
        float f3 = p_147046_5_.rotationYaw;
        float f4 = p_147046_5_.rotationPitch;
        float f5 = p_147046_5_.prevRotationYawHead;
        float f6 = p_147046_5_.rotationYawHead;

        GL11.glRotatef(135.0F, 0.0F, 1.0F, 0.0F);
        RenderHelper.enableStandardItemLighting();
        GL11.glRotatef(-135.0F, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(-((float)Math.atan((double)(p_147046_4_ / 40.0F))) * 20.0F, 1.0F, 0.0F, 0.0F);

        p_147046_5_.renderYawOffset = (float)Math.atan((double)(p_147046_3_ / 40.0F)) * 20.0F;
        p_147046_5_.rotationYaw = (float)Math.atan((double)(p_147046_3_ / 40.0F)) * 40.0F;
        p_147046_5_.rotationPitch = -((float)Math.atan((double)(p_147046_4_ / 40.0F))) * 20.0F;
        p_147046_5_.rotationYawHead = p_147046_5_.rotationYaw;
        p_147046_5_.prevRotationYawHead = p_147046_5_.rotationYaw;

        GL11.glTranslatef(0.0F, p_147046_5_.yOffset, 0.0F);

        RenderManager.instance.playerViewY = 180.0F;
        RenderManager.instance.renderEntityWithPosYaw(p_147046_5_, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F);

        p_147046_5_.renderYawOffset = f2;
        p_147046_5_.rotationYaw = f3;
        p_147046_5_.rotationPitch = f4;
        p_147046_5_.prevRotationYawHead = f5;
        p_147046_5_.rotationYawHead = f6;

        GL11.glPopMatrix();

        RenderHelper.disableStandardItemLighting();

        GL11.glDisable(GL12.GL_RESCALE_NORMAL);

        OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
    }

    protected void actionPerformed(GuiButton button) {
        if (button.id == 0) {
            this.mc.displayGuiScreen(new GuiAchievements(this, this.mc.thePlayer.getStatFileWriter()));
        }

        if (button.id == 1) {
            this.mc.displayGuiScreen(new GuiStats(this, this.mc.thePlayer.getStatFileWriter()));
        }
    }
}
