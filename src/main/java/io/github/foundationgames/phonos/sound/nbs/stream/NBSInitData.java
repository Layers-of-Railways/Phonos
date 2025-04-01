package io.github.foundationgames.phonos.sound.nbs.stream;

import cz.koca2000.nbs4j.CustomInstrument;
import cz.koca2000.nbs4j.Layer;
import cz.koca2000.nbs4j.Song;
import io.github.foundationgames.phonos.mixin.nbs.SongAccessor;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

import java.util.ArrayList;
import java.util.List;

public record NBSInitData(int length, List<LayerInit> layers, float tempo, int nonCustomInstrumentsCount, List<CustomInstrument> customInstruments, Metadata metadata) {

    private record LayerInit(int volume) {
        private static final PacketCodec<PacketByteBuf, LayerInit> PACKET_CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT,
            LayerInit::volume,
            LayerInit::new
        );

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
        private static final PacketCodec<PacketByteBuf, Metadata> PACKET_CODEC = PacketCodec.tuple(
            PacketCodecs.BOOL,
            Metadata::doLoop,
            PacketCodecs.BYTE,
            Metadata::loopCount,
            PacketCodecs.BYTE,
            Metadata::timeSignature,
            Metadata::new
        );

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
    private static final PacketCodec<PacketByteBuf, CustomInstrument> CUSTOM_INSTRUMENT_PACKET_CODEC = PacketCodec.tuple(
        PacketCodecs.STRING,
        CustomInstrument::getName,
        PacketCodecs.STRING,
        CustomInstrument::getFileName,
        PacketCodecs.VAR_INT,
        CustomInstrument::getKey,
        NBSInitData::makeCustomInstrument
    );

    public static final PacketCodec<PacketByteBuf, NBSInitData> PACKET_CODEC = PacketCodec.tuple(
        PacketCodecs.VAR_INT,
        NBSInitData::length,
        PacketCodecs.collection(ArrayList::new, LayerInit.PACKET_CODEC),
        NBSInitData::layers,
        PacketCodecs.FLOAT,
        NBSInitData::tempo,
        PacketCodecs.VAR_INT,
        NBSInitData::nonCustomInstrumentsCount,
        PacketCodecs.collection(ArrayList::new, CUSTOM_INSTRUMENT_PACKET_CODEC),
        NBSInitData::customInstruments,
        Metadata.PACKET_CODEC,
        NBSInitData::metadata,
        NBSInitData::new
    );

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
