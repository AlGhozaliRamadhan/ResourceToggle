package com.resourcetoggle.debug;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

/**
 * A completely isolated debug command for development testing.
 * Type "/rtdebug" in-game to trigger whatever logic you are testing.
 * This ensures you don't create spaghetti code in the main mod classes.
 */
@Mod.EventBusSubscriber(modid = "resourcetoggle")
public class DebugCommand {

    @SubscribeEvent
    public static void onCommandRegister(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        
        dispatcher.register(Commands.literal("rtdebug")
            .executes(context -> {
                runDebugTests(context.getSource());
                return 1;
            })
        );
    }

    private static void runDebugTests(CommandSourceStack source) {
        source.sendSystemMessage(Component.literal("§e[Resource Toggle] §fRunning debug tests..."));
        
        // ----------------------------------------------------
        // PUT ALL YOUR ISOLATED TEST CODE HERE
        // ----------------------------------------------------
        
        try {
            
            // Example:
            // ResourcePackManager.toggleResourcePack();
            
            source.sendSystemMessage(Component.literal("§aTest logic executed successfully."));
        } catch (Exception e) {
            source.sendSystemMessage(Component.literal("§cTest failed: " + e.getMessage()));
            e.printStackTrace();
        }
    }
}
