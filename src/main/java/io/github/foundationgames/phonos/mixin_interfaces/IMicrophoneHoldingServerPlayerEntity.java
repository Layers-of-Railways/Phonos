package io.github.foundationgames.phonos.mixin_interfaces;

import io.github.foundationgames.phonos.block.entity.MicrophoneBaseBlockEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface IMicrophoneHoldingServerPlayerEntity {
    void phonos$setBaseStation(@Nullable MicrophoneBaseBlockEntity baseStation);

    @Nullable
    MicrophoneBaseBlockEntity phonos$getBaseStation();

    /**
     * Clear the base station if it is the same as the one passed in
     * @param compareTo The base station to compare to
     * @return true if the base station was cleared, false if it was not
     */
    boolean phonos$clearBaseStation(@NotNull MicrophoneBaseBlockEntity compareTo);
}
