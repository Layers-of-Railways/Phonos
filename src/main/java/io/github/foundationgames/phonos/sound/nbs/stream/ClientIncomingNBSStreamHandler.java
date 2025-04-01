package io.github.foundationgames.phonos.sound.nbs.stream;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import java.util.concurrent.CompletableFuture;

public class ClientIncomingNBSStreamHandler {
    private static final Long2ObjectMap<SynchronizedSong> STREAMS = Long2ObjectMaps.synchronize(new Long2ObjectOpenHashMap<>());
    private static final Long2ObjectMap<CompletableFuture<SynchronizedSong>> WAITING = new Long2ObjectOpenHashMap<>();

    public static CompletableFuture<SynchronizedSong> getStream(long id) {
        if (STREAMS.containsKey(id)) {
            return CompletableFuture.completedFuture(STREAMS.get(id));
        }

        var future = new CompletableFuture<SynchronizedSong>();
        WAITING.put(id, future);
        return future;
    }

    public static void initStream(long id, NBSInitData initData) {
        STREAMS.put(id, new SynchronizedSong(initData.makeSong()));

        if (WAITING.containsKey(id)) {
            WAITING.remove(id).complete(STREAMS.get(id));
        }
    }

    public static void receiveChunk(long id, NBSChunk chunk) {
        if (STREAMS.containsKey(id)) {
            var song = STREAMS.get(id);
            song.run(chunk::apply);
        }
    }

    public static void endStream(long id) {
        STREAMS.remove(id);
    }

    public static void reset() {
        STREAMS.clear();
    }
}
