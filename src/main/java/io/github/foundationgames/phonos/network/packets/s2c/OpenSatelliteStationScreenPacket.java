package io.github.foundationgames.phonos.network.packets.s2c;

import io.github.foundationgames.phonos.block.entity.SatelliteStationBlockEntity;
import io.github.foundationgames.phonos.client.screen.CrashSatelliteStationScreen;
import io.github.foundationgames.phonos.client.screen.LaunchSatelliteStationScreen;
import io.github.foundationgames.phonos.network.packets.S2CPacket;
import io.github.foundationgames.phonos.util.PhonosUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public record OpenSatelliteStationScreenPacket(BlockPos pos, int screenType) implements S2CPacket {
    public static final PacketCodec<PacketByteBuf, OpenSatelliteStationScreenPacket> PACKET_CODEC = PacketCodec.tuple(
        BlockPos.PACKET_CODEC,
        OpenSatelliteStationScreenPacket::pos,
        PacketCodecs.VAR_INT,
        OpenSatelliteStationScreenPacket::screenType,
        OpenSatelliteStationScreenPacket::new
    );

    @Override
    @Environment(EnvType.CLIENT)
    public void handle(MinecraftClient mc) {
        World world = PhonosUtil.getClientWorld();
        if (world != null && world.getBlockEntity(pos) instanceof SatelliteStationBlockEntity sat) {
            mc.setScreen(switch (screenType) {
                case SatelliteStationBlockEntity.SCREEN_LAUNCH -> new LaunchSatelliteStationScreen(sat);
                case SatelliteStationBlockEntity.SCREEN_CRASH -> new CrashSatelliteStationScreen(sat);
                default -> null;
            });
        }
    }
}
