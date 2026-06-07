package com.NovaInv;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import org.lwjgl.input.Mouse;

public final class GuiContainerInventoryPager
{
    //Container slot layout:
    //--------------------------
    //0        crafting output
    //1 - 4    crafting grid
    //5 - 8    armor
    //9 - 62   expanded main inventory
    //63 - 71  hotbar

    //Most of the calls and variables here are copied directly from the vanilla class
    private static final int MAIN_PAGE_SIZE = 27;

    private static final int PAGE_0_START = 9;
    private static final int PAGE_0_END = 36;

    private static final int PAGE_1_START = 36;
    private static final int PAGE_1_END = 63;

    private static final int HIDDEN_X = -10000;
    private static final int HIDDEN_Y = -10000;

    private static final int SCROLLBAR_WIDTH = 162;
    private static final int SCROLLBAR_HEIGHT = 3;
    private static final int SCROLLBAR_CLICK_PADDING = 5;

    private static final Map<Slot, int[]> ORIGINAL_SLOT_POSITIONS = Collections.synchronizedMap(new WeakHashMap<Slot, int[]>());
    private static final Map<Container, Boolean> SYNCED_CONTAINERS = Collections.synchronizedMap(new WeakHashMap<Container, Boolean>());

    private static Field inventorySlotsField;
    private static Field guiLeftField;
    private static Field guiTopField;

    private static final int SCROLLBAR_X_OFFSET = -1;
    private static final int SCROLLBAR_Y_OFFSET = 0;

    private GuiContainerInventoryPager()
    {
    }

    public static void updateSlots(GuiContainer gui)
    {
        if (gui == null || isExcludedGui(gui))
        {
            return;
        }

        Container container = getContainer(gui);

        if (container == null)
        {
            return;
        }

        List<Slot> page0 = getPlayerSlots(container, PAGE_0_START, PAGE_0_END);
        List<Slot> page1 = getPlayerSlots(container, PAGE_1_START, PAGE_1_END);

        if (page0.size() != MAIN_PAGE_SIZE || page1.size() != MAIN_PAGE_SIZE)
        {
            return;
        }

        if (!SYNCED_CONTAINERS.containsKey(container))
        {
            syncPageToServer(InventoryPageState.getPage());
            SYNCED_CONTAINERS.put(container, Boolean.TRUE);
        }

        captureOriginalPositions(page0);

        int page = InventoryPageState.getPage();

        for (int i = 0; i < MAIN_PAGE_SIZE; i++)
        {
            Slot page0Slot = page0.get(i);
            Slot page1Slot = page1.get(i);

            int[] original = getOriginalPosition(page0Slot);

            int originalX = original[0];
            int originalY = original[1];

            if (page == 0)
            {
                page0Slot.xDisplayPosition = originalX;
                page0Slot.yDisplayPosition = originalY;

                page1Slot.xDisplayPosition = HIDDEN_X;
                page1Slot.yDisplayPosition = HIDDEN_Y;
            }
            else
            {
                page1Slot.xDisplayPosition = originalX;
                page1Slot.yDisplayPosition = originalY;

                page0Slot.xDisplayPosition = HIDDEN_X;
                page0Slot.yDisplayPosition = HIDDEN_Y;
            }
        }
    }

    public static void drawScrollbar(GuiContainer gui) {
        if (gui == null || isExcludedGui(gui))
        {
            return;
        }

        if (!hasPagedInventory(gui))
        {
            return;
        }

        Container container = getContainer(gui);
        List<Slot> page0 = getPlayerSlots(container, PAGE_0_START, PAGE_0_END);

        if (page0.size() != MAIN_PAGE_SIZE)
        {
            return;
        }

        captureOriginalPositions(page0);

        int left = getGuiLeft(gui);
        int top = getGuiTop(gui);

        Slot row3First = page0.get(18);
        int[] original = getOriginalPosition(row3First);

        int trackX = left + original[0] - 1;
        int trackY = top + original[1] + 18;

        Gui.drawRect(trackX, trackY, trackX + SCROLLBAR_WIDTH, trackY + SCROLLBAR_HEIGHT, 0xFF202020);

        int thumbWidth = SCROLLBAR_WIDTH / 2;
        int thumbX = trackX;

        if (InventoryPageState.getPage() == 1) {
            thumbX = trackX + thumbWidth;
        }

        Gui.drawRect(thumbX, trackY - 1, thumbX + thumbWidth, trackY + SCROLLBAR_HEIGHT + 1, 0xFFAAAAAA);
        Gui.drawRect(thumbX + 1, trackY, thumbX + thumbWidth - 1, trackY + SCROLLBAR_HEIGHT, 0xFFFFFFFF);
    }

