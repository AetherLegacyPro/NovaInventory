package com.NovaInv;

import cpw.mods.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.RemappingClassAdapter;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.ListIterator;
import java.util.Map;

public class InventoryOverhaulTransformer implements IClassTransformer {
    private static final String INVENTORY_PLAYER = "net.minecraft.entity.player.InventoryPlayer";

    private static final String CONTAINER_PLAYER = "net.minecraft.inventory.ContainerPlayer";

    private static final String GUI_INVENTORY = "net.minecraft.client.gui.inventory.GuiInventory";

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

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) {
            return null;
        }

        try {
            if (INVENTORY_PLAYER.equals(transformedName)) {
                System.out.println("[NovaInventory] Patching InventoryPlayer mainInventory size");
                return patchInventoryPlayer(basicClass);
            }

            if (CONTAINER_PLAYER.equals(transformedName)) {
                System.out.println("[NovaInventory] Replacing ContainerPlayer");
                return replaceClass(REPLACEMENT_CONTAINER_PLAYER, TARGET_CONTAINER_PLAYER);
            }

            if (GUI_INVENTORY.equals(transformedName)) {
                System.out.println("[NovaInventory] Replacing GuiInventory");
                return replaceClass(REPLACEMENT_GUI_INVENTORY, TARGET_GUI_INVENTORY);
            }

            if (GUI_CONTAINER_CREATIVE.equals(transformedName)) {
                System.out.println("[NovaInventory] Replacing GuiContainerCreative");
                return replaceClass(REPLACEMENT_GUI_CONTAINER_CREATIVE, TARGET_GUI_CONTAINER_CREATIVE);
            }

            if (GUI_CONTAINER_CREATIVE_CONTAINER.equals(transformedName)) {
                System.out.println("[NovaInventory] Replacing GuiContainerCreative$ContainerCreative");
                return replaceClass(REPLACEMENT_GUI_CONTAINER_CREATIVE_CONTAINER, TARGET_GUI_CONTAINER_CREATIVE_CONTAINER);
            }

            if (GUI_CONTAINER_CREATIVE_SLOT.equals(transformedName)) {
                System.out.println("[NovaInventory] Replacing GuiContainerCreative$CreativeSlot");
                return replaceClass(REPLACEMENT_GUI_CONTAINER_CREATIVE_SLOT, TARGET_GUI_CONTAINER_CREATIVE_SLOT);
            }

            if (NET_HANDLER_PLAY_SERVER.equals(transformedName)) {
                System.out.println("[NovaInventory] Patching NetHandlerPlayServer creative inventory slot limit");
                return patchNetHandlerPlayServer(basicClass);
            }

            if (CONTAINER.equals(transformedName)) {
                System.out.println("[NovaInventory] Patching Container.addSlotToContainer");
                return patchContainerAddSlotToContainer(basicClass);
            }

            if (GUI_CONTAINER.equals(transformedName)) {
                System.out.println("[NovaInventory] Patching GuiContainer inventory pager");
                return patchGuiContainer(basicClass);
            }
        }
        catch (Throwable throwable) {
            System.err.println("[NovaInventory] Failed transforming " + transformedName);
            throwable.printStackTrace();

            throw new RuntimeException("[NovaInventory] Critical transformer failure for " + transformedName, throwable);
        }

        return basicClass;
    }

    //Changes public ItemStack[] mainInventory = new ItemStack[36]; to public ItemStack[] mainInventory = new ItemStack[63];
    private byte[] patchInventoryPlayer(byte[] basicClass)
    {
        ClassNode classNode = new ClassNode();
        ClassReader reader = new ClassReader(basicClass);
        reader.accept(classNode, 0);

        int patchedCount = 0;

        for (MethodNode method : classNode.methods) {
            ListIterator<AbstractInsnNode> iterator = method.instructions.iterator();

            while (iterator.hasNext()) {
                AbstractInsnNode insn = iterator.next();

                if (insn instanceof IntInsnNode) {
                    IntInsnNode intInsn = (IntInsnNode)insn;

                    if (intInsn.operand == 36 && isFollowedByItemStackAnewarray(intInsn)) {
                        intInsn.operand = 63;
                        patchedCount++;

                        System.out.println("[NovaInventory] Changed InventoryPlayer ItemStack array size 36 -> 63 in "
                                + method.name + method.desc);
                    }
                }
            }
        }

        if (patchedCount == 0) {
            System.err.println("[NovaInventory] WARNING: Could not find any InventoryPlayer mainInventory allocations");
        }
        else {
            System.out.println("[NovaInventory] Patched " + patchedCount + " InventoryPlayer ItemStack[36] allocation(s)");
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classNode.accept(writer);
        return writer.toByteArray();
    }

    private boolean isFollowedByItemStackAnewarray(AbstractInsnNode start) {
        AbstractInsnNode current = start.getNext();

        for (int i = 0; i < 8 && current != null; i++) {
            if (current instanceof TypeInsnNode) {
                TypeInsnNode typeInsn = (TypeInsnNode)current;

                if (typeInsn.getOpcode() == org.objectweb.asm.Opcodes.ANEWARRAY) {
                    return true;
                }
            }

            current = current.getNext();
        }

        return false;
    }

    //Reads one of our replacement classes from the mod jar,
    //Remaps its internal class name to the vanilla target class name and,
    //Returns the resulting bytecode.
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


    private byte[] patchNetHandlerPlayServer(byte[] basicClass) {
        ClassNode classNode = new ClassNode();
        ClassReader reader = new ClassReader(basicClass);
        reader.accept(classNode, 0);

        int patchedCount = 0;

        for (MethodNode method : classNode.methods) {
            ListIterator<AbstractInsnNode> iterator = method.instructions.iterator();

            while (iterator.hasNext()) {
                AbstractInsnNode insn = iterator.next();

                if (insn instanceof IntInsnNode) {
                    IntInsnNode intInsn = (IntInsnNode)insn;

                    if (intInsn.operand == 36 && isNearStaticIntCallAndIAdd(intInsn)) {
                        intInsn.operand = 63;
                        patchedCount++;

                        System.out.println("[NovaInventory] Patched NetHandlerPlayServer creative base 36 -> 63 in " + method.name + method.desc);
                    }
                    else if (intInsn.operand == 45 && methodLooksLikeCreativeInventoryHandler(method)) {
                        intInsn.operand = 72;
                        patchedCount++;

                        System.out.println("[NovaInventory] Patched NetHandlerPlayServer creative limit 45 -> 72 in " + method.name + method.desc);
                    }
                }
                else if (insn instanceof LdcInsnNode) {
                    LdcInsnNode ldc = (LdcInsnNode)insn;

                    if (ldc.cst instanceof Integer) {
                        int value = ((Integer)ldc.cst).intValue();

                        if (value == 36 && isNearStaticIntCallAndIAdd(ldc)) {
                            ldc.cst = Integer.valueOf(63);
                            patchedCount++;

                            System.out.println("[NovaInventory] Patched NetHandlerPlayServer creative LDC base 36 -> 63 in " + method.name + method.desc);
                        }
                        else if (value == 45 && methodLooksLikeCreativeInventoryHandler(method)) {
                            ldc.cst = Integer.valueOf(72);
                            patchedCount++;

                            System.out.println("[NovaInventory] Patched NetHandlerPlayServer creative LDC limit 45 -> 72 in " + method.name + method.desc);
                        }
                    }
                }
            }
        }

        if (patchedCount == 0) {
            System.err.println("[NovaInventory] WARNING: NetHandlerPlayServer creative slot limit was NOT patched.");
        }
        else {
            System.out.println("[NovaInventory] NetHandlerPlayServer creative slot patch count: " + patchedCount);
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classNode.accept(writer);
        return writer.toByteArray();
    }

    private boolean isNearStaticIntCallAndIAdd(AbstractInsnNode start) {
        boolean foundStaticIntCall = false;
        boolean foundIAdd = false;
        AbstractInsnNode current = start.getNext();

        for (int i = 0; i < 12 && current != null; i++) {
            if (current instanceof MethodInsnNode) {
                MethodInsnNode methodInsn = (MethodInsnNode)current;

                if (methodInsn.getOpcode() == org.objectweb.asm.Opcodes.INVOKESTATIC && "()I".equals(methodInsn.desc)) {
                    foundStaticIntCall = true;
                }
            }

            if (current.getOpcode() == org.objectweb.asm.Opcodes.IADD) {
                foundIAdd = true;
            }

            current = current.getNext();
        }

        if (foundStaticIntCall && foundIAdd) {
            return true;
        }

        foundStaticIntCall = false;
        foundIAdd = false;
        current = start.getPrevious();

        for (int i = 0; i < 12 && current != null; i++) {
            if (current instanceof MethodInsnNode) {
                MethodInsnNode methodInsn = (MethodInsnNode)current;

                if (methodInsn.getOpcode() == org.objectweb.asm.Opcodes.INVOKESTATIC && "()I".equals(methodInsn.desc)) {
                    foundStaticIntCall = true;
                }
            }

            if (current.getOpcode() == org.objectweb.asm.Opcodes.IADD) {
                foundIAdd = true;
            }

            current = current.getPrevious();
        }

        return foundStaticIntCall && foundIAdd;
    }

    //Is the container call for things like the player inventory, hotbar etc: still needs work...
    private boolean methodLooksLikeCreativeInventoryHandler(MethodNode method) {
        if ("processCreativeInventoryAction".equals(method.name) || "func_147344_a".equals(method.name) || method.desc.contains("C10PacketCreativeInventoryAction")) {
            return true;
        }

        boolean hasNegativeSlotCheck = false;
        boolean hasItemStackReference = false;

        ListIterator<AbstractInsnNode> iterator = method.instructions.iterator();

        while (iterator.hasNext()) {
            AbstractInsnNode insn = iterator.next();

            if (insn instanceof TypeInsnNode) {
                TypeInsnNode typeInsn = (TypeInsnNode)insn;

                if (typeInsn.desc != null && typeInsn.desc.contains("ItemStack"))
                {
                    hasItemStackReference = true;
                }
            }

            if (insn instanceof MethodInsnNode) {
                MethodInsnNode methodInsn = (MethodInsnNode)insn;

                if (methodInsn.desc != null && methodInsn.desc.contains("ItemStack"))
                {
                    hasItemStackReference = true;
                }
            }

            if (insn.getOpcode() == org.objectweb.asm.Opcodes.IFGE || insn.getOpcode() == org.objectweb.asm.Opcodes.IFLT) {
                hasNegativeSlotCheck = true;
            }
        }

        return hasNegativeSlotCheck && hasItemStackReference;
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

            System.out.println("[NovaInventory] Found Container.addSlotToContainer candidate: " + method.name + method.desc);

            ListIterator<AbstractInsnNode> iterator = method.instructions.iterator();

            while (iterator.hasNext()) {
                AbstractInsnNode insn = iterator.next();

                if (insn.getOpcode() == org.objectweb.asm.Opcodes.ARETURN) {
                    InsnList inject = new InsnList();
                    inject.add(new VarInsnNode(org.objectweb.asm.Opcodes.ALOAD, 0));
                    inject.add(new VarInsnNode(org.objectweb.asm.Opcodes.ALOAD, 1));
                    inject.add(new MethodInsnNode(org.objectweb.asm.Opcodes.INVOKESTATIC, "com/NovaInv/InventoryContainerHooks", "onSlotAdded", "(Lnet/minecraft/inventory/Container;Lnet/minecraft/inventory/Slot;)V"));
                    method.instructions.insertBefore(insn, inject);

                    patchedCount++;

                    System.out.println("[NovaInventory] Injected InventoryContainerHooks.onSlotAdded into " + method.name + method.desc);
                }
            }
        }

        if (patchedCount == 0) {
            System.err.println("[NovaInventory] WARNING: Could not patch Container.addSlotToContainer");
        }
        else {
            System.out.println("[NovaInventory] Patched Container.addSlotToContainer injection count: " + patchedCount);
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classNode.accept(writer);
        return writer.toByteArray();
    }

    private boolean isContainerAddSlotCandidate(MethodNode method) {
        if (!isOneObjectArgSameObjectReturn(method.desc)) {
            return false;
        }

        if ((method.access & org.objectweb.asm.Opcodes.ACC_STATIC) != 0) {
            return false;
        }

        int listAddCalls = 0;
        boolean hasAreturn = false;

        ListIterator<AbstractInsnNode> iterator = method.instructions.iterator();

        while (iterator.hasNext()) {
            AbstractInsnNode insn = iterator.next();

            if (insn instanceof MethodInsnNode) {
                MethodInsnNode methodInsn = (MethodInsnNode)insn;

                if ("java/util/List".equals(methodInsn.owner) && "add".equals(methodInsn.name) && "(Ljava/lang/Object;)Z".equals(methodInsn.desc)) {
                    listAddCalls++;
                }
            }

            if (insn.getOpcode() == org.objectweb.asm.Opcodes.ARETURN) {
                hasAreturn = true;
            }
        }

        return listAddCalls >= 2 && hasAreturn;
    }

    private boolean isOneObjectArgSameObjectReturn(String desc) {
        if (desc == null) {
            return false;
        }

        try {
            Type[] args = Type.getArgumentTypes(desc);
            Type returnType = Type.getReturnType(desc);

            if (args.length != 1) {
                return false;
            }

            if (args[0].getSort() != Type.OBJECT) {
                return false;
            }

            if (returnType.getSort() != Type.OBJECT) {
                return false;
            }

            return args[0].getInternalName().equals(returnType.getInternalName());
        }
        catch (Throwable t) {
            return false;
        }
    }

    private byte[] patchGuiContainer(byte[] basicClass) {
        ClassNode classNode = new ClassNode();
        ClassReader reader = new ClassReader(basicClass);
        reader.accept(classNode, 0);
        String owner = classNode.name;
        System.out.println("[NovaInventory] Patching GuiContainer owner=" + owner);

        int patchedCount = 0;
        boolean patchedDrawScreen = false;
        boolean patchedMouseClicked = false;
        boolean patchedMouseInput = false;

        for (MethodNode method : classNode.methods) {
            if (isGuiContainerDrawScreen(owner, method)) {
                System.out.println("[NovaInventory] Found GuiContainer.drawScreen: " + method.name + method.desc + " owner=" + owner);

                InsnList startInject = new InsnList();
                startInject.add(new VarInsnNode(org.objectweb.asm.Opcodes.ALOAD, 0));
                startInject.add(new MethodInsnNode(org.objectweb.asm.Opcodes.INVOKESTATIC, "com/NovaInv/GuiContainerInventoryPager", "updateSlots", "(Lnet/minecraft/client/gui/inventory/GuiContainer;)V"));
                AbstractInsnNode first = getFirstRealInstruction(method);

                if (first != null) {
                    method.instructions.insertBefore(first, startInject);
                }
                else {
                    method.instructions.insert(startInject);
                }

                patchedCount++;
                boolean injectedAfterPopMatrix = false;
                ListIterator<AbstractInsnNode> iterator = method.instructions.iterator();

                while (iterator.hasNext()) {
                    AbstractInsnNode insn = iterator.next();

                    if (isGL11PopMatrixCall(insn)) {
                        InsnList drawInject = new InsnList();
                        drawInject.add(new VarInsnNode(org.objectweb.asm.Opcodes.ALOAD, 0));
                        drawInject.add(new MethodInsnNode(org.objectweb.asm.Opcodes.INVOKESTATIC, "com/NovaInv/GuiContainerInventoryPager", "drawScrollbar", "(Lnet/minecraft/client/gui/inventory/GuiContainer;)V"));
                        method.instructions.insert(insn, drawInject);

                        patchedCount++;
                        injectedAfterPopMatrix = true;

                        System.out.println("[NovaInventory] Injected scrollbar after GL11.glPopMatrix in " + method.name + method.desc);

                        break;
                    }
                }

                if (!injectedAfterPopMatrix) {
                    System.err.println("[NovaInventory] WARNING: Could not find GL11.glPopMatrix; using RETURN fallback");

                    ListIterator<AbstractInsnNode> returnIterator = method.instructions.iterator();

                    while (returnIterator.hasNext()) {
                        AbstractInsnNode insn = returnIterator.next();

                        if (insn.getOpcode() == org.objectweb.asm.Opcodes.RETURN) {
                            InsnList returnInject = new InsnList();

                            returnInject.add(new VarInsnNode(org.objectweb.asm.Opcodes.ALOAD, 0));
                            returnInject.add(new MethodInsnNode(org.objectweb.asm.Opcodes.INVOKESTATIC, "com/NovaInv/GuiContainerInventoryPager", "drawScrollbar", "(Lnet/minecraft/client/gui/inventory/GuiContainer;)V"));

                            method.instructions.insertBefore(insn, returnInject);
                            patchedCount++;
                        }
                    }
                }

                patchedDrawScreen = true;
            }
            else if (isGuiContainerMouseClicked(owner, method)) {
                System.out.println("[NovaInventory] Found GuiContainer.mouseClicked: " + method.name + method.desc + " owner=" + owner);

                LabelNode continueLabel = new LabelNode();
                InsnList inject = new InsnList();
                inject.add(new VarInsnNode(org.objectweb.asm.Opcodes.ALOAD, 0));
                inject.add(new VarInsnNode(org.objectweb.asm.Opcodes.ILOAD, 1));
                inject.add(new VarInsnNode(org.objectweb.asm.Opcodes.ILOAD, 2));
                inject.add(new VarInsnNode(org.objectweb.asm.Opcodes.ILOAD, 3));
                inject.add(new MethodInsnNode(org.objectweb.asm.Opcodes.INVOKESTATIC, "com/NovaInv/GuiContainerInventoryPager", "mouseClicked", "(Lnet/minecraft/client/gui/inventory/GuiContainer;III)Z"));
                inject.add(new JumpInsnNode(org.objectweb.asm.Opcodes.IFEQ, continueLabel));
                inject.add(new InsnNode(org.objectweb.asm.Opcodes.RETURN));
                inject.add(continueLabel);

                AbstractInsnNode first = getFirstRealInstruction(method);

                if (first != null) {
                    method.instructions.insertBefore(first, inject);
                }
                else {
                    method.instructions.insert(inject);
                }

                patchedCount++;
                patchedMouseClicked = true;
            }
            else if (isGuiContainerHandleMouseInput(owner, method)) {
                System.out.println("[NovaInventory] Found GuiContainer.handleMouseInput: " + method.name + method.desc + " owner=" + owner);

                ListIterator<AbstractInsnNode> iterator = method.instructions.iterator();

                while (iterator.hasNext()) {
                    AbstractInsnNode insn = iterator.next();

                    if (insn.getOpcode() == org.objectweb.asm.Opcodes.RETURN) {
                        InsnList inject = new InsnList();

                        inject.add(new VarInsnNode(org.objectweb.asm.Opcodes.ALOAD, 0));
                        inject.add(new MethodInsnNode(org.objectweb.asm.Opcodes.INVOKESTATIC, "com/NovaInv/GuiContainerInventoryPager", "handleMouseInput", "(Lnet/minecraft/client/gui/inventory/GuiContainer;)V"));

                        method.instructions.insertBefore(insn, inject);
                        patchedCount++;
                    }
                }

                patchedMouseInput = true;
            }
        }

        if (!patchedDrawScreen) {
            System.err.println("[NovaInventory] WARNING: GuiContainer drawScreen was not patched");
        }

        if (!patchedMouseClicked) {
            System.err.println("[NovaInventory] WARNING: GuiContainer mouseClicked was not patched");
        }

        if (!patchedMouseInput) {
            System.err.println("[NovaInventory] WARNING: GuiContainer handleMouseInput was not patched");
        }

        if (patchedCount == 0) {
            System.err.println("[NovaInventory] WARNING: Could not patch GuiContainer");
        }
        else {
            System.out.println("[NovaInventory] Patched GuiContainer hook count: " + patchedCount);
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classNode.accept(writer);
        return writer.toByteArray();
    }

    //Like a dozen helper methods because apparently calling the names of obs vs unobs classes is hell..

    private boolean isGuiContainerDrawScreen(String owner, MethodNode method) {
        return methodNameMatches(owner, method, "drawScreen", "func_73863_a") && "(IIF)V".equals(method.desc);
    }

    private boolean isGuiContainerMouseClicked(String owner, MethodNode method) {
        return methodNameMatches(owner, method, "mouseClicked", "func_73864_a") && "(III)V".equals(method.desc);
    }

    private boolean isGuiContainerHandleMouseInput(String owner, MethodNode method) {
        return methodNameMatches(owner, method, "handleMouseInput", "func_146274_d") && "()V".equals(method.desc);
    }

    private boolean isGL11PopMatrixCall(AbstractInsnNode insn) {
        if (!(insn instanceof MethodInsnNode)) {
            return false;
        }

        MethodInsnNode methodInsn = (MethodInsnNode)insn;

        return "org/lwjgl/opengl/GL11".equals(methodInsn.owner) && "glPopMatrix".equals(methodInsn.name) && "()V".equals(methodInsn.desc);
    }

    private AbstractInsnNode getFirstRealInstruction(MethodNode method) {
        AbstractInsnNode insn = method.instructions.getFirst();

        while (insn != null) {
            int type = insn.getType();

            if (type != AbstractInsnNode.LABEL && type != AbstractInsnNode.LINE && type != AbstractInsnNode.FRAME) {
                return insn;
            }

            insn = insn.getNext();
        }

        return null;
    }

    private String mapMethodName(String ownerInternalName, MethodNode method) {
        try {
            return FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(ownerInternalName, method.name, method.desc);
        }
        catch (Throwable t) {
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

        String deobfOwner = "net/minecraft/client/gui/inventory/GuiContainer";
        String mappedWithDeobfOwner = mapMethodName(deobfOwner, method);

        return mappedWithDeobfOwner.equals(mcpName) || mappedWithDeobfOwner.equals(srgName);
    }
}