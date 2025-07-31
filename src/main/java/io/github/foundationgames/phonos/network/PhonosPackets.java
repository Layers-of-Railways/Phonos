package io.github.foundationgames.phonos.network;

import io.github.foundationgames.phonos.network.packets.PacketSet;
import io.github.foundationgames.phonos.network.packets.c2s.*;
import io.github.foundationgames.phonos.network.packets.s2c.*;

public class PhonosPackets {
    public static final PacketSet PACKETS = PacketSet.builder("phonos", 1)
        .s2c(SoundPlayPacket.class, SoundPlayPacket.PACKET_CODEC)
        .s2c(SoundStopPacket.class, SoundStopPacket.PACKET_CODEC)
        .s2c(SoundUpdatePacket.class, SoundUpdatePacket.PACKET_CODEC)
        .s2c(OpenSatelliteStationScreenPacket.class, OpenSatelliteStationScreenPacket.PACKET_CODEC)
        .s2c(OpenEnderMusicBoxScreenPacket.class, OpenEnderMusicBoxScreenPacket.PACKET_CODEC)
        .s2c(AudioUploadStopPacket.class, AudioUploadStopPacket.PACKET_CODEC)
        .s2c(AudioUploadStatusPacket.class, AudioUploadStatusPacket.PACKET_CODEC)
        .s2c(AudioStreamDataPacket.class, AudioStreamDataPacket.PACKET_CODEC)
        .s2c(AudioStreamEndPacket.class, AudioStreamEndPacket.PACKET_CODEC)
        .s2c(SatelliteActionPacket.class, SatelliteActionPacket.PACKET_CODEC)
        .s2c(MicrophoneChannelOpenPacket.class, MicrophoneChannelOpenPacket.PACKET_CODEC)
        .s2c(MicrophoneChannelClosePacket.class, MicrophoneChannelClosePacket.PACKET_CODEC)
        .s2c(SetConfigPacket.class, SetConfigPacket.PACKET_CODEC)
        .s2c(NBSStreamStartPacket.class, NBSStreamStartPacket.PACKET_CODEC)
        .s2c(NBSStreamDataPacket.class, NBSStreamDataPacket.PACKET_CODEC)
        .s2c(NBSStreamEndPacket.class, NBSStreamEndPacket.PACKET_CODEC)
        .s2c(PrepareForResyncPacket.class, PrepareForResyncPacket.PACKET_CODEC)

        .c2s(FakeCreativeSlotClickPacket.class, FakeCreativeSlotClickPacket.PACKET_CODEC)
        .c2s(RequestEnderMusicBoxUploadSessionPacket.class, RequestEnderMusicBoxUploadSessionPacket.PACKET_CODEC)
        .c2s(DeleteEnderMusicBoxStreamPacket.class, DeleteEnderMusicBoxStreamPacket.PACKET_CODEC)
        .c2s(RequestSatelliteActionPacket.class, RequestSatelliteActionPacket.PACKET_CODEC)
        .c2s(AudioUploadPacket.class, AudioUploadPacket.PACKET_CODEC)
        .c2s(ConfigChangePacket.class, ConfigChangePacket.PACKET_CODEC)
        .c2s(ConfigurePortableSatelliteRadioChannelPacket.class, ConfigurePortableSatelliteRadioChannelPacket.PACKET_CODEC)
        .c2s(ReadyForResyncPacket.class, ReadyForResyncPacket.PACKET_CODEC)
        .build();
}
