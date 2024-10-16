package io.github.foundationgames.phonos.network;

import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.FieldAccess;
import io.github.foundationgames.phonos.Phonos;
import io.github.foundationgames.phonos.block.entity.EnderMusicBoxBlockEntity;
import io.github.foundationgames.phonos.block.entity.SatelliteStationBlockEntity;
import io.github.foundationgames.phonos.client.screen.CrashSatelliteStationScreen;
import io.github.foundationgames.phonos.client.screen.EnderMusicBoxScreen;
import io.github.foundationgames.phonos.client.screen.LaunchSatelliteStationScreen;
import io.github.foundationgames.phonos.config.PhonosServerConfig;
import io.github.foundationgames.phonos.config.serializers.NetworkConfigSerializer;
import io.github.foundationgames.phonos.sound.SoundStorage;
import io.github.foundationgames.phonos.sound.custom.ClientCustomAudioUploader;
import io.github.foundationgames.phonos.sound.emitter.SoundEmitterTree;
import io.github.foundationgames.phonos.sound.stream.ClientIncomingStreamHandler;
import io.github.foundationgames.phonos.util.PhonosUtil;
import io.github.foundationgames.phonos.util.compat.PhonosVoicechatProxy;
import io.github.foundationgames.phonos.world.sound.data.SoundData;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.ClickType;

import java.nio.ByteBuffer;
import java.util.UUID;

