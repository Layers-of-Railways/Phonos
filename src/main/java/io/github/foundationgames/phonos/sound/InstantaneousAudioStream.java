package io.github.foundationgames.phonos.sound;

import net.minecraft.client.sound.AudioStream;
import org.lwjgl.BufferUtils;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.nio.ByteBuffer;

public class InstantaneousAudioStream implements AudioStream {
    private static final AudioFormat FORMAT = new AudioFormat(20_000, 16, 1, true, false);
    private final ByteBuffer buffer;

    public InstantaneousAudioStream() {
        this.buffer = BufferUtils.createByteBuffer(8192);
    }

    @Override
    public AudioFormat getFormat() {
        return FORMAT;
    }

    @Override
    public ByteBuffer read(int size) throws IOException {
        buffer.flip();
        return buffer;
    }

    @Override
    public void close() throws IOException {}
}
