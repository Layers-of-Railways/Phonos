package io.github.foundationgames.phonos.mixin.nbs;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import cz.koca2000.nbs4j.Note;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "cz.koca2000.nbs4j.NBSReader", remap = false)
public class NBSReaderMixin {
    @WrapOperation(method = "readNotes", at = @At(value = "INVOKE", target = "Lcz/koca2000/nbs4j/Note;setVolume(I)Lcz/koca2000/nbs4j/Note;"))
    private static Note clampVolume(Note instance, int volume, Operation<Note> original) {
        if (volume < 0) volume += 256;
        if (volume > 100) volume = 100;

        return original.call(instance, volume);
    }
}
