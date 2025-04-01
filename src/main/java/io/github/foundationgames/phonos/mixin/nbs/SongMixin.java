package io.github.foundationgames.phonos.mixin.nbs;

import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import cz.koca2000.nbs4j.Song;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.TreeMap;

@Mixin(Song.class)
public class SongMixin {
    @Shadow @Final private TreeMap<Integer, Float> tempoChanges;

    /**
     * @author SlimeistDev
     * @reason temp patches until we switch to a better library
     */
    @Overwrite(remap = false)
    public float getTempo(int tick){
        if (tempoChanges.size() == 0)
            return 10;
        return tempoChanges.floorEntry(tick).getValue();
    }
}
