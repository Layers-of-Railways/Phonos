package io.github.foundationgames.phonos.network.packets;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;

public interface S2CPacket {
    @Environment(EnvType.CLIENT)
    void handle(MinecraftClient mc);
}
