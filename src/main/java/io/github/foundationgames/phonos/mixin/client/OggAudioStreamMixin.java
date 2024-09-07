package io.github.foundationgames.phonos.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.foundationgames.phonos.mixin_interfaces.ISeekableAudioStream;
import io.github.foundationgames.phonos.util.CleanableBufferedInputStream;
import io.github.foundationgames.phonos.util.OggSeeker;
import net.minecraft.client.sound.OggAudioStream;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
    @Shadow @Final @Mutable // must make mutable for init-replacement
    private InputStream inputStream;

    @Shadow protected abstract boolean readHeader() throws IOException;

    @Shadow protected abstract void increaseBufferSize();

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
        if (this.pointer == 0L) {
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

        phonos$remainingSamplesToSkip += new OggSeeker(this.pointer, this.format, is, () -> this.buffer, this::readHeader, this::increaseBufferSize).seek(seconds);
    }

    @Inject(method = "readChannels(Ljava/nio/FloatBuffer;Lnet/minecraft/client/sound/OggAudioStream$ChannelList;)V", at = @At("HEAD"))
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
    }
}
