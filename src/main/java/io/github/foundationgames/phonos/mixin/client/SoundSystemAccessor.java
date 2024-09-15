package io.github.foundationgames.phonos.mixin.client;

import net.minecraft.client.sound.SoundLoader;
import net.minecraft.client.sound.SoundSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SoundSystem.class)
public interface SoundSystemAccessor {
    @Accessor("soundLoader")
    SoundLoader getSoundLoader();
}
