package com.resourcetoggle;

import com.resourcetoggle.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import com.resourcetoggle.diagnostics.DiagnosticsManager;
import com.resourcetoggle.utils.LowEndOptimizations;

import java.util.ArrayList;
import java.util.List;

public class ResourcePackManager {
    private static PackRepository packRepository;
    private static List<String> vanillaPackStates;
    private static final java.util.Set<String> activePacks = new java.util.HashSet<>();
    private static volatile boolean isReloading = false;
    private static long lastReloadTime = 0;
    private static final long RELOAD_COOLDOWN = 1000; // 1 second cooldown
    private static volatile boolean isProcessing = false;
    private static volatile boolean isSilentReloadRequested = false;
    private static final long MIN_MEMORY_REQUIRED = 52428800; // 50MB minimum
    private static final long DESIRED_FREE_MEMORY = 104857600; // 100MB desired
    private static final int GC_ATTEMPTS = 3;
    private static boolean isLowEndMode = false;

    // Safety timeout: if reload doesn't complete within this time, force-reset flags.
    // This prevents permanent lockout when RRLS or other mods swallow the reload future.
    private static final long SAFETY_TIMEOUT_MS = 30_000; // 30 seconds
    private static long reloadStartTime = 0;

    public static void init() {
        MinecraftForge.EVENT_BUS.register(ResourcePackManager.class);
        packRepository = Minecraft.getInstance().getResourcePackRepository();
        saveVanillaState();
        DiagnosticsManager.init(); // Initialize diagnostics after other systems
        DiagnosticsManager.setDevKey("Milhae77");
        checkSystemSpecs();
    }

    private static void saveVanillaState() {
        vanillaPackStates = new ArrayList<>(packRepository.getSelectedIds());
    }

    private static void checkSystemSpecs() {
        Runtime runtime = Runtime.getRuntime();
        isLowEndMode = runtime.maxMemory() < 1073741824L; // Less than 1GB RAM
        DiagnosticsManager.logDiagnostic("Low-end mode: " + isLowEndMode);
    }

