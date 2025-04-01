package io.github.foundationgames.phonos.sound.nbs;

import cz.koca2000.nbs4j.NBSVersion;
import cz.koca2000.nbs4j.Song;
import cz.koca2000.nbs4j.SongMetadata;
import io.github.foundationgames.phonos.sound.custom.PhonosAudioRecord;
import io.github.foundationgames.phonos.sound.nbs.stream.NBSInitData;
import io.github.foundationgames.phonos.sound.nbs.stream.ServerOutgoingNBSStreamHandler;
import io.github.foundationgames.phonos.world.sound.data.SoundData;
import io.github.foundationgames.phonos.world.sound.data.StreamSoundData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sound.SoundCategory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class NBSRecord implements PhonosAudioRecord<NBSRecord> {
    private final int totalSize;
    public final Song song;

    public NBSRecord(int totalSize, Song song) {
        this.totalSize = totalSize;
        this.song = song.freezeSong();
    }

    @Override
    public int getPlayTicks() {
        SongMetadata meta = song.getMetadata();

        int songTicks = song.getSongLength();
        if (meta.isLoop()) {
            int loopCount = meta.getLoopMaxCount();

            if (loopCount == 0) {
                songTicks = Integer.MAX_VALUE;
            } else {
                int loopTicks = songTicks - meta.getLoopStartTick();
                int interval = Math.max(2, Math.min(meta.getTimeSignature() & 0xff, 8)) * 4;
                int intervalPadding = interval - (interval % songTicks);
                int ticksPerLoop = loopTicks + intervalPadding;

                songTicks += loopCount * ticksPerLoop;
            }
        }
        return (int) Math.ceil(songTicks / song.getTempo(0) * 20);
    }

    public NBSInitData getInitData() {
        return new NBSInitData(song);
    }

    @Override
    public SoundData startPlaying(long emitterId, long streamId, SoundCategory category, float volume, float pitch, MinecraftServer server) {
        ServerOutgoingNBSStreamHandler.startStream(streamId, this, server);
        return StreamSoundData.createNBS(emitterId, streamId, category, volume, pitch);
    }

    @Override
    public void stopPlaying(long streamId, MinecraftServer server) {
        ServerOutgoingNBSStreamHandler.endStream(streamId, server);
    }

    @Override
    public NBSRecord copy() {
        return new NBSRecord(totalSize, song);
    }

    @Override
    public int getDataSize() {
        return totalSize;
    }

    private static void writeInt(OutputStream out, int value) throws IOException {
        out.write(value >> 24);
        out.write(value >> 16);
        out.write(value >> 8);
        out.write(value);
    }

    private static int readInt(InputStream in) throws IOException {
        return (in.read() << 24) | (in.read() << 16) | (in.read() << 8) | in.read();
    }

    @Override
    public void write(OutputStream out) throws IOException {
        PhonosAudioRecord.writeHeader(out, FileType.NBS);
        writeInt(out, totalSize);
        song.save(NBSVersion.LATEST, out);
    }

    public static NBSRecord read(InputStream in) throws IOException {
        int totalSize = readInt(in);
        Song song = Song.fromStream(in);
        return new NBSRecord(totalSize, song);
    }
}
