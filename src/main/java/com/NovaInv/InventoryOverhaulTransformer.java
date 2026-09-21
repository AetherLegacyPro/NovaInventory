package com.NovaInv;

import cpw.mods.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.Launch;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.RemappingClassAdapter;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;

public class InventoryOverhaulTransformer implements IClassTransformer {
    private static final String INVENTORY_PLAYER = "net.minecraft.entity.player.InventoryPlayer";

    private static final String CONTAINER_PLAYER = "net.minecraft.inventory.ContainerPlayer";

    private static final String GUI_CONTAINER = "net.minecraft.client.gui.inventory.GuiContainer";

    private static final String GUI_CONTAINER_CREATIVE = "net.minecraft.client.gui.inventory.GuiContainerCreative";

    private static final String GUI_CONTAINER_CREATIVE_CONTAINER = "net.minecraft.client.gui.inventory.GuiContainerCreative$ContainerCreative";

    private static final String GUI_CONTAINER_CREATIVE_SLOT = "net.minecraft.client.gui.inventory.GuiContainerCreative$CreativeSlot";

    private static final String NET_HANDLER_PLAY_SERVER = "net.minecraft.network.NetHandlerPlayServer";

    private static final String CONTAINER = "net.minecraft.inventory.Container";

    private static final String REPLACEMENT_CONTAINER_PLAYER = "com/NovaInv/ContainerPlayerOverwrite";

    private static final String REPLACEMENT_GUI_INVENTORY = "com/NovaInv/GuiInventoryOverwrite";

    private static final String REPLACEMENT_GUI_CONTAINER_CREATIVE = "com/NovaInv/GuiContainerCreativeOverwrite";

    private static final String REPLACEMENT_GUI_CONTAINER_CREATIVE_CONTAINER = "com/NovaInv/GuiContainerCreativeOverwrite$ContainerCreative";

    private static final String REPLACEMENT_GUI_CONTAINER_CREATIVE_SLOT = "com/NovaInv/GuiContainerCreativeOverwrite$CreativeSlot";

    private static final String TARGET_CONTAINER_PLAYER = "net/minecraft/inventory/ContainerPlayer";

    private static final String TARGET_GUI_INVENTORY = "net/minecraft/client/gui/inventory/GuiInventory";

    private static final String TARGET_GUI_CONTAINER_CREATIVE = "net/minecraft/client/gui/inventory/GuiContainerCreative";

    private static final String TARGET_GUI_CONTAINER_CREATIVE_CONTAINER = "net/minecraft/client/gui/inventory/GuiContainerCreative$ContainerCreative";

    private static final String TARGET_GUI_CONTAINER_CREATIVE_SLOT = "net/minecraft/client/gui/inventory/GuiContainerCreative$CreativeSlot";


