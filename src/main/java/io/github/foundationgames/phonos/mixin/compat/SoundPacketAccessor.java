package io.github.foundationgames.phonos.mixin.compat;

import de.maxhenkel.voicechat.voice.common.SoundPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SoundPacket.class)
public interface SoundPacketAccessor {
    @Accessor("category")
    void setCategory(String category);
}