    public static boolean mouseClicked(GuiContainer gui, int mouseX, int mouseY, int button)
    {
        if (gui == null || isExcludedGui(gui))
        {
            return false;
        }

        if (button != 0)
        {
            return false;
        }

        if (!hasPagedInventory(gui))
        {
            return false;
        }

        if (isMouseOverScrollbar(gui, mouseX, mouseY))
        {
            setPageFromMouse(gui, mouseX);
            updateSlots(gui);
            return true;
        }

        return false;
    }

    //Use mousewheel to easily switch from both inventory screens
    public static void handleMouseInput(GuiContainer gui)
    {
        if (gui == null || isExcludedGui(gui))
        {
            return;
        }

        if (!hasPagedInventory(gui))
        {
            return;
        }

        int wheel = Mouse.getEventDWheel();

        if (wheel == 0)
        {
            return;
        }

        if (wheel < 0)
        {
            setPage(InventoryPageState.getPage() + 1);
        }
        else
        {
            setPage(InventoryPageState.getPage() - 1);
        }

        updateSlots(gui);
    }

    private static boolean hasPagedInventory(GuiContainer gui)
    {
        Container container = getContainer(gui);

        if (container == null)
        {
            return false;
        }

        List<Slot> page0 = getPlayerSlots(container, PAGE_0_START, PAGE_0_END);
        List<Slot> page1 = getPlayerSlots(container, PAGE_1_START, PAGE_1_END);

        return page0.size() == MAIN_PAGE_SIZE && page1.size() == MAIN_PAGE_SIZE;
    }

    private static List<Slot> getPlayerSlots(Container container, int startInclusive, int endExclusive)
    {
        List<Slot> result = new ArrayList<Slot>();

        if (container == null)
        {
            return result;
        }

        for (int wanted = startInclusive; wanted < endExclusive; wanted++)
        {
            Slot found = null;

            for (int i = 0; i < container.inventorySlots.size(); i++)
            {
                Slot slot = (Slot)container.inventorySlots.get(i);

                if (slot.inventory instanceof InventoryPlayer && slot.getSlotIndex() == wanted)
                {
                    found = slot;
                    break;
                }
            }

            if (found != null)
            {
                result.add(found);
            }
        }

        return result;
    }

    private static void captureOriginalPositions(List<Slot> page0) {
        for (int i = 0; i < page0.size(); i++)
        {
            Slot slot = page0.get(i);

            if (!ORIGINAL_SLOT_POSITIONS.containsKey(slot))
            {

                if (slot.xDisplayPosition != HIDDEN_X && slot.yDisplayPosition != HIDDEN_Y) {
                    ORIGINAL_SLOT_POSITIONS.put(slot, new int[] {slot.xDisplayPosition, slot.yDisplayPosition});
                }
            }
        }
    }

    private static int[] getOriginalPosition(Slot slot)
    {
        int[] original = ORIGINAL_SLOT_POSITIONS.get(slot);

        if (original != null)
        {
            return original;
        }

        return new int[] {slot.xDisplayPosition, slot.yDisplayPosition};
    }

    private static boolean isMouseOverScrollbar(GuiContainer gui, int mouseX, int mouseY)
    {
        Container container = getContainer(gui);

        if (container == null)
        {
            return false;
        }

        List<Slot> page0 = getPlayerSlots(container, PAGE_0_START, PAGE_0_END);

        if (page0.size() != MAIN_PAGE_SIZE)
        {
            return false;
        }

        captureOriginalPositions(page0);

        int trackX = getScrollbarTrackX(gui);
        int trackY = getScrollbarTrackY(gui);

        return mouseX >= trackX && mouseX < trackX + SCROLLBAR_WIDTH && mouseY >= trackY - SCROLLBAR_CLICK_PADDING && mouseY < trackY + SCROLLBAR_HEIGHT + SCROLLBAR_CLICK_PADDING;
    }

