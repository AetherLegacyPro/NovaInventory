package com.NovaInv;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.achievement.GuiAchievements;
import net.minecraft.client.gui.achievement.GuiStats;
import net.minecraft.client.gui.inventory.CreativeCrafting;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.InventoryEffectRenderer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

@SideOnly(Side.CLIENT)
public class GuiContainerCreativeOverwrite extends InventoryEffectRenderer {
    private static final ResourceLocation field_147061_u = new ResourceLocation("textures/gui/container/creative_inventory/tabs.png");

    //Most of the calls and variables here are copied directly from the vanilla class
    //Slight modifications were made to add the other slots and logic to the creative inventory

    private static InventoryBasic field_147060_v = new InventoryBasic("tmp", true, 45);

    //Currently selected creative inventory tab
    private static int selectedTabIndex = CreativeTabs.tabBlock.getTabIndex();

    //Amount scrolled in Creative mode inventory.
    private float currentScroll;

    //True if the scrollbar is being dragged.
    private boolean isScrolling;

    //True if the left mouse button was held down last time drawScreen was called.
    private boolean wasClicking;

    private GuiTextField searchField;
    private List field_147063_B;
    private Slot field_147064_C;
    private boolean field_147057_D;
    private CreativeCrafting field_147059_E;

    private static int tabPage = 0;
    private int maxPages = 0;

    //Container slot layout:
    //--------------------------
    //0        crafting output
    //1 - 4    crafting grid
    //5 - 8    armor
    //9 - 62   expanded main inventory
    //63 - 71  hotbar

    private static final int NOVA_MAIN_START = 9;
    private static final int NOVA_MAIN_VISIBLE_SIZE = 27;
    private static final int NOVA_MAIN_TOTAL_SIZE = 54;
    private static final int NOVA_MAIN_END = NOVA_MAIN_START + NOVA_MAIN_TOTAL_SIZE;

    private static final int NOVA_HOTBAR_START = 63;
    private static final int NOVA_HOTBAR_SIZE = 9;
    private static final int NOVA_HOTBAR_END = NOVA_HOTBAR_START + NOVA_HOTBAR_SIZE;

    private static final int NOVA_HIDDEN_X = -2000;
    private static final int NOVA_HIDDEN_Y = -2000;

    private static final int NOVA_CREATIVE_INV_X = 9;
    private static final int NOVA_CREATIVE_INV_Y = 54;
    private static final int NOVA_CREATIVE_HOTBAR_Y = 112;

    private static final int NOVA_SCROLLBAR_X = 8;
    private static final int NOVA_SCROLLBAR_Y = 108;
    private static final int NOVA_SCROLLBAR_WIDTH = 162;
    private static final int NOVA_SCROLLBAR_HEIGHT = 3;
    private static final int NOVA_SCROLLBAR_CLICK_PADDING = 3;

    private int novaCreativeInventoryPage = 0;
    private boolean novaDraggingInventoryScrollbar = false;

    public GuiContainerCreativeOverwrite(EntityPlayer player) {
        super(new GuiContainerCreativeOverwrite.ContainerCreative(player));
        player.openContainer = this.inventorySlots;
        this.allowUserInput = true;
        this.ySize = 136;
        this.xSize = 195;

        //Remember what page was last open
        this.novaCreativeInventoryPage = InventoryPageState.getPage();
    }

    public void updateScreen() {
        if (!this.mc.playerController.isInCreativeMode()) {
            this.mc.displayGuiScreen(new GuiInventory(this.mc.thePlayer));
        }
    }

