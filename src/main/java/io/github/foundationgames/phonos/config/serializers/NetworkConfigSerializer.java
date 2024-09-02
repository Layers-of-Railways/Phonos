package io.github.foundationgames.phonos.config.serializers;

import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.ConfigField;
import dev.isxander.yacl3.config.v2.api.FieldAccess;
import net.minecraft.network.PacketByteBuf;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static java.util.Map.entry;

public class NetworkConfigSerializer {
    private static final Map<Class<?>, NetCodec<?>> CODECS = Map.ofEntries(
        entry(boolean.class, new NetCodec<>(PacketByteBuf::readBoolean, PacketByteBuf::writeBoolean)),
        entry(Boolean.class, new NetCodec<>(PacketByteBuf::readBoolean, PacketByteBuf::writeBoolean)),

        entry(byte.class, new NetCodec<Byte>(PacketByteBuf::readByte, PacketByteBuf::writeByte)),
        entry(Byte.class, new NetCodec<Byte>(PacketByteBuf::readByte, PacketByteBuf::writeByte)),

        entry(short.class, new NetCodec<Short>(PacketByteBuf::readShort, PacketByteBuf::writeShort)),
        entry(Short.class, new NetCodec<Short>(PacketByteBuf::readShort, PacketByteBuf::writeShort)),

        entry(int.class, new NetCodec<>(PacketByteBuf::readInt, PacketByteBuf::writeInt)),
        entry(Integer.class, new NetCodec<>(PacketByteBuf::readInt, PacketByteBuf::writeInt)),

        entry(long.class, new NetCodec<>(PacketByteBuf::readLong, PacketByteBuf::writeLong)),
        entry(Long.class, new NetCodec<>(PacketByteBuf::readLong, PacketByteBuf::writeLong)),

        entry(float.class, new NetCodec<>(PacketByteBuf::readFloat, PacketByteBuf::writeFloat)),
        entry(Float.class, new NetCodec<>(PacketByteBuf::readFloat, PacketByteBuf::writeFloat)),

        entry(double.class, new NetCodec<>(PacketByteBuf::readDouble, PacketByteBuf::writeDouble)),
        entry(Double.class, new NetCodec<>(PacketByteBuf::readDouble, PacketByteBuf::writeDouble)),

        entry(String.class, new NetCodec<>(PacketByteBuf::readString, PacketByteBuf::writeString))
    );

    @SuppressWarnings("unchecked")
    public static <T> void write(PacketByteBuf buf, FieldAccess<T> field) {
        NetCodec<T> codec = (NetCodec<T>) CODECS.get(field.typeClass());
        if (codec == null) throw new IllegalArgumentException("No codec for type " + field.typeClass());
        codec.write.accept(buf, field.get());
    }

    @SuppressWarnings("unchecked")
    public static <T> void read(PacketByteBuf buf, FieldAccess<T> field) {
        NetCodec<T> codec = (NetCodec<T>) CODECS.get(field.typeClass());
        if (codec == null) throw new IllegalArgumentException("No codec for type " + field.typeClass());
        field.set(codec.read.apply(buf));
    }

    public static void write(PacketByteBuf buf, ConfigClassHandler<?> config) {
        for (ConfigField<?> field : config.fields()) {
            write(buf, field.access());
        }
    }

    public static void read(PacketByteBuf buf, ConfigClassHandler<?> config) {
        for (ConfigField<?> field : config.fields()) {
            read(buf, field.access());
        }
    }

    private record NetCodec<T>(Function<PacketByteBuf, T> read, BiConsumer<PacketByteBuf, T> write) {}
}
