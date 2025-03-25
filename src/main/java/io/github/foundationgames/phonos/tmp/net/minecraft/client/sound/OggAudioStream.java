package io.github.foundationgames.phonos.tmp.net.minecraft.client.sound;

import com.jcraft.jogg.Packet;
import com.jcraft.jogg.Page;
import com.jcraft.jogg.StreamState;
import com.jcraft.jogg.SyncState;
import com.jcraft.jorbis.Block;
import com.jcraft.jorbis.Comment;
import com.jcraft.jorbis.DspState;
import com.jcraft.jorbis.Info;
import it.unimi.dsi.fastutil.floats.FloatConsumer;
import java.io.IOException;
import java.io.InputStream;
import javax.sound.sampled.AudioFormat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.sound.BufferedAudioStream;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
@SuppressWarnings("unused")
public class OggAudioStream implements BufferedAudioStream {
    private static final int BUFSIZE = 8192;
    private static final int PAGEOUT_RECAPTURE = -1;
    private static final int PAGEOUT_NEED_MORE_DATA = 0;
    private static final int PAGEOUT_OK = 1;
    private static final int PACKETOUT_ERROR = -1;
    private static final int PACKETOUT_NEED_MORE_DATA = 0;
    private static final int PACKETOUT_OK = 1;

    private final Info info = new Info();

    // responsible for transforming a byte stream into pages
    private final SyncState syncState = new SyncState();
    private final Page page = new Page();

    // responsible for transforming pages into packets
    private final StreamState streamState = new StreamState();
    private final Packet packet = new Packet();

    // responsible for decoding packets into audio
    private final DspState dspState = new DspState();
    private final Block block = new Block(this.dspState);

    private final AudioFormat format;
    private final InputStream inputStream;
    private long samplesWritten;
    private long totalSamplesInStream = Long.MAX_VALUE;

    public OggAudioStream(InputStream inputStream) throws IOException {
        this.inputStream = inputStream;
        Comment comment = new Comment();
        Page page = this.readPage();
        if (page == null) {
            throw new IOException("Invalid Ogg file - can't find first page");
        } else {
            Packet packet = this.readIdentificationPacket(page);
            if (isError(this.info.synthesis_headerin(comment, packet))) {
                throw new IOException("Invalid Ogg identification packet");
            } else {
                for (int i = 0; i < 2; i++) {
                    packet = this.readPacket();
                    if (packet == null) {
                        throw new IOException("Unexpected end of Ogg stream");
                    }

                    if (isError(this.info.synthesis_headerin(comment, packet))) {
                        throw new IOException("Invalid Ogg header packet " + i);
                    }
                }

                this.dspState.synthesis_init(this.info);
                this.block.init(this.dspState);
                this.format = new AudioFormat((float)this.info.rate, 16, this.info.channels, true, false);
            }
        }
    }

    private static boolean isError(int code) {
        return code < 0;
    }

    @Override
    public AudioFormat getFormat() {
        return this.format;
    }

    private boolean readToBuffer() throws IOException { // was read()
        int alreadyFilled = this.syncState.buffer(BUFSIZE);
        byte[] bs = this.syncState.data;
        int readCount = this.inputStream.read(bs, alreadyFilled, BUFSIZE);
        if (readCount == -1) {
            return false;
        } else {
            this.syncState.wrote(readCount);
            return true;
        }
    }

    @Nullable
    private Page readPage() throws IOException {
        while (true) {
            int result = this.syncState.pageout(this.page);
            switch (result) {
                case PAGEOUT_RECAPTURE:
                    throw new IllegalStateException("Corrupt or missing data in bitstream");
                case PAGEOUT_NEED_MORE_DATA:
                    if (this.readToBuffer()) {
                        break;
                    }

                    return null;
                case PAGEOUT_OK:
                    if (this.page.eos() != 0) {
                        this.totalSamplesInStream = this.page.granulepos();
                    }

                    return this.page;
                default:
                    throw new IllegalStateException("Unknown page decode result: " + result);
            }
        }
    }

