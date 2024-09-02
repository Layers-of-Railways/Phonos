package io.github.foundationgames.phonos.config.serializers;

import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.ConfigSerializer;

import java.util.function.Consumer;

public class SaveCallbackConfigSerializer<T> extends WrappingConfigSerializer<T> {
    private final Consumer<ConfigClassHandler<T>> saveCallback;

    public SaveCallbackConfigSerializer(ConfigClassHandler<T> config, ConfigSerializer<T> wrapped, Consumer<ConfigClassHandler<T>> saveCallback) {
        super(config, wrapped);
        this.saveCallback = saveCallback;
    }

    @Override
    public void save() {
        super.save();
        saveCallback.accept(config);
    }
}
