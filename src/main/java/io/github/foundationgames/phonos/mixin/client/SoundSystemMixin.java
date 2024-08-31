package io.github.foundationgames.phonos.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.foundationgames.phonos.mixin_interfaces.ISkippableSource;
import io.github.foundationgames.phonos.sound.SkippableSoundInstance;
import net.minecraft.client.sound.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundSystem.class)
public class SoundSystemMixin {

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
}
