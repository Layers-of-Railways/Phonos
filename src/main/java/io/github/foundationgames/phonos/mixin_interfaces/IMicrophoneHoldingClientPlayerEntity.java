package io.github.foundationgames.phonos.mixin_interfaces;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

public interface IMicrophoneHoldingClientPlayerEntity {
    @Environment(EnvType.CLIENT)
    void phonos$setHolding(boolean holding);

    @Environment(EnvType.CLIENT)
    boolean phonos$isHolding();
}
