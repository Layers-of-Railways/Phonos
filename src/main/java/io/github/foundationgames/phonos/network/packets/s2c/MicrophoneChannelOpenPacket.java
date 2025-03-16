package io.github.foundationgames.phonos.network.packets.s2c;

import io.github.foundationgames.phonos.network.packets.S2CPacket;
import io.github.foundationgames.phonos.util.compat.PhonosVoicechatProxy;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.Uuids;

import java.util.UUID;

public record MicrophoneChannelOpenPacket(UUID channelId, long streamId, UUID speakingPlayer) implements S2CPacket {
    public static final PacketCodec<PacketByteBuf, MicrophoneChannelOpenPacket> PACKET_CODEC = PacketCodec.tuple(
        Uuids.PACKET_CODEC,
        MicrophoneChannelOpenPacket::channelId,
        PacketCodecs.VAR_LONG,
        MicrophoneChannelOpenPacket::streamId,
        Uuids.PACKET_CODEC,
        MicrophoneChannelOpenPacket::speakingPlayer,
        MicrophoneChannelOpenPacket::new
    );

    @Override
    @Environment(EnvType.CLIENT)
    public void handle(MinecraftClient mc) {
        PhonosVoicechatProxy.startClientMicrophoneStream(channelId, streamId, speakingPlayer);
    }
}
