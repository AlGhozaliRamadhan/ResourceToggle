package com.resourcetoggle;

import com.resourcetoggle.config.ModConfig;
import com.resourcetoggle.config.ResourceToggleConfigScreen;
import com.resourcetoggle.diagnostics.DiagnosticsManager;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;


@Mod(ResourceToggleMod.MOD_ID)
public class ResourceToggleMod {
    public static final String MOD_ID = "resourcetoggle";
    private static final Logger LOGGER = LogManager.getLogger();

    public static final KeyMapping[] NUMPAD_KEYS = new KeyMapping[10];

    static {
        NUMPAD_KEYS[0] = new KeyMapping(
                "key.resourcetoggle.clear_all",
                GLFW.GLFW_KEY_KP_0,
                "key.categories.resourcetoggle");

        for (int i = 1; i <= 9; i++) {
            NUMPAD_KEYS[i] = new KeyMapping(
                    "key.resourcetoggle.toggle_pack_" + i,
                    GLFW.GLFW_KEY_KP_0 + i,
                    "key.categories.resourcetoggle");
        }
    }

    private static long lastKeyPressTime = 0;
    private static final long KEY_PRESS_COOLDOWN = 1000; // 1 second cooldown
    private static final long MIN_MEMORY_REQUIRED = 524288000; // 500MB

    public ResourceToggleMod() {
        try {
            // Check system memory before initializing
            if (Runtime.getRuntime().maxMemory() < MIN_MEMORY_REQUIRED) {
                LOGGER.warn("Low memory detected. Some features may be limited.");
            }

            ModConfig.register();
            IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
            modEventBus.addListener(this::registerKeybindings);
            MinecraftForge.EVENT_BUS.register(this);

            // Safe initialization with retry
            initializeModSafely();

            // Register config screen with error handling
            try {
                ModLoadingContext.get().registerExtensionPoint(
                        ConfigScreenHandler.ConfigScreenFactory.class,
                        () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, screen) -> new ResourceToggleConfigScreen(screen)));
            } catch (Exception e) {
                LOGGER.error("Failed to register config screen", e);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to initialize mod", e);
        }
    }

    private void initializeModSafely() {
        int attempts = 0;
        boolean success = false;
        while (attempts < 3 && !success) {
            try {
                ResourcePackManager.init();
                success = true;
            } catch (Exception e) {
                attempts++;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    // Ignore
                }
            }
        }
    }

    private void registerKeybindings(RegisterKeyMappingsEvent event) {
        for (KeyMapping key : NUMPAD_KEYS) {
            event.register(key);
        }
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.Key event) {
        // Check for Control + Alt + D combination
        if (event.getKey() == GLFW.GLFW_KEY_D && 
            Screen.hasControlDown() && Screen.hasAltDown()) {
            if (Screen.hasShiftDown()) {
                // Ctrl + Alt + Shift + D toggles debug mode
                DiagnosticsManager.toggleDebugMode();
            } else {
                // Ctrl + Alt + D shows diagnostics
                DiagnosticsManager.showDiagnostics();
            }
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        // Consume toggle keybindings and trigger the toggle directly.
        if (NUMPAD_KEYS[0].consumeClick()) {
            ResourcePackManager.clearAllPacks();
        } else {
            for (int i = 1; i <= 9; i++) {
                if (NUMPAD_KEYS[i].consumeClick()) {
                    ResourcePackManager.togglePackAtIndex(i);
                    break;
                }
            }
        }
    }
}

