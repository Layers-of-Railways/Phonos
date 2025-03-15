package io.github.foundationgames.phonos.network.packets;

import net.minecraft.server.network.ServerPlayerEntity;

public interface C2SPacket {
    void handle(ServerPlayerEntity sender);
}
