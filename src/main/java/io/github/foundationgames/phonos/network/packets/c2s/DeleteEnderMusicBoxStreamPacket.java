package io.github.foundationgames.phonos.network.packets.c2s;

import io.github.foundationgames.phonos.block.entity.EnderMusicBoxBlockEntity;
import io.github.foundationgames.phonos.network.packets.C2SPacket;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

public record DeleteEnderMusicBoxStreamPacket(BlockPos pos, long streamId) implements C2SPacket {
    public static final PacketCodec<PacketByteBuf, DeleteEnderMusicBoxStreamPacket> PACKET_CODEC = PacketCodec.tuple(
        BlockPos.PACKET_CODEC,
        DeleteEnderMusicBoxStreamPacket::pos,
        PacketCodecs.VAR_LONG,
        DeleteEnderMusicBoxStreamPacket::streamId,
        DeleteEnderMusicBoxStreamPacket::new
    );

    @Override
    public void handle(ServerPlayerEntity sender) {
        var world = sender.getWorld();

        if (world.getBlockEntity(pos) instanceof EnderMusicBoxBlockEntity entity && entity.canModifyStreams(sender)) {
            entity.deleteStream(streamId);
        }
    }
}
