package io.github.foundationgames.phonos.network.packets.c2s;

import io.github.foundationgames.phonos.Phonos;
import io.github.foundationgames.phonos.block.entity.SatelliteStationBlockEntity;
import io.github.foundationgames.phonos.item.PortableSatelliteRadioItem;
import io.github.foundationgames.phonos.network.packets.C2SPacket;
import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.server.network.ServerPlayerEntity;

public record ConfigurePortableSatelliteRadioChannelPacket(String channel) implements C2SPacket {
    public static final PacketCodec<ByteBuf, ConfigurePortableSatelliteRadioChannelPacket> PACKET_CODEC = PacketCodecs.string(256).xmap(
        ConfigurePortableSatelliteRadioChannelPacket::new,
        ConfigurePortableSatelliteRadioChannelPacket::channel
    );

    @Override
    public void handle(ServerPlayerEntity sender) {
        var channel = SatelliteStationBlockEntity.cleanChannel(this.channel);

        if (!SatelliteStationBlockEntity.validateChannel(channel)) {
            Phonos.LOG.warn("Player {} tried to set invalid channel {}", sender, channel);
            return;
        }

        ItemStack handStack = sender.getMainHandStack();
        if (handStack.getItem() instanceof PortableSatelliteRadioItem satelliteRadio) {
            satelliteRadio.setChannel(handStack, channel);
        }
    }
}
