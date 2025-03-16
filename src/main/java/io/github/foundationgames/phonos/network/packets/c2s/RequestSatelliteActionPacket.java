package io.github.foundationgames.phonos.network.packets.c2s;

import io.github.foundationgames.phonos.block.entity.SatelliteStationBlockEntity;
import io.github.foundationgames.phonos.network.packets.C2SPacket;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

public record RequestSatelliteActionPacket(BlockPos pos, int actionId, String data) implements C2SPacket {
    public static final PacketCodec<PacketByteBuf, RequestSatelliteActionPacket> PACKET_CODEC = PacketCodec.tuple(
        BlockPos.PACKET_CODEC,
        RequestSatelliteActionPacket::pos,
        PacketCodecs.VAR_INT,
        RequestSatelliteActionPacket::actionId,
        PacketCodecs.string(256),
        RequestSatelliteActionPacket::data,
        RequestSatelliteActionPacket::new
    );

    @Override
    public void handle(ServerPlayerEntity sender) {
        var world = sender.getWorld();

        switch (actionId) {
            case SatelliteStationBlockEntity.ACTION_LAUNCH -> {
                if (world.getBlockEntity(pos) instanceof SatelliteStationBlockEntity entity && entity.canLaunch(sender)) {
                    entity.performAction(SatelliteStationBlockEntity.ACTION_LAUNCH, data);
                }
            }

            case SatelliteStationBlockEntity.ACTION_CRASH -> {
                if (world.getBlockEntity(pos) instanceof SatelliteStationBlockEntity entity && entity.canCrash(sender)) {
                    entity.performAction(SatelliteStationBlockEntity.ACTION_CRASH, data);
                }
            }
        }
    }
}
