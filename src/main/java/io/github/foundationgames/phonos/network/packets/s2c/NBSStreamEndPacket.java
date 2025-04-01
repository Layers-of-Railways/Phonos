package io.github.foundationgames.phonos.network.packets.s2c;

import io.github.foundationgames.phonos.network.packets.S2CPacket;
import io.github.foundationgames.phonos.sound.nbs.stream.ClientIncomingNBSStreamHandler;
import io.github.foundationgames.phonos.sound.stream.ClientIncomingStreamHandler;
import io.netty.buffer.ByteBuf;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

public record NBSStreamEndPacket(long streamId) implements S2CPacket {
    public static final PacketCodec<ByteBuf, NBSStreamEndPacket> PACKET_CODEC = PacketCodecs.VAR_LONG.xmap(
        NBSStreamEndPacket::new,
        NBSStreamEndPacket::streamId
    );

    @Override
    @Environment(EnvType.CLIENT)
    public void handle(MinecraftClient mc) {
        ClientIncomingNBSStreamHandler.endStream(streamId);
    }
}
