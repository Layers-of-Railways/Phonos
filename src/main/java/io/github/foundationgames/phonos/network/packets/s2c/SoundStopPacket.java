package io.github.foundationgames.phonos.network.packets.s2c;

import io.github.foundationgames.phonos.network.packets.S2CPacket;
import io.github.foundationgames.phonos.sound.SoundStorage;
import io.netty.buffer.ByteBuf;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

public record SoundStopPacket(long sourceId) implements S2CPacket {
    public static final PacketCodec<ByteBuf, SoundStopPacket> PACKET_CODEC = PacketCodecs.VAR_LONG.xmap(
        SoundStopPacket::new,
        SoundStopPacket::sourceId
    );

    @Override
    @Environment(EnvType.CLIENT)
    public void handle(MinecraftClient mc) {
        if (mc.world == null) return;
        SoundStorage.getInstance(mc.world).stop(mc.world, sourceId);
    }
}
