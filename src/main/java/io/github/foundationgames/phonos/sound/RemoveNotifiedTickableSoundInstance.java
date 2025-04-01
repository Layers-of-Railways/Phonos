package io.github.foundationgames.phonos.sound;

import net.minecraft.client.sound.TickableSoundInstance;

public interface RemoveNotifiedTickableSoundInstance extends TickableSoundInstance {
    void setDone();
}
