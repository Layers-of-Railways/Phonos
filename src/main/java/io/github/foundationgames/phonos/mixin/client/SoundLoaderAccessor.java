package io.github.foundationgames.phonos.mixin.client;

import net.minecraft.client.sound.SoundLoader;
import net.minecraft.resource.ResourceFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SoundLoader.class)
public interface SoundLoaderAccessor {
    @Accessor("resourceFactory")
    ResourceFactory getResourceFactory();
}