    private static void setPageFromMouse(GuiContainer gui, int mouseX)
    {
        Container container = getContainer(gui);

        if (container == null)
        {
            return;
        }

        List<Slot> page0 = getPlayerSlots(container, PAGE_0_START, PAGE_0_END);

        if (page0.size() != MAIN_PAGE_SIZE)
        {
            return;
        }

        captureOriginalPositions(page0);

        int trackX = getScrollbarTrackX(gui);
        int relativeX = mouseX - trackX;

        if (relativeX < SCROLLBAR_WIDTH / 2)
        {
            setPage(0);
        }
        else
        {
            setPage(1);
        }
    }

    private static void setPage(int page)
    {
        if (page < 0)
        {
            page = 0;
        }

        if (page > 1)
        {
            page = 1;
        }

        InventoryPageState.setPage(page);
        syncPageToServer(page);
    }

    private static void syncPageToServer(int page)
    {
        try {
            //This allows for shift clicking to prioritize the tab open first
            NovaInventory.NETWORK.sendToServer(new PacketInventoryPage(page));
        }
        catch (Throwable ignored)
        {

        }
    }

    private static boolean isExcludedGui(GuiContainer gui) {
        String name = gui.getClass().getName();
        return "net.minecraft.client.gui.inventory.GuiInventory".equals(name) || "net.minecraft.client.gui.inventory.GuiContainerCreative".equals(name);
    }

    private static Container getContainer(GuiContainer gui) {
        try {
            if (inventorySlotsField == null)
            {
                inventorySlotsField = findField(GuiContainer.class, new String[] {"inventorySlots", "field_147002_h"});

                inventorySlotsField.setAccessible(true);
            }

            return (Container)inventorySlotsField.get(gui);
        }
        catch (Throwable t)
        {
            return null;
        }
    }

    private static int getGuiLeft(GuiContainer gui) {
        try {
            if (guiLeftField == null) {
                guiLeftField = findField(GuiContainer.class, new String[] {"guiLeft", "field_147003_i"});

                guiLeftField.setAccessible(true);
            }

            return guiLeftField.getInt(gui);
        }
        catch (Throwable t)
        {
            return 0;
        }
    }

    private static int getGuiTop(GuiContainer gui) {
        try {
            if (guiTopField == null)
            {
                guiTopField = findField(GuiContainer.class, new String[] {"guiTop", "field_147009_r"});
                guiTopField.setAccessible(true);
            }

            return guiTopField.getInt(gui);
        }
        catch (Throwable t)
        {
            return 0;
        }
    }

    private static Field findField(Class clazz, String[] names) throws NoSuchFieldException {
        for (int i = 0; i < names.length; i++)
        {
            try
            {
                return clazz.getDeclaredField(names[i]);
            }
            catch (NoSuchFieldException ignored)
            {
            }
        }

        throw new NoSuchFieldException("Could not find field in " + clazz.getName());
    }

    private static int getScrollbarTrackX(GuiContainer gui) {
        Container container = getContainer(gui);
        if (container == null) {
            return 0;
        }

        List<Slot> page0 = getPlayerSlots(container, PAGE_0_START, PAGE_0_END);
        if (page0.size() != MAIN_PAGE_SIZE) {
            return 0;
        }

        captureOriginalPositions(page0);
        int left = getGuiLeft(gui);
        Slot row3First = page0.get(18);
        int[] original = getOriginalPosition(row3First);

        return left + original[0] + SCROLLBAR_X_OFFSET;
    }

    private static int getScrollbarTrackY(GuiContainer gui) {
        Container container = getContainer(gui);
        if (container == null) {
            return 0;
        }

        List<Slot> page0 = getPlayerSlots(container, PAGE_0_START, PAGE_0_END);
        if (page0.size() != MAIN_PAGE_SIZE) {
            return 0;
        }

        captureOriginalPositions(page0);
        int top = getGuiTop(gui);
        Slot row3First = page0.get(18);
        int[] original = getOriginalPosition(row3First);

        return top + original[1] + 18 + SCROLLBAR_Y_OFFSET;
    }
}
