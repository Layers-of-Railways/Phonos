package io.github.foundationgames.phonos.world.sound;

import net.minecraft.util.DyeColor;
import org.jetbrains.annotations.Nullable;

public interface RenderableCableConnection {
    CablePlugPoint getStart();
    CablePlugPoint getEnd();
    @Nullable
    DyeColor getColor();

    boolean isStatic();
}