    protected void handleMouseClick(Slot slot, int slotId, int mouseButton, int clickMode) {
        this.field_147057_D = true;

        boolean flag = clickMode == 1;
        clickMode = slotId == -999 && clickMode == 0 ? 4 : clickMode;

        ItemStack itemstack1;
        InventoryPlayer inventoryplayer;

        if (slot == null && selectedTabIndex != CreativeTabs.tabInventory.getTabIndex() && clickMode != 5) {
            inventoryplayer = this.mc.thePlayer.inventory;

            if (inventoryplayer.getItemStack() != null) {
                if (mouseButton == 0) {
                    this.mc.thePlayer.dropPlayerItemWithRandomChoice(inventoryplayer.getItemStack(), true);
                    this.mc.playerController.sendPacketDropItem(inventoryplayer.getItemStack());
                    inventoryplayer.setItemStack(null);
                }

                if (mouseButton == 1) {
                    itemstack1 = inventoryplayer.getItemStack().splitStack(1);
                    this.mc.thePlayer.dropPlayerItemWithRandomChoice(itemstack1, true);
                    this.mc.playerController.sendPacketDropItem(itemstack1);

                    if (inventoryplayer.getItemStack().stackSize == 0) {
                        inventoryplayer.setItemStack(null);
                    }
                }
            }
        }
        else {
            int l;

            if (slot == this.field_147064_C && flag) {
                for (l = 0; l < this.mc.thePlayer.inventoryContainer.getInventory().size(); ++l) {
                    this.mc.playerController.sendSlotPacket(null, l);
                }
            }
            else {
                ItemStack itemstack;

                if (selectedTabIndex == CreativeTabs.tabInventory.getTabIndex()) {
                    if (slot == this.field_147064_C) {
                        this.mc.thePlayer.inventory.setItemStack(null);
                    }
                    else if (clickMode == 4 && slot != null && slot.getHasStack()) {
                        itemstack = slot.decrStackSize(mouseButton == 0 ? 1 : slot.getStack().getMaxStackSize());
                        this.mc.thePlayer.dropPlayerItemWithRandomChoice(itemstack, true);
                        this.mc.playerController.sendPacketDropItem(itemstack);
                    }
                    else if (clickMode == 4 && this.mc.thePlayer.inventory.getItemStack() != null) {
                        this.mc.thePlayer.dropPlayerItemWithRandomChoice(this.mc.thePlayer.inventory.getItemStack(), true);
                        this.mc.playerController.sendPacketDropItem(this.mc.thePlayer.inventory.getItemStack());
                        this.mc.thePlayer.inventory.setItemStack(null);
                    }
                    else {
                        this.mc.thePlayer.inventoryContainer.slotClick(slot == null ? slotId : ((GuiContainerCreativeOverwrite.CreativeSlot)slot).field_148332_b.slotNumber, mouseButton, clickMode, this.mc.thePlayer);

                        this.mc.thePlayer.inventoryContainer.detectAndSendChanges();
                    }
                }
                else if (clickMode != 5 && slot != null && slot.inventory == field_147060_v) {
                    inventoryplayer = this.mc.thePlayer.inventory;
                    itemstack1 = inventoryplayer.getItemStack();
                    ItemStack itemstack2 = slot.getStack();
                    ItemStack itemstack3;

                    if (clickMode == 2) {
                        if (itemstack2 != null && mouseButton >= 0 && mouseButton < 9) {
                            itemstack3 = itemstack2.copy();
                            itemstack3.stackSize = itemstack3.getMaxStackSize();
                            this.mc.thePlayer.inventory.setInventorySlotContents(mouseButton, itemstack3);
                            this.mc.thePlayer.inventoryContainer.detectAndSendChanges();
                        }

                        return;
                    }

                    if (clickMode == 3) {
                        if (inventoryplayer.getItemStack() == null && slot.getHasStack()) {
                            itemstack3 = slot.getStack().copy();
                            itemstack3.stackSize = itemstack3.getMaxStackSize();
                            inventoryplayer.setItemStack(itemstack3);
                        }

                        return;
                    }

                    if (clickMode == 4) {
                        if (itemstack2 != null) {
                            itemstack3 = itemstack2.copy();
                            itemstack3.stackSize = mouseButton == 0 ? 1 : itemstack3.getMaxStackSize();
                            this.mc.thePlayer.dropPlayerItemWithRandomChoice(itemstack3, true);
                            this.mc.playerController.sendPacketDropItem(itemstack3);
                        }

                        return;
                    }

                    if (itemstack1 != null && itemstack2 != null && itemstack1.isItemEqual(itemstack2) && ItemStack.areItemStackTagsEqual(itemstack1, itemstack2)) {
                        if (mouseButton == 0) {
                            if (flag) {
                                itemstack1.stackSize = itemstack1.getMaxStackSize();
                            }
                            else if (itemstack1.stackSize < itemstack1.getMaxStackSize()) {
                                ++itemstack1.stackSize;
                            }
                        }
                        else if (itemstack1.stackSize <= 1) {
                            inventoryplayer.setItemStack(null);
                        }
                        else {
                            --itemstack1.stackSize;
                        }
                    }
                    else if (itemstack2 != null && itemstack1 == null) {
                        inventoryplayer.setItemStack(ItemStack.copyItemStack(itemstack2));
                        itemstack1 = inventoryplayer.getItemStack();

                        if (flag) {
                            itemstack1.stackSize = itemstack1.getMaxStackSize();
                        }
                    }
                    else {
                        inventoryplayer.setItemStack(null);
                    }
                }
                else {

                    //Hands clicks in regular creative item tabs with hotbar
                    this.inventorySlots.slotClick(slot == null ? slotId : slot.slotNumber, mouseButton, clickMode, this.mc.thePlayer);

                    if (Container.func_94532_c(mouseButton) == 2) {
                        for (l = 0; l < 9; ++l) {
                            this.mc.playerController.sendSlotPacket(this.inventorySlots.getSlot(45 + l).getStack(), NOVA_HOTBAR_START + l);
                        }
                    }
                    else if (slot != null) {
                        itemstack = this.inventorySlots.getSlot(slot.slotNumber).getStack();
                        this.mc.playerController.sendSlotPacket(itemstack, slot.slotNumber - this.inventorySlots.inventorySlots.size() + 9 + NOVA_HOTBAR_START);
                    }
                }
            }
        }
    }

    public void initGui() {
        if (this.mc.playerController.isInCreativeMode()) {
            super.initGui();
            this.buttonList.clear();

            Keyboard.enableRepeatEvents(true);

            this.searchField = new GuiTextField(this.fontRendererObj, this.guiLeft + 82, this.guiTop + 6, 89, this.fontRendererObj.FONT_HEIGHT);

            this.searchField.setMaxStringLength(15);
            this.searchField.setEnableBackgroundDrawing(false);
            this.searchField.setVisible(false);
            this.searchField.setTextColor(16777215);

            int i = selectedTabIndex;
            selectedTabIndex = -1;
            this.setCurrentCreativeTab(CreativeTabs.creativeTabArray[i]);

            this.field_147059_E = new CreativeCrafting(this.mc);
            this.mc.thePlayer.inventoryContainer.addCraftingToCrafters(this.field_147059_E);

            int tabCount = CreativeTabs.creativeTabArray.length;

            if (tabCount > 12) {
                this.buttonList.add(new GuiButton(101, this.guiLeft, this.guiTop - 50, 20, 20, "<"));
                this.buttonList.add(new GuiButton(102, this.guiLeft + this.xSize - 20, this.guiTop - 50, 20, 20, ">"));
                this.maxPages = ((tabCount - 12) / 10) + 1;
            }
        }
        else {
            this.mc.displayGuiScreen(new GuiInventory(this.mc.thePlayer));
        }
    }

