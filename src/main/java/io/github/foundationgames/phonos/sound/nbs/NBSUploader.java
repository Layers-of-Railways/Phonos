package io.github.foundationgames.phonos.sound.nbs;

import cz.koca2000.nbs4j.NBSVersion;
import cz.koca2000.nbs4j.Song;
import io.github.foundationgames.phonos.sound.custom.PhonosAudioRecordUploader;
import io.netty.buffer.ByteBuf;

import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;

public class NBSUploader implements PhonosAudioRecordUploader {
    private static final int BYTES_PER_FRAGMENT = 7000;

    private final Deque<ByteBuffer> buffers = new ArrayDeque<>();
    private int totalSize = 0;

    public NBSUploader(Song song) {
        song.save(NBSVersion.LATEST, new BatchedBufferedOutputStream());
        for (ByteBuffer buffer : this.buffers) {
            buffer.flip();
        }
    }

    @Override
    public UploadFragment getNextFragment() {
        return new UploadFragment(totalSize, buffers.removeFirst(), buffers.isEmpty());
    }

    private class BatchedBufferedOutputStream extends OutputStream {
        @Override
        @SuppressWarnings("DataFlowIssue")
        public void write(int b) {
            if (buffers.isEmpty() || buffers.peekLast().remaining() == 0) {
                buffers.addLast(ByteBuffer.allocate(BYTES_PER_FRAGMENT));
            }

            buffers.peekLast().put((byte) b);
            totalSize++;
        }
    }
}
