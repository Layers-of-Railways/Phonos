package io.github.foundationgames.phonos.network.packets.s2c;

import io.github.foundationgames.phonos.Phonos;
import io.github.foundationgames.phonos.client.screen.EnderMusicBoxScreen;
import io.github.foundationgames.phonos.network.packets.S2CPacket;
import io.github.foundationgames.phonos.sound.custom.ClientCustomAudioUploader;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

public record AudioUploadStatusPacket(long uploadId, boolean ok) implements S2CPacket {
    public static final PacketCodec<PacketByteBuf, AudioUploadStatusPacket> PACKET_CODEC = PacketCodec.tuple(
        PacketCodecs.VAR_LONG,
        AudioUploadStatusPacket::uploadId,
        PacketCodecs.BOOL,
        AudioUploadStatusPacket::ok,
        AudioUploadStatusPacket::new
    );

    @Override
    @Environment(EnvType.CLIENT)
    public void handle(MinecraftClient mc) {
        if (mc.currentScreen instanceof EnderMusicBoxScreen screen) {
            screen.onAudioUploadStatus(uploadId, ok);
        }
        if (ok) {
            ClientCustomAudioUploader.sendUploadPackets(uploadId);
        } else {
            Phonos.LOG.warn("Denied upload for sound {}", Long.toHexString(uploadId));
        }
    }
}
