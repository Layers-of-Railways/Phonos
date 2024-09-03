/*
 * Decompiled with CFR 0.2.2 (FabricMC 7c48b8c4).
 */
package io.github.foundationgames.phonos.sound.stream;

import com.google.common.collect.Lists;
import io.github.foundationgames.phonos.Phonos;
import io.github.foundationgames.phonos.mixin_interfaces.ISeekableAudioStream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.sound.AudioStream;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.stb.STBVorbisInfo;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;

// copy to design mixin & to understand locals
@Environment(EnvType.CLIENT)
public class OggAudioStream implements AudioStream, ISeekableAudioStream {
    private static final int BUFFER_SIZE = 8192;
    private long pointer;
    private final AudioFormat format;
    private final InputStream inputStream;
    private ByteBuffer buffer = MemoryUtil.memAlloc(BUFFER_SIZE);
    private int sampleOffsetToSkipTo = 0;

    public OggAudioStream(InputStream inputStream) throws IOException {
        this.inputStream = inputStream;
        this.buffer.limit(0);
        try (MemoryStack memoryStack = MemoryStack.stackPush();){
            IntBuffer intBuffer = memoryStack.mallocInt(1);
            IntBuffer intBuffer2 = memoryStack.mallocInt(1);
            while (this.pointer == 0L) {
                if (!this.fillBufferFromInputStream()) {
                    throw new IOException("Failed to find Ogg header");
                }
                int i = this.buffer.position();
                this.buffer.position(0);
                this.pointer = STBVorbis.stb_vorbis_open_pushdata(this.buffer, intBuffer, intBuffer2, null);
                this.buffer.position(i);
                int j = intBuffer2.get(0);
                if (j == 1) {
                    this.increaseBufferSize();
                    continue;
                }
                if (j == 0) continue;
                throw new IOException("Failed to read Ogg file " + j);
            }
            this.buffer.position(this.buffer.position() + intBuffer.get(0));
            STBVorbisInfo sTBVorbisInfo = STBVorbisInfo.mallocStack(memoryStack);
            STBVorbis.stb_vorbis_get_info(this.pointer, sTBVorbisInfo);
            this.format = new AudioFormat(sTBVorbisInfo.sample_rate(), 16, sTBVorbisInfo.channels(), true, false);
        }
    }

    @Override
    public void phonos$seekForwardFromHere(float seconds) throws IOException {
        if (pointer == 0L) {
            return;
        }

        final int samplesPerSecond = (int) this.format.getSampleRate();
        final int bytesPerSecond = samplesPerSecond * this.format.getChannels() * 2;

        int startSampleOffset = STBVorbis.stb_vorbis_get_sample_offset(pointer);

        int quickSeekBytes = (int) (bytesPerSecond * seconds * 0.9); // bytes to seek in the inputStream

        // seek in the buffer and then the inputStream if needed
        int bufferRemaining = this.buffer.remaining();
        if (bufferRemaining >= quickSeekBytes) {
            this.buffer.position(this.buffer.position() + quickSeekBytes);
        } else {
            // seek to the limit
            this.buffer.position(this.buffer.limit());

            int toSeekInStream = quickSeekBytes - bufferRemaining;
            long actuallySkipped = 0;
            while (actuallySkipped < toSeekInStream) {
                long skipped = this.inputStream.skip(toSeekInStream - actuallySkipped);
                if (skipped == 0) {
                    break;
                }
                actuallySkipped += skipped;
            }
        }

        // Notify vorbis that we skipped the input
        STBVorbis.stb_vorbis_flush_pushdata(this.pointer);

        sampleOffsetToSkipTo = Math.max(sampleOffsetToSkipTo, startSampleOffset + (int) (samplesPerSecond * seconds));
    }

    /**
     * Fills the buffer from the input stream.<br/>
     * <i>Note: formerly known as <code>readHeader</code></i>
     * @return true if the header was read successfully or the buffer is full, false if the end of the stream was reached
     */
    private boolean fillBufferFromInputStream() throws IOException {
        int currentFill = this.buffer.limit();
        int remainingCapacity = this.buffer.capacity() - currentFill;
        if (remainingCapacity == 0) {
            return true;
        }

        byte[] bs = new byte[remainingCapacity];
        int bytesRead = this.inputStream.read(bs);
        if (bytesRead == -1) {
            return false;
        }

        int originalPosition = this.buffer.position();

        this.buffer.limit(currentFill + bytesRead); // make space for new data

        this.buffer.position(currentFill); // write at end of current data
        this.buffer.put(bs, 0, bytesRead);

        this.buffer.position(originalPosition); // return to original position

        return true;
    }

    private void increaseBufferSize() {
        boolean atStart = this.buffer.position() == 0;
        boolean atLimit = this.buffer.position() == this.buffer.limit();
        if (atLimit && !atStart) {
            this.buffer.position(0);
            this.buffer.limit(0);
        } else {
            ByteBuffer newBuffer = MemoryUtil.memAlloc(atStart ? 2 * this.buffer.capacity() : this.buffer.capacity());
            newBuffer.put(this.buffer);
            MemoryUtil.memFree(this.buffer);
            newBuffer.flip();
            this.buffer = newBuffer;
        }
    }

