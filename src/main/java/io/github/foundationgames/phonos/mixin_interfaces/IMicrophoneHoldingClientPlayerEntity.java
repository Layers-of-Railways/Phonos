package io.github.foundationgames.phonos.mixin_interfaces;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

public interface IMicrophoneHoldingClientPlayerEntity {
    @Environment(EnvType.CLIENT)
    void phonos$setHoldingState(State state);

    @Environment(EnvType.CLIENT)
    State phonos$getHoldingState();

    enum State {
        NONE,
        WIRED,
        WIRELESS
    }
}
