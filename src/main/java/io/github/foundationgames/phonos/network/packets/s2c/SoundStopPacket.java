package io.github.foundationgames.phonos.network.packets.s2c;

import io.github.foundationgames.phonos.network.packets.S2CPacket;
import io.github.foundationgames.phonos.sound.SoundStorage;
import io.github.foundationgames.phonos.util.PhonosUtil;
import io.netty.buffer.ByteBuf;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.world.World;

public record SoundStopPacket(long sourceId) implements S2CPacket {
    public static final PacketCodec<ByteBuf, SoundStopPacket> PACKET_CODEC = PacketCodecs.VAR_LONG.xmap(
        SoundStopPacket::new,
        SoundStopPacket::sourceId
    );

    @Override
    @Environment(EnvType.CLIENT)
    public void handle(MinecraftClient mc) {
        World world = PhonosUtil.getClientWorld();
        if (world == null) return;
        SoundStorage.getInstance(world).stop(world, sourceId);
    }
}
