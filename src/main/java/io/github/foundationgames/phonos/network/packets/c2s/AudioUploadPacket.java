package io.github.foundationgames.phonos.network.packets.c2s;

import io.github.foundationgames.phonos.network.packets.C2SPacket;
import io.github.foundationgames.phonos.sound.custom.ServerCustomAudio;
import io.github.foundationgames.phonos.util.PhonosUtil;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.server.network.ServerPlayerEntity;

import java.nio.ByteBuffer;

public record AudioUploadPacket(long streamId, int sampleRate, ByteBuffer samples, boolean last) implements C2SPacket {
    public static final PacketCodec<PacketByteBuf, AudioUploadPacket> PACKET_CODEC = PacketCodec.tuple(
        PacketCodecs.VAR_LONG,
        AudioUploadPacket::streamId,
        PacketCodecs.INTEGER,
        AudioUploadPacket::sampleRate,
        PhonosUtil.BYTE_BUFFER_PACKET_CODEC,
        AudioUploadPacket::samples,
        PacketCodecs.BOOL,
        AudioUploadPacket::last,
        AudioUploadPacket::new
    );

    @Override
    public void handle(ServerPlayerEntity sender) {
        ServerCustomAudio.receiveUpload(sender.server, sender, streamId, sampleRate, samples, last);
    }
}
