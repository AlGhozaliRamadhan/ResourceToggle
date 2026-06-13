# Resource Toggle v2.6.0 

## Overview
Resource Toggle v2.6.0 introduces major underlying architectural improvements and quality-of-life developer features for Minecraft Forge 1.20.1. The focus of this update is on seamless in-game transitions and robust development tools.

## Changelog

### 1. Isolated Developer Debug Command
- **Feature**: Added a dedicated in-game command (`/rtdebug`) exclusively for development testing.
- **How it works**: A new, fully isolated class (`DebugCommand.java`) was created and registered to the Forge event bus.
- **Result**: Developers can safely inject messy, experimental test code into the `runDebugTests()` method without risking spaghetti code creeping into the main mod logic (`ResourceToggleMod.java`).

### 2. Build & Compilation Fixes
- **Fix**: Resolved critical ForgeGradle compilation failures (`package net.minecraft.client does not exist`). 
- **Cause**: The previous development environment was executing the Gradle daemon using an outdated Java 8 installation, causing corrupted metadata generation when attempting to decompile Minecraft 1.20.1 (which strictly requires Java 17).
- **Solution**: Purged corrupted Forge caches, forced the Gradle daemon to strictly utilize the Eclipse Adoptium Java 17 JDK via `gradle.properties`, and successfully re-mapped the official Minecraft and Forge dependencies.

### 3. Keybinding Update
- **Feature**: Replaced the previous `R` keybinding with Numpad keys (1-9) for toggling specific resource packs, and Numpad 0 to clear all active resource packs.
- **Result**: Players can now assign up to 9 different resource packs to specific Numpad keys, rather than relying on a single toggle button.

## Technical Details for Developers
- **Target Version**: Minecraft 1.20.1
- **Mod Loader**: Forge 47.4.20
- **Mixin Version**: 0.8.5
- **Required Java**: Java 17 (Enforced via `org.gradle.java.home`)
