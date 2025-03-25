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
import io.github.foundationgames.phonos.mixin_interfaces.ISeekableAudioStream;
import io.github.foundationgames.phonos.util.CleanableBufferedInputStream;
import io.github.foundationgames.phonos.util.OggSeeker;
import it.unimi.dsi.fastutil.floats.FloatConsumer;
import net.minecraft.client.sound.OggAudioStream;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.io.InputStream;

@Mixin(OggAudioStream.class)
public abstract class OggAudioStreamMixin implements ISeekableAudioStream {
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
}
