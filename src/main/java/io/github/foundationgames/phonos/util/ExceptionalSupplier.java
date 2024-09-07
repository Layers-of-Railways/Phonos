package io.github.foundationgames.phonos.util;

@FunctionalInterface
public interface ExceptionalSupplier<T, E extends Throwable> {
    T get() throws E;
}
