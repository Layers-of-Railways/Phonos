package io.github.foundationgames.phonos.radio;

import org.jetbrains.annotations.Nullable;

import java.util.function.LongConsumer;

@FunctionalInterface
public interface RadioLongConsumer extends LongConsumer {
    @Override
    default void accept(long value) {
        accept(value, null);
    }

    void accept(long value, @Nullable RadioMetadata pos);

    static LongConsumer cast(RadioLongConsumer radioLongConsumer) {
        return radioLongConsumer;
    }
}
