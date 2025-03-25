package io.github.foundationgames.phonos.mixin.client;

import com.jcraft.jogg.SyncState;
import io.github.foundationgames.phonos.mixin_interfaces.ISyncStateCopyFrom;
import net.minecraft.client.MinecraftClient;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(SyncState.class)
public class SyncStateMixin implements ISyncStateCopyFrom {
    @Shadow public byte[] data;

    @Shadow int storage;

    @Shadow int fill;

    @Shadow int returned;

    @Shadow int unsynced;

    @Shadow int headerbytes;

    @Shadow int bodybytes;

    @Override
    public void phonos$copyFrom(@NotNull SyncState other) {
        var $other = (SyncStateMixin) (Object) other;
        this.data = $other.data.clone();
        this.storage = $other.storage;
        this.fill = $other.fill;
        this.returned = $other.returned;
        this.unsynced = $other.unsynced;
        this.headerbytes = $other.headerbytes;
        this.bodybytes = $other.bodybytes;
    }
}
