package io.github.foundationgames.phonos.network.packets.c2s;

import io.github.foundationgames.phonos.Phonos;
import io.github.foundationgames.phonos.block.entity.EnderMusicBoxBlockEntity;
import io.github.foundationgames.phonos.network.PayloadPackets;
import io.github.foundationgames.phonos.network.packets.C2SPacket;
import io.github.foundationgames.phonos.sound.custom.PhonosAudioRecord;
import io.github.foundationgames.phonos.sound.custom.ServerCustomAudio;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

public record RequestEnderMusicBoxUploadSessionPacket(BlockPos pos, String name, PhonosAudioRecord.FileType fileType) implements C2SPacket {
    public static final PacketCodec<PacketByteBuf, RequestEnderMusicBoxUploadSessionPacket> PACKET_CODEC = PacketCodec.tuple(
        BlockPos.PACKET_CODEC,
        RequestEnderMusicBoxUploadSessionPacket::pos,
        PacketCodecs.string(512),
        RequestEnderMusicBoxUploadSessionPacket::name,
        PhonosAudioRecord.FileType.PACKET_CODEC,
        RequestEnderMusicBoxUploadSessionPacket::fileType,
        RequestEnderMusicBoxUploadSessionPacket::new
    );

    @Override
    public void handle(ServerPlayerEntity sender) {
        var world = sender.getWorld();

        if (world.getBlockEntity(pos) instanceof EnderMusicBoxBlockEntity entity) {
            Long streamId;

            if (!entity.canModifyStreams(sender)) {
                PayloadPackets.sendUploadStatus(sender, entity.emitterId(), false);
                Phonos.LOG.warn("Player {} tried to start upload session at ender music box {} without permission", sender, pos);
            } else if ((streamId = entity.allocateStreamId(name)) == null) {
                PayloadPackets.sendUploadStatus(sender, entity.emitterId(), false);
                Phonos.LOG.warn("Player {} tried to start upload session at ender music box {} but no stream IDs were available", sender, pos);
            } else {
                ServerCustomAudio.beginUploadSession(sender, streamId, fileType);
                PayloadPackets.sendUploadStatus(sender, streamId, true);

                Phonos.LOG.info("Allowed player {} to upload audio to ender music box {}. Will be saved to <world>/phonos/{}",
                    sender, pos, Long.toHexString(streamId) + ServerCustomAudio.FILE_EXT);
            }
        } else {
            Phonos.LOG.warn("Player {} tried to start upload session at invalid ender music box {}", sender, pos);
            PayloadPackets.sendUploadStatus(sender, -1, false);
        }
    }
}
