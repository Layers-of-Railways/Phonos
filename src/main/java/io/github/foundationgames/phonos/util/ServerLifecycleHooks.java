package io.github.foundationgames.phonos.util;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;

public class ServerLifecycleHooks {
    @Nullable
    private static WeakReference<MinecraftServer> currentServer = null;

    public static void init() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            currentServer = new WeakReference<>(server);
        });
    }

    public static @Nullable MinecraftServer getCurrentServer() {
        if (currentServer == null)
            return null;
        return currentServer.get();
    }
}
