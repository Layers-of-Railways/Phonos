package io.github.foundationgames.phonos.sound.nbs.stream;

import io.github.foundationgames.phonos.network.PayloadPackets;
import io.github.foundationgames.phonos.sound.nbs.NBSRecord;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ServerOutgoingNBSStreamHandler {
    public static final Long2ObjectMap<Streaming> STREAMS = new Long2ObjectOpenHashMap<>();

    public static void startStream(long streamId, NBSRecord record, MinecraftServer server) {
        STREAMS.put(streamId, new Streaming(streamId, record, server));
    }

    public static void endStream(long streamId, MinecraftServer server) {
        STREAMS.remove(streamId);

        for (var player : server.getPlayerManager().getPlayerList()) {
            PayloadPackets.sendNBSStreamEnd(player, streamId);
        }
    }

    public static void tick(MinecraftServer server) {
        STREAMS.forEach((k, v) -> v.tick(server));
    }

    public static void reset() {
        STREAMS.clear();
    }

    public static void resumeStream(ServerPlayerEntity player, long streamId) {
        var stream = STREAMS.get(streamId);

        if (stream != null) {
            stream.uninitializedListeners.add(player.getUuid());
            stream.listeners.add(player.getUuid());
        }
    }

    public static class Streaming {
        private static final int CHUNK_LENGTH_SECONDS = 5;

        private int tickDelay = -10;
        private final Set<UUID> uninitializedListeners = new HashSet<>();
        private final Set<UUID> listeners = new HashSet<>();
        public final long streamId;

        private final NBSInitData initData;
        private final int tickInterval;
        private final Deque<NBSChunk> chunks = new ArrayDeque<>();

        // keep this around for resuming
        private @Nullable NBSChunk lastChunk;

        public Streaming(long streamId, NBSRecord record, MinecraftServer server) {
            final int chunkLength = (int) Math.ceil(CHUNK_LENGTH_SECONDS * record.song.getTempo(0));
            this.tickInterval = CHUNK_LENGTH_SECONDS * 20 - 1;

            this.streamId = streamId;
            this.initData = record.getInitData();
            server.getPlayerManager().getPlayerList().stream().map(Entity::getUuid).forEach(listeners::add);
            uninitializedListeners.addAll(listeners);

            // build chunks
            int tick = -1;
            int endOfChunk = chunkLength;
            var chunk = new NBSChunk(record.song.getLayersCount());
            while (true) {
                tick = record.song.getNextNonEmptyTick(tick);
                if (tick == -1) {
                    chunks.addLast(chunk);
                    break;
                }
                if (tick >= endOfChunk) {
                    chunks.addLast(chunk);
                    chunk = new NBSChunk(record.song.getLayersCount());
                    endOfChunk += chunkLength;
                }
                for (int layer = 0; layer < record.song.getLayersCount(); layer++) {
                    var note = record.song.getLayer(layer).getNote(tick);
                    if (note != null) {
                        chunk.layerUpdates()[layer].put(tick, note);
                    }
                }
            }

            chunks.addLast(chunks.removeLast().setLast());
        }

        void tick(MinecraftServer server) {
            listeners.removeIf(id -> server.getPlayerManager().getPlayer(id) == null);
            uninitializedListeners.removeIf(id -> !listeners.contains(id));

            if (tickDelay <= 0) {
                for (var id : uninitializedListeners) {
                    ServerPlayerEntity player = server.getPlayerManager().getPlayer(id);
                    PayloadPackets.sendNBSStreamStart(player, streamId, initData);
                    if (lastChunk != null) { // resume
                        PayloadPackets.sendNBSStreamData(player, streamId, lastChunk);
                    }
                }
                uninitializedListeners.clear();

                if (chunks.isEmpty()) {
                    lastChunk = null;
                } else {
                    lastChunk = chunks.removeFirst();
                    for (var id : listeners) {
                        PayloadPackets.sendNBSStreamData(server.getPlayerManager().getPlayer(id), streamId, lastChunk);
                    }
                }

                tickDelay += tickInterval;
            } else tickDelay--;
        }
    }
}
