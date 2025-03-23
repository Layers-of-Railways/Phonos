package io.github.foundationgames.phonos.world.sound;

import java.util.function.BiConsumer;

public interface ConnectionCollection {
    void forEach(BiConsumer<Integer, CableConnection> action);

    default int getOutputCount() {
        int[] count = new int[] {0};
        forEach((i, conn) -> count[0]++);
        return count[0];
    }
}
