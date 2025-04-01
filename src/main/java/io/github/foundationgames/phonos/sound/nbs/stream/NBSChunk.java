package io.github.foundationgames.phonos.sound.nbs.stream;

import cz.koca2000.nbs4j.Note;
import cz.koca2000.nbs4j.Song;
import io.github.foundationgames.phonos.Phonos;
import io.github.foundationgames.phonos.util.PhonosPacketCodecs;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

import java.util.HashMap;

public record NBSChunk(HashMap<Integer, Note>[] layerUpdates) {
    private static Note newNote(int instrument, boolean isCustomInstrument, int key, int pitch, int panning, byte volume) {
        var note = new Note();
        note.setInstrument(instrument, isCustomInstrument);
        note.setKey(key);
        note.setPitch(pitch);
        note.setPanning(panning);
        note.setVolume(volume & 0xFF);
        return note;
    }

    private static final PacketCodec<PacketByteBuf, Note> NOTE_PACKET_CODEC = PacketCodec.tuple(
        PacketCodecs.VAR_INT, Note::getInstrument,
        PacketCodecs.BOOL,    Note::isCustomInstrument,
        PacketCodecs.VAR_INT, Note::getKey,
        PacketCodecs.VAR_INT, Note::getPitch,
        PacketCodecs.VAR_INT, Note::getPanning,
        PacketCodecs.BYTE, Note::getVolume,
        NBSChunk::newNote
    );

    private static final PacketCodec<PacketByteBuf, HashMap<Integer, Note>> NOTE_MAP_PACKET_CODEC = PacketCodecs.map(
        HashMap::new,
        PacketCodecs.VAR_INT,
        NOTE_PACKET_CODEC
    );

    public static final PacketCodec<PacketByteBuf, NBSChunk> PACKET_CODEC = PhonosPacketCodecs.array(
        HashMap.class,
        NOTE_MAP_PACKET_CODEC,
        255
    ).xmap(
        NBSChunk::new,
        NBSChunk::layerUpdates
    );

    @SuppressWarnings("unchecked")
    public NBSChunk(int layerCount) {
        this(new HashMap[layerCount]);
        for (int i = 0; i < layerCount; i++) {
            layerUpdates[i] = new HashMap<>();
        }
    }

    public void apply(Song song) {
        int minTick = Integer.MAX_VALUE;
        int maxTick = Integer.MIN_VALUE;
        for (int layer = 0; layer < layerUpdates.length; layer++) {
            var updates = layerUpdates[layer];
            for (var entry : updates.entrySet()) {
                song.setNote(entry.getKey(), layer, entry.getValue());
                if (entry.getKey() < minTick) {
                    minTick = entry.getKey();
                }
                if (entry.getKey() > maxTick) {
                    maxTick = entry.getKey();
                }
            }
        }
        Phonos.LOG.info("Applied NBS chunk [{} - {}] to song", minTick, maxTick);
    }
}