    public void onGuiClosed() {
        //Save selected expanded inventory page
        InventoryPageState.setPage(this.novaCreativeInventoryPage);

        super.onGuiClosed();

        if (this.mc.thePlayer != null && this.mc.thePlayer.inventory != null)
        {
            this.mc.thePlayer.inventoryContainer.removeCraftingFromCrafters(this.field_147059_E);
        }

        Keyboard.enableRepeatEvents(false);
    }

    protected void keyTyped(char typedChar, int keyCode) {
        if (!CreativeTabs.creativeTabArray[selectedTabIndex].hasSearchBar())
        {
            if (GameSettings.isKeyDown(this.mc.gameSettings.keyBindChat))
            {
                this.setCurrentCreativeTab(CreativeTabs.tabAllSearch);
            }
            else
            {
                super.keyTyped(typedChar, keyCode);
            }
        }
        else
        {
            if (this.field_147057_D)
            {
                this.field_147057_D = false;
                this.searchField.setText("");
            }

            if (!this.checkHotbarKeys(keyCode))
            {
                if (this.searchField.textboxKeyTyped(typedChar, keyCode))
                {
                    this.updateCreativeSearch();
                }
                else
                {
                    super.keyTyped(typedChar, keyCode);
                }
            }
        }
    }

    private void updateCreativeSearch() {
        GuiContainerCreativeOverwrite.ContainerCreative containercreative = (GuiContainerCreativeOverwrite.ContainerCreative)this.inventorySlots;
        containercreative.itemList.clear();
        CreativeTabs tab = CreativeTabs.creativeTabArray[selectedTabIndex];

        if (tab.hasSearchBar() && tab != CreativeTabs.tabAllSearch) {
            tab.displayAllReleventItems(containercreative.itemList);
            this.updateFilteredItems(containercreative);
            return;
        }

        Iterator iterator = Item.itemRegistry.iterator();

        while (iterator.hasNext()) {
            Item item = (Item)iterator.next();

            if (item != null && item.getCreativeTab() != null) {
                item.getSubItems(item, null, containercreative.itemList);
            }
        }

        this.updateFilteredItems(containercreative);
    }

    private void updateFilteredItems(GuiContainerCreativeOverwrite.ContainerCreative containercreative) {
        Iterator iterator;
        Enchantment[] enchantments = Enchantment.enchantmentsList;
        int j = enchantments.length;

        if (CreativeTabs.creativeTabArray[selectedTabIndex] != CreativeTabs.tabAllSearch) {
            j = 0;
        }

        for (int i = 0; i < j; ++i) {
            Enchantment enchantment = enchantments[i];

            if (enchantment != null && enchantment.type != null) {
                Items.enchanted_book.func_92113_a(enchantment, containercreative.itemList);
            }
        }

        iterator = containercreative.itemList.iterator();
        String search = this.searchField.getText().toLowerCase();

        while (iterator.hasNext()) {
            ItemStack stack = (ItemStack)iterator.next();
            boolean flag = false;

            Iterator tooltipIterator = stack.getTooltip(this.mc.thePlayer, this.mc.gameSettings.advancedItemTooltips).iterator();

            while (true) {
                if (tooltipIterator.hasNext()) {
                    String line = (String)tooltipIterator.next();

                    if (!line.toLowerCase().contains(search)) {
                        continue;
                    }

                    flag = true;
                }

                if (!flag) {
                    iterator.remove();
                }

                break;
            }
        }

        this.currentScroll = 0.0F;
        containercreative.scrollTo(0.0F);
    }

    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        CreativeTabs tab = CreativeTabs.creativeTabArray[selectedTabIndex];

