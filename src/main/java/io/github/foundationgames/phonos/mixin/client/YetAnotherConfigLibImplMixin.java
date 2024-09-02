package io.github.foundationgames.phonos.mixin.client;

import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.impl.YetAnotherConfigLibImpl;
import io.github.foundationgames.phonos.mixin_interfaces.YetAnotherConfigLibImplDuck;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(YetAnotherConfigLibImpl.class)
@SuppressWarnings("UnstableApiUsage")
public class YetAnotherConfigLibImplMixin implements YetAnotherConfigLibImplDuck {

    @Unique
    private boolean phonos$serverSided = false;

    @Unique
    private ConfigClassHandler<?> phonos$handler;

    @Override
    public void phonos$setServerSided(boolean serverSided) {
        this.phonos$serverSided = serverSided;
    }

    @Override
    public boolean phonos$isServerSided() {
        return this.phonos$serverSided;
    }

    @Override
    public void phonos$setHandler(ConfigClassHandler<?> handler) {
        this.phonos$handler = handler;
    }

    @Override
    public ConfigClassHandler<?> phonos$getHandler() {
        return this.phonos$handler;
    }
}
