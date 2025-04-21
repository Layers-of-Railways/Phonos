package io.github.foundationgames.phonos.network.packets.s2c;

import io.github.foundationgames.phonos.network.packets.S2CPacket;
import io.github.foundationgames.phonos.sound.stream.ClientIncomingStreamHandler;
import io.github.foundationgames.phonos.util.PhonosUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

import java.nio.ByteBuffer;

public record AudioStreamDataPacket(long streamId, int sampleRate, byte[] samples) implements S2CPacket {
    public static final PacketCodec<PacketByteBuf, AudioStreamDataPacket> PACKET_CODEC = PacketCodec.tuple(
        PacketCodecs.VAR_LONG,
        AudioStreamDataPacket::streamId,
        PacketCodecs.INTEGER,
        AudioStreamDataPacket::sampleRate,
        PhonosUtil.BYTE_ARRAY_PACKET_CODEC,
        AudioStreamDataPacket::samples,
        AudioStreamDataPacket::new
    );

    @Override
    @Environment(EnvType.CLIENT)
    public void handle(MinecraftClient mc) {
        var buffer = PhonosUtil.byteArrayToByteBuffer(samples, ByteBuffer::allocate);
        ClientIncomingStreamHandler.receiveStream(streamId, sampleRate, buffer);
    }
}
