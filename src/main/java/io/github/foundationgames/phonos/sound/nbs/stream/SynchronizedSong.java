package io.github.foundationgames.phonos.sound.nbs.stream;

import cz.koca2000.nbs4j.Song;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;

public class SynchronizedSong {
    private final Song song;
    private boolean isComplete = false;

    public SynchronizedSong(Song song) {
        this.song = song;
    }

    public synchronized void run(Consumer<Song> consumer) {
        consumer.accept(song);
    }

    public synchronized <T> T apply(Function<Song, T> function) {
        return function.apply(song);
    }

    public synchronized void markComplete() {
        this.isComplete = true;
    }

    public synchronized boolean isComplete() {
        return this.isComplete;
    }
}
