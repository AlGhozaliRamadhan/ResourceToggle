package com.resourcetoggle.diagnostics;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import com.resourcetoggle.ResourcePackManager;  
import java.util.ArrayList;
import java.util.List;

public class DiagnosticsManager {
    private static final String DEV_KEY = "";
    private static boolean isDevKeyValid = false;
    private static final List<String> diagnosticLogs = new ArrayList<>();
    private static final int MAX_LOGS = 100;
    private static boolean debugMode = false;
    private static long lastDebugUpdate = 0;
    private static final long DEBUG_UPDATE_INTERVAL = 1000;
    private static final String DEBUG_PREFIX = "§8[ResourceToggle] ";
    private static Component lastDebugMessage = null;
    private static DiagnosticsManager INSTANCE;
    private static boolean debugForceDisabled = false;  // Added this
    private static boolean isEventRegistered = false;   // Added this

    private DiagnosticsManager() {
        // Private constructor - don't register events here
    }

    public static void init() {
        if (INSTANCE == null) {
            INSTANCE = new DiagnosticsManager();
            debugMode = false; // Start with debug disabled until dev key is verified
            verifyAndInitialize();
        }
    }

    private static void verifyAndInitialize() {
        if (isDevKeyValid) {
            NeoForge.EVENT_BUS.register(INSTANCE);
            isEventRegistered = true;
            debugMode = true;
            updateDebugStats();
        } else {
            // Silently disable all debug features if dev key is not valid
            debugMode = false;
            isEventRegistered = false;
            if (INSTANCE != null) {
                NeoForge.EVENT_BUS.unregister(INSTANCE);
            }
        }
    }

    public static void setDevKey(String key) {
        boolean wasValid = isDevKeyValid;
        isDevKeyValid = DEV_KEY.equals(key);
        
        // Handle changes in dev key validity
        if (wasValid != isDevKeyValid) {
            verifyAndInitialize();
        }
    }

    public static void logDiagnostic(String message) {
        if (!isDevKeyValid) return; // Silently ignore if dev key not valid
        String timeStamp = String.format("[%tT] ", System.currentTimeMillis());
        String logMessage = timeStamp + message;
        diagnosticLogs.add(logMessage);
        
        if (debugMode) {
            showDebugOverlay(message);
        }
        
        if (diagnosticLogs.size() > MAX_LOGS) {
            diagnosticLogs.remove(0);
        }
    }

    public static void showDiagnostics() {
        if (!isDevKeyValid) return; // Silently ignore if dev key not valid
        Minecraft.getInstance().gui.getChat().addMessage(
            Component.literal("§6=== ResourceToggle Diagnostics ===")
        );

        // Show memory usage
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / 1024 / 1024;
        long totalMemory = runtime.totalMemory() / 1024 / 1024;
        long freeMemory = runtime.freeMemory() / 1024 / 1024;
        long usedMemory = totalMemory - freeMemory;

        Minecraft.getInstance().gui.getChat().addMessage(
            Component.literal(String.format("§7Memory - Used: %dMB, Free: %dMB, Total: %dMB, Max: %dMB",
                usedMemory, freeMemory, totalMemory, maxMemory))
        );

        // Show recent logs
        for (int i = Math.max(0, diagnosticLogs.size() - 5); i < diagnosticLogs.size(); i++) {
            Minecraft.getInstance().gui.getChat().addMessage(
                Component.literal("§7" + diagnosticLogs.get(i))
            );
        }
    }

    public static void toggleDebugMode() {
        if (!isDevKeyValid) {
            return; // Silently ignore if dev key not valid
        }
        
        debugForceDisabled = !debugForceDisabled;
        debugMode = !debugForceDisabled;
        
        if (debugMode) {
            ensureDebugEnabled();
        } else {
            if (isEventRegistered) {
                NeoForge.EVENT_BUS.unregister(INSTANCE);
                isEventRegistered = false;
            }
            showMessage("§c" + DEBUG_PREFIX + "Debug mode disabled");
        }
    }

    private static void ensureDebugEnabled() {
        if (!isEventRegistered) {
            NeoForge.EVENT_BUS.register(INSTANCE);
            isEventRegistered = true;
        }
        showMessage("§a" + DEBUG_PREFIX + "Debug mode active");
        updateDebugStats();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {  // Changed from static to instance method
        if (!debugMode || !isDevKeyValid || event.phase != TickEvent.Phase.END) return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastDebugUpdate < DEBUG_UPDATE_INTERVAL) return;
        lastDebugUpdate = currentTime;
        
        updateDebugStats();
    }

    private static void updateDebugStats() {
        Runtime runtime = Runtime.getRuntime();
        long freeMemory = runtime.freeMemory() / 1024 / 1024;
        long totalMemory = runtime.totalMemory() / 1024 / 1024;
        long usedMemory = totalMemory - freeMemory;

        Component newMessage = Component.literal(String.format(
            DEBUG_PREFIX + "§7Memory: §f%d§7/§f%d MB §8| §7Logs: §f%d §8| §7Pack: %s",
            usedMemory,
            totalMemory,
            diagnosticLogs.size(),
            ResourcePackManager.isPackEnabled() ? "§aEnabled" : "§cDisabled"
        ));

        // Only show message if it's different from last one
        if (lastDebugMessage == null || !lastDebugMessage.getString().equals(newMessage.getString())) {
            if (Minecraft.getInstance().gui != null) {
                Minecraft.getInstance().gui.getChat().addMessage(newMessage);
                lastDebugMessage = newMessage;
            }
        }
    }

    private static void showDebugOverlay(String message) {
        if (Minecraft.getInstance().gui != null) {
            Minecraft.getInstance().gui.getChat().addMessage(
                Component.literal(DEBUG_PREFIX + "§7" + message)
            );
        }
    }

    private static void showMessage(String message) {
        if (Minecraft.getInstance().gui != null) {
            Minecraft.getInstance().gui.getChat().addMessage(
                Component.literal(message)
            );
        }
    }

    private static void showErrorMessage(String message) {
        if (Minecraft.getInstance().gui != null) {
            Minecraft.getInstance().gui.getChat().addMessage(
                Component.literal("§c" + message)
            );
        }
    }
}
