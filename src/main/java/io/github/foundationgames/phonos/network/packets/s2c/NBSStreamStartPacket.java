package io.github.foundationgames.phonos.network.packets.s2c;

import io.github.foundationgames.phonos.network.packets.S2CPacket;
import io.github.foundationgames.phonos.sound.nbs.stream.ClientIncomingNBSStreamHandler;
import io.github.foundationgames.phonos.sound.nbs.stream.NBSInitData;
import io.github.foundationgames.phonos.sound.stream.ClientIncomingStreamHandler;
import io.github.foundationgames.phonos.util.PhonosUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

import java.nio.ByteBuffer;

public record NBSStreamStartPacket(long streamId, NBSInitData initData) implements S2CPacket {
    public static final PacketCodec<PacketByteBuf, NBSStreamStartPacket> PACKET_CODEC = PacketCodec.tuple(
        PacketCodecs.VAR_LONG,
        NBSStreamStartPacket::streamId,
        NBSInitData.PACKET_CODEC,
        NBSStreamStartPacket::initData,
        NBSStreamStartPacket::new
    );

    @Override
    @Environment(EnvType.CLIENT)
    public void handle(MinecraftClient mc) {
        ClientIncomingNBSStreamHandler.initStream(streamId, initData);
    }
}
