package io.github.foundationgames.phonos.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.foundationgames.phonos.mixin_interfaces.ISeekableAudioStream;
import io.github.foundationgames.phonos.util.CleanableBufferedInputStream;
import it.unimi.dsi.fastutil.floats.FloatConsumer;
import net.minecraft.client.sound.OggAudioStream;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.io.InputStream;

@Mixin(OggAudioStream.class)
public abstract class OggAudioStreamMixin implements ISeekableAudioStream {
    /*@Shadow private long pointer;
    @Shadow @Final private AudioFormat format;
    @Shadow private ByteBuffer buffer;*/
    @Shadow @Final @Mutable // must make mutable for init-replacement
    private InputStream inputStream;

/*    @Shadow protected abstract boolean readHeader() throws IOException;

    @Shadow protected abstract void increaseBufferSize();*/

    @Shadow public abstract AudioFormat getFormat();

    @Unique
    private int phonos$remainingSamplesToSkip = 0;

    @WrapOperation(method = "<init>", at = @At(value = "FIELD", target = "Lnet/minecraft/client/sound/OggAudioStream;inputStream:Ljava/io/InputStream;"))
    private void bufferStream(OggAudioStream instance, InputStream value, Operation<Void> original) {
        value = new CleanableBufferedInputStream(value);
        original.call(instance, value);
    }

    @Unique
    private CleanableBufferedInputStream phonos$is() {
        return (CleanableBufferedInputStream) this.inputStream;
    }

    @Override
    @Unique
    @SuppressWarnings("resource")
    public void phonos$seekForwardFromHere(float seconds) throws IOException {
        // TODO do this properly with jorbis
        phonos$remainingSamplesToSkip += (int) (seconds * getFormat().getSampleRate() * getFormat().getFrameSize());
        /*if (this.pointer == 0L) {
            return;
        }

        final CleanableBufferedInputStream is = phonos$is();

        int historySize = this.buffer.limit() - this.buffer.position();

        if (historySize > 0) {
            byte[] history = new byte[historySize];
            this.buffer.get(history);

            is.addHistoryResetAndMark(history, Integer.MAX_VALUE);
        } else {
            is.mark(Integer.MAX_VALUE);
        }

        phonos$remainingSamplesToSkip += new OggSeeker(this.pointer, this.format, is, () -> this.buffer, this::readHeader, this::increaseBufferSize).seek(seconds);*/
    }

    @WrapOperation(
        method = "read(Lit/unimi/dsi/fastutil/floats/FloatConsumer;)Z",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/sound/OggAudioStream;method_59760([FIJLit/unimi/dsi/fastutil/floats/FloatConsumer;)V"
        )
    )
    private void doSkip(float[] source, int startIndex, long samplesToWrite, FloatConsumer output, Operation<Void> original) {
        if (phonos$remainingSamplesToSkip > 0) {
            int skip = (int) Math.min(phonos$remainingSamplesToSkip, samplesToWrite);
            phonos$remainingSamplesToSkip -= skip;
            original.call(source, startIndex + skip, samplesToWrite - skip, output);
        } else {
            original.call(source, startIndex, samplesToWrite, output);
        }
    }

    @WrapOperation(
        method = "read(Lit/unimi/dsi/fastutil/floats/FloatConsumer;)Z",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/sound/OggAudioStream;method_59761([FI[FIJLit/unimi/dsi/fastutil/floats/FloatConsumer;)V"
        )
    )
    private void doSkip(float[] leftSource, int leftStartIndex, float[] rightSource, int rightStartIndex, long samplesToWrite, FloatConsumer output, Operation<Void> original) {
        if (phonos$remainingSamplesToSkip > 0) {
            int skip = (int) Math.min(phonos$remainingSamplesToSkip, samplesToWrite);
            phonos$remainingSamplesToSkip -= skip;
            original.call(leftSource, leftStartIndex + skip, rightSource, rightStartIndex + skip, samplesToWrite - skip, output);
        } else {
            original.call(leftSource, leftStartIndex, rightSource, rightStartIndex, samplesToWrite, output);
        }
    }

    @WrapOperation(
        method = "read(Lit/unimi/dsi/fastutil/floats/FloatConsumer;)Z",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/sound/OggAudioStream;method_59762([[FI[IJLit/unimi/dsi/fastutil/floats/FloatConsumer;)V"
        )
    )
    private void doSkip(float[][] source, int channels, int[] startIndexes, long samplesToWrite, FloatConsumer output, Operation<Void> original) {
        if (phonos$remainingSamplesToSkip > 0) {
            int skip = (int) Math.min(phonos$remainingSamplesToSkip, samplesToWrite);
            phonos$remainingSamplesToSkip -= skip;
            int[] newIndexes = new int[startIndexes.length];
            for (int i = 0; i < startIndexes.length; i++) {
                newIndexes[i] = startIndexes[i] + skip;
            }
            original.call(source, channels, newIndexes, samplesToWrite - skip, output);
        } else {
            original.call(source, channels, startIndexes, samplesToWrite, output);
        }
    }

    /*@Inject(method = "readChannels(Ljava/nio/FloatBuffer;Lnet/minecraft/client/sound/OggAudioStream$ChannelList;)V", at = @At("HEAD"))
    private void doSkip(FloatBuffer buf, @Coerce Object channelList, CallbackInfo ci) {
        if (phonos$remainingSamplesToSkip > 0) {
            int skip = Math.min(phonos$remainingSamplesToSkip, buf.remaining());
            buf.position(buf.position() + skip);
            phonos$remainingSamplesToSkip -= skip;
        }
    }

    @Inject(method = "readChannels(Ljava/nio/FloatBuffer;Ljava/nio/FloatBuffer;Lnet/minecraft/client/sound/OggAudioStream$ChannelList;)V", at = @At("HEAD"))
    private void doSkip(FloatBuffer buf, FloatBuffer buf2, @Coerce Object channelList, CallbackInfo ci) {
        if (phonos$remainingSamplesToSkip > 0) {
            int skip = Math.min(phonos$remainingSamplesToSkip, Math.min(buf.remaining(), buf2.remaining()));
            buf.position(buf.position() + skip);
            buf2.position(buf2.position() + skip);
            phonos$remainingSamplesToSkip -= skip;
        }
    }*/
}