public final class ClientPayloadPackets {
    @Environment(EnvType.CLIENT)
    public static void initClient() {
        ClientPlayNetworking.registerGlobalReceiver(Phonos.id("sound_play"), (client, handler, buf, responseSender) -> {
            var data = SoundData.fromPacket(buf);
            var tree = SoundEmitterTree.fromPacket(buf);

            client.execute(() -> SoundStorage.getInstance(client.world).play(client.world, data, tree));
        });

        ClientPlayNetworking.registerGlobalReceiver(Phonos.id("sound_stop"), (client, handler, buf, responseSender) -> {
            long id = buf.readLong();

            client.execute(() -> SoundStorage.getInstance(client.world).stop(client.world, id));
        });

        ClientPlayNetworking.registerGlobalReceiver(Phonos.id("sound_update"), (client, handler, buf, responseSender) -> {
            SoundEmitterTree.Delta delta = SoundEmitterTree.Delta.fromPacket(buf);

            client.execute(() -> SoundStorage.getInstance(client.world).update(delta));
        });

        ClientPlayNetworking.registerGlobalReceiver(Phonos.id("open_satellite_station_screen"), (client, handler, buf, responseSender) -> {
            var pos = buf.readBlockPos();

            int screenType = buf.readInt();

            client.execute(() -> {
                if (client.world.getBlockEntity(pos) instanceof SatelliteStationBlockEntity sat) {
                    client.setScreen(switch (screenType) {
                        case SatelliteStationBlockEntity.SCREEN_LAUNCH -> new LaunchSatelliteStationScreen(sat);
                        case SatelliteStationBlockEntity.SCREEN_CRASH -> new CrashSatelliteStationScreen(sat);
                        default -> null;
                    });
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(Phonos.id("open_ender_music_box_screen"), (client, handler, buf, responseSender) -> {
            var pos = buf.readBlockPos();

            client.execute(() -> {
                if (client.world.getBlockEntity(pos) instanceof EnderMusicBoxBlockEntity box) {
                    client.setScreen(new EnderMusicBoxScreen(box));
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(Phonos.id("audio_upload_status"), (client, handler, buf, responseSender) -> {
            long id = buf.readLong();
            boolean ok = buf.readBoolean();

            client.execute(() -> {
                if (client.currentScreen instanceof EnderMusicBoxScreen screen) {
                    screen.onAudioUploadStatus(id, ok);
                }
                if (ok) {
                    ClientCustomAudioUploader.sendUploadPackets(id);
                } else {
                    Phonos.LOG.warn("Denied upload for sound " + Long.toHexString(id));
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(Phonos.id("audio_upload_stop"), (client, handler, buf, responseSender) -> {
            long id = buf.readLong();
            Text message = buf.readText();

            client.execute(() -> {
                if (client.currentScreen instanceof EnderMusicBoxScreen screen) {
                    screen.onAudioUploadCancel(message);
                }

                ClientCustomAudioUploader.cancelUpload(id);
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(Phonos.id("audio_stream_data"), (client, handler, buf, responseSender) -> {
            long id = buf.readLong();
            int sampleRate = buf.readInt();
            var samples = PhonosUtil.readBufferFromPacket(buf, ByteBuffer::allocate);

            client.execute(() -> ClientIncomingStreamHandler.receiveStream(id, sampleRate, samples));
        });

        ClientPlayNetworking.registerGlobalReceiver(Phonos.id("audio_stream_end"), (client, handler, buf, responseSender) -> {
            long id = buf.readLong();

            client.execute(() -> ClientIncomingStreamHandler.endStream(id));
        });

        ClientPlayNetworking.registerGlobalReceiver(Phonos.id("satellite_action"), (client, handler, buf, responseSender) -> {
            var pos = buf.readBlockPos();
            int action = buf.readInt();
            int data = buf.readInt();

            client.execute(() -> {
                if (client.world.getBlockEntity(pos) instanceof SatelliteStationBlockEntity be) {
                    be.performAction(action, data);
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(Phonos.id("microphone_channel_open"), (client, handler, buf, responseSender) -> {
            UUID channelId = buf.readUuid();
            long streamId = buf.readLong();
            UUID speakerId = buf.readUuid();

            PhonosVoicechatProxy.startClientMicrophoneStream(channelId, streamId, speakerId);
        });

        ClientPlayNetworking.registerGlobalReceiver(Phonos.id("microphone_channel_close"), (client, handler, buf, responseSender) -> {
            UUID channelId = buf.readUuid();
            long streamId = buf.readLong();

            PhonosVoicechatProxy.endClientMicrophoneStream(channelId, streamId);
        });

        ClientPlayNetworking.registerGlobalReceiver(Phonos.id("set_config"), (client, handler, buf, responseSender) -> {
            ConfigClassHandler<PhonosServerConfig> config = PhonosServerConfig.getHandler(client.world);
            NetworkConfigSerializer.read(buf, config);
        });
    }

    public static void sendFakeCreativeSlotClick(ItemStack onto, ItemStack with, ClickType click) {
        var buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeItemStack(onto);
        buf.writeItemStack(with);
        buf.writeInt(click.ordinal());

        ClientPlayNetworking.send(Phonos.id("fake_creative_slot_click"), buf);
    }

    public static void sendRequestEnderMusicBoxUploadSession(EnderMusicBoxBlockEntity entity, String name) {
        var buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeBlockPos(entity.getPos());
        buf.writeString(name, 512);

        ClientPlayNetworking.send(Phonos.id("request_ender_music_box_upload_session"), buf);
    }

    public static void sendDeleteEnderMusicBoxStream(EnderMusicBoxBlockEntity entity, long streamId) {
        var buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeBlockPos(entity.getPos());
        buf.writeLong(streamId);

        ClientPlayNetworking.send(Phonos.id("delete_ender_music_box_stream"), buf);
    }

    public static void sendRequestSatelliteAction(SatelliteStationBlockEntity entity, int actionId, int data) {
        var buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeBlockPos(entity.getPos());
        buf.writeInt(actionId);
        buf.writeInt(data);

        ClientPlayNetworking.send(Phonos.id("request_satellite_action"), buf);
    }

    public static void sendAudioUploadPacket(long streamId, int sampleRate, ByteBuffer samples, boolean last) {
        var buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeLong(streamId);
        buf.writeInt(sampleRate);
        PhonosUtil.writeBufferToPacket(buf, samples);
        buf.writeBoolean(last);

        ClientPlayNetworking.send(Phonos.id("audio_upload"), buf);
    }

    public static void sendConfigChange(int i, FieldAccess<?> access) {
        var buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeVarInt(i);
        buf.writeString(access.name());
        NetworkConfigSerializer.write(buf, access);

        ClientPlayNetworking.send(Phonos.id("config_change"), buf);
    }
}
