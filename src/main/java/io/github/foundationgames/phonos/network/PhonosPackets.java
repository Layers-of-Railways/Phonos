package io.github.foundationgames.phonos.network;

import io.github.foundationgames.phonos.network.packets.PacketSet;
import io.github.foundationgames.phonos.network.packets.s2c.SoundPlayPacket;

public class PhonosPackets {
    public static final PacketSet PACKETS = PacketSet.builder("phonos", 1)
        .s2c(SoundPlayPacket.class, SoundPlayPacket.PACKET_CODEC)
        .build();
}
