package io.github.foundationgames.phonos.util;

import net.minecraft.util.math.BlockPos;

import java.util.Random;
import java.util.UUID;

public final class UniqueId {
    public static long random() {
        return new Random().nextLong();
    }

    public static long ofBlock(BlockPos pos) {
        return obf(pos.asLong() + 0xABCDEF);
    }

    public static long ofRadioChannel(int channel) {
        return obf(channel + 0xFADECAB);
    }

    public static long obf(long uniqueId) {
        return new Random(uniqueId).nextLong();
    }

    public static UUID uuidOf(long uniqueId) {
        Random random = new Random(uniqueId);
        return new UUID(random.nextLong(), random.nextLong());
    }

    public static UUID uuidOf(long uniqueId, UUID uuid) {
        Random random = new Random(uniqueId * uuid.getMostSignificantBits() * uuid.getLeastSignificantBits());
        return new UUID(random.nextLong(), random.nextLong());
    }
}
