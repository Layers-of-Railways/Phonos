package io.github.foundationgames.phonos.network.packets.s2c;

import io.github.foundationgames.phonos.network.packets.S2CPacket;
import io.github.foundationgames.phonos.sound.SoundStorage;
import io.github.foundationgames.phonos.sound.emitter.SoundEmitterTree;
import io.github.foundationgames.phonos.util.PhonosUtil;
import io.github.foundationgames.phonos.world.sound.data.SoundData;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.world.World;

public record SoundPlayPacket(SoundData data, SoundEmitterTree tree) implements S2CPacket {
    public static final PacketCodec<RegistryByteBuf, SoundPlayPacket> PACKET_CODEC = PacketCodec.tuple(
        SoundData.PACKET_CODEC,
        SoundPlayPacket::data,
        SoundEmitterTree.PACKET_CODEC,
        SoundPlayPacket::tree,
        SoundPlayPacket::new
    );

    @Override
    @Environment(EnvType.CLIENT)
    public void handle(MinecraftClient mc) {
        World world = PhonosUtil.getClientWorld();
        if (world == null) return;
        SoundStorage.getInstance(world).play(world, data(), tree());
    }
}
