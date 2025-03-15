package io.github.foundationgames.phonos.mixin;

import io.github.foundationgames.phonos.datapack.MusicDiscOverrides;
import net.minecraft.block.jukebox.JukeboxSong;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(JukeboxSong.class)
public class JukeboxSongMixin {
    @Inject(method = "lengthInSeconds", at = @At("RETURN"), cancellable = true)
    private void overrideLength(CallbackInfoReturnable<Float> cir) {
        Float length = MusicDiscOverrides.getSecondLength((JukeboxSong) (Object)this);
        if(length != null) cir.setReturnValue(length);
    }
}