    /**
     * Safety timeout tick handler.
     * Runs every client tick to check if a reload has been stuck for too long.
     * This handles the case where RRLS (or another mod) intercepts reloadResourcePacks()
     * and never completes the CompletableFuture, which would otherwise leave
     * isReloading/isProcessing stuck as true permanently.
     */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        if (isReloading && reloadStartTime > 0) {
            long elapsed = System.currentTimeMillis() - reloadStartTime;
            if (elapsed > SAFETY_TIMEOUT_MS) {
                DiagnosticsManager.logDiagnostic(
                    "Safety timeout triggered after " + (elapsed / 1000) + "s — resetting reload state");
                resetReloadState();
            }
        }
    }

    public static void clearAllPacks() {
        if (isProcessing || isReloading || activePacks.isEmpty()) return;
        
        long currentTime = System.currentTimeMillis();
        if ((currentTime - lastReloadTime) < RELOAD_COOLDOWN) return;
        
        if (!checkAndPrepareMemory()) return;
        
        isProcessing = true;
        isReloading = true;
        reloadStartTime = System.currentTimeMillis();
        DiagnosticsManager.logDiagnostic("Clearing all resource packs");
        
        try {
            LowEndOptimizations.optimizeMemory();
            
            packRepository.setSelected(new ArrayList<>(vanillaPackStates));
            activePacks.clear();
            
            triggerReload();
        } catch (Exception e) {
            DiagnosticsManager.logDiagnostic("Toggle error: " + e.getMessage());
            handleReloadFailure();
            resetReloadState();
        }
    }

    public static void togglePackAtIndex(int numpadNumber) {
        // Prevent concurrent operations
        if (isProcessing || isReloading) return;

        // Cooldown
        long currentTime = System.currentTimeMillis();
        if ((currentTime - lastReloadTime) < RELOAD_COOLDOWN) return;

        List<String> selectedPacks = ModConfig.get().getSelectedPacks();
        int index = numpadNumber - 1;
        if (selectedPacks == null || index < 0 || index >= selectedPacks.size()) {
            return; // No pack mapped to this key
        }

        String targetPack = selectedPacks.get(index);

        if (!checkAndPrepareMemory()) return;

        // Set flags BEFORE any work
        isProcessing = true;
        isReloading = true;
        reloadStartTime = System.currentTimeMillis();
        DiagnosticsManager.logDiagnostic("Toggling resource pack at index " + index);

        try {
            LowEndOptimizations.optimizeMemory();

            // Toggle logic
            if (activePacks.contains(targetPack)) {
                activePacks.remove(targetPack);
            } else {
                activePacks.add(targetPack);
            }

            // Build new selection
            List<String> newSelection = new ArrayList<>(vanillaPackStates);
            for (String pack : activePacks) {
                if (!newSelection.contains(pack)) {
                    newSelection.add(pack);
                }
            }

            packRepository.setSelected(newSelection);
            triggerReload();
        } catch (Exception e) {
            DiagnosticsManager.logDiagnostic("Toggle error: " + e.getMessage());
            handleReloadFailure();
            resetReloadState();
        }
    }

    private static void triggerReload() {
        Minecraft mc = Minecraft.getInstance();
        mc.reloadResourcePacks().whenComplete((result, error) -> {
            mc.execute(() -> {
                try {
                    if (error != null) {
                        DiagnosticsManager.logDiagnostic("Reload failed: " + error.getMessage());
                        handleReloadFailure();
                    } else {
                        DiagnosticsManager.logDiagnostic("Reload completed successfully");
                        if (mc.levelRenderer != null) {
                            mc.levelRenderer.allChanged();
                        }
                        LowEndOptimizations.optimizeMemory();
                    }
                } finally {
                    resetReloadState();
                }
            });
        });
    }

    /**
     * Resets all reload/processing flags to allow future toggles.
     * Called from whenComplete() callback, safety timeout, and error handlers.
     */
    private static void resetReloadState() {
        isReloading = false;
        isProcessing = false;
        isSilentReloadRequested = false;
        reloadStartTime = 0;
        lastReloadTime = System.currentTimeMillis();
    }

    private static boolean checkAndPrepareMemory() {
        if (!ensureEnoughMemory()) {
            cleanupResources();
            if (!ensureEnoughMemory()) {
                DiagnosticsManager.logDiagnostic("Toggle failed — insufficient memory after cleanup");
                showLowMemoryWarning();
                return false;
            }
        }
        return true;
    }

    private static boolean ensureEnoughMemory() {
        Runtime runtime = Runtime.getRuntime();

        // If we already have enough memory, return early
        if (runtime.freeMemory() >= DESIRED_FREE_MEMORY) {
            return true;
        }

        // Try to free memory through garbage collection
        for (int i = 0; i < GC_ATTEMPTS; i++) {
            System.gc();
            if (runtime.freeMemory() >= MIN_MEMORY_REQUIRED) {
                return true;
            }
            try {
                Thread.sleep(100); // Give GC some time
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        return runtime.freeMemory() >= MIN_MEMORY_REQUIRED;
    }

    private static void cleanupResources() {
        // Minimal cleanup without reloading resources
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            mc.level.clearTintCaches();
        }
    }

    /**
     * Handles a reload failure by reverting to vanilla pack state.
     * Does NOT retry or recurse — just resets to a safe state.
     */
    private static void handleReloadFailure() {
        activePacks.clear();
        try {
            packRepository.setSelected(vanillaPackStates);
        } catch (Exception e) {
            // Last resort: clear all packs
            try {
                packRepository.setSelected(new ArrayList<>());
            } catch (Exception ignored) {
            }
        }
        Minecraft.getInstance().gui.getChat().addMessage(
                Component.literal("§c[ResourceToggle] Failed to load resource pack. Reverted to default."));
    }

    private static void showLowMemoryWarning() {
        Minecraft.getInstance().gui.getChat().addMessage(
                Component.literal("§c[ResourceToggle] Low memory — please close other applications")
        );
    }

    public static boolean isPackEnabled() {
        return !activePacks.isEmpty();
    }

    public static boolean isReloading() {
        return isReloading;
    }

    public static boolean isSilentReloadRequested() {
        return isSilentReloadRequested;
    }

    public static void updateVanillaState() {
        if (activePacks.isEmpty()) {
            saveVanillaState();
        }
    }
}

