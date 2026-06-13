package com.resourcetoggle.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig.Type;

import java.util.ArrayList;
import java.util.List;

public class ModConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC; // Made public

    private static final ForgeConfigSpec.BooleanValue INCLUDE_ALL_PACKS;
    private static final ForgeConfigSpec.BooleanValue INCLUDE_VANILLA;
    private static final ForgeConfigSpec.ConfigValue<String> DEFAULT_PACK;
    private static final ForgeConfigSpec.ConfigValue<List<String>> RESOURCE_PACKS;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> SELECTED_PACKS;

    static {
        BUILDER.push("Resource Toggle Settings");

        INCLUDE_ALL_PACKS = BUILDER
            .comment("Include all available resource packs")
            .define("includeAllPacks", false);

        INCLUDE_VANILLA = BUILDER
            .comment("Include vanilla resources in the toggle rotation")
            .define("includeVanilla", true);

        DEFAULT_PACK = BUILDER
            .comment("Default resource pack to load on startup")
            .define("defaultPack", "");

        RESOURCE_PACKS = BUILDER
            .comment("List of resource packs to include in rotation")
            .define("resourcePacks", new ArrayList<>());

        SELECTED_PACKS = BUILDER
            .comment("Currently selected resource packs")
            .defineList("selectedPacks", new ArrayList<>(), obj -> obj instanceof String);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    private static ModConfig INSTANCE;

    public static void register() {
        ModLoadingContext.get().registerConfig(Type.COMMON, SPEC);
        INSTANCE = new ModConfig();
    }

    public static ModConfig get() {
        return INSTANCE;
    }

    public boolean includeAllPacks() {
        return INCLUDE_ALL_PACKS.get();
    }

    public boolean includeVanilla() {
        return INCLUDE_VANILLA.get();
    }

    public String defaultPack() {
        return DEFAULT_PACK.get();
    }

    public List<String> resourcePacks() {
        return RESOURCE_PACKS.get();
    }

    public void setSelectedPacks(List<String> packIds) {
        SELECTED_PACKS.set(packIds);
    }

    @SuppressWarnings("unchecked")
    public List<String> getSelectedPacks() {
        return (List<String>) SELECTED_PACKS.get();
    }

    public void saveConfig() {
        SPEC.save();
    }
}
