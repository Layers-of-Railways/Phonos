package io.github.foundationgames.phonos.util.compat;

import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatClientApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import de.maxhenkel.voicechat.api.events.*;
import de.maxhenkel.voicechat.voice.common.LocationSoundPacket;
import io.github.foundationgames.phonos.Phonos;
import io.github.foundationgames.phonos.mixin.compat.LocationSoundPacketAccessor;
import io.github.foundationgames.phonos.network.PayloadPackets;
import io.github.foundationgames.phonos.sound.custom.SVCSoundMetadata;
import io.github.foundationgames.phonos.util.ServerLifecycleHooks;
import io.github.foundationgames.phonos.util.UniqueId;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;

public class PhonosVoicechatPlugin implements VoicechatPlugin {
    @Nullable
    public static VoicechatApi api;
    @Nullable
    public static VoicechatServerApi serverApi;
    @Nullable
    public static VoicechatClientApi clientApi;

    // Server
    private static final WeakHashMap<ServerPlayerEntity, LocationalAudioChannel> microphonePlayers = new WeakHashMap<>();
    private static final Long2ObjectOpenHashMap<WeakReference<LocationalAudioChannel>> removalHelperMap = new Long2ObjectOpenHashMap<>();

    // Client
    private static final HashMap<UUID, SVCSoundMetadata> clientMetadataByChannelId = new HashMap<>();
    private static final Long2ObjectOpenHashMap<SVCSoundMetadata> clientMetadataByStreamId = new Long2ObjectOpenHashMap<>();
    private static final Long2ObjectOpenHashMap<CompletableFuture<SVCSoundMetadata>> waitingClientMetadata = new Long2ObjectOpenHashMap<>();

    public static boolean isStreaming(long streamID) {
        return removalHelperMap.containsKey(streamID) && removalHelperMap.get(streamID).get() != null;
    }

    public static boolean isStreaming(ServerPlayerEntity serverPlayer) {
        return microphonePlayers.containsKey(serverPlayer);
    }

    public static boolean startStream(ServerPlayerEntity serverPlayer, long streamID) {
        if (serverApi == null)
            return false;

        var server = serverPlayer.getServer();
        if (server == null)
            return false;

        UUID channelId = UniqueId.uuidOf(streamID);
        var channel = serverApi.createLocationalAudioChannel(
            channelId,
            serverApi.fromServerLevel(serverPlayer.getServerWorld()),
            serverApi.createPosition(0, 0, 0)
        );
        if (channel == null)
            return false;

        channel.setDistance(Float.MAX_VALUE);
        for (var player : server.getPlayerManager().getPlayerList()) {
            PayloadPackets.sendMicrophoneChannelOpen(player, channelId, streamID);
            Phonos.LOG.info("Opened microphone channel for player {} with stream ID {} and channel ID {}", player, streamID, channelId);
        }

        microphonePlayers.put(serverPlayer, channel);
        removalHelperMap.put(streamID, new WeakReference<>(channel));
        return true;
    }

    public static void resumeStream(ServerPlayerEntity target, long streamID) {
        var removalData = removalHelperMap.get(streamID);
        if (removalData == null)
            return;
        var key = removalData.get();
        if (key == null)
            return;
        UUID channelId = key.getId();
        PayloadPackets.sendMicrophoneChannelOpen(target, channelId, streamID);
        Phonos.LOG.info("Resumed microphone channel for player {} with stream ID {} and channel ID {}", target, streamID, channelId);
    }

    public static void stopStreaming(long streamID) {
        var removalData = removalHelperMap.remove(streamID);
        if (removalData == null)
            return;
        var key = removalData.get();
        if (key == null)
            return;

        microphonePlayers.values().remove(key);
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null)
            return;

        server.getPlayerManager().getPlayerList().forEach(player -> {
            PayloadPackets.sendMicrophoneChannelClose(player, key.getId(), streamID);
        });
    }

    @Environment(EnvType.CLIENT)
    public static CompletableFuture<SVCSoundMetadata> getClientMicrophoneChannel(long streamId) {
        if (clientMetadataByStreamId.containsKey(streamId)) {
            return CompletableFuture.completedFuture(clientMetadataByStreamId.get(streamId));
        } else if (waitingClientMetadata.containsKey(streamId)) {
            return waitingClientMetadata.get(streamId);
        } else {
            CompletableFuture<SVCSoundMetadata> future = new CompletableFuture<>();
            waitingClientMetadata.put(streamId, future);
            return future;
        }
    }

    @Environment(EnvType.CLIENT)
    public static void startClientMicrophoneStream(UUID channelId, long streamId) {
        if (clientApi == null)
            return;

        var metadata = new SVCSoundMetadata();
        clientMetadataByChannelId.put(channelId, metadata);
        clientMetadataByStreamId.put(streamId, metadata);
        if (waitingClientMetadata.containsKey(streamId)) {
            waitingClientMetadata.remove(streamId).complete(metadata);
        }
    }

    @Environment(EnvType.CLIENT)
    public static void endClientMicrophoneStream(UUID channelId, long streamId) {
        clientMetadataByChannelId.remove(channelId);
        clientMetadataByStreamId.remove(streamId);
        waitingClientMetadata.remove(streamId);
    }

    @Override
    public String getPluginId() {
        return "phonos";
    }

    @Override
    public void initialize(VoicechatApi api) {
        PhonosVoicechatPlugin.api = api;

        Phonos.LOG.info("Phonos voice chat plugin initialized");
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(VoicechatServerStartedEvent.class, event -> {
            serverApi = event.getVoicechat();
        });
        registration.registerEvent(VoicechatServerStoppedEvent.class, event -> {
            serverApi = null;
            microphonePlayers.clear();
            removalHelperMap.clear();
        });
        registration.registerEvent(ClientVoicechatInitializationEvent.class, event -> {
            clientApi = event.getVoicechat();
        });
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophoneEvent);
    }

    private void onMicrophoneEvent(MicrophonePacketEvent event) {
        var senderConnection = event.getSenderConnection();
        if (senderConnection == null)
            return;

        var senderPlayer = senderConnection.getPlayer();
        if (senderPlayer == null)
            return;

        var channel = microphonePlayers.get((ServerPlayerEntity) senderPlayer.getPlayer());
        if (channel == null)
            return;

        channel.send(event.getPacket());
    }

    public static void onLocationalSoundPacket(LocationSoundPacket packet, short[] audioData) {
        var metadata = clientMetadataByChannelId.get(packet.getSender());
        if (metadata == null || clientApi == null)
            return;

        ((LocationSoundPacketAccessor) packet).setLocation(metadata.getPosition());
        ((LocationSoundPacketAccessor) packet).setDistance(metadata.getDistance());
        ((LocationSoundPacketAccessor) packet).setCategory(metadata.getCategory());

        float volume = metadata.getVolume();
        if (volume != 1.0f) for (int i = 0; i < audioData.length; i++) {
            audioData[i] = (short) (audioData[i] * volume);
        }
    }
}
