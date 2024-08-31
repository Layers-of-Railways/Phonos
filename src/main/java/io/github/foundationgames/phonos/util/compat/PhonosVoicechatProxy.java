package io.github.foundationgames.phonos.util.compat;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Supplier;

public class PhonosVoicechatProxy {
    public static boolean isLoaded() {
        return FabricLoader.getInstance().isModLoaded("voicechat");
    }

    public static <T> @Nullable T runIfLoaded(Supplier<Supplier<T>> supplier) {
        return runIfLoaded(supplier, null);
    }

    public static <T> T runIfLoaded(Supplier<Supplier<T>> supplier, T defaultValue) {
        if (isLoaded()) {
            return supplier.get().get();
        }
        return defaultValue;
    }

    public static void executeIfLoaded(Supplier<Runnable> runnable) {
        if (isLoaded()) {
            runnable.get().run();
        }
    }

    public static boolean isStreaming(long streamId) {
        return runIfLoaded(() -> () -> PhonosVoicechatPlugin.isStreaming(streamId), false);
    }

    public static boolean isStreaming(ServerPlayerEntity serverPlayer) {
        return runIfLoaded(() -> () -> PhonosVoicechatPlugin.isStreaming(serverPlayer), false);
    }

    public static boolean startStream(ServerPlayerEntity serverPlayer, long streamId) {
        return runIfLoaded(() -> () -> PhonosVoicechatPlugin.startStream(serverPlayer, streamId), false);
    }

    public static void resumeStream(ServerPlayerEntity target, long streamId) {
        executeIfLoaded(() -> () -> PhonosVoicechatPlugin.resumeStream(target, streamId));
    }

    public static void stopStreaming(long streamId) {
        executeIfLoaded(() -> () -> PhonosVoicechatPlugin.stopStreaming(streamId));
    }

    @Environment(EnvType.CLIENT)
    public static void startClientMicrophoneStream(UUID channelId, long streamId) {
        executeIfLoaded(() -> () -> PhonosVoicechatPlugin.startClientMicrophoneStream(channelId, streamId));
    }

    @Environment(EnvType.CLIENT)
    public static void endClientMicrophoneStream(UUID channelId, long streamId) {
        executeIfLoaded(() -> () -> PhonosVoicechatPlugin.endClientMicrophoneStream(channelId, streamId));
    }
}
