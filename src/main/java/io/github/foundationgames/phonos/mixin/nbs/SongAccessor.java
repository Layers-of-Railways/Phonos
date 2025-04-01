package io.github.foundationgames.phonos.mixin.nbs;

import cz.koca2000.nbs4j.Song;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Song.class)
@SuppressWarnings("UnusedReturnValue")
public interface SongAccessor {
    @Invoker
    Song callIncreaseNonCustomInstrumentsCountTo(int nonCustomInstrumentsCount);
}
