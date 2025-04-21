package io.github.foundationgames.phonos.network;

import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import io.github.foundationgames.phonos.block.entity.SatelliteStationBlockEntity;
import io.github.foundationgames.phonos.config.PhonosServerConfig;
import io.github.foundationgames.phonos.network.packets.s2c.*;
import io.github.foundationgames.phonos.sound.emitter.SoundEmitterTree;
import io.github.foundationgames.phonos.sound.nbs.stream.NBSChunk;
import io.github.foundationgames.phonos.sound.nbs.stream.NBSInitData;
import io.github.foundationgames.phonos.world.sound.data.SoundData;
import net.minecraft.network.listener.ClientCommonPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

public final class PayloadPackets {
    public static void sendSoundPlay(ServerPlayerEntity player, SoundData data, SoundEmitterTree tree) {
        PhonosPackets.PACKETS.sendTo(player, new SoundPlayPacket(data, tree));
    }

    public static void sendSoundStop(ServerPlayerEntity player, long sourceId) {
        PhonosPackets.PACKETS.sendTo(player, new SoundStopPacket(sourceId));
    }

    public static void sendSoundUpdate(ServerPlayerEntity player, SoundEmitterTree.Delta delta) {
        PhonosPackets.PACKETS.sendTo(player, new SoundUpdatePacket(delta));
    }

    public static void sendOpenSatelliteStationScreen(ServerPlayerEntity player, BlockPos pos, int screenType) {
        PhonosPackets.PACKETS.sendTo(player, new OpenSatelliteStationScreenPacket(pos, screenType));
    }

    public static void sendOpenEnderMusicBoxScreen(ServerPlayerEntity player, BlockPos pos) {
        PhonosPackets.PACKETS.sendTo(player, new OpenEnderMusicBoxScreenPacket(pos));
    }

    public static void sendUploadStop(ServerPlayerEntity player, long uploadId, Text message) {
        PhonosPackets.PACKETS.sendTo(player, new AudioUploadStopPacket(uploadId, message));
    }

    public static void sendUploadStatus(ServerPlayerEntity player, long uploadId, boolean ok) {
        PhonosPackets.PACKETS.sendTo(player, new AudioUploadStatusPacket(uploadId, ok));
    }

    public static void sendAudioStreamData(ServerPlayerEntity player, long streamId, int sampleRate, byte[] samples) {
        PhonosPackets.PACKETS.sendTo(player, new AudioStreamDataPacket(streamId, sampleRate, samples));
    }

    public static void sendAudioStreamEnd(ServerPlayerEntity player, long streamId) {
        PhonosPackets.PACKETS.sendTo(player, new AudioStreamEndPacket(streamId));
    }

    public static Packet<ClientCommonPacketListener> pktSatelliteAction(SatelliteStationBlockEntity be, int action, String data) {
        return PhonosPackets.PACKETS.tunnelPacket(new SatelliteActionPacket(be.getPos(), action, data));
    }

    public static void sendMicrophoneChannelOpen(ServerPlayerEntity player, UUID channelId, long streamId, UUID speakingPlayer) {
        PhonosPackets.PACKETS.sendTo(player, new MicrophoneChannelOpenPacket(channelId, streamId, speakingPlayer));
    }

    public static void sendMicrophoneChannelClose(ServerPlayerEntity player, UUID channelId, long streamId) {
        PhonosPackets.PACKETS.sendTo(player, new MicrophoneChannelClosePacket(channelId, streamId));
    }

    public static void sendConfig(ServerPlayerEntity player, ConfigClassHandler<PhonosServerConfig> config) {
        PhonosPackets.PACKETS.sendTo(player, new SetConfigPacket(config));
    }

    public static void sendNBSStreamStart(ServerPlayerEntity player, long streamId, NBSInitData initData) {
        PhonosPackets.PACKETS.sendTo(player, new NBSStreamStartPacket(streamId, initData));
    }

    public static void sendNBSStreamData(ServerPlayerEntity player, long streamId, NBSChunk chunk) {
        PhonosPackets.PACKETS.sendTo(player, new NBSStreamDataPacket(streamId, chunk));
    }

    public static void sendNBSStreamEnd(ServerPlayerEntity player, long streamId) {
        PhonosPackets.PACKETS.sendTo(player, new NBSStreamEndPacket(streamId));
    }
}
