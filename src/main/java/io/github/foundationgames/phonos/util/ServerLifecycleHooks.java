package io.github.foundationgames.phonos.util;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;

public class ServerLifecycleHooks {
    private static MinecraftServer currentServer;

    public static void init() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            currentServer = server;
        });
    }

    public static MinecraftServer getCurrentServer() {
        return currentServer;
    }
}
