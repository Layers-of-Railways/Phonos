package io.github.foundationgames.phonos.sound.nbs.stream;

import cz.koca2000.nbs4j.Note;
import cz.koca2000.nbs4j.Song;
import io.github.foundationgames.phonos.Phonos;
import net.minecraft.network.PacketByteBuf;

import java.util.HashMap;

public record NBSChunk(HashMap<Integer, Note>[] layerUpdates, boolean isLast) {
    private static Note newNote(int instrument, boolean isCustomInstrument, int key, int pitch, int panning, byte volume) {
        var note = new Note();
        note.setInstrument(instrument, isCustomInstrument);
        note.setKey(key);
        note.setPitch(pitch);
        note.setPanning(panning);
        note.setVolume(volume & 0xFF);
        return note;
    }

    private static void writeNote(PacketByteBuf buf, Note note) {
        buf.writeVarInt(note.getInstrument());
        buf.writeBoolean(note.isCustomInstrument());
        buf.writeVarInt(note.getKey());
        buf.writeVarInt(note.getPitch());
        buf.writeVarInt(note.getPanning());
        buf.writeByte(note.getVolume());
    }

    private static Note readNote(PacketByteBuf buf) {
        int instrument = buf.readVarInt();
        boolean isCustomInstrument = buf.readBoolean();
        int key = buf.readVarInt();
        int pitch = buf.readVarInt();
        int panning = buf.readVarInt();
        byte volume = buf.readByte();
        return newNote(instrument, isCustomInstrument, key, pitch, panning, volume);
    }

    private static void writeNoteMap(PacketByteBuf buf, HashMap<Integer, Note> map) {
        buf.writeVarInt(map.size());
        for (var entry : map.entrySet()) {
            buf.writeVarInt(entry.getKey());
            writeNote(buf, entry.getValue());
        }
    }

    private static HashMap<Integer, Note> readNoteMap(PacketByteBuf buf) {
        int size = buf.readVarInt();
        var map = new HashMap<Integer, Note>(size);
        for (int i = 0; i < size; i++) {
            int tick = buf.readVarInt();
            Note note = readNote(buf);
            map.put(tick, note);
        }
        return map;
    }

    public void toPacket(PacketByteBuf buf) {
        buf.writeVarInt(layerUpdates.length);
        for (var map : layerUpdates) {
            writeNoteMap(buf, map);
        }
        buf.writeBoolean(isLast);
    }

    @SuppressWarnings("unchecked")
    public static NBSChunk fromPacket(PacketByteBuf buf) {
        int layerCount = buf.readVarInt();
        var layerUpdates = new HashMap[layerCount];
        for (int i = 0; i < layerCount; i++) {
            layerUpdates[i] = readNoteMap(buf);
        }
        boolean isLast = buf.readBoolean();
        return new NBSChunk(layerUpdates, isLast);
    }

    @SuppressWarnings("unchecked")
    public NBSChunk(int layerCount) {
        this(new HashMap[layerCount], false);
        for (int i = 0; i < layerCount; i++) {
            layerUpdates[i] = new HashMap<>();
        }
    }

    public NBSChunk setLast() {
        return new NBSChunk(layerUpdates, true);
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
