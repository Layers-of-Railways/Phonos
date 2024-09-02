package io.github.foundationgames.phonos.config.serializers;

import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.ConfigField;
import dev.isxander.yacl3.config.v2.api.ConfigSerializer;
import dev.isxander.yacl3.config.v2.api.FieldAccess;

import java.util.Map;

public class WrappingConfigSerializer<T> extends ConfigSerializer<T> {
    private final ConfigSerializer<T> wrapped;

    public WrappingConfigSerializer(ConfigClassHandler<T> config, ConfigSerializer<T> wrapped) {
        super(config);
        this.wrapped = wrapped;
    }

    @Override
    public LoadResult loadSafely(Map<ConfigField<?>, FieldAccess<?>> bufferAccessMap) {
        return wrapped.loadSafely(bufferAccessMap);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void load() {
        wrapped.load();
    }

    @Override
    public void save() {
        wrapped.save();
    }
}
