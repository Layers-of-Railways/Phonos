package io.github.foundationgames.phonos.mixin_interfaces;

import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;

public interface YetAnotherConfigLibImplDuck {
    void phonos$setServerSided(boolean serverSided);
    boolean phonos$isServerSided();

    void phonos$setHandler(ConfigClassHandler<?> handler);
    ConfigClassHandler<?> phonos$getHandler();
}
