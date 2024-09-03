package io.github.foundationgames.phonos.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.foundationgames.phonos.mixin_interfaces.ISeekableAudioStream;
import net.minecraft.client.sound.OggAudioStream;
import org.lwjgl.stb.STBVorbis;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

@Mixin(OggAudioStream.class)
public abstract class OggAudioStreamMixin implements ISeekableAudioStream {
    @Shadow private long pointer;
    @Shadow @Final private AudioFormat format;
    @Shadow private ByteBuffer buffer;
    @Shadow @Final private InputStream inputStream;

    @Shadow protected abstract boolean readHeader() throws IOException;

    @Unique
    private int phonos$sampleOffsetToSkipTo = 0;

    @Override
    @Unique
    public void phonos$seekForwardFromHere(float seconds) throws IOException {
        if (this.pointer == 0L) {
            return;
        }

        final int samplesPerSecond = (int) this.format.getSampleRate();
        final int bytesPerSecond = samplesPerSecond * this.format.getChannels();// * 2;

        int startSampleOffset = STBVorbis.stb_vorbis_get_sample_offset(this.pointer);

        int quickSeekBytes = (int) (bytesPerSecond * seconds * 0.9); // bytes to seek in the inputStream

        // seek in the buffer and then the inputStream if needed
        int bufferRemaining = this.buffer.remaining();
        if (bufferRemaining >= quickSeekBytes) {
            this.buffer.position(this.buffer.position() + quickSeekBytes);
        } else {
            // seek to the limit
            this.buffer.position(0);
            this.buffer.limit(0);

            int toSeekInStream = quickSeekBytes - bufferRemaining;
            long actuallySkipped = 0;
            while (actuallySkipped < toSeekInStream) {
                long skipped = this.inputStream.skip(toSeekInStream - actuallySkipped);
                if (skipped == 0) {
                    break;
                }
                actuallySkipped += skipped;
            }

            this.readHeader();
        }

        // Notify vorbis that we skipped the input
        STBVorbis.stb_vorbis_flush_pushdata(this.pointer);

        phonos$sampleOffsetToSkipTo = Math.max(phonos$sampleOffsetToSkipTo, startSampleOffset + (int) (samplesPerSecond * seconds));
    }

    @WrapOperation(method = "readOggFile", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sound/OggAudioStream;readChannels(Ljava/nio/FloatBuffer;Lnet/minecraft/client/sound/OggAudioStream$ChannelList;)V"))
    private void phonos$skipSamples(OggAudioStream instance, FloatBuffer buf, @Coerce Object channelList, Operation<Void> original, @Local(name = "k") int numSamples) {
        // numSamples is result of intBuffer2.get(0);
        // intBuffer2 is the last arg to STBVorbis.stb_vorbis_decode_frame_pushdata

        if (phonos$sampleOffsetToSkipTo == 0) {
            original.call(instance, buf, channelList);
            return;
        }

        int currentSampleOffset = STBVorbis.stb_vorbis_get_sample_offset(this.pointer);
        int samplesToSkip = Math.min(Math.max(0, phonos$sampleOffsetToSkipTo - currentSampleOffset), numSamples);

        if (samplesToSkip > 0) {
            buf.position(buf.position() + samplesToSkip);
        }

        if (samplesToSkip + currentSampleOffset >= phonos$sampleOffsetToSkipTo) {
            phonos$sampleOffsetToSkipTo = 0;
        }

        original.call(instance, buf, channelList);
    }

    @WrapOperation(method = "readOggFile", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sound/OggAudioStream;readChannels(Ljava/nio/FloatBuffer;Ljava/nio/FloatBuffer;Lnet/minecraft/client/sound/OggAudioStream$ChannelList;)V"))
    private void phonos$skipSamples2(OggAudioStream instance, FloatBuffer buf, FloatBuffer buf2, @Coerce Object channelList, Operation<Void> original, @Local(name = "k") int numSamples) {
        // numSamples is result of intBuffer2.get(0);
        // intBuffer2 is the last arg to STBVorbis.stb_vorbis_decode_frame_pushdata

        if (phonos$sampleOffsetToSkipTo == 0) {
            original.call(instance, buf, buf2, channelList);
            return;
        }

        int currentSampleOffset = STBVorbis.stb_vorbis_get_sample_offset(this.pointer);
        int samplesToSkip = Math.min(Math.max(0, phonos$sampleOffsetToSkipTo - currentSampleOffset), numSamples);

        if (samplesToSkip > 0) {
            buf.position(buf.position() + samplesToSkip);
            buf2.position(buf2.position() + samplesToSkip);
        }

        if (samplesToSkip + currentSampleOffset >= phonos$sampleOffsetToSkipTo) {
            phonos$sampleOffsetToSkipTo = 0;
        }

        original.call(instance, buf, buf2, channelList);
    }
}
