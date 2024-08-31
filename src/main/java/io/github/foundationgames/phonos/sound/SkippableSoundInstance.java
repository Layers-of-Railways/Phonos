package io.github.foundationgames.phonos.sound;

import net.minecraft.client.sound.SoundInstance;

public interface SkippableSoundInstance extends SoundInstance {
    long getSkippedTicks();
}
