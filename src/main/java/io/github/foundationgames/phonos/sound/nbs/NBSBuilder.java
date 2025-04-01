package io.github.foundationgames.phonos.sound.nbs;

import cz.koca2000.nbs4j.Song;
import io.github.foundationgames.phonos.sound.custom.PhonosAudioRecordBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;

public class NBSBuilder implements PhonosAudioRecordBuilder<NBSRecord> {
    private final int totalSize;
    private int currentSize = 0;
    private final Deque<ByteBuffer> buffers = new ArrayDeque<>();

    public NBSBuilder(int totalSize) {
        this.totalSize = totalSize;
    }

    @Override
    public void pushUploadBytes(ByteBuffer buffer) {
        currentSize += buffer.remaining();
        if (currentSize > totalSize) {
            throw new IllegalStateException("Pushed too many bytes to NBSBuilder");
        }
        buffers.addLast(buffer);
    }

    @Override
    public int getDataSize() {
        return totalSize;
    }

    @Override
    public NBSRecord build() {
        return new NBSRecord(totalSize, Song.fromStream(new BufferArrayInputStream()));
    }

    private class BufferArrayInputStream extends InputStream {
        @Override
        public int read() {
            if (buffers.isEmpty()) {
                return -1;
            }
            ByteBuffer buffer = buffers.peek();
            if (buffer.remaining() == 0) {
                buffers.pop();
                return read();
            }
            return buffer.get() & 0xff;
        }
    }
}
