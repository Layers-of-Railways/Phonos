package io.github.foundationgames.phonos.mixin.compat;

import de.maxhenkel.voicechat.voice.common.LocationSoundPacket;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LocationSoundPacket.class)
public interface LocationSoundPacketAccessor extends SoundPacketAccessor {
    @Accessor("location")
    void setLocation(Vec3d location);

    @Accessor("distance")
    void setDistance(float distance);
}
