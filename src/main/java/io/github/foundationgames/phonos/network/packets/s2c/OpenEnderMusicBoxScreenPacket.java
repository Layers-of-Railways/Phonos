package io.github.foundationgames.phonos.network.packets.s2c;

import io.github.foundationgames.phonos.block.entity.EnderMusicBoxBlockEntity;
import io.github.foundationgames.phonos.client.screen.EnderMusicBoxScreen;
import io.github.foundationgames.phonos.network.packets.S2CPacket;
import io.netty.buffer.ByteBuf;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.util.math.BlockPos;

public record OpenEnderMusicBoxScreenPacket(BlockPos pos) implements S2CPacket {
    public static final PacketCodec<ByteBuf, OpenEnderMusicBoxScreenPacket> PACKET_CODEC = BlockPos.PACKET_CODEC.xmap(
        OpenEnderMusicBoxScreenPacket::new,
        OpenEnderMusicBoxScreenPacket::pos
    );

    @Override
    @Environment(EnvType.CLIENT)
    public void handle(MinecraftClient mc) {
        if (mc.world != null && mc.world.getBlockEntity(pos) instanceof EnderMusicBoxBlockEntity box) {
            mc.setScreen(new EnderMusicBoxScreen(box));
        }
    }
}