    private Packet readIdentificationPacket(Page page) throws IOException {
        this.streamState.init(page.serialno());
        if (isError(this.streamState.pagein(page))) {
            throw new IOException("Failed to parse page");
        } else {
            int result = this.streamState.packetout(this.packet);
            if (result != PACKETOUT_OK) {
                throw new IOException("Failed to read identification packet: " + result);
            } else {
                return this.packet;
            }
        }
    }

    @Nullable
    private Packet readPacket() throws IOException {
        while (true) {
            int result = this.streamState.packetout(this.packet);
            switch (result) {
                case PACKETOUT_ERROR:
                    throw new IOException("Failed to parse packet");
                case PACKETOUT_NEED_MORE_DATA:
                    Page page = this.readPage();
                    if (page == null) {
                        return null;
                    }

                    if (!isError(this.streamState.pagein(page))) {
                        break;
                    }

                    throw new IOException("Failed to parse page");
                case PACKETOUT_OK:
                    return this.packet;
                default:
                    throw new IllegalStateException("Unknown packet decode result: " + result);
            }
        }
    }

    private long getSamplesToWrite(int availableSamples) {
        long newTotal = this.samplesWritten + (long)availableSamples;
        long ret;
        if (newTotal > this.totalSamplesInStream) {
            ret = this.totalSamplesInStream - this.samplesWritten;
            this.samplesWritten = this.totalSamplesInStream;
        } else {
            this.samplesWritten = newTotal;
            ret = availableSamples;
        }

        return ret;
    }

    @Override
    public boolean read(FloatConsumer consumer) throws IOException {
        float[][][] sourcePtr = new float[1][][];
        int[] startIndexes = new int[this.info.channels];
        Packet packet = this.readPacket();
        if (packet == null) {
            return false;
        } else if (isError(this.block.synthesis(packet))) {
            throw new IOException("Can't decode audio packet");
        } else {
            this.dspState.synthesis_blockin(this.block);

            int availableSamples;
            while ((availableSamples = this.dspState.synthesis_pcmout(sourcePtr, startIndexes)) > 0) {
                float[][] source = sourcePtr[0];
                long samplesToWrite = this.getSamplesToWrite(availableSamples);
                switch (this.info.channels) {
                    case 1:
                        copyMono(source[0], startIndexes[0], samplesToWrite, consumer);
                        break;
                    case 2:
                        copyStereo(source[0], startIndexes[0], source[1], startIndexes[1], samplesToWrite, consumer);
                        break;
                    default:
                        copyAnyChannels(source, this.info.channels, startIndexes, samplesToWrite, consumer);
                }

                this.dspState.synthesis_read(availableSamples);
            }

            return true;
        }
    }

    private static void copyAnyChannels(float[][] source, int channels, int[] startIndexes, long samplesToWrite, FloatConsumer output) {
        for (int j = 0; (long)j < samplesToWrite; j++) {
            for (int chan = 0; chan < channels; chan++) {
                int startIdx = startIndexes[chan];
                float value = source[chan][startIdx + j];
                output.accept(value);
            }
        }
    }

    private static void copyMono(float[] source, int startIndex, long samplesToWrite, FloatConsumer output) {
        for (int j = startIndex; (long)j < (long)startIndex + samplesToWrite; j++) {
            output.accept(source[j]);
        }
    }

    private static void copyStereo(float[] leftSource, int leftStartIndex, float[] rightSource, int rightStartIndex, long samplesToWrite, FloatConsumer output) {
        for (int k = 0; (long)k < samplesToWrite; k++) {
            output.accept(leftSource[leftStartIndex + k]);
            output.accept(rightSource[rightStartIndex + k]);
        }
    }

    public void close() throws IOException {
        this.inputStream.close();
    }
}
