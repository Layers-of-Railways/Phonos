package io.github.foundationgames.phonos.util;

import io.github.foundationgames.phonos.Phonos;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.function.Supplier;

public final class BufferUtil {

    private BufferUtil() {}

    private static final Unsafe UNSAFE = ((Supplier<Unsafe>) () -> {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            return (Unsafe) f.get(null);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }
    }).get();

    /** Frees the specified buffer's direct memory allocation.<br>
     * The buffer should not be used after calling this method; you should
     * instead allow it to be garbage-collected by removing all references of it
     * from your program.
     *
     * @param directBuffer The direct buffer whose memory allocation will be
     *            freed
     * @return Whether or not the memory allocation was freed */
    public static boolean freeDirectBufferMemory(ByteBuffer directBuffer) {
        if(!directBuffer.isDirect()) {
            return false;
        }
        try {
            UNSAFE.invokeCleaner(directBuffer);
            return true;
        } catch (IllegalArgumentException ex) {
            Phonos.LOG.error("Failed to free direct buffer", ex);
            return false;
        }
    }

}
