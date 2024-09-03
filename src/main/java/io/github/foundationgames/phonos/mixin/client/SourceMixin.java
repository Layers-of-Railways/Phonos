package io.github.foundationgames.phonos.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.foundationgames.phonos.Phonos;
import io.github.foundationgames.phonos.mixin_interfaces.ISeekableAudioStream;
import io.github.foundationgames.phonos.mixin_interfaces.ISkippableSource;
import io.github.foundationgames.phonos.util.BufferUtil;
import net.minecraft.client.sound.AudioStream;
import net.minecraft.client.sound.Source;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.nio.ByteBuffer;

@Mixin(Source.class)
public class SourceMixin implements ISkippableSource {
    @Shadow private @Nullable AudioStream stream;
    @Unique
    private long phonos$ticksToSkip = 0;

    @Override
    @Unique
    public void phonos$skipTicks(long ticks) {
        phonos$ticksToSkip += ticks;
    }

    @WrapOperation(method = "read", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sound/AudioStream;getBuffer(I)Ljava/nio/ByteBuffer;"))
    private ByteBuffer skipRead(AudioStream instance, int size, Operation<ByteBuffer> original) {
        if (phonos$ticksToSkip <= 0)
            return original.call(instance, size);

        if (this.stream instanceof ISeekableAudioStream seekableAudioStream) {
            try {
                seekableAudioStream.phonos$seekForwardFromHere(phonos$ticksToSkip / 20.0f);
            } catch (IOException e) {
                Phonos.LOG.error("Failed to skip audio stream", e);
            }
            phonos$ticksToSkip = 0;
            return original.call(instance, size);
        }

        AudioFormat format = instance.getFormat();
        float conversionFactor = format.getFrameRate() * format.getFrameSize();
        long framesToSkip = (long) (phonos$ticksToSkip / 20.0f * conversionFactor);

        ByteBuffer buffer;
        while (true) {
            buffer = original.call(instance, Math.max(size, (int) Math.min(Integer.MAX_VALUE, framesToSkip)));

            if (framesToSkip <= 0) break;

            if (buffer == null) break;

            if (buffer.remaining() < framesToSkip) {
                framesToSkip -= buffer.remaining();

                BufferUtil.freeDirectBufferMemory(buffer);
                //noinspection UnusedAssignment
                buffer = null; // help GC out
            } else {
                buffer.position(buffer.position() + (int) framesToSkip);
                framesToSkip = 0;
                break;
            }
        }

        phonos$ticksToSkip = (long) (framesToSkip / conversionFactor * 20.0f);

        return buffer;
    }
}