    private boolean readOggFile(ChannelList channelList) throws IOException {
        if (this.pointer == 0L) {
            return false;
        }
        try (MemoryStack memoryStack = MemoryStack.stackPush()){

            PointerBuffer outputPointerBuffer = memoryStack.mallocPointer(1);
            IntBuffer channelsBuffer = memoryStack.mallocInt(1);
            IntBuffer samplesBuffer = memoryStack.mallocInt(1);

            int numSamples;
            while (true) {
                int bytesUsed = STBVorbis.stb_vorbis_decode_frame_pushdata(this.pointer, this.buffer, channelsBuffer, outputPointerBuffer, samplesBuffer);
                this.buffer.position(this.buffer.position() + bytesUsed);

                int errorCode = STBVorbis.stb_vorbis_get_error(this.pointer);
                if (errorCode == STBVorbis.VORBIS_need_more_data) {
                    this.increaseBufferSize();
                    if (this.fillBufferFromInputStream()) continue; // there was more data to read
                    return false;
                }

                if (errorCode != 0) {
                    throw new IOException("Failed to read Ogg file " + errorCode);
                }

                numSamples = samplesBuffer.get(0);
                if (numSamples != 0) break;
            }

            int currentSampleOffset = STBVorbis.stb_vorbis_get_sample_offset(this.pointer);
            int samplesToSkip = Math.min(Math.max(0, sampleOffsetToSkipTo - currentSampleOffset), numSamples);

            int channelCount = channelsBuffer.get(0);
            PointerBuffer pointerBuffer2 = outputPointerBuffer.getPointerBuffer(channelCount);
            if (channelCount == 1) {
                FloatBuffer channel = pointerBuffer2.getFloatBuffer(0, numSamples);

                if (samplesToSkip > 0) {
                    channel.position(channel.position() + samplesToSkip);
                }

                this.readChannels(channel, channelList);
                return true;
            }
            if (channelCount == 2) {
                FloatBuffer channel1 = pointerBuffer2.getFloatBuffer(0, numSamples);
                FloatBuffer channel2 = pointerBuffer2.getFloatBuffer(1, numSamples);

                if (samplesToSkip > 0) {
                    channel1.position(channel1.position() + samplesToSkip);
                    channel2.position(channel2.position() + samplesToSkip);
                }

                this.readChannels(channel1, channel2, channelList);
                return true;
            }
            throw new IllegalStateException("Invalid number of channels: " + channelCount);
        }
    }

    private void readChannels(FloatBuffer buf, ChannelList channelList) {
        while (buf.hasRemaining()) {
            channelList.addChannel(buf.get());
        }
    }

    private void readChannels(FloatBuffer buf, FloatBuffer buf2, ChannelList channelList) {
        while (buf.hasRemaining() && buf2.hasRemaining()) {
            channelList.addChannel(buf.get());
            channelList.addChannel(buf2.get());
        }
    }

    @Override
    public void close() throws IOException {
        if (this.pointer != 0L) {
            STBVorbis.stb_vorbis_close(this.pointer);
            this.pointer = 0L;
        }
        MemoryUtil.memFree(this.buffer);
        this.inputStream.close();
    }

    @Override
    public AudioFormat getFormat() {
        return this.format;
    }

    @Override
    @SuppressWarnings("StatementWithEmptyBody")
    public ByteBuffer getBuffer(int size) throws IOException {
        ChannelList channelList = new ChannelList(size + BUFFER_SIZE);
        while (this.readOggFile(channelList) && channelList.currentBufferSize < size) {}
        return channelList.getBuffer();
    }

    @SuppressWarnings("StatementWithEmptyBody")
    public ByteBuffer getBuffer() throws IOException {
        ChannelList channelList = new ChannelList(16384);
        while (this.readOggFile(channelList)) {}
        return channelList.getBuffer();
    }

    @Environment(value=EnvType.CLIENT)
    static class ChannelList {
        private final List<ByteBuffer> buffers = Lists.newArrayList();
        private final int size;
        int currentBufferSize;
        private ByteBuffer buffer;

        public ChannelList(int size) {
            this.size = size + 1 & 0xFFFFFFFE;
            this.init();
        }

        private void init() {
            this.buffer = BufferUtils.createByteBuffer(this.size);
        }

        public void addChannel(float data) {
            if (this.buffer.remaining() == 0) {
                this.buffer.flip();
                this.buffers.add(this.buffer);
                this.init();
            }
            int i = MathHelper.clamp((int)(data * 32767.5f - 0.5f), Short.MIN_VALUE, Short.MAX_VALUE);
            this.buffer.putShort((short)i);
            this.currentBufferSize += 2;
        }

        public ByteBuffer getBuffer() {
            this.buffer.flip();
            if (this.buffers.isEmpty()) {
                return this.buffer;
            }
            ByteBuffer byteBuffer = BufferUtils.createByteBuffer(this.currentBufferSize);
            this.buffers.forEach(byteBuffer::put);
            byteBuffer.put(this.buffer);
            byteBuffer.flip();
            return byteBuffer;
        }
    }
}

