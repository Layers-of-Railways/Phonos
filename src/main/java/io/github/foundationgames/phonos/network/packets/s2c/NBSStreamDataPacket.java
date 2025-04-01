package io.github.foundationgames.phonos.network.packets.s2c;

import io.github.foundationgames.phonos.network.packets.S2CPacket;
import io.github.foundationgames.phonos.sound.nbs.stream.ClientIncomingNBSStreamHandler;
import io.github.foundationgames.phonos.sound.nbs.stream.NBSChunk;
import io.github.foundationgames.phonos.sound.nbs.stream.NBSInitData;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

public record NBSStreamDataPacket(long streamId, NBSChunk chunk) implements S2CPacket {
    public static final PacketCodec<PacketByteBuf, NBSStreamDataPacket> PACKET_CODEC = PacketCodec.tuple(
        PacketCodecs.VAR_LONG,
        NBSStreamDataPacket::streamId,
        NBSChunk.PACKET_CODEC,
        NBSStreamDataPacket::chunk,
        NBSStreamDataPacket::new
    );

    @Override
    @Environment(EnvType.CLIENT)
    public void handle(MinecraftClient mc) {
        ClientIncomingNBSStreamHandler.receiveChunk(streamId, chunk);
    }
}
