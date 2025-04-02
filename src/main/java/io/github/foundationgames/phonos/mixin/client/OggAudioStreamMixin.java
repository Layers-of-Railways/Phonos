package io.github.foundationgames.phonos.mixin.client;

import com.jcraft.jogg.Packet;
import com.jcraft.jogg.Page;
import com.jcraft.jogg.StreamState;
import com.jcraft.jogg.SyncState;
import com.jcraft.jorbis.Block;
import com.jcraft.jorbis.DspState;
import com.jcraft.jorbis.Info;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.foundationgames.phonos.mixin_interfaces.IMonoForceableAudioStream;
import io.github.foundationgames.phonos.mixin_interfaces.ISeekableAudioStream;
import io.github.foundationgames.phonos.util.CleanableBufferedInputStream;
import io.github.foundationgames.phonos.util.OggSeeker;
import it.unimi.dsi.fastutil.floats.FloatConsumer;
import net.minecraft.client.sound.OggAudioStream;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.io.InputStream;

@Mixin(OggAudioStream.class)
public abstract class OggAudioStreamMixin implements ISeekableAudioStream, IMonoForceableAudioStream {
    @Shadow @Final @Mutable // must make mutable for init-replacement
    private InputStream inputStream;

    @Shadow public abstract AudioFormat getFormat();

    @Shadow @Final private AudioFormat format;
    @Shadow @Final private Info info;
    @Shadow @Final private SyncState syncState;
    @Shadow @Final private Page page;
    @Shadow @Final private StreamState streamState;
    @Shadow @Final private Packet packet;
    @Shadow @Final private DspState dspState;
    @Shadow @Final private Block block;

    @Shadow
    private static void method_59760(float[] fs, int i, long l, FloatConsumer floatConsumer) {
        throw new AssertionError("Mixin did not apply");
    }

    @Unique
    private int phonos$remainingSamplesToSkip = 0;

    @Unique
    private boolean phonos$forceMono = false;
    @Unique
    private AudioFormat phonos$forcedMonoFormat = null;

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
    public void phonos$seekForwardFromHere(float seconds) throws IOException {
        final CleanableBufferedInputStream is = phonos$is();
        is.mark(Integer.MAX_VALUE);

        phonos$remainingSamplesToSkip += new OggSeeker(this.format, is, this.info, this.syncState, this.page, this.streamState, this.packet, this.dspState, this.block).seek(seconds);
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
        int skip;
        if (phonos$remainingSamplesToSkip > 0) {
            skip = (int) Math.min(phonos$remainingSamplesToSkip, samplesToWrite);
            phonos$remainingSamplesToSkip -= skip;
        } else {
            skip = 0;
        }

        if (phonos$forceMono) {
            float[] out = new float[(int) (samplesToWrite - skip)];
            for (int i = 0; i < out.length; i++) {
                out[i] = (leftSource[leftStartIndex + skip + i] + rightSource[rightStartIndex + skip + i]) / 2;
            }
            method_59760(out, 0, samplesToWrite - skip, output); // mono output
        } else {
            original.call(leftSource, leftStartIndex + skip, rightSource, rightStartIndex + skip, samplesToWrite - skip, output);
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
        int skip;
        int[] newIndexes;
        if (phonos$remainingSamplesToSkip > 0) {
            skip = (int) Math.min(phonos$remainingSamplesToSkip, samplesToWrite);
            phonos$remainingSamplesToSkip -= skip;
            newIndexes = new int[startIndexes.length];
            for (int i = 0; i < startIndexes.length; i++) {
                newIndexes[i] = startIndexes[i] + skip;
            }
        } else {
            skip = 0;
            newIndexes = startIndexes;
        }

        if (phonos$forceMono) {
            float[] out = new float[(int) (samplesToWrite - skip)];
            for (int i = 0; i < out.length; i++) {
                float sum = 0;
                for (int j = 0; j < channels; j++) {
                    sum += source[j][newIndexes[j] + i];
                }
                out[i] = sum / channels;
            }
            method_59760(out, 0, samplesToWrite - skip, output); // mono output
        } else {
            original.call(source, channels, newIndexes, samplesToWrite - skip, output);
        }
    }

    @Inject(method = "getFormat", at = @At("HEAD"), cancellable = true)
    private void phonos$forceMonoGetFormat(CallbackInfoReturnable<AudioFormat> cir) {
        if (phonos$forceMono) {
            cir.setReturnValue(phonos$forcedMonoFormat);
        }
    }

    @Override
    public void phonos$forceMono() {
        if (phonos$forceMono) return;
        if (format.getChannels() == 1) return;
        phonos$forceMono = true;
        int channels = 1;
        int sampleSizeInBits = format.getSampleSizeInBits();
        phonos$forcedMonoFormat = new AudioFormat(
            format.getEncoding(),
            format.getSampleRate(),
            sampleSizeInBits,
            channels,
            sampleSizeInBits != -1 ? (sampleSizeInBits + 7) / 8 * channels : -1,
            format.getFrameRate(),
            format.isBigEndian()
        );
    }
}
