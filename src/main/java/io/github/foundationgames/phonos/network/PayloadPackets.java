package io.github.foundationgames.phonos.network;

import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.ConfigField;
import dev.isxander.yacl3.config.v2.api.FieldAccess;
import io.github.foundationgames.phonos.Phonos;
import io.github.foundationgames.phonos.block.entity.SatelliteStationBlockEntity;
import io.github.foundationgames.phonos.config.PhonosServerConfig;
import io.github.foundationgames.phonos.config.serializers.NetworkConfigSerializer;
import io.github.foundationgames.phonos.sound.custom.ServerCustomAudio;
import io.github.foundationgames.phonos.sound.emitter.SoundEmitterTree;
import io.github.foundationgames.phonos.util.PhonosUtil;
import io.github.foundationgames.phonos.world.sound.data.SoundData;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.inventory.StackReference;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ClickType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import java.nio.ByteBuffer;
import java.util.UUID;

public final class PayloadPackets {
    public static void initCommon() {
        ServerPlayNetworking.registerGlobalReceiver(Phonos.id("fake_creative_slot_click"), (server, player, handler, buf, responseSender) -> {
            var onto = buf.readItemStack();
            var with = buf.readItemStack();
            var click = ClickType.values()[buf.readInt()];

            server.execute(() ->
                    onto.getItem().onClicked(onto, with, null, click, player, StackReference.EMPTY));
        });

        /*ServerPlayNetworking.registerGlobalReceiver(Phonos.id("request_satellite_upload_session"), (server, player, handler, buf, responseSender) -> {
            var pos = buf.readBlockPos();

            server.execute(() -> {
                var world = player.getWorld();

                if (world.getBlockEntity(pos) instanceof SatelliteStationBlockEntity entity) {
                    if (entity.canUpload(player)) {
                        ServerCustomAudio.beginUploadSession(player, entity.streamId);
                        sendUploadStatus(player, entity.streamId, true);

                        Phonos.LOG.info("Allowed player {} to upload audio at satellite station {}. Will be saved to <world>/phonos/{}",
                                player, pos, Long.toHexString(entity.streamId) + ServerCustomAudio.FILE_EXT);
                    } else {
                        sendUploadStatus(player, entity.streamId, false);
                    }
                }
            });
        });*/

        ServerPlayNetworking.registerGlobalReceiver(Phonos.id("request_satellite_crash"), (server, player, handler, buf, responseSender) -> {
            var pos = buf.readBlockPos();

            server.execute(() -> {
                var world = player.getWorld();

                if (world.getBlockEntity(pos) instanceof SatelliteStationBlockEntity entity && entity.canCrash(player)) {
                    entity.performAction(SatelliteStationBlockEntity.ACTION_CRASH);
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(Phonos.id("audio_upload"), (server, player, handler, buf, responseSender) -> {
            long streamId = buf.readLong();
            int sampleRate = buf.readInt();
            var samples = PhonosUtil.readBufferFromPacket(buf, ByteBuffer::allocate);

            boolean last = buf.readBoolean();

            server.execute(() -> ServerCustomAudio.receiveUpload(server, player, streamId, sampleRate, samples, last));
        });

        ServerPlayNetworking.registerGlobalReceiver(Phonos.id("config_change"), ((server, player, handler, buf, responseSender) -> {
            if (!PhonosServerConfig.isAuthorizedToChange(player)) {
                Phonos.LOG.warn("Player {} tried to change config without permission", player);
                handler.disconnect(Text.of("You are not authorized to change Phonos config"));
                return;
            }

            ConfigClassHandler<PhonosServerConfig> config = PhonosServerConfig.getHandler(player.getServerWorld());

            int idx = buf.readVarInt();
            String name = buf.readString();

            FieldAccess<?> access = config.fields()[MathHelper.clamp(idx, 0, config.fields().length-1)].access();

            if (access.name().equals(name)) {
                NetworkConfigSerializer.read(buf, access);
                config.save();
                return;
            }

            for (ConfigField<?> field : config.fields()) {
                access = field.access();
                if (access.name().equals(name)) {
                    NetworkConfigSerializer.read(buf, access);
                    config.save();
                    return;
                }
            }

            Phonos.LOG.warn("Failed to find config field with name {}", name);
        }));
    }

    public static void sendSoundPlay(ServerPlayerEntity player, SoundData data, SoundEmitterTree tree) {
        var buf = new PacketByteBuf(Unpooled.buffer());
        data.toPacket(buf);
        tree.toPacket(buf);
        ServerPlayNetworking.send(player, Phonos.id("sound_play"), buf);
    }

    public static void sendSoundStop(ServerPlayerEntity player, long sourceId) {
        var buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeLong(sourceId);
        ServerPlayNetworking.send(player, Phonos.id("sound_stop"), buf);
    }

    public static void sendSoundUpdate(ServerPlayerEntity player, SoundEmitterTree.Delta delta) {
        var buf = new PacketByteBuf(Unpooled.buffer());
        SoundEmitterTree.Delta.toPacket(buf, delta);
        ServerPlayNetworking.send(player, Phonos.id("sound_update"), buf);
    }

    public static void sendOpenSatelliteStationCrashScreen(ServerPlayerEntity player, BlockPos pos) {
        var buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeBlockPos(pos);

        ServerPlayNetworking.send(player, Phonos.id("open_satellite_station_crash_screen"), buf);
    }

    public static void sendUploadStop(ServerPlayerEntity player, long uploadId) {
        var buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeLong(uploadId);

        ServerPlayNetworking.send(player, Phonos.id("audio_upload_stop"), buf);
    }

    public static void sendUploadStatus(ServerPlayerEntity player, long uploadId, boolean ok) {
        var buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeLong(uploadId);
        buf.writeBoolean(ok);

        ServerPlayNetworking.send(player, Phonos.id("audio_upload_status"), buf);
    }

    public static void sendAudioStreamData(ServerPlayerEntity player, long streamId, int sampleRate, ByteBuffer samples) {
        var buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeLong(streamId);
        buf.writeInt(sampleRate);
        PhonosUtil.writeBufferToPacket(buf, samples);

        ServerPlayNetworking.send(player, Phonos.id("audio_stream_data"), buf);
    }

    public static void sendAudioStreamEnd(ServerPlayerEntity player, long streamId) {
        var buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeLong(streamId);

        ServerPlayNetworking.send(player, Phonos.id("audio_stream_end"), buf);
    }

    public static Packet<ClientPlayPacketListener> pktSatelliteAction(SatelliteStationBlockEntity be, int action) {
        var buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeBlockPos(be.getPos());
        buf.writeInt(action);

        return ServerPlayNetworking.createS2CPacket(Phonos.id("satellite_action"), buf);
    }

    public static void sendMicrophoneChannelOpen(ServerPlayerEntity player, UUID channelId, long streamId, UUID speakingPlayer) {
        var buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeUuid(channelId);
        buf.writeLong(streamId);
        buf.writeUuid(speakingPlayer);

        ServerPlayNetworking.send(player, Phonos.id("microphone_channel_open"), buf);
    }

    public static void sendMicrophoneChannelClose(ServerPlayerEntity player, UUID channelId, long streamId) {
        var buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeUuid(channelId);
        buf.writeLong(streamId);

        ServerPlayNetworking.send(player, Phonos.id("microphone_channel_close"), buf);
    }

    public static void sendConfig(ServerPlayerEntity player, ConfigClassHandler<PhonosServerConfig> config) {
        var buf = new PacketByteBuf(Unpooled.buffer());
        NetworkConfigSerializer.write(buf, config);

        ServerPlayNetworking.send(player, Phonos.id("set_config"), buf);
    }
}
