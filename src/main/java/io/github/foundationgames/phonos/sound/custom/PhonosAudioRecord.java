package io.github.foundationgames.phonos.sound.custom;

import io.github.foundationgames.phonos.sound.nbs.NBSBuilder;
import io.github.foundationgames.phonos.sound.nbs.NBSRecord;
import io.github.foundationgames.phonos.sound.stream.AudioDataQueue;
import io.github.foundationgames.phonos.util.PhonosUtil;
import io.github.foundationgames.phonos.world.sound.data.SoundData;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sound.SoundCategory;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public interface PhonosAudioRecord<T extends PhonosAudioRecord<T>> {
    int getPlayTicks();
    SoundData startPlaying(long emitterId, long streamId, SoundCategory category, float volume, float pitch, MinecraftServer server);
    void stopPlaying(long streamId, MinecraftServer server);

    T copy();
    int getDataSize();
    void write(OutputStream out) throws IOException;

    @ApiStatus.Internal
    static void writeHeader(OutputStream out, FileType fileType) throws IOException {
        PhonosUtil.writeInt(out, 0);
        out.write(fileType.id);
    }

    /**
     * Reads a PhonosAudioRecord from the given InputStream to a .phonosaud file.
     * The file format is as follows:
     * For legacy AudioDataQueue:
     * - 4 bytes: sample rate
     * - ADQ data
     * <br>
     * For modern format:
     * - 4 bytes: 0 (to distinguish)
     * - 1 byte: file type: 0 = ADQ, 1 = NBS
     * - n bytes: format-specific data
     */
    static @NotNull PhonosAudioRecord<?> read(InputStream in) throws IOException {
        var buffered = new BufferedInputStream(in, 16);
        buffered.mark(16);
        int sampleRate = PhonosUtil.readInt(buffered);

        if (sampleRate == 0) { // modern format
            int fileTypeId = buffered.read();
            FileType fileType = FileType.fromId(fileTypeId);
            if (fileType == null) {
                throw new IOException("Unknown file type: " + fileTypeId);
            }
            return switch (fileType) {
                case ADQ -> AudioDataQueue.read(buffered, ByteBuffer::allocate);
                case NBS -> NBSRecord.read(buffered);
            };
        } else {
            buffered.reset();
            return AudioDataQueue.read(buffered, ByteBuffer::allocate);
        }
    }

    enum FileType {
        ADQ(0, PhonosAudioRecordBuilder.Factory.cast(AudioDataQueue::new)),
        NBS(1, PhonosAudioRecordBuilder.Factory.cast(NBSBuilder::new));

        public void writeBuf(PacketByteBuf buf) {
            buf.writeVarInt(id);
        }

        public static FileType readBuf(PacketByteBuf buf) {
            return fromId(buf.readVarInt());
        }

        public final int id;
        private final PhonosAudioRecordBuilder.Factory<?> factory;

        FileType(int id, PhonosAudioRecordBuilder.Factory<?> factory) {
            this.id = id;
            this.factory = factory;
        }

        public PhonosAudioRecordBuilder<?> createBuilder(int initData) {
            return factory.create(initData);
        }

        public static FileType fromId(int id) {
            for (var type : values()) {
                if (type.id == id) {
                    return type;
                }
            }
            return null;
        }
    }
}
