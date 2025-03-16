package io.github.foundationgames.phonos.network;

import dev.isxander.yacl3.config.v2.api.FieldAccess;
import io.github.foundationgames.phonos.block.entity.EnderMusicBoxBlockEntity;
import io.github.foundationgames.phonos.block.entity.SatelliteStationBlockEntity;
import io.github.foundationgames.phonos.network.packets.c2s.*;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ClickType;

import java.nio.ByteBuffer;

public final class ClientPayloadPackets {
    public static void sendFakeCreativeSlotClick(ItemStack onto, ItemStack with, ClickType click) {
        PhonosPackets.PACKETS.send(new FakeCreativeSlotClickPacket(onto, with, click));
    }

    public static void sendRequestEnderMusicBoxUploadSession(EnderMusicBoxBlockEntity entity, String name) {
        PhonosPackets.PACKETS.send(new RequestEnderMusicBoxUploadSessionPacket(entity.getPos(), name));
    }

    public static void sendDeleteEnderMusicBoxStream(EnderMusicBoxBlockEntity entity, long streamId) {
        PhonosPackets.PACKETS.send(new DeleteEnderMusicBoxStreamPacket(entity.getPos(), streamId));
    }

    public static void sendRequestSatelliteAction(SatelliteStationBlockEntity entity, int actionId, String data) {
        PhonosPackets.PACKETS.send(new RequestSatelliteActionPacket(entity.getPos(), actionId, data));
    }

    public static void sendAudioUploadPacket(long streamId, int sampleRate, ByteBuffer samples, boolean last) {
        PhonosPackets.PACKETS.send(new AudioUploadPacket(streamId, sampleRate, samples, last));
    }

    public static void sendConfigChange(int i, FieldAccess<?> access) {
        PhonosPackets.PACKETS.send(new ConfigChangePacket(i, access));
    }

    public static void sendConfigurePortableSatelliteRadioChannel(String channel) {
        PhonosPackets.PACKETS.send(new ConfigurePortableSatelliteRadioChannelPacket(channel));
    }
}
