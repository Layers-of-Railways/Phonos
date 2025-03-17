package io.github.foundationgames.phonos.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.world.World;

public class PhonosUtilClient {
    @Environment(EnvType.CLIENT)
    static World $getClientWorld() {
        return MinecraftClient.getInstance().world;
    }
}
