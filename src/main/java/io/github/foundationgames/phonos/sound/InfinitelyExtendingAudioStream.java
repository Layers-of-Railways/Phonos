package io.github.foundationgames.phonos.sound;

import net.minecraft.client.sound.AudioStream;
import org.lwjgl.BufferUtils;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.nio.ByteBuffer;

public class InfinitelyExtendingAudioStream implements AudioStream {
    private AudioFormat format = new AudioFormat(20_000, 8, 1, true, false);
    private AudioStream wrapped;

    public InfinitelyExtendingAudioStream(AudioStream wrapped) {
        this.wrapped = wrapped;
        this.format = wrapped.getFormat();
    }

    @Override
    public AudioFormat getFormat() {
        return format;
    }

    @Override
    public ByteBuffer getBuffer(int size) throws IOException {
        if (wrapped != null) {
            ByteBuffer wrappedBuffer = wrapped.getBuffer(size);
            if (wrappedBuffer == null || wrappedBuffer.limit() == 0) {
                wrapped.close();
                wrapped = null;
            } else {
                return wrappedBuffer;
            }
        }
        ByteBuffer out = BufferUtils.createByteBuffer(size);
        byte[] bytes = new byte[size];
        out.put(bytes);
        out.flip();

        return out;
    }

    @Override
    public void close() throws IOException {
        if (wrapped != null)
            wrapped.close();
    }
}
