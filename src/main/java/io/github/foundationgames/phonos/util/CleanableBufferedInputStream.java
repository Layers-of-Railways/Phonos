package io.github.foundationgames.phonos.util;

import org.jetbrains.annotations.ApiStatus;
import sun.misc.Unsafe;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Arrays;

public class CleanableBufferedInputStream extends BufferedInputStream {
    private static final Unsafe U;
    static {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            U = (Unsafe) f.get(null);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static final long BUF_OFFSET;
    static {
        try {
            BUF_OFFSET = U.objectFieldOffset(BufferedInputStream.class.getDeclaredField("buf"));
        } catch (NoSuchFieldException ex) {
            throw new RuntimeException(ex);
        }
    }

    private final int size;

    public CleanableBufferedInputStream(InputStream in) {
        super(in);
        this.size = buf.length;
    }

    public CleanableBufferedInputStream(InputStream in, int size) {
        super(in, size);
        this.size = size;
    }

    private byte[] getBufIfOpen() throws IOException {
        byte[] buffer = buf;
        if (buffer == null)
            throw new IOException("Stream closed");
        return buffer;
    }

    /**
     * Discards unnecessary leading bytes from the buffer and shrinks it to the minimum size.
     */
    public synchronized void gcBuf() throws IOException {
        gcBuf(false);
    }

    /**
     * Discards unnecessary leading bytes from the buffer and shrinks it to the minimum size.
     *
     * @param force Whether to force garbage collection even if not a substantial gain.
     */
    public synchronized void gcBuf(boolean force) throws IOException {
        byte[] buffer = getBufIfOpen();

        int unnecessary = pos;
        if (markpos >= 0 && markpos < unnecessary) {
            unnecessary = markpos;
        }

        // throw away unnecessary leading bytes
        if (!force && unnecessary * 4 < size) {
            unnecessary = 0;
        }
        if (unnecessary > 0) {
            byte[] nbuf = new byte[Math.max(count - unnecessary, size)];
            System.arraycopy(buffer, unnecessary, nbuf, 0, count - unnecessary);

            if (!U.compareAndSwapObject(this, BUF_OFFSET, buffer, nbuf)) {
                throw new RuntimeException("Stream closed");
            }

            count -= unnecessary;
            pos -= unnecessary;
            markpos -= unnecessary;
        }

        // throw away unnecessary trailing bytes
        if (buffer.length > size && buffer.length > count) {
            int nsize = Math.max(size, count);

            int saved = buffer.length - nsize;
            if (!force && saved * 4 < size) {
                saved = 0;
            }

            if (saved > 0) {
                byte[] nbuf = new byte[nsize];

                System.arraycopy(buffer, 0, nbuf, 0, count);

                if (!U.compareAndSwapObject(this, BUF_OFFSET, buffer, nbuf)) {
                    throw new RuntimeException("Stream closed");
                }
            }
        }
    }

    /**
     * Clears the mark position and limit.
     * <p>
     *     Will call {@link #gcBuf()} to discard unnecessary leading bytes and shrink the buffer.
     * </p>
     */
    public synchronized void clearMark() throws IOException {
        this.markpos = -1;
        this.marklimit = 0;

        gcBuf();
    }

    /**
     * Adds the given history and sets the mark position to the beginning of the buffer.
     * @param history externally-derived history to be prepended to the buffer
     * @param readlimit the maximum number of bytes that can be read before the mark position becomes invalid
     * @see java.io.InputStream#mark(int)
     */
    public synchronized void addHistoryResetAndMark(byte[] history, int readlimit) throws IOException {
        gcBuf(true); // so it is a simple operation to add history

        byte[] buffer = getBufIfOpen();

        byte[] nbuf = new byte[buffer.length + history.length];
        System.arraycopy(history, 0, nbuf, 0, history.length);
        System.arraycopy(buffer, 0, nbuf, history.length, buffer.length);

        if (!U.compareAndSwapObject(this, BUF_OFFSET, buffer, nbuf)) {
            throw new RuntimeException("Stream closed");
        }

        this.marklimit = readlimit;
        this.markpos = 0;
        this.pos = 0;
        this.count += history.length;
    }

    @SuppressWarnings("SameParameterValue")
    private static String summary(byte[] buf, int startLength, int endLength) {
        if (startLength + endLength >= buf.length) {
            return Arrays.toString(buf) + "#"+buf.length;
        }
        byte[] start = new byte[startLength];
        byte[] end = new byte[endLength];

        System.arraycopy(buf, 0, start, 0, startLength);
        System.arraycopy(buf, buf.length - endLength, end, 0, endLength);

        String start$ = Arrays.toString(start);
        String end$ = Arrays.toString(end);

        start$ = start$.substring(0, start$.length() - 1) + ", ...";
        end$ = ", " + end$.substring(1);

        String descriptor = "#"+buf.length;

        return start$ + end$ + descriptor;
    }

    @ApiStatus.Internal
    public String debugState() {
        return "CleanableBufferedInputStream{\n" +
                "\tbuf=" + summary(buf, 6, 4) + ",\n" +
                "\tpos=" + pos +              ",\n" +
                "\tcount=" + count +          ",\n" +
                "\tmarkpos=" + markpos +      ",\n" +
                "\tmarklimit=" + marklimit +  "\n"  +
                '}';
    }
}
