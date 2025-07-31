package io.github.foundationgames.phonos.network.packets.s2c;

import io.github.foundationgames.phonos.network.ClientPayloadPackets;
import io.github.foundationgames.phonos.network.packets.S2CPacket;
import io.github.foundationgames.phonos.sound.ResyncManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;

public record PrepareForResyncPacket() implements S2CPacket {
    public static final PacketCodec<PacketByteBuf, PrepareForResyncPacket> PACKET_CODEC = PacketCodec.unit(new PrepareForResyncPacket());

    @Override
    public void handle(MinecraftClient mc) {
        ResyncManager.prepareClient();
        ClientPayloadPackets.sendReadyForResync();
    }
}
