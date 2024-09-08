package io.github.foundationgames.phonos.util;

import io.github.foundationgames.phonos.Phonos;
import org.lwjgl.PointerBuffer;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.system.MemoryStack;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.function.Supplier;

/**
 * Seeks an Ogg stream to a specified position.
 * @param pointer Pointer to the Ogg stream.
 * @param format Audio format of the Ogg stream.
 * @param is File input stream.
 * @param buffer Buffer of the Ogg stream.
 * @param readData Function to read data from the input stream into the buffer.
 */
public record OggSeeker(
    long pointer,
    AudioFormat format,
    CleanableBufferedInputStream is,
    Supplier<ByteBuffer> buffer,
    ExceptionalSupplier<Boolean, ? extends IOException> readData,
    Runnable increaseBufferSize
) {
    private boolean $readData() throws IOException {
        return readData.get();
    }

    private void $increaseBufferSize() {
        increaseBufferSize.run();
    }

    /**
     * Seeks the given Ogg stream to the specified position.
     *
     * @param seconds The amount to seek forward, in seconds.
     * @return Additional samples to skip.
     * @throws IOException If an I/O error occurs.
     */
    public int seek(
        final float seconds
    ) throws IOException {
        if (this.pointer == 0L) {
            Phonos.LOG.warn("Attempted to seek an Ogg stream with a null pointer");
            return 0;
        }

        final int samplesPerSecond = (int) this.format.getSampleRate();
        final int bytesPerSecond = samplesPerSecond * this.format.getFrameSize();

        final int targetSample = (int) (seconds * samplesPerSecond);

        // this is a totally arbitrary number that seeks to help approximate the right position to start at
        final int compressionFactor = 4;

        // How much to increase by to find the right bound
        final int increaseStep = (int) (bytesPerSecond * seconds / (4 * compressionFactor));

        int samplesAtLeft = 0;
        int leftBound = 0;

        int samplesAtRight = Integer.MAX_VALUE;
        int rightBound = Integer.MAX_VALUE;

        int currentPos = (int) (bytesPerSecond * seconds / (2 * compressionFactor));

        int totalTries = 200;

        while (leftBound < currentPos && currentPos < rightBound) {
            // clear out buffer
            this.buffer.get().position(0);
            this.buffer.get().limit(0);

            is.reset();
            is.skipNBytes(currentPos);

            if (!this.$readData()) {
                throw new IOException("Failed to read OGG stream");
            }

            STBVorbis.stb_vorbis_flush_pushdata(this.pointer);

            // decode until STBVorbis.stb_vorbis_get_sample_offset returns an actual value

            boolean found = false;
            int sampleOffset = STBVorbis.stb_vorbis_get_sample_offset(this.pointer);
            int offsetToAcquire = 0;
            try (MemoryStack memoryStack = MemoryStack.stackPush()) {
                PointerBuffer outputPointerBuffer = memoryStack.mallocPointer(1);
                IntBuffer channelsBuffer = memoryStack.mallocInt(1);
                IntBuffer samplesBuffer = memoryStack.mallocInt(1);

                int numSamples = 0;
                int tries = 0;
                while (sampleOffset == -1) {
                    if (this.buffer.get().limit() - this.buffer.get().position() < 4) { // STBVorbis can get stuck looking for the acquire magic code 'OggS' otherwise
                        this.$increaseBufferSize();
                        if (!this.$readData())
                            throw new IOException("Failed to read OGG stream");
                    }

                    int bytesUsed = STBVorbis.stb_vorbis_decode_frame_pushdata(this.pointer, this.buffer.get(), channelsBuffer, outputPointerBuffer, samplesBuffer);
                    this.buffer.get().position(this.buffer.get().position() + bytesUsed);

                    int errorCode = STBVorbis.stb_vorbis_get_error(this.pointer);
                    if (errorCode == STBVorbis.VORBIS_need_more_data || bytesUsed == 0) {
                        offsetToAcquire += bytesUsed;

                        this.$increaseBufferSize();
                        if (this.$readData()) continue; // there was more data to read
                        throw new IOException("Failed to read OGG stream"); // we've reached the end of the stream
                    }

                    if (errorCode != 0) {
                        throw new IOException("Failed to read Ogg file " + explain(errorCode));
                    }

                    numSamples += samplesBuffer.get(0);
                    sampleOffset = STBVorbis.stb_vorbis_get_sample_offset(this.pointer);

                    if (sampleOffset == -1) {
                        offsetToAcquire += bytesUsed;
                    }

                    if (tries++ > 9 || rightBound - leftBound < 4096 || --totalTries < 0) {
                        found = true;
                        break;
                    }
                }

                if (numSamples > 0) {
                    Phonos.LOG.warn("Unexpectedly decoded {} samples while seeking", numSamples);
                }
            }

            Phonos.LOG.info("Seeking to {} samples, got {}", targetSample, sampleOffset);

            if (found) {
                break;
            }

            if (sampleOffset < targetSample) {
                if (sampleOffset >= samplesAtRight) {
                    break;
                }

                currentPos += offsetToAcquire - 1;

                leftBound = currentPos;
                samplesAtLeft = sampleOffset;

                if (rightBound == Integer.MAX_VALUE) {
                    currentPos += increaseStep;
                } else {
                    currentPos = (currentPos + rightBound) / 2;
                }

                if ((targetSample - samplesAtLeft) / (float) samplesPerSecond < 0.5) {
                    break;
                }
            } else {
                rightBound = currentPos;
                samplesAtRight = sampleOffset;

                currentPos = (currentPos + leftBound) / 2;
            }
        }

        Phonos.LOG.info("Found goal, closest sample to {} is {}, at offset {}", targetSample, samplesAtLeft, leftBound);

        // seek to left bound
        STBVorbis.stb_vorbis_flush_pushdata(this.pointer);

        is.reset();
        is.skipNBytes(leftBound);

        buffer.get().position(0);
        buffer.get().limit(0);

        if (!$readData()) {
            throw new IOException("Failed to read OGG stream");
        }

        int sampleOffset = STBVorbis.stb_vorbis_get_sample_offset(this.pointer);

        // acquire again for decoder
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            PointerBuffer outputPointerBuffer = memoryStack.mallocPointer(1);
            IntBuffer channelsBuffer = memoryStack.mallocInt(1);
            IntBuffer samplesBuffer = memoryStack.mallocInt(1);

            int numSamples = 0;
            while (sampleOffset == -1) {
                if (buffer.get().limit() - buffer.get().position() < 4) {
                    this.$increaseBufferSize();
                    if (!this.$readData())
                        throw new IOException("Failed to read OGG stream");
                }

                int bytesUsed = STBVorbis.stb_vorbis_decode_frame_pushdata(this.pointer, buffer.get(), channelsBuffer, outputPointerBuffer, samplesBuffer);
                buffer.get().position(buffer.get().position() + bytesUsed);

                int errorCode = STBVorbis.stb_vorbis_get_error(this.pointer);
                if (errorCode == STBVorbis.VORBIS_need_more_data || bytesUsed == 0) {
                    this.$increaseBufferSize();
                    if (this.$readData()) continue;
                    throw new IOException("Failed to read OGG stream");
                }

                if (errorCode != 0) {
                    throw new IOException("Failed to read Ogg file " + explain(errorCode));
                }

                numSamples += samplesBuffer.get(0);
                sampleOffset = STBVorbis.stb_vorbis_get_sample_offset(this.pointer);
            }

            if (numSamples > 0) {
                Phonos.LOG.warn("Unexpectedly decoded {} samples while acquiring post-seek", numSamples);
            }
        }

        is.clearMark();

        return targetSample - sampleOffset;
    }

    private static String explain(int vorbisErrorCode) {
        return switch (vorbisErrorCode) {
            case STBVorbis.VORBIS__no_error                        -> "No error";
            case STBVorbis.VORBIS_need_more_data                   -> "Need more data";
            case STBVorbis.VORBIS_invalid_api_mixing               -> "Invalid API mixing";
            case STBVorbis.VORBIS_outofmem                         -> "Out of memory";
            case STBVorbis.VORBIS_feature_not_supported            -> "Feature not supported";
            case STBVorbis.VORBIS_too_many_channels                -> "Too many channels";
            case STBVorbis.VORBIS_file_open_failure                -> "File open failure";
            case STBVorbis.VORBIS_seek_without_length              -> "Seek without length";
            case STBVorbis.VORBIS_unexpected_eof                   -> "Unexpected EOF";
            case STBVorbis.VORBIS_seek_invalid                     -> "Seek invalid";
            case STBVorbis.VORBIS_invalid_setup                    -> "Invalid setup";
            case STBVorbis.VORBIS_invalid_stream                   -> "Invalid stream";
            case STBVorbis.VORBIS_missing_capture_pattern          -> "Missing capture pattern";
            case STBVorbis.VORBIS_invalid_stream_structure_version -> "Invalid stream structure version";
            case STBVorbis.VORBIS_continued_packet_flag_invalid    -> "Continued packet flag invalid";
            case STBVorbis.VORBIS_incorrect_stream_serial_number   -> "Incorrect stream serial number";
            case STBVorbis.VORBIS_invalid_first_page               -> "Invalid first page";
            case STBVorbis.VORBIS_bad_packet_type                  -> "Bad packet type";
            case STBVorbis.VORBIS_cant_find_last_page              -> "Can't find last page";
            case STBVorbis.VORBIS_seek_failed                      -> "Seek failed";
            case STBVorbis.VORBIS_ogg_skeleton_not_supported       -> "Ogg skeleton not supported";
            default -> "Unknown error code " + vorbisErrorCode;
        };
    }
}