        if (tab != null && tab.drawInForegroundOfTab()) {
            GL11.glDisable(GL11.GL_BLEND);
            this.fontRendererObj.drawString(I18n.format(tab.getTranslatedTabLabel(), new Object[0]), 8, 6, 4210752);
        }
    }

    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0 && this.novaIsMouseOverCreativeInventoryScrollbar(mouseX, mouseY)) {
            this.novaDraggingInventoryScrollbar = true;
            this.novaSetCreativeInventoryPageFromMouse(mouseX);
            return;
        }

        if (mouseButton == 0) {
            int l = mouseX - this.guiLeft;
            int i1 = mouseY - this.guiTop;

            CreativeTabs[] tabs = CreativeTabs.creativeTabArray;
            int tabCount = tabs.length;

            for (int k1 = 0; k1 < tabCount; ++k1) {
                CreativeTabs tab = tabs[k1];

                if (tab != null && this.func_147049_a(tab, l, i1))
                {
                    return;
                }
            }
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (this.novaDraggingInventoryScrollbar) {
            this.novaSetCreativeInventoryPageFromMouse(mouseX);
            return;
        }

        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    protected void mouseMovedOrUp(int mouseX, int mouseY, int state) {
        if (state == 0) {
            this.novaDraggingInventoryScrollbar = false;

            int l = mouseX - this.guiLeft;
            int i1 = mouseY - this.guiTop;

            CreativeTabs[] tabs = CreativeTabs.creativeTabArray;
            int tabCount = tabs.length;

            for (int k1 = 0; k1 < tabCount; ++k1) {
                CreativeTabs tab = tabs[k1];

                if (tab != null && this.func_147049_a(tab, l, i1)) {
                    this.setCurrentCreativeTab(tab);
                    return;
                }
            }
        }

        super.mouseMovedOrUp(mouseX, mouseY, state);
    }

    private boolean needsScrollBars() {
        if (CreativeTabs.creativeTabArray[selectedTabIndex] == null) {
            return false;
        }

        return selectedTabIndex != CreativeTabs.tabInventory.getTabIndex() && CreativeTabs.creativeTabArray[selectedTabIndex].shouldHidePlayerInventory() && ((GuiContainerCreativeOverwrite.ContainerCreative)this.inventorySlots).func_148328_e();
    }

    private void setCurrentCreativeTab(CreativeTabs tab) {
        if (tab == null) {
            return;
        }

        int oldTab = selectedTabIndex;
        selectedTabIndex = tab.getTabIndex();

        GuiContainerCreativeOverwrite.ContainerCreative containercreative = (GuiContainerCreativeOverwrite.ContainerCreative)this.inventorySlots;

        this.field_147008_s.clear();
        containercreative.itemList.clear();

        tab.displayAllReleventItems(containercreative.itemList);

        if (tab == CreativeTabs.tabInventory) {
            this.novaCreativeInventoryPage = InventoryPageState.getPage();
            NovaInventory.NETWORK.sendToServer(new PacketInventoryPage(this.novaCreativeInventoryPage));
            Container playerContainer = this.mc.thePlayer.inventoryContainer;

            if (this.field_147063_B == null) {
                this.field_147063_B = containercreative.inventorySlots;
            }

            containercreative.inventorySlots = new ArrayList();

            for (int j = 0; j < playerContainer.inventorySlots.size(); ++j) {
                GuiContainerCreativeOverwrite.CreativeSlot creativeSlot = new GuiContainerCreativeOverwrite.CreativeSlot((Slot)playerContainer.inventorySlots.get(j), j);
                containercreative.inventorySlots.add(creativeSlot);

                if (j >= 5 && j < 9) {
                    int k = j - 5;
                    int l = k / 2;
                    int i1 = k % 2;

                    creativeSlot.xDisplayPosition = 9 + l * 54;
                    creativeSlot.yDisplayPosition = 6 + i1 * 27;
                }
                else if (j >= 0 && j < 5) {
                    creativeSlot.xDisplayPosition = NOVA_HIDDEN_X;
                    creativeSlot.yDisplayPosition = NOVA_HIDDEN_Y;
                }
                else if (j >= NOVA_MAIN_START && j < NOVA_MAIN_END) {
                    this.novaPositionCreativeInventorySlot(creativeSlot, j);
                }
                else if (j >= NOVA_HOTBAR_START && j < NOVA_HOTBAR_END) {
                    int hotbarIndex = j - NOVA_HOTBAR_START;

                    creativeSlot.xDisplayPosition = NOVA_CREATIVE_INV_X + hotbarIndex * 18;
                    creativeSlot.yDisplayPosition = NOVA_CREATIVE_HOTBAR_Y;
                }
                else {
                    creativeSlot.xDisplayPosition = NOVA_HIDDEN_X;
                    creativeSlot.yDisplayPosition = NOVA_HIDDEN_Y;
                }
            }

            this.field_147064_C = new Slot(field_147060_v, 0, 173, 112);
            containercreative.inventorySlots.add(this.field_147064_C);

            this.novaUpdateCreativeInventoryTabSlots();
        }
        else if (oldTab == CreativeTabs.tabInventory.getTabIndex()) {
            containercreative.inventorySlots = this.field_147063_B;
            this.field_147063_B = null;
        }

        if (this.searchField != null) {
            if (tab.hasSearchBar()) {
                this.searchField.setVisible(true);
                this.searchField.setCanLoseFocus(false);
                this.searchField.setFocused(true);
                this.searchField.setText("");
                this.searchField.width = tab.getSearchbarWidth();
                this.searchField.xPosition = this.guiLeft + 82 + 89 - this.searchField.width;
                this.updateCreativeSearch();
            }
            else {
                this.searchField.setVisible(false);
                this.searchField.setCanLoseFocus(true);
                this.searchField.setFocused(false);
            }
        }

        this.currentScroll = 0.0F;
        containercreative.scrollTo(0.0F);
    }

    public void handleMouseInput() {
        super.handleMouseInput();

        int wheel = Mouse.getEventDWheel();

        if (wheel != 0 && selectedTabIndex == CreativeTabs.tabInventory.getTabIndex()) {
            if (wheel < 0) {
                this.novaSetCreativeInventoryPage(this.novaCreativeInventoryPage + 1);
            }
            else {
                this.novaSetCreativeInventoryPage(this.novaCreativeInventoryPage - 1);
            }

            return;
        }

        if (wheel != 0 && this.needsScrollBars()) {
            int j = ((GuiContainerCreativeOverwrite.ContainerCreative)this.inventorySlots).itemList.size() / 9 - 5 + 1;

            if (wheel > 0) {
                wheel = 1;
            }

            if (wheel < 0) {
                wheel = -1;
            }

            this.currentScroll = (float)((double)this.currentScroll - (double)wheel / (double)j);

            if (this.currentScroll < 0.0F) {
                this.currentScroll = 0.0F;
            }

            if (this.currentScroll > 1.0F) {
                this.currentScroll = 1.0F;
            }

            ((GuiContainerCreativeOverwrite.ContainerCreative)this.inventorySlots).scrollTo(this.currentScroll);
        }
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        boolean flag = Mouse.isButtonDown(0);

        int k = this.guiLeft;
        int l = this.guiTop;
        int i1 = k + 175;
        int j1 = l + 18;
        int k1 = i1 + 14;
        int l1 = j1 + 112;

        if (!this.wasClicking && flag && mouseX >= i1 && mouseY >= j1 && mouseX < k1 && mouseY < l1) {
            this.isScrolling = this.needsScrollBars();
        }

        if (!flag) {
            this.isScrolling = false;
        }

        this.wasClicking = flag;

        if (this.isScrolling) {
            this.currentScroll = ((float)(mouseY - j1) - 7.5F) / ((float)(l1 - j1) - 15.0F);

            if (this.currentScroll < 0.0F) {
                this.currentScroll = 0.0F;
            }

            if (this.currentScroll > 1.0F) {
                this.currentScroll = 1.0F;
            }

            ((GuiContainerCreativeOverwrite.ContainerCreative)this.inventorySlots).scrollTo(this.currentScroll);
        }

        this.novaUpdateCreativeInventoryTabSlots();

        super.drawScreen(mouseX, mouseY, partialTicks);

        CreativeTabs[] tabs = CreativeTabs.creativeTabArray;
        int start = tabPage * 10;
        int end = Math.min(tabs.length, (tabPage + 1) * 10 + 2);

        if (tabPage != 0) {
            start += 2;
        }

        boolean rendered = false;

        for (int j2 = start; j2 < end; ++j2) {
            CreativeTabs tab = tabs[j2];

            if (tab == null) {
                continue;
            }

            if (this.renderCreativeInventoryHoveringText(tab, mouseX, mouseY)) {
                rendered = true;
                break;
            }
        }

        if (!rendered && this.renderCreativeInventoryHoveringText(CreativeTabs.tabAllSearch, mouseX, mouseY)) {
            this.renderCreativeInventoryHoveringText(CreativeTabs.tabInventory, mouseX, mouseY);
        }

        if (this.field_147064_C != null && selectedTabIndex == CreativeTabs.tabInventory.getTabIndex() && this.func_146978_c(this.field_147064_C.xDisplayPosition, this.field_147064_C.yDisplayPosition, 16, 16, mouseX, mouseY)) {
            this.drawCreativeTabHoveringText(I18n.format("inventory.binSlot", new Object[0]), mouseX, mouseY);
        }

        if (this.maxPages != 0) {
            String page = String.format("%d / %d", tabPage + 1, this.maxPages + 1);
            int width = this.fontRendererObj.getStringWidth(page);

            GL11.glDisable(GL11.GL_LIGHTING);

            this.zLevel = 300.0F;
            itemRender.zLevel = 300.0F;

            this.fontRendererObj.drawString(page, this.guiLeft + this.xSize / 2 - width / 2, this.guiTop - 44, -1);

            this.zLevel = 0.0F;
            itemRender.zLevel = 0.0F;
        }

        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glDisable(GL11.GL_LIGHTING);
    }

    protected void renderToolTip(ItemStack stack, int mouseX, int mouseY) {
        if (selectedTabIndex == CreativeTabs.tabAllSearch.getTabIndex()) {
            List list = stack.getTooltip(this.mc.thePlayer, this.mc.gameSettings.advancedItemTooltips);
            CreativeTabs tab = stack.getItem().getCreativeTab();

            if (tab == null && stack.getItem() == Items.enchanted_book) {
                Map map = EnchantmentHelper.getEnchantments(stack);

                if (map.size() == 1) {
                    Enchantment enchantment = Enchantment.enchantmentsList[((Integer)map.keySet().iterator().next()).intValue()];
                    CreativeTabs[] tabs = CreativeTabs.creativeTabArray;

                    for (int l = 0; l < tabs.length; ++l) {
                        CreativeTabs tab1 = tabs[l];

                        if (tab1.func_111226_a(enchantment.type)) {
                            tab = tab1;
                            break;
                        }
                    }
                }
            }

            if (tab != null) {
                list.add(1, "" + EnumChatFormatting.BOLD + EnumChatFormatting.BLUE + I18n.format(tab.getTranslatedTabLabel(), new Object[0]));
            }

            for (int i1 = 0; i1 < list.size(); ++i1) {
                if (i1 == 0) {
                    list.set(i1, stack.getRarity().rarityColor + (String)list.get(i1));
                }
                else {
                    list.set(i1, EnumChatFormatting.GRAY + (String)list.get(i1));
                }
            }

            this.func_146283_a(list, mouseX, mouseY);
        }
        else
        {
            super.renderToolTip(stack, mouseX, mouseY);
        }
    }

    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        RenderHelper.enableGUIStandardItemLighting();

        CreativeTabs tab = CreativeTabs.creativeTabArray[selectedTabIndex];
        CreativeTabs[] tabs = CreativeTabs.creativeTabArray;

        int start = tabPage * 10;
        int end = Math.min(tabs.length, (tabPage + 1) * 10 + 2);

        if (tabPage != 0) {
            start += 2;
        }

        for (int l = start; l < end; ++l) {
            CreativeTabs tab1 = tabs[l];

            this.mc.getTextureManager().bindTexture(field_147061_u);

            if (tab1 == null) {
                continue;
            }

            if (tab1.getTabIndex() != selectedTabIndex) {
                this.func_147051_a(tab1);
            }
        }

        if (tabPage != 0) {
            if (tab != CreativeTabs.tabAllSearch) {
                this.mc.getTextureManager().bindTexture(field_147061_u);
                this.func_147051_a(CreativeTabs.tabAllSearch);
            }

            if (tab != CreativeTabs.tabInventory) {
                this.mc.getTextureManager().bindTexture(field_147061_u);
                this.func_147051_a(CreativeTabs.tabInventory);
            }
        }

        this.mc.getTextureManager().bindTexture(new ResourceLocation("textures/gui/container/creative_inventory/tab_" + tab.getBackgroundImageName()));

        this.drawTexturedModalRect(this.guiLeft, this.guiTop, 0, 0, this.xSize, this.ySize);
        this.novaDrawCreativeInventoryScrollbar();
        this.searchField.drawTextBox();

        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        int i1 = this.guiLeft + 175;
        int k = this.guiTop + 18;
        int l = k + 112;

        this.mc.getTextureManager().bindTexture(field_147061_u);

        if (tab.shouldHidePlayerInventory()) {
            this.drawTexturedModalRect(i1, k + (int)((float)(l - k - 17) * this.currentScroll), 232 + (this.needsScrollBars() ? 0 : 12), 0, 12, 15);
        }

        if (tab == null || tab.getTabPage() != tabPage) {
            if (tab != CreativeTabs.tabAllSearch && tab != CreativeTabs.tabInventory) {
                return;
            }
        }

        this.func_147051_a(tab);

        if (tab == CreativeTabs.tabInventory) {
            GuiInventory.func_147046_a(this.guiLeft + 43, this.guiTop + 45, 20, (float)(this.guiLeft + 43 - mouseX), (float)(this.guiTop + 45 - 30 - mouseY), this.mc.thePlayer);
        }
    }

    protected boolean func_147049_a(CreativeTabs tab, int mouseX, int mouseY) {
        if (tab.getTabPage() != tabPage) {
            if (tab != CreativeTabs.tabAllSearch && tab != CreativeTabs.tabInventory) {
                return false;
            }
        }

        int k = tab.getTabColumn();
        int l = 28 * k;
        byte b0 = 0;

        if (k == 5) {
            l = this.xSize - 28 + 2;
        }
        else if (k > 0) {
            l += k;
        }

        int i1;

        if (tab.isTabInFirstRow()) {
            i1 = b0 - 32;
        }
        else {
            i1 = b0 + this.ySize;
        }

        return mouseX >= l && mouseX <= l + 28 && mouseY >= i1 && mouseY <= i1 + 32;
    }

    protected boolean renderCreativeInventoryHoveringText(CreativeTabs tab, int mouseX, int mouseY) {
        int k = tab.getTabColumn();
        int l = 28 * k;
        byte b0 = 0;

        if (k == 5) {
            l = this.xSize - 28 + 2;
        }
        else if (k > 0) {
            l += k;
        }

        int i1;

        if (tab.isTabInFirstRow()) {
            i1 = b0 - 32;
        }
        else {
            i1 = b0 + this.ySize;
        }

        if (this.func_146978_c(l + 3, i1 + 3, 23, 27, mouseX, mouseY)) {
            this.drawCreativeTabHoveringText(I18n.format(tab.getTranslatedTabLabel(), new Object[0]), mouseX, mouseY);
            return true;
        }

        return false;
    }

    protected void func_147051_a(CreativeTabs tab) {
        boolean selected = tab.getTabIndex() == selectedTabIndex;
        boolean firstRow = tab.isTabInFirstRow();

        int i = tab.getTabColumn();
        int j = i * 28;
        int k = 0;
        int l = this.guiLeft + 28 * i;
        int i1 = this.guiTop;
        byte b0 = 32;

        if (selected) {
            k += 32;
        }

        if (i == 5) {
            l = this.guiLeft + this.xSize - 28;
        }
        else if (i > 0)
        {
            l += i;
        }

        if (firstRow) {
            i1 -= 28;
        }
        else {
            k += 64;
            i1 += this.ySize - 4;
        }

        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glColor3f(1F, 1F, 1F);
        GL11.glEnable(GL11.GL_BLEND);

        this.drawTexturedModalRect(l, i1, j, k, 28, b0);

        this.zLevel = 100.0F;
        itemRender.zLevel = 100.0F;

        l += 6;
        i1 += 8 + (firstRow ? 1 : -1);

        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);

        ItemStack icon = tab.getIconItemStack();

        itemRender.renderItemAndEffectIntoGUI(this.fontRendererObj, this.mc.getTextureManager(), icon, l, i1);
        itemRender.renderItemOverlayIntoGUI(this.fontRendererObj, this.mc.getTextureManager(), icon, l, i1);

        GL11.glDisable(GL11.GL_LIGHTING);

        itemRender.zLevel = 0.0F;
        this.zLevel = 0.0F;
    }

    protected void actionPerformed(GuiButton button) {
        if (button.id == 0) {
            this.mc.displayGuiScreen(new GuiAchievements(this, this.mc.thePlayer.getStatFileWriter()));
        }

        if (button.id == 1) {
            this.mc.displayGuiScreen(new GuiStats(this, this.mc.thePlayer.getStatFileWriter()));
        }

        if (button.id == 101) {
            tabPage = Math.max(tabPage - 1, 0);
        }
        else if (button.id == 102) {
            tabPage = Math.min(tabPage + 1, this.maxPages);
        }
    }

    public int func_147056_g()
    {
        return selectedTabIndex;
    }

    private void novaPositionCreativeInventorySlot(Slot slot, int containerSlotIndex) {
        int visibleStart = NOVA_MAIN_START + this.novaCreativeInventoryPage * NOVA_MAIN_VISIBLE_SIZE;
        int visibleEnd = visibleStart + NOVA_MAIN_VISIBLE_SIZE;

        if (containerSlotIndex >= visibleStart && containerSlotIndex < visibleEnd) {
            int visibleIndex = containerSlotIndex - visibleStart;
            int col = visibleIndex % 9;
            int row = visibleIndex / 9;

            slot.xDisplayPosition = NOVA_CREATIVE_INV_X + col * 18;
            slot.yDisplayPosition = NOVA_CREATIVE_INV_Y + row * 18;
        }
        else {
            slot.xDisplayPosition = NOVA_HIDDEN_X;
            slot.yDisplayPosition = NOVA_HIDDEN_Y;
        }
    }

    private void novaUpdateCreativeInventoryTabSlots() {
        if (selectedTabIndex != CreativeTabs.tabInventory.getTabIndex()) {
            return;
        }

        if (!(this.inventorySlots instanceof GuiContainerCreativeOverwrite.ContainerCreative)) {
            return;
        }

        GuiContainerCreativeOverwrite.ContainerCreative containercreative = (GuiContainerCreativeOverwrite.ContainerCreative)this.inventorySlots;

        for (int i = 0; i < containercreative.inventorySlots.size(); ++i) {
            Slot slot = (Slot)containercreative.inventorySlots.get(i);

            //Stops Desyncing with the hotbar in creative
            int realContainerSlot = this.novaGetRealPlayerContainerSlotIndex(slot);

            if (realContainerSlot >= NOVA_MAIN_START && realContainerSlot < NOVA_MAIN_END) {
                this.novaPositionCreativeInventorySlot(slot, realContainerSlot);
            }
            else if (realContainerSlot >= NOVA_HOTBAR_START && realContainerSlot < NOVA_HOTBAR_END) {
                int hotbarIndex = realContainerSlot - NOVA_HOTBAR_START;
                slot.xDisplayPosition = NOVA_CREATIVE_INV_X + hotbarIndex * 18;
                slot.yDisplayPosition = NOVA_CREATIVE_HOTBAR_Y;
            }
        }
    }

    private void novaDrawCreativeInventoryScrollbar() {
        if (selectedTabIndex != CreativeTabs.tabInventory.getTabIndex()) {
            return;
        }

        int trackX = this.guiLeft + NOVA_SCROLLBAR_X;
        int trackY = this.guiTop + NOVA_SCROLLBAR_Y;
        drawRect(trackX, trackY, trackX + NOVA_SCROLLBAR_WIDTH, trackY + NOVA_SCROLLBAR_HEIGHT, 0xFF202020);
        int pageCount = 2;
        int thumbWidth = NOVA_SCROLLBAR_WIDTH / pageCount;
        int maxThumbTravel = NOVA_SCROLLBAR_WIDTH - thumbWidth;
        int thumbX = trackX + maxThumbTravel * this.novaCreativeInventoryPage;
        drawRect(thumbX, trackY - 1, thumbX + thumbWidth, trackY + NOVA_SCROLLBAR_HEIGHT + 1, 0xFFAAAAAA);
        drawRect(thumbX + 1, trackY, thumbX + thumbWidth - 1, trackY + NOVA_SCROLLBAR_HEIGHT, 0xFFFFFFFF);
    }

    private boolean novaIsMouseOverCreativeInventoryScrollbar(int mouseX, int mouseY) {
        if (selectedTabIndex != CreativeTabs.tabInventory.getTabIndex()) {
            return false;
        }

        int trackX = this.guiLeft + NOVA_SCROLLBAR_X;
        int trackY = this.guiTop + NOVA_SCROLLBAR_Y;

        return mouseX >= trackX && mouseX < trackX + NOVA_SCROLLBAR_WIDTH && mouseY >= trackY - NOVA_SCROLLBAR_CLICK_PADDING && mouseY < trackY + NOVA_SCROLLBAR_HEIGHT + NOVA_SCROLLBAR_CLICK_PADDING;
    }

    private void novaSetCreativeInventoryPageFromMouse(int mouseX) {
        int trackX = this.guiLeft + NOVA_SCROLLBAR_X;
        int relativeX = mouseX - trackX;

        if (relativeX < 0) {
            relativeX = 0;
        }

        if (relativeX > NOVA_SCROLLBAR_WIDTH) {
            relativeX = NOVA_SCROLLBAR_WIDTH;
        }

        if (relativeX < NOVA_SCROLLBAR_WIDTH / 2) {
            this.novaSetCreativeInventoryPage(0);
        }

        else
        {
            this.novaSetCreativeInventoryPage(1);
        }
    }

    private void novaSetCreativeInventoryPage(int page) {
        if (page < 0) {
            page = 0;
        }

        if (page > 1) {
            page = 1;
        }

        this.novaCreativeInventoryPage = page;

        InventoryPageState.setPage(page);

        //Syncs selected creative inventory page to server otherwise hotbar is unusable in creative mode
        NovaInventory.NETWORK.sendToServer(new PacketInventoryPage(page));
        this.novaUpdateCreativeInventoryTabSlots();
    }

    @SideOnly(Side.CLIENT)
    static class ContainerCreative extends Container {
        public List itemList = new ArrayList();

        public ContainerCreative(EntityPlayer player) {
            InventoryPlayer inventoryplayer = player.inventory;

            int i;

            for (i = 0; i < 5; ++i) {
                for (int j = 0; j < 9; ++j) {
                    this.addSlotToContainer(new Slot(GuiContainerCreativeOverwrite.field_147060_v, i * 9 + j, 9 + j * 18, 18 + i * 18));
                }
            }

            for (i = 0; i < 9; ++i) {
                this.addSlotToContainer(new Slot(inventoryplayer, i, 9 + i * 18, 112));
            }

            this.scrollTo(0.0F);
        }

        public boolean canInteractWith(EntityPlayer player)
        {
            return true;
        }

        public void scrollTo(float scroll) {
            int i = this.itemList.size() / 9 - 5 + 1;
            int j = (int)((double)(scroll * (float)i) + 0.5D);

            if (j < 0) {
                j = 0;
            }

            for (int k = 0; k < 5; ++k) {
                for (int l = 0; l < 9; ++l) {
                    int i1 = l + (k + j) * 9;

                    if (i1 >= 0 && i1 < this.itemList.size()) {
                        GuiContainerCreativeOverwrite.field_147060_v.setInventorySlotContents(l + k * 9, (ItemStack)this.itemList.get(i1));
                    }
                    else {
                        GuiContainerCreativeOverwrite.field_147060_v.setInventorySlotContents(l + k * 9, null);
                    }
                }
            }
        }

        public boolean func_148328_e()
        {
            return this.itemList.size() > 45;
        }

        protected void retrySlotClick(int slotId, int mouseButton, boolean shift, EntityPlayer player) {
        }

        public ItemStack transferStackInSlot(EntityPlayer player, int slotIndex) {
            if (slotIndex >= this.inventorySlots.size() - 9 && slotIndex < this.inventorySlots.size()) {
                Slot slot = (Slot)this.inventorySlots.get(slotIndex);

                if (slot != null && slot.getHasStack()) {
                    slot.putStack(null);
                }
            }

            return null;
        }

        public boolean func_94530_a(ItemStack stack, Slot slot)
        {
            return slot.yDisplayPosition > 90;
        }

        public boolean canDragIntoSlot(Slot slot) {
            return slot.inventory instanceof InventoryPlayer || slot.yDisplayPosition > 90 && slot.xDisplayPosition <= 162;
        }
    }

    @SideOnly(Side.CLIENT)
    static class CreativeSlot extends Slot {
        public final Slot field_148332_b;

        public CreativeSlot(Slot slot, int slotNumber) {
            super(slot.inventory, slotNumber, 0, 0);
            this.field_148332_b = slot;
        }

        public void onPickupFromSlot(EntityPlayer player, ItemStack stack) {
            this.field_148332_b.onPickupFromSlot(player, stack);
        }

        public boolean isItemValid(ItemStack stack)
        {
            return this.field_148332_b.isItemValid(stack);
        }

        public ItemStack getStack()
        {
            return this.field_148332_b.getStack();
        }

        public boolean getHasStack()
        {
            return this.field_148332_b.getHasStack();
        }

        public void putStack(ItemStack stack)
        {
            this.field_148332_b.putStack(stack);
        }

        public void onSlotChanged()
        {
            this.field_148332_b.onSlotChanged();
        }

        public int getSlotStackLimit()
        {
            return this.field_148332_b.getSlotStackLimit();
        }

        public IIcon getBackgroundIconIndex()
        {
            return this.field_148332_b.getBackgroundIconIndex();
        }

        public ItemStack decrStackSize(int amount)
        {
            return this.field_148332_b.decrStackSize(amount);
        }

        public boolean isSlotInInventory(IInventory inventory, int slotIndex)
        {
            return this.field_148332_b.isSlotInInventory(inventory, slotIndex);
        }
    }

    private int novaGetRealPlayerContainerSlotIndex(Slot slot) {
        if (slot instanceof GuiContainerCreativeOverwrite.CreativeSlot) {
            return ((GuiContainerCreativeOverwrite.CreativeSlot)slot).field_148332_b.slotNumber;
        }

        return slot.slotNumber;
    }
}

