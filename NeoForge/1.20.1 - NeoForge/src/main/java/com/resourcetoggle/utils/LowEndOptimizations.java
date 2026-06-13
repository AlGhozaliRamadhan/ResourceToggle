package com.resourcetoggle.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import java.util.concurrent.CompletableFuture;

public class LowEndOptimizations {
    private static final long TEXTURE_CLEANUP_INTERVAL = 30000; // 30 seconds
    private static long lastTextureCleanup = 0;

    public static void preloadOptimization() {
        try {
            if (isLowEndSystem()) {
                TextureManager textureManager = Minecraft.getInstance().getTextureManager();
                ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
                if (textureManager != null && resourceManager != null) {
                    CompletableFuture.runAsync(() -> {
                        try {
                            textureManager.tick();
                            // In 1.20.2 we need to be more careful with texture handling
                            textureManager.bindForSetup(new ResourceLocation("textures/atlas/blocks.png"));
                            System.gc();
                        } catch (Exception e) {
                            // Silently handle texture manager errors
                        }
                    });
                }
            }
        } catch (Exception e) {
            // Fail silently to prevent crashes
        }
    }

    public static boolean isLowEndSystem() {
        try {
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            
            // Consider system low-end if either condition is met
            return maxMemory < 1073741824L || // Less than 1GB max
                   (totalMemory - freeMemory) > (maxMemory * 0.8); // Using more than 80% of max
        } catch (Exception e) {
            return true; // Assume low-end if we can't check
        }
    }

    public static void cleanupTextures() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastTextureCleanup > TEXTURE_CLEANUP_INTERVAL) {
            lastTextureCleanup = currentTime;
            
            try {
                CompletableFuture.runAsync(() -> {
                    try {
                        TextureManager textureManager = Minecraft.getInstance().getTextureManager();
                        if (textureManager != null) {
                            textureManager.tick();
                        }
                        System.gc();
                        Thread.sleep(50); // Brief pause to allow GC to work
                    } catch (Exception e) {
                        // Silently handle cleanup errors
                    }
                });
            } catch (Exception e) {
                // Fail silently
            }
        }
    }

    public static void optimizeMemory() {
        try {
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            
            if (maxMemory < 1073741824L || (totalMemory - freeMemory) > (maxMemory * 0.75)) {
                // More aggressive optimization for constrained memory
                System.gc();
                Thread.sleep(100); // Give GC some time to work
                
                if (Minecraft.getInstance().levelRenderer != null) {
                    Minecraft.getInstance().levelRenderer.graphicsChanged();
                }
                
                // Force another GC if memory is still tight
                if ((runtime.totalMemory() - runtime.freeMemory()) > (maxMemory * 0.85)) {
                    System.gc();
                }
            }
        } catch (Exception e) {
            // Fail silently
        }
    }
}
