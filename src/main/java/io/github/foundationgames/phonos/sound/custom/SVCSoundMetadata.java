package io.github.foundationgames.phonos.sound.custom;

import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SVCSoundMetadata {
    private @NotNull Vec3d position = Vec3d.ZERO;
    private float distance = 0.0f;
    private @Nullable String category = null;
    private float volume = 1.0f;

    public @NotNull Vec3d getPosition() {
        return position;
    }

    public void setPosition(@NotNull Vec3d position) {
        this.position = position;
    }

    public float getDistance() {
        return distance;
    }

    public void setDistance(float distance) {
        this.distance = distance;
    }

    public @Nullable String getCategory() {
        return category;
    }

    public void setCategory(@Nullable String category) {
        this.category = category;
    }

    public float getVolume() {
        return volume;
    }

    public void setVolume(float volume) {
        this.volume = volume;
    }
}
