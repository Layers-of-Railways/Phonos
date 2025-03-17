package io.github.foundationgames.phonos.network.packets.s2c;

import io.github.foundationgames.phonos.network.packets.S2CPacket;
import io.github.foundationgames.phonos.sound.SoundStorage;
import io.github.foundationgames.phonos.sound.emitter.SoundEmitterTree;
import io.github.foundationgames.phonos.util.PhonosUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.world.World;

public record SoundUpdatePacket(SoundEmitterTree.Delta delta) implements S2CPacket {
    public static final PacketCodec<PacketByteBuf, SoundUpdatePacket> PACKET_CODEC = SoundEmitterTree.Delta.PACKET_CODEC.xmap(
        SoundUpdatePacket::new,
        SoundUpdatePacket::delta
    );

    @Override
    @Environment(EnvType.CLIENT)
    public void handle(MinecraftClient mc) {
        World world = PhonosUtil.getClientWorld();
        if (world == null) return;
        mc.execute(() -> SoundStorage.getInstance(world).update(delta));
    }
}
