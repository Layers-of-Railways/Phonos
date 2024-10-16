package io.github.foundationgames.phonos.mixin.client;

import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(WorldRenderer.class)
public interface WorldRendererAccess {

    @Accessor("frustum")
    Frustum phonos$getFrustum();
}
