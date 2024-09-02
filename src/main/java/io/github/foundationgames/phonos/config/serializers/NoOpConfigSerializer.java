package io.github.foundationgames.phonos.config.serializers;

import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.ConfigField;
import dev.isxander.yacl3.config.v2.api.ConfigSerializer;
import dev.isxander.yacl3.config.v2.api.FieldAccess;

import java.util.Map;

public class NoOpConfigSerializer<T> extends ConfigSerializer<T> {
    public NoOpConfigSerializer(ConfigClassHandler<T> config) {
        super(config);
    }

    @Override
    public void save() {}

    @Override
    public LoadResult loadSafely(Map<ConfigField<?>, FieldAccess<?>> bufferAccessMap) {
        return LoadResult.NO_CHANGE;
    }
}
