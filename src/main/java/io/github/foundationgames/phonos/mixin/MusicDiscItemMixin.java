package io.github.foundationgames.phonos.mixin;

import io.github.foundationgames.phonos.sound.MusicDiscOverrides;
import net.minecraft.item.MusicDiscItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MusicDiscItem.class)
public class MusicDiscItemMixin {
    @Inject(method = "getSongLengthInTicks", at = @At("RETURN"), cancellable = true)
    private void overrideLength(CallbackInfoReturnable<Integer> cir) {
        Integer length = MusicDiscOverrides.getTickLength((MusicDiscItem)(Object)this);
        if(length != null) cir.setReturnValue(length);
    }
}