    private static boolean archaicFixDetected;
    private static boolean archaicFixMessagePrinted;

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) {
            return null;
        }

        try {
            //ArchaicFix Mixin conflict fix, should have just did a mixin to disable it...
            if (isCreativeInventoryClass(transformedName) && isArchaicFixPresent()) {
                if (!archaicFixMessagePrinted) {
                    archaicFixMessagePrinted = true;
                    System.out.println("[NovaInventory] ArchaicFix detected. " + "NovaInventory's creative inventory " + "overhaul is disabled.");
                }

                System.out.println("[NovaInventory] Leaving creative class untouched: " + transformedName);

                return basicClass;
            }

            if (INVENTORY_PLAYER.equals(transformedName)) {
                System.out.println("[NovaInventory] Patching InventoryPlayer " + "mainInventory size");

                return patchInventoryPlayer(basicClass);
            }

            if (CONTAINER_PLAYER.equals(transformedName)) {
                System.out.println("[NovaInventory] Replacing ContainerPlayer");

                return replaceClass(REPLACEMENT_CONTAINER_PLAYER, TARGET_CONTAINER_PLAYER);
            }

            //ArchaicFix Mixin conflict fix, should have just did a mixin to disable it...
            if (NET_HANDLER_PLAY_SERVER.equals(transformedName)) {
                if (isArchaicFixPresent()) {
                    System.out.println("[NovaInventory] ArchaicFix detected; " + "skipping creative packet slot patch.");

                    return basicClass;
                }

                System.out.println("[NovaInventory] Patching creative inventory " + "packet limit");

                return patchNetHandlerPlayServer(basicClass);
            }

            //Global slot hook for other containers
            if (CONTAINER.equals(transformedName)) {
                System.out.println("[NovaInventory] Patching " + "Container.addSlotToContainer");

                return patchContainerAddSlotToContainer(basicClass);
            }

            //Pager for vanilla blocks only
            if (GUI_CONTAINER.equals(transformedName)) {
                System.out.println("[NovaInventory] Patching GuiContainer " + "inventory pager");

                return patchGuiContainer(basicClass);
            }

            if (GUI_CONTAINER_CREATIVE.equals(transformedName)) {
                System.out.println("[NovaInventory] Replacing GuiContainerCreative");

                return replaceClass(REPLACEMENT_GUI_CONTAINER_CREATIVE, TARGET_GUI_CONTAINER_CREATIVE);
            }

            if (GUI_CONTAINER_CREATIVE_CONTAINER.equals(transformedName)) {
                System.out.println("[NovaInventory] Replacing " + "GuiContainerCreative$ContainerCreative");

                return replaceClass(REPLACEMENT_GUI_CONTAINER_CREATIVE_CONTAINER, TARGET_GUI_CONTAINER_CREATIVE_CONTAINER);
            }

            if (GUI_CONTAINER_CREATIVE_SLOT.equals(transformedName)) {
                System.out.println("[NovaInventory] Replacing " + "GuiContainerCreative$CreativeSlot");

                return replaceClass(REPLACEMENT_GUI_CONTAINER_CREATIVE_SLOT, TARGET_GUI_CONTAINER_CREATIVE_SLOT);
            }
        }
        catch (Throwable throwable) {
            System.err.println("[NovaInventory] Failed transforming " + transformedName);

            throwable.printStackTrace();

            throw new RuntimeException("[NovaInventory] Critical transformer failure for " + transformedName, throwable);
        }

        return basicClass;
    }

    private static boolean isCreativeInventoryClass(String transformedName) {
        return GUI_CONTAINER_CREATIVE.equals(transformedName) || GUI_CONTAINER_CREATIVE_CONTAINER.equals(transformedName) || GUI_CONTAINER_CREATIVE_SLOT.equals(transformedName);
    }

    //ArchaicFix compact fix...
    private static boolean isArchaicFixPresent() {
        if (archaicFixDetected) {
            return true;
        }

        try {
            if (Launch.classLoader.getResource("mixins.archaicfix.early.json") != null) {
                archaicFixDetected = true;

                System.out.println("[NovaInventory] Detected ArchaicFix through " + "mixins.archaicfix.early.json");

                return true;
            }

            if (Launch.classLoader.getResource("mixins.archaicfix.json") != null) {
                archaicFixDetected = true;

                System.out.println("[NovaInventory] Detected ArchaicFix through " + "mixins.archaicfix.json");

                return true;
            }

            for (URL source : Launch.classLoader.getSources()) {
                String path = source.toString().toLowerCase(Locale.ROOT);

                if (path.contains("archaicfix")) {
                    archaicFixDetected = true;

                    System.out.println("[NovaInventory] Detected ArchaicFix " + "on classpath: " + source);

                    return true;
                }
            }

            Object tweakClasses = Launch.blackboard.get("TweakClasses");

            if (tweakClasses != null && tweakClasses.toString().toLowerCase(Locale.ROOT).contains("archaicfix")) {
                archaicFixDetected = true;

                System.out.println("[NovaInventory] Detected ArchaicFix " + "through TweakClasses");

                return true;
            }
        }
        catch (Throwable throwable) {
            System.err.println("[NovaInventory] Error detecting ArchaicFix");

            throwable.printStackTrace();
        }

        return false;
    }

    //Overrides InventoryPlayer ItemStack[36] allocations to ItemStack[63].
    private byte[] patchInventoryPlayer(byte[] basicClass) {
        ClassNode classNode = new ClassNode();
        ClassReader reader = new ClassReader(basicClass);
        reader.accept(classNode, 0);

        int patchedCount = 0;

        for (MethodNode method : classNode.methods) {
            ListIterator<AbstractInsnNode> iterator = method.instructions.iterator();

            while (iterator.hasNext()) {
                AbstractInsnNode instruction = iterator.next();

                if (!(instruction instanceof IntInsnNode)) {
                    continue;
                }

                IntInsnNode integerInstruction = (IntInsnNode)instruction;
                if (integerInstruction.operand == 36 && isFollowedByItemStackAnewarray(integerInstruction)) {
                    integerInstruction.operand = 63;
                    ++patchedCount;

                    System.out.println("[NovaInventory] Changed InventoryPlayer " + "ItemStack array size 36 -> 63 in " + method.name + method.desc);
                }
            }
        }

        if (patchedCount == 0) {
            System.err.println("[NovaInventory] WARNING: Could not find any " + "InventoryPlayer mainInventory allocations");
        } else {
            System.out.println("[NovaInventory] Patched " + patchedCount + " InventoryPlayer ItemStack[36] allocation(s)");
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);

        classNode.accept(writer);
        return writer.toByteArray();
    }

    private boolean isFollowedByItemStackAnewarray(AbstractInsnNode start) {
        AbstractInsnNode current = start.getNext();

        for (int i = 0; i < 8 && current != null; ++i) {
            if (current instanceof TypeInsnNode) {
                TypeInsnNode typeInstruction = (TypeInsnNode)current;

                if (typeInstruction.getOpcode() == Opcodes.ANEWARRAY) {
                    return true;
                }
            }

            current = current.getNext();
        }

        return false;
    }

    private byte[] replaceClass(String replacementInternalName, String targetInternalName) throws IOException {
        byte[] replacementBytes = readClassBytes(replacementInternalName);

        ClassReader reader = new ClassReader(replacementBytes);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);

        Map<String, String> mappings = new HashMap<String, String>();
        mappings.put(replacementInternalName, targetInternalName);

        mappings.put(REPLACEMENT_CONTAINER_PLAYER, TARGET_CONTAINER_PLAYER);
        mappings.put(REPLACEMENT_GUI_INVENTORY, TARGET_GUI_INVENTORY);
        mappings.put(REPLACEMENT_GUI_CONTAINER_CREATIVE, TARGET_GUI_CONTAINER_CREATIVE);
        mappings.put(REPLACEMENT_GUI_CONTAINER_CREATIVE_CONTAINER, TARGET_GUI_CONTAINER_CREATIVE_CONTAINER);
        mappings.put(REPLACEMENT_GUI_CONTAINER_CREATIVE_SLOT, TARGET_GUI_CONTAINER_CREATIVE_SLOT);

        ClassVisitor remapper = new RemappingClassAdapter(writer, new SimpleRemapper(mappings));
        reader.accept(remapper, ClassReader.EXPAND_FRAMES);

        System.out.println("[NovaInventory] Remapped " + replacementInternalName + " -> " + targetInternalName);

        return writer.toByteArray();
    }

    private byte[] readClassBytes(String internalName) throws IOException {
        String path = "/" + internalName + ".class";

        InputStream stream = InventoryOverhaulTransformer.class.getResourceAsStream(path);
        if (stream == null) {
            throw new IOException("Could not find replacement class resource: " + path);
        }

        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();

            byte[] buffer = new byte[4096];
            int read;

            while ((read = stream.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }

            return output.toByteArray();
        }
        finally {
            stream.close();
        }
    }

    //Server side overrides for the number of slots
    private byte[] patchNetHandlerPlayServer(byte[] basicClass) {
        ClassNode classNode = new ClassNode();
        ClassReader reader = new ClassReader(basicClass);
        reader.accept(classNode, 0);

        int patchedCount = 0;

        for (MethodNode method : classNode.methods) {
            ListIterator<AbstractInsnNode> iterator = method.instructions.iterator();

            while (iterator.hasNext()) {
                AbstractInsnNode instruction = iterator.next();

                if (instruction instanceof IntInsnNode) {
                    IntInsnNode integerInstruction = (IntInsnNode)instruction;

                    if (integerInstruction.operand == 36 && isNearStaticIntCallAndIAdd(integerInstruction)) {
                        integerInstruction.operand = 63;
                        ++patchedCount;
                    }

                    else if (integerInstruction.operand == 45 && methodLooksLikeCreativeInventoryHandler(method)) {
                        integerInstruction.operand = 72;
                        ++patchedCount;
                    }
                }
                else if (instruction instanceof LdcInsnNode) {
                    LdcInsnNode ldcInstruction = (LdcInsnNode)instruction;

                    if (!(ldcInstruction.cst instanceof Integer)) {
                        continue;
                    }

                    int value = ((Integer)ldcInstruction.cst).intValue();

                    if (value == 36 && isNearStaticIntCallAndIAdd(ldcInstruction)) {
                        ldcInstruction.cst = Integer.valueOf(63);

                        ++patchedCount;
                    }

                    else if (value == 45 && methodLooksLikeCreativeInventoryHandler(method)) {
                        ldcInstruction.cst = Integer.valueOf(72);
                        ++patchedCount;
                    }
                }
            }
        }

        if (patchedCount == 0) {
            System.err.println("[NovaInventory] WARNING: Creative packet " + "slot limit was not patched");
        } else {
            System.out.println("[NovaInventory] Creative packet patch count: " + patchedCount);
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classNode.accept(writer);
        return writer.toByteArray();
    }

    private boolean isNearStaticIntCallAndIAdd(AbstractInsnNode start) {
        if (searchForStaticIntCallAndAdd(start.getNext(), true)) {
            return true;
        }

        return searchForStaticIntCallAndAdd(start.getPrevious(), false);
    }

    private boolean searchForStaticIntCallAndAdd(AbstractInsnNode start, boolean forward) {
        boolean foundStaticIntCall = false;
        boolean foundAdd = false;

        AbstractInsnNode current = start;

        for (int i = 0; i < 12 && current != null; ++i) {
            if (current instanceof MethodInsnNode) {
                MethodInsnNode methodInstruction = (MethodInsnNode)current;

                if (methodInstruction.getOpcode() == Opcodes.INVOKESTATIC && "()I".equals(methodInstruction.desc)) {
                    foundStaticIntCall = true;
                }
            }

            if (current.getOpcode() == Opcodes.IADD) {
                foundAdd = true;
            }

            current = forward ? current.getNext() : current.getPrevious();
        }

        return foundStaticIntCall && foundAdd;
    }

    private boolean methodLooksLikeCreativeInventoryHandler(MethodNode method) {
        if ("processCreativeInventoryAction".equals(method.name) || "func_147344_a".equals(method.name) || method.desc.contains("C10PacketCreativeInventoryAction")) {
            return true;
        }

        boolean hasNegativeSlotCheck = false;
        boolean hasItemStackReference = false;

        ListIterator<AbstractInsnNode> iterator = method.instructions.iterator();

        while (iterator.hasNext()) {
            AbstractInsnNode instruction = iterator.next();

            if (instruction instanceof TypeInsnNode) {
                TypeInsnNode typeInstruction = (TypeInsnNode)instruction;

                if (typeInstruction.desc != null && typeInstruction.desc.contains("ItemStack")) {
                    hasItemStackReference = true;
                }
            }

            if (instruction instanceof MethodInsnNode) {
                MethodInsnNode methodInstruction = (MethodInsnNode)instruction;

                if (methodInstruction.desc != null && methodInstruction.desc.contains("ItemStack")) {
                    hasItemStackReference = true;
                }
            }

            if (instruction.getOpcode() == Opcodes.IFGE || instruction.getOpcode() == Opcodes.IFLT) {
                hasNegativeSlotCheck = true;
            }
        }

        return hasNegativeSlotCheck
                && hasItemStackReference;
    }

    private byte[] patchContainerAddSlotToContainer(byte[] basicClass) {
        ClassNode classNode = new ClassNode();
        ClassReader reader = new ClassReader(basicClass);
        reader.accept(classNode, 0);

        int patchedCount = 0;

        for (MethodNode method : classNode.methods) {
            if (!isContainerAddSlotCandidate(method)) {
                continue;
            }

            ListIterator<AbstractInsnNode> iterator = method.instructions.iterator();

            while (iterator.hasNext()) {
                AbstractInsnNode instruction = iterator.next();

                if (instruction.getOpcode() != Opcodes.ARETURN) {
                    continue;
                }

                InsnList injection = new InsnList();
                injection.add(new VarInsnNode(Opcodes.ALOAD, 0));
                injection.add(new VarInsnNode(Opcodes.ALOAD, 1));

                injection.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "com/NovaInv/InventoryContainerHooks", "onSlotAdded", "(Lnet/minecraft/inventory/Container;" + "Lnet/minecraft/inventory/Slot;)V"));

                method.instructions.insertBefore(instruction, injection);

                ++patchedCount;
            }
        }

        if (patchedCount == 0) {
            System.err.println("[NovaInventory] WARNING: Could not patch " + "Container.addSlotToContainer");
        } else {
            System.out.println("[NovaInventory] Container slot-hook count: " + patchedCount);
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);

        classNode.accept(writer);
        return writer.toByteArray();
    }

    private boolean isContainerAddSlotCandidate(MethodNode method) {
        if (!isOneObjectArgSameObjectReturn(method.desc)) {
            return false;
        }

        if ((method.access & Opcodes.ACC_STATIC) != 0) {
            return false;
        }

        int listAddCalls = 0;
        boolean hasAreturn = false;

        ListIterator<AbstractInsnNode> iterator = method.instructions.iterator();

        while (iterator.hasNext()) {
            AbstractInsnNode instruction = iterator.next();

            if (instruction instanceof MethodInsnNode) {
                MethodInsnNode methodInstruction = (MethodInsnNode)instruction;

                if ("java/util/List".equals(methodInstruction.owner) && "add".equals(methodInstruction.name) && "(Ljava/lang/Object;)Z".equals(methodInstruction.desc)) {
                    ++listAddCalls;
                }
            }

            if (instruction.getOpcode() == Opcodes.ARETURN) {
                hasAreturn = true;
            }
        }

        return listAddCalls >= 2 && hasAreturn;
    }

    private boolean isOneObjectArgSameObjectReturn(String descriptor) {
        if (descriptor == null) {
            return false;
        }

        try {
            Type[] arguments = Type.getArgumentTypes(descriptor);
            Type returnType = Type.getReturnType(descriptor);

            return arguments.length == 1 && arguments[0].getSort() == Type.OBJECT && returnType.getSort() == Type.OBJECT && arguments[0].getInternalName().equals(returnType.getInternalName());
        }
        catch (Throwable ignored) {
            return false;
        }
    }

    private byte[] patchGuiContainer(byte[] basicClass) {
        ClassNode classNode = new ClassNode();
        ClassReader reader = new ClassReader(basicClass);
        reader.accept(classNode, 0);

        String owner = classNode.name;

        int patchedCount = 0;
        boolean patchedDrawScreen = false;
        boolean patchedMouseClicked = false;
        boolean patchedMouseInput = false;

        for (MethodNode method : classNode.methods) {
            if (isGuiContainerDrawScreen(owner, method)) {
                InsnList startInjection = new InsnList();

                startInjection.add(new VarInsnNode(Opcodes.ALOAD, 0));

                startInjection.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "com/NovaInv/GuiContainerInventoryPager", "updateSlots", "(Lnet/minecraft/client/gui/inventory/" + "GuiContainer;)V"));

                AbstractInsnNode first = getFirstRealInstruction(method);

                if (first != null) {
                    method.instructions.insertBefore(first, startInjection);
                } else {
                    method.instructions.insert(startInjection);
                }

                ++patchedCount;

                ListIterator<AbstractInsnNode> iterator = method.instructions.iterator();

                while (iterator.hasNext()) {
                    AbstractInsnNode instruction = iterator.next();

                    if (!isGL11PopMatrixCall(instruction)) {
                        continue;
                    }

                    InsnList drawInjection = new InsnList();
                    drawInjection.add(new VarInsnNode(Opcodes.ALOAD, 0));

                    drawInjection.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "com/NovaInv/GuiContainerInventoryPager", "drawScrollbar", "(Lnet/minecraft/client/gui/inventory/" + "GuiContainer;)V"));

                    method.instructions.insert(instruction, drawInjection);

                    ++patchedCount;
                    break;
                }

                patchedDrawScreen = true;
            }
            else if (isGuiContainerMouseClicked(owner, method)) {
                LabelNode continueLabel = new LabelNode();
                InsnList injection = new InsnList();

                injection.add(new VarInsnNode(Opcodes.ALOAD, 0));
                injection.add(new VarInsnNode(Opcodes.ILOAD, 1));
                injection.add(new VarInsnNode(Opcodes.ILOAD, 2));
                injection.add(new VarInsnNode(Opcodes.ILOAD, 3));

                injection.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "com/NovaInv/GuiContainerInventoryPager", "mouseClicked", "(Lnet/minecraft/client/gui/inventory/" + "GuiContainer;III)Z"));

                injection.add(new JumpInsnNode(Opcodes.IFEQ, continueLabel));

                injection.add(new InsnNode(Opcodes.RETURN));

                injection.add(continueLabel);

                AbstractInsnNode first = getFirstRealInstruction(method);

                if (first != null) {
                    method.instructions.insertBefore(first, injection);
                } else {
                    method.instructions.insert(injection);
                }

                ++patchedCount;
                patchedMouseClicked = true;
            }
            else if (isGuiContainerHandleMouseInput(owner, method)) {
                ListIterator<AbstractInsnNode> iterator = method.instructions.iterator();

                while (iterator.hasNext()) {
                    AbstractInsnNode instruction = iterator.next();

                    if (instruction.getOpcode() != Opcodes.RETURN) {
                        continue;
                    }

                    InsnList injection = new InsnList();

                    injection.add(new VarInsnNode(Opcodes.ALOAD, 0));

                    injection.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "com/NovaInv/GuiContainerInventoryPager", "handleMouseInput", "(Lnet/minecraft/client/gui/inventory/" + "GuiContainer;)V"));

                    method.instructions.insertBefore(instruction, injection);

                    ++patchedCount;
                }

                patchedMouseInput = true;
            }
        }

        if (!patchedDrawScreen) {
            System.err.println("[NovaInventory] WARNING: GuiContainer.drawScreen " + "was not patched");
        }

        if (!patchedMouseClicked) {
            System.err.println("[NovaInventory] WARNING: GuiContainer.mouseClicked " + "was not patched");
        }

        if (!patchedMouseInput) {
            System.err.println("[NovaInventory] WARNING: GuiContainer.handleMouseInput " + "was not patched");
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);

        classNode.accept(writer);
        return writer.toByteArray();
    }

    private boolean isGuiContainerDrawScreen(String owner, MethodNode method) {
        return methodNameMatches(owner, method, "drawScreen", "func_73863_a") && "(IIF)V".equals(method.desc);
    }

    private boolean isGuiContainerMouseClicked(String owner, MethodNode method) {
        return methodNameMatches(owner, method, "mouseClicked", "func_73864_a") && "(III)V".equals(method.desc);
    }

    private boolean isGuiContainerHandleMouseInput(String owner, MethodNode method) {
        return methodNameMatches(owner, method, "handleMouseInput", "func_146274_d") && "()V".equals(method.desc);
    }

    private boolean isGL11PopMatrixCall(AbstractInsnNode instruction) {
        if (!(instruction instanceof MethodInsnNode)) {
            return false;
        }

        MethodInsnNode methodInstruction = (MethodInsnNode)instruction;

        return "org/lwjgl/opengl/GL11".equals(methodInstruction.owner) && "glPopMatrix".equals(methodInstruction.name) && "()V".equals(methodInstruction.desc);
    }

    private AbstractInsnNode getFirstRealInstruction(MethodNode method) {
        AbstractInsnNode instruction = method.instructions.getFirst();
        while (instruction != null) {
            int type = instruction.getType();

            if (type != AbstractInsnNode.LABEL && type != AbstractInsnNode.LINE && type != AbstractInsnNode.FRAME) {
                return instruction;
            }

            instruction = instruction.getNext();
        }

        return null;
    }

    private String mapMethodName(String ownerInternalName, MethodNode method) {
        try {
            return FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(ownerInternalName, method.name, method.desc);
        }
        catch (Throwable ignored) {
            return method.name;
        }
    }

    private boolean methodNameMatches(String ownerInternalName, MethodNode method, String mcpName, String srgName) {
        if (method.name.equals(mcpName) || method.name.equals(srgName)) {
            return true;
        }

        String mappedName = mapMethodName(ownerInternalName, method);
        if (mappedName.equals(mcpName) || mappedName.equals(srgName)) {
            return true;
        }

        String mappedWithDeobfuscatedOwner = mapMethodName("net/minecraft/client/gui/inventory/GuiContainer", method);

        return mappedWithDeobfuscatedOwner.equals(mcpName) || mappedWithDeobfuscatedOwner.equals(srgName);
    }
}