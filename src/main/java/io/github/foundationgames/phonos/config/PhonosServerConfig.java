package io.github.foundationgames.phonos.config;

import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.autogen.*;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import io.github.foundationgames.phonos.Phonos;
import io.github.foundationgames.phonos.config.serializers.NoOpConfigSerializer;
import io.github.foundationgames.phonos.config.serializers.SaveCallbackConfigSerializer;
import io.github.foundationgames.phonos.mixin_interfaces.YetAnotherConfigLibImplDuck;
import io.github.foundationgames.phonos.network.PayloadPackets;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.World;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;

public class PhonosServerConfig {
    private static final ConfigClassHandler<PhonosServerConfig> CLIENT_HANDLER = ConfigClassHandler.createBuilder(PhonosServerConfig.class)
        .id(Phonos.id("server_config"))
        .serializer(NoOpConfigSerializer::new)
        .build();

    @Nullable
    private static ConfigClassHandler<PhonosServerConfig> SERVER_HANDLER = null;

    @Nullable
    private static WeakReference<MinecraftServer> CURRENT_SERVER = null;

    @SerialEntry(comment = "Max Microphone Cable Length [2, 32]")
    @AutoGen(category = "general")
    @IntSlider(min = 2, max = 32, step = 1)
    public int maxMicrophoneRange = 8;

    @SerialEntry(comment = "Max Wireless Microphone Range [2, 96]")
    @AutoGen(category = "general")
    @IntSlider(min = 2, max = 96, step = 1)
    public int maxWirelessMicrophoneRange = 32;

    @SerialEntry(comment = "Restrict Ender Music Box uploads to OPs")
    @AutoGen(category = "general", group = "ender_music_box")
    @TickBox
    public boolean restrictEnderMusicBoxUploads = false;

    @SerialEntry(comment = "Max Upload Size in KB [0 for unlimited, 100000]")
    @AutoGen(category = "general", group = "ender_music_box")
    @IntSlider(min = 0, max = 100*1000, step = 100) // max 100MB
    public int uploadLimitKB = 0;

    @SerialEntry(comment = "Base Radio Range [16, 4096]")
    @AutoGen(category = "radio")
    @IntField(min = 16, max = 4096)
    public int radioBaseRange = 16;

    @SerialEntry(comment = "Added Range per Block Above Sea Level [0, 16]")
    @AutoGen(category = "radio")
    @FloatSlider(min = 0, max = 16, step = 0.5f)
    public float worldHeightMultiplier = 0.0f;

    @SerialEntry(comment = "Added Range per Transmission Tower Block [0, 128]")
    @AutoGen(category = "radio")
    @FloatSlider(min = 0, max = 128, step = 1.0f)
    public float transmissionTowerMultiplier = 8.0f;

    @SerialEntry(comment = "Max Height for Transmission Tower [0, 256]")
    @AutoGen(category = "radio")
    @IntSlider(min = 0, max = 256, step = 1)
    public int maxTransmissionTowerHeight = 64;

    @ApiStatus.Internal
    public PhonosServerConfig() {
    }

    public static PhonosServerConfig get(World world) {
        return getHandler(world).instance();
    }

    public static ConfigClassHandler<PhonosServerConfig> getHandler(World world) {
        if (world instanceof ServerWorld serverWorld) {
            return getOrCreateServerHandler(serverWorld.getServer());
        } else if (world == null || world.isClient) {
            return CLIENT_HANDLER;
        } else {
            throw new IllegalArgumentException("World is neither client nor server world!");
        }
    }

    private static ConfigClassHandler<PhonosServerConfig> getOrCreateServerHandler(@NotNull MinecraftServer currentServer) {
        boolean replace = SERVER_HANDLER == null || CURRENT_SERVER == null || CURRENT_SERVER.get() != currentServer;

        if (replace) {
            if (SERVER_HANDLER != null) {
                SERVER_HANDLER.save();
            }

            CURRENT_SERVER = new WeakReference<>(currentServer);

            SERVER_HANDLER = ConfigClassHandler.createBuilder(PhonosServerConfig.class)
                .id(Phonos.id("server_config"))
                .serializer(config -> new SaveCallbackConfigSerializer<>(
                    config,
                    GsonConfigSerializerBuilder.create(config)
                        .setPath(currentServer.getSavePath(WorldSavePath.ROOT).resolve("serverconfig").resolve("phonos-server.json5"))
                        .setJson5(true)
                        .build(),
                    cfg -> currentServer.getPlayerManager().getPlayerList().forEach(p -> PayloadPackets.sendConfig(p, cfg))
                ))
                .build();

            SERVER_HANDLER.load();
        }

        return SERVER_HANDLER;
    }

    public PhonosServerConfig copy() {
        PhonosServerConfig copy = new PhonosServerConfig();

        for (var field : PhonosServerConfig.class.getDeclaredFields()) {
            if (field.isAnnotationPresent(SerialEntry.class)) {
                try {
                    field.set(copy, field.get(this));
                } catch (IllegalAccessException e) {
                    Phonos.LOG.error("Failed to copy field {} in PhonosServerConfig", field.getName(), e);
                }
            }
        }

        return copy;
    }

    @Environment(EnvType.CLIENT)
    public static Screen createScreen(Screen parent) {
        YetAnotherConfigLib yacl = CLIENT_HANDLER.generateGui();
        ((YetAnotherConfigLibImplDuck) yacl).phonos$setServerSided(true);
        ((YetAnotherConfigLibImplDuck) yacl).phonos$setHandler(CLIENT_HANDLER);
        return yacl.generateScreen(parent);
    }

    public static boolean isAuthorizedToChange(PlayerEntity player) {
        return player.hasPermissionLevel(2);
        //return player.getEquippedStack(EquipmentSlot.HEAD).getItem() == Items.GOLDEN_HELMET;
    }
}
