package io.github.foundationgames.phonos.util;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

import java.lang.reflect.Array;

public class PhonosPacketCodecs {
    public static <B extends ByteBuf, V> PacketCodec<B, V[]> array(Class<? super V> elementClass, PacketCodec<? super B, V> elementCodec) {
        return array(elementClass, elementCodec, Integer.MAX_VALUE);
    }

    public static <B extends ByteBuf, V> PacketCodec<B, V[]> array(Class<? super V> elementClass, PacketCodec<? super B, V> elementCodec, int maxSize) {
        return new PacketCodec<>() {
            @Override
            public V[] decode(B buf) {
                int i = PacketCodecs.readCollectionSize(buf, maxSize);
                @SuppressWarnings("unchecked")
                V[] values = (V[]) Array.newInstance(elementClass, i);

                for (int j = 0; j < i; j++) {
                    values[j] = elementCodec.decode(buf);
                }

                return values;
            }

            @Override
            public void encode(B buf, V[] value) {
                PacketCodecs.writeCollectionSize(buf, value.length, maxSize);

                for (V v : value) {
                    elementCodec.encode(buf, v);
                }
            }
        };
    }
}
