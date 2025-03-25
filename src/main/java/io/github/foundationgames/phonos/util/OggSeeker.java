package io.github.foundationgames.phonos.util;

import com.jcraft.jogg.Packet;
import com.jcraft.jogg.Page;
import com.jcraft.jogg.StreamState;
import com.jcraft.jogg.SyncState;
import com.jcraft.jorbis.Block;
import com.jcraft.jorbis.DspState;
import com.jcraft.jorbis.Info;
import com.mojang.datafixers.util.Either;
import io.github.foundationgames.phonos.Phonos;
import io.github.foundationgames.phonos.mixin_interfaces.ISyncStateCopyFrom;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import javax.sound.sampled.AudioFormat;
import java.io.EOFException;
import java.io.IOException;

@ApiStatus.Internal
public class OggSeeker {
    private static final int BUFSIZE = 8192;

    private final AudioFormat format;
    private final CleanableBufferedInputStream inputStream;

    private final Info info;

    // responsible for transforming a byte stream into pages
    private final SyncState syncState;
    private final Page page;

    // responsible for transforming pages into packets
    private final StreamState streamState;
    private final Packet packet;

    // responsible for decoding packets into audio
    private final DspState dspState;
    private final Block block;

    public OggSeeker(AudioFormat format, CleanableBufferedInputStream inputStream, Info info, SyncState syncState, Page page, StreamState streamState, Packet packet, DspState dspState, Block block) {
        this.format = format;
        this.inputStream = inputStream;
        this.info = info;
        this.syncState = syncState;
        this.page = page;
        this.streamState = streamState;
        this.packet = packet;
        this.dspState = dspState;
        this.block = block;
    }

