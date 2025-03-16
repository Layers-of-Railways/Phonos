package io.github.foundationgames.phonos.network.packets.s2c;

import io.github.foundationgames.phonos.block.entity.SatelliteStationBlockEntity;
import io.github.foundationgames.phonos.network.packets.S2CPacket;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.math.BlockPos;

public record SatelliteActionPacket(BlockPos pos, int action, String data) implements S2CPacket {
    public static final PacketCodec<PacketByteBuf, SatelliteActionPacket> PACKET_CODEC = PacketCodec.tuple(
        BlockPos.PACKET_CODEC,
        SatelliteActionPacket::pos,
        PacketCodecs.VAR_INT,
        SatelliteActionPacket::action,
        PacketCodecs.string(256),
        SatelliteActionPacket::data,
        SatelliteActionPacket::new
    );

    @Override
    @Environment(EnvType.CLIENT)
    public void handle(MinecraftClient mc) {
        if (mc.world != null && mc.world.getBlockEntity(pos) instanceof SatelliteStationBlockEntity be) {
            be.performAction(action, data);
        }
    }
}
