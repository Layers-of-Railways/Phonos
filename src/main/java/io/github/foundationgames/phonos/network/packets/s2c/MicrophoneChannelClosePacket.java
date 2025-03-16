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

public record MicrophoneChannelClosePacket(UUID channelId, long streamId) implements S2CPacket {
    public static final PacketCodec<PacketByteBuf, MicrophoneChannelClosePacket> PACKET_CODEC = PacketCodec.tuple(
        Uuids.PACKET_CODEC,
        MicrophoneChannelClosePacket::channelId,
        PacketCodecs.VAR_LONG,
        MicrophoneChannelClosePacket::streamId,
        MicrophoneChannelClosePacket::new
    );

    @Override
    @Environment(EnvType.CLIENT)
    public void handle(MinecraftClient mc) {
        PhonosVoicechatProxy.endClientMicrophoneStream(channelId, streamId);
    }
}