    private boolean readToBuffer() throws IOException {
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

    /**
     * Reads a page from the input stream.
     * @param maxOffset maximum offset to seek to. Will return an error if the page is beyond this offset.
     * @return either a found page or an error code
     * @throws IOException if more data is needed but is not available
     */
    @NotNull
    private Either<PageSeekResult, PageSeekError> readPage(long maxOffset) throws IOException {
        int offset = 0;
        while (true) {
            int result = this.syncState.pageseek(this.page);
            if (result == 0) { // need more data
                if (!this.readToBuffer()) {
                    return Either.right(PageSeekError.EOF); // no more could be read, we will never find a page
                }
            } else if (result > 0) { // found a page
                return Either.left(new PageSeekResult(this.page, offset, result));
            } else { // page is (-result) bytes forward
                offset -= result;
                if (maxOffset > 0 && offset >= maxOffset) {
                    return Either.right(PageSeekError.BEYOND_BOUNDS);
                }
            }
        }
    }

    private void seekHelper(long offset) throws IOException {
        this.syncState.reset();
        this.inputStream.reset();
        this.inputStream.skipNBytes(offset);
    }

    /**
     * Seeks the parent Ogg stream to the specified position
     * @param seconds The amount to seek forward, in seconds.
     * @return Additional samples to skip.
     * @throws IOException If an I/O error occurs.
     */
    public int seek(final float seconds) throws IOException {
        final SyncState backupSyncState = new SyncState();
        ((ISyncStateCopyFrom) backupSyncState).phonos$copyFrom(this.syncState);

        final int samplesPerSecond = (int) this.format.getSampleRate();
        final int bytesPerSecond = samplesPerSecond * this.format.getFrameSize();

        final int targetSample = (int) (seconds * samplesPerSecond);

        Phonos.LOG.info("Beginning seek to sample {} ({} seconds)", targetSample, seconds);

        // this is a totally arbitrary number that seeks to help approximate the right position to start at
        final int compressionFactor = 4;

        // How much to increase by to find the right bound
        final long increaseStep = (int) (bytesPerSecond * seconds / (4 * compressionFactor));

        long begin = 0;
        long end = Integer.MAX_VALUE;
        long best = begin; // seek target for best page

        // we subtract increaseStep because the first iteration will add it back
        long bisect = (long) (bytesPerSecond * seconds / (2 * compressionFactor)) - increaseStep;

        // find the page with the highest granule position less than our target
        while (begin < end) {
            // determine candidate seek position
            if (end - begin < BUFSIZE) {
                bisect = begin;
            } else {
                if (end == Integer.MAX_VALUE) { // still looking for the right bound
                    bisect += increaseStep;
                } else {
                    bisect = (end + begin) / 2;
                }
            }

            try {
                seekHelper(bisect);
            } catch (EOFException e) {
                Phonos.LOG.warn("EOFException from seeking near end of stream");
                this.inputStream.reset();
                this.inputStream.clearMark();
                ((ISyncStateCopyFrom) this.syncState).phonos$copyFrom(backupSyncState);
                return targetSample;
            }
            var result = readPage(end - bisect);
            var maybeError = result.right();
            if (maybeError.isPresent()) {
                switch (maybeError.get()) {
                    case EOF -> {
                        this.inputStream.reset();
                        this.inputStream.clearMark();
                        ((ISyncStateCopyFrom) this.syncState).phonos$copyFrom(backupSyncState);
                        return targetSample; // couldn't read a page, not our problem
                    }
                    case BEYOND_BOUNDS -> {
                        end = bisect;
                    }
                };
            } else {
                assert result.left().isPresent();
                var pageResult = result.left().get();
                long granulePos = pageResult.page.granulepos();
                if (granulePos < targetSample) {
                    best = bisect + pageResult.offset;
                    begin = bisect + pageResult.offset + pageResult.length; // raw offset of next packet
                } else {
                    end = bisect;
                }
            }
        }

        // Next up:
        // - read the page we found
        // - load the last full packet
        // - prepare the decoder, by fully decoding and discarding the last packet
        //   (so that OggAudioStream can start decoding by grabbing the next packet)

        // read the page we found
        seekHelper(best);
        if (readPage(-1).left().isEmpty()) {
            Phonos.LOG.warn("Failed to find page after seeking");
            this.inputStream.reset();
            this.inputStream.clearMark();
            ((ISyncStateCopyFrom) this.syncState).phonos$copyFrom(backupSyncState);
            return targetSample;
        }

        // prepare packet processor
        int serialNo = this.page.serialno();
        this.streamState.init(serialNo);
        this.streamState.reset();

        // prepare decoder
        this.dspState.synthesis_init(this.info);
        this.block.init(this.dspState);

        // read page into streamState
        this.streamState.pagein(this.page);

        // now we can start pulling packets out of the stream
        while (true) {
            int result = this.streamState.packetout(this.packet);
            if (result <= 0) {
                Phonos.LOG.warn("Failed to find packet after seeking");
                this.inputStream.reset();
                this.inputStream.clearMark();
                ((ISyncStateCopyFrom) this.syncState).phonos$copyFrom(backupSyncState);
                return targetSample;
            }

            if (this.block.synthesis(this.packet) == 0) { // will be non-zero for headers, which we can just skip past
                this.dspState.synthesis_blockin(this.block);

                long granulePos = this.packet.granulepos; // position of the last sample that we're about to decode
                if (granulePos != -1) { // we've found the last packet in the page
                    int samples = this.dspState.synthesis_pcmout(null, null);
                    this.dspState.synthesis_read(samples); // discard samples

                    // drop unnecessary data
                    this.inputStream.clearMark();

                    int remaining = (int) (targetSample - granulePos);
                    float secondsRemaining = (targetSample - granulePos) / (float) samplesPerSecond;
                    Phonos.LOG.info("Successful seek to {}. Closest achieved was {}, with offset {}. Remaining: {} samples, {} seconds", targetSample, granulePos, best, remaining, secondsRemaining);
                    return remaining;
                }
            }
        }
    }

    private record PageSeekResult(Page page, int offset, int length) {}

    private enum PageSeekError {
        EOF,
        BEYOND_BOUNDS
    }
}
