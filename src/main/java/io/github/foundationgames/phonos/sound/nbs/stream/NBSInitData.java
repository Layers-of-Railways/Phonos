package io.github.foundationgames.phonos.sound.nbs.stream;

import cz.koca2000.nbs4j.CustomInstrument;
import cz.koca2000.nbs4j.Layer;
import cz.koca2000.nbs4j.Song;
import io.github.foundationgames.phonos.mixin.nbs.SongAccessor;
import net.minecraft.network.PacketByteBuf;

import java.util.ArrayList;
import java.util.List;

public record NBSInitData(int length, List<LayerInit> layers, float tempo, int nonCustomInstrumentsCount, List<CustomInstrument> customInstruments, Metadata metadata) {

    private record LayerInit(int volume) {
        private void writeBuf(PacketByteBuf buf) {
            buf.writeVarInt(volume);
        }

        private static LayerInit readBuf(PacketByteBuf buf) {
            return new LayerInit(buf.readVarInt());
        }

        private LayerInit(Layer layer) {
            this(layer.getVolume());
        }

        private Layer makeLayer() {
            Layer layer = new Layer();
            layer.setVolume(volume);
            return layer;
        }
    }

    private record Metadata(boolean doLoop, byte loopCount, byte timeSignature) {
        private void writeBuf(PacketByteBuf buf) {
            buf.writeBoolean(doLoop);
            buf.writeByte(loopCount);
            buf.writeByte(timeSignature);
        }

        private static Metadata readBuf(PacketByteBuf buf) {
            return new Metadata(buf.readBoolean(), buf.readByte(), buf.readByte());
        }

        private Metadata(Song song) {
            this(song.getMetadata().isLoop(), song.getMetadata().getLoopMaxCount(), song.getMetadata().getTimeSignature());
        }

        private void apply(Song song) {
            song.getMetadata().setLoop(doLoop);
            song.getMetadata().setLoopMaxCount(loopCount);
            song.getMetadata().setTimeSignature(timeSignature);
        }
    }

    private static CustomInstrument makeCustomInstrument(String name, String fileName, int key) {
        var ci = new CustomInstrument();
        ci.setName(name);
        ci.setFileName(fileName);
        ci.setKey(key);
        return ci;
    }

    private static void writeCustomInstrument(PacketByteBuf buf, CustomInstrument ci) {
        buf.writeString(ci.getName());
        buf.writeString(ci.getFileName());
        buf.writeVarInt(ci.getKey());
    }

    private static CustomInstrument readCustomInstrument(PacketByteBuf buf) {
        return makeCustomInstrument(buf.readString(32767), buf.readString(32767), buf.readVarInt());
    }

    public void toPacket(PacketByteBuf buf) {
        buf.writeVarInt(length);
        buf.writeVarInt(layers.size());
        for (var layer : layers) {
            layer.writeBuf(buf);
        }
        buf.writeFloat(tempo);
        buf.writeVarInt(nonCustomInstrumentsCount);
        buf.writeVarInt(customInstruments.size());
        for (var ci : customInstruments) {
            writeCustomInstrument(buf, ci);
        }
        metadata.writeBuf(buf);
    }

    public static NBSInitData fromPacket(PacketByteBuf buf) {
        int length = buf.readVarInt();
        int layersCount = buf.readVarInt();
        List<LayerInit> layers = new ArrayList<>(layersCount);
        for (int i = 0; i < layersCount; i++) {
            layers.add(LayerInit.readBuf(buf));
        }
        float tempo = buf.readFloat();
        int nonCustomInstrumentsCount = buf.readVarInt();
        int customInstrumentsCount = buf.readVarInt();
        List<CustomInstrument> customInstruments = new ArrayList<>(customInstrumentsCount);
        for (int i = 0; i < customInstrumentsCount; i++) {
            customInstruments.add(readCustomInstrument(buf));
        }
        Metadata metadata = Metadata.readBuf(buf);
        return new NBSInitData(length, layers, tempo, nonCustomInstrumentsCount, customInstruments, metadata);
    }

    private static List<LayerInit> getLayerInits(Song song) {
        List<LayerInit> layerInits = new ArrayList<>();
        for (int i = 0; i < song.getLayersCount(); i++) {
            layerInits.add(new LayerInit(song.getLayer(i)));
        }
        return layerInits;
    }

    private static List<CustomInstrument> getAllCustomInstruments(Song song) {
        List<CustomInstrument> customInstruments = new ArrayList<>();
        for (int i = 0; i < song.getCustomInstrumentsCount(); i++) {
            customInstruments.add(song.getCustomInstrument(i));
        }
        return customInstruments;
    }

    public NBSInitData(Song song) {
        this(song.getSongLength(), getLayerInits(song), song.getTempo(0), song.getNonCustomInstrumentsCount(), getAllCustomInstruments(song), new Metadata(song));
    }

    public Song makeSong() {
        var song = new Song();
        song.setLength(length);
        for (LayerInit layer : layers) {
            song.addLayer(layer.makeLayer());
        }
        song.setTempoChange(-1, tempo);
        ((SongAccessor) song).callIncreaseNonCustomInstrumentsCountTo(nonCustomInstrumentsCount);
        for (CustomInstrument customInstrument : customInstruments) {
            song.addCustomInstrument(customInstrument);
        }
        metadata.apply(song);
        return song;
    }
}
