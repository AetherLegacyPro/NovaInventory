package com.NovaInv;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import java.util.Map;

public class InventoryOverride implements IFMLLoadingPlugin {

    @Override
    public String[] getASMTransformerClass() {
        System.out.println("[NovaInv] getASMTransformerClass called!");
        return new String[] { "com.NovaInv.InventoryOverhaulTransformer" };
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
        System.out.println("[NovaInv] injectData called!");
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }

}
