package io.github.foundationgames.phonos.item;

import com.mojang.serialization.Codec;
import io.github.foundationgames.phonos.Phonos;
import io.github.foundationgames.phonos.item.AudioCableItem.CableConnectionPoint;
import net.minecraft.component.ComponentType;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Unit;

import java.util.function.UnaryOperator;

@SuppressWarnings("SameParameterValue")
public class PhonosDataComponents {
    public static final ComponentType<ItemStack> PORTABLE_RECORD_CONTENTS = register(
        "portable_record_contents",
        ItemStack.OPTIONAL_CODEC,
        ItemStack.OPTIONAL_PACKET_CODEC,
        true
    );
    public static final ComponentType<Long> EMITTER_ID = register(
        "emitter_id",
        Codec.LONG,
        PacketCodecs.VAR_LONG,
        false
    );
    public static final ComponentType<Integer> CHANNEL = register(
        "channel",
        Codec.INT,
        PacketCodecs.VAR_INT,
        false
    );
    public static final ComponentType<String> SATELLITE_CHANNEL = register(
        "satellite_channel",
        Codec.STRING,
        PacketCodecs.STRING,
        false
    );
    public static final ComponentType<CableConnectionPoint>
        AUDIO_CABLE_INPUT = register("audio_cable_input", CableConnectionPoint.CODEC, CableConnectionPoint.PACKET_CODEC),
        AUDIO_CABLE_OUTPUT = register("audio_cable_output", CableConnectionPoint.CODEC, CableConnectionPoint.PACKET_CODEC)
    ;
    public static ComponentType<Unit>
        NOISE_CANCELLING = register("noise_cancelling"),
        GLOWING = register("glowing")
    ;

    private static ComponentType<Unit> register(String id) {
        return register(id, Unit.CODEC, PacketCodec.unit(Unit.INSTANCE));
    }

    private static <T> ComponentType<T> register(String id, Codec<T> codec, PacketCodec<? super RegistryByteBuf, T> packetCodec) {
        return register(id, codec, packetCodec, false);
    }

    private static <T> ComponentType<T> register(String id, Codec<T> codec, PacketCodec<? super RegistryByteBuf, T> packetCodec, boolean cache) {
        return register(id, builder -> {
            builder = builder.codec(codec).packetCodec(packetCodec);
            if (cache) {
                builder = builder.cache();
            }
            return builder;
        });
    }

    private static <T> ComponentType<T> register(String id, UnaryOperator<ComponentType.Builder<T>> builder) {
        return Registry.register(Registries.DATA_COMPONENT_TYPE, Phonos.id(id), builder.apply(ComponentType.builder()).build());
    }

    public static void init() {}
}
