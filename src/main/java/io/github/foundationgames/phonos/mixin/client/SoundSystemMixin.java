package io.github.foundationgames.phonos.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.foundationgames.phonos.mixin_interfaces.ISkippableSource;
import io.github.foundationgames.phonos.sound.RemoveNotifiedTickableSoundInstance;
import io.github.foundationgames.phonos.sound.UnlimitedPitchSoundInstance;
import io.github.foundationgames.phonos.sound.SkippableSoundInstance;
import net.minecraft.client.sound.*;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(SoundSystem.class)
public class SoundSystemMixin {

    @Shadow @Final private List<TickableSoundInstance> tickingSounds;

    @Inject(
        method = "play(Lnet/minecraft/client/sound/SoundInstance;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sound/Channel$SourceManager;run(Ljava/util/function/Consumer;)V")
    )
    private void skip(SoundInstance sound, CallbackInfo ci, @Local Channel.SourceManager sourceManager) {
        if (sound instanceof SkippableSoundInstance skippableSoundInstance) {
            sourceManager.run(source -> {
                if (source instanceof ISkippableSource skippableSource) {
                    skippableSource.phonos$skipTicks(skippableSoundInstance.getSkippedTicks());
                }
            });
        }
    }

    @Inject(method = "getAdjustedPitch", at = @At("HEAD"), cancellable = true)
    private void unlimitedPitch(SoundInstance sound, CallbackInfoReturnable<Float> cir) {
        if (sound instanceof UnlimitedPitchSoundInstance) {
            cir.setReturnValue(Math.max(sound.getPitch(), 1e-6f));
        }
    }

    @WrapOperation(method = "tick()V", at = @At(value = "INVOKE", target = "Ljava/util/List;remove(Ljava/lang/Object;)Z"))
    private boolean markRemovedSoundDone(List<?> instance, Object o, Operation<Boolean> original) {
        if (instance == this.tickingSounds && o instanceof RemoveNotifiedTickableSoundInstance rntsi) {
            rntsi.setDone();
        }

        return original.call(instance, o);
    }
}
