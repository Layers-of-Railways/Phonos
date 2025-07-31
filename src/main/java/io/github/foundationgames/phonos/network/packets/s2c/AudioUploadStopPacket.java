package io.github.foundationgames.phonos.network.packets.s2c;

import io.github.foundationgames.phonos.Phonos;
import io.github.foundationgames.phonos.client.screen.EnderMusicBoxScreen;
import io.github.foundationgames.phonos.network.packets.S2CPacket;
import io.github.foundationgames.phonos.sound.custom.ClientCustomAudioUploader;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;

public record AudioUploadStopPacket(long uploadId, Text message) implements S2CPacket {
    public static final PacketCodec<RegistryByteBuf, AudioUploadStopPacket> PACKET_CODEC = PacketCodec.tuple(
        PacketCodecs.VAR_LONG,
        AudioUploadStopPacket::uploadId,
        TextCodecs.REGISTRY_PACKET_CODEC,
        AudioUploadStopPacket::message,
        AudioUploadStopPacket::new
    );

    @Override
    @Environment(EnvType.CLIENT)
    public void handle(MinecraftClient mc) {
        if (mc.currentScreen instanceof EnderMusicBoxScreen screen) {
            screen.onAudioUploadCancel(message);
        }

        ClientCustomAudioUploader.cancelUpload(uploadId);
        Phonos.LOG.warn("Upload canceled by server for sound {}: {}", Long.toHexString(uploadId), message.getString());
    }
}
