package io.github.foundationgames.phonos.mixin;

import io.github.foundationgames.phonos.block.entity.MicrophoneBaseBlockEntity;
import io.github.foundationgames.phonos.mixin_interfaces.IMicrophoneHoldingServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.lang.ref.WeakReference;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin implements IMicrophoneHoldingServerPlayerEntity {
    @Nullable
    @Unique
    private WeakReference<MicrophoneBaseBlockEntity> phonos$baseStation = null;

    @Override
    @Final
    public void phonos$setBaseStation(@Nullable MicrophoneBaseBlockEntity baseStation) {
        if (baseStation == null) {
            this.phonos$baseStation = null;
        } else {
            this.phonos$baseStation = new WeakReference<>(baseStation);
        }
    }

    @Override
    @Final
    public @Nullable MicrophoneBaseBlockEntity phonos$getBaseStation() {
        return this.phonos$baseStation == null ? null : this.phonos$baseStation.get();
    }

    @Override
    @Final
    public boolean phonos$clearBaseStation(@NotNull MicrophoneBaseBlockEntity compareTo) {
        if (this.phonos$baseStation == null) return false;

        MicrophoneBaseBlockEntity baseStation = this.phonos$baseStation.get();
        if (baseStation == null || baseStation == compareTo) {
            this.phonos$baseStation = null;
        }
        return baseStation == compareTo;
    }
}
