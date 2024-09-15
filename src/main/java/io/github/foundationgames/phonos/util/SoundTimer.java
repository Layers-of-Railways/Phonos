package io.github.foundationgames.phonos.util;

import io.github.foundationgames.phonos.mixin.client.SoundLoaderAccessor;
import io.github.foundationgames.phonos.mixin.client.SoundManagerAccessor;
import io.github.foundationgames.phonos.mixin.client.SoundSystemAccessor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Xoroshiro128PlusPlusRandom;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

@Environment(EnvType.CLIENT)
public class SoundTimer {
    public static float getSoundLength(Identifier soundId) throws IOException {
        MinecraftClient mc = MinecraftClient.getInstance();

        var soundSet = mc.getSoundManager().get(soundId);
        if (soundSet == null) {
            throw new IOException("Sound set not found: " + soundId);
        }

        var sound = soundSet.getSound(new Xoroshiro128PlusPlusRandom(0L));

        if (sound == null) {
            throw new IOException("Sound not found: " + soundId);
        }

        var soundSystem = ((SoundManagerAccessor) mc.getSoundManager()).getSoundSystem();
        var soundLoader = ((SoundSystemAccessor) soundSystem).getSoundLoader();
        var resourceFactory = ((SoundLoaderAccessor) soundLoader).getResourceFactory();

        ByteBuffer buffer = null;
        try (InputStream stream = resourceFactory.open(sound.getLocation())) {
            buffer = MemoryUtil.memAlloc(8192);
            buffer.position(0);
            buffer.limit(0);

            // read entire stream into buffer
            while (true) {
                int limit = buffer.limit();
                int remaining = buffer.capacity() - limit;

                if (remaining == 0) {
                    // buffer is full, allocate a new one
                    ByteBuffer newBuffer = MemoryUtil.memAlloc(2 * buffer.capacity());
                    newBuffer.put(buffer);
                    MemoryUtil.memFree(buffer);
                    newBuffer.flip();
                    buffer = newBuffer;

                    limit = buffer.limit();
                    remaining = buffer.capacity() - limit;
                }

                byte[] bs = new byte[remaining];
                int read = stream.read(bs);
                if (read == -1) {
                    break;
                }
                int pos = buffer.position();
                buffer.limit(limit + read);
                buffer.position(limit);
                buffer.put(bs, 0, read);
                buffer.position(pos);
            }

            try (MemoryStack memoryStack = MemoryStack.stackPush()) {
                IntBuffer error = memoryStack.mallocInt(1);
                long ptr = STBVorbis.stb_vorbis_open_memory(buffer, error, null);
                if (ptr == MemoryUtil.NULL) {
                    throw new IOException("Failed to open Ogg Vorbis stream: " + OggSeeker.explain(error.get(0)));
                }

                float length = STBVorbis.stb_vorbis_stream_length_in_seconds(ptr);
                STBVorbis.stb_vorbis_close(ptr);

                return length;
            }
        } finally {
            if (buffer != null)
                MemoryUtil.memFree(buffer);
        }
    }
}
