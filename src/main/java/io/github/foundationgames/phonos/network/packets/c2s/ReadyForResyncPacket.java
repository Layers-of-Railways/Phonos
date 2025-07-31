package io.github.foundationgames.phonos.network.packets.c2s;

import io.github.foundationgames.phonos.network.packets.C2SPacket;
import io.github.foundationgames.phonos.sound.ResyncManager;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.server.network.ServerPlayerEntity;

public record ReadyForResyncPacket() implements C2SPacket {
    public static final PacketCodec<PacketByteBuf, ReadyForResyncPacket> PACKET_CODEC = PacketCodec.unit(new ReadyForResyncPacket());

    @Override
    public void handle(ServerPlayerEntity sender) {
        ResyncManager.resyncServer(sender);
    }
}
