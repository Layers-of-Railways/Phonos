package io.github.foundationgames.phonos.mixin_interfaces;

import com.jcraft.jogg.SyncState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.NotNull;

@Environment(EnvType.CLIENT)
public interface ISyncStateCopyFrom {
    void phonos$copyFrom(@NotNull SyncState other);
}
