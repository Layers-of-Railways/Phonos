package io.github.foundationgames.phonos.radio;

import io.github.foundationgames.phonos.sound.emitter.SoundEmitter;
import io.github.foundationgames.phonos.sound.emitter.SoundEmitterStorage;
import io.github.foundationgames.phonos.sound.emitter.SoundSource;
import io.github.foundationgames.phonos.util.UniqueId;
import io.github.foundationgames.phonos.world.sound.entity.HeadsetSoundSource;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

public class RadioStorage {
    public static final int RADIO_CHANNEL_COUNT = 30;
    public static final int SATELLITE_CHANNEL_COUNT = 30;

    public static final int CHANNEL_COUNT = RADIO_CHANNEL_COUNT + SATELLITE_CHANNEL_COUNT;

    public static final LongList RADIO_EMITTERS = new LongArrayList();

    private static RadioStorage CLIENT;
    private static final Map<RegistryKey<World>, RadioStorage> SERVER = new HashMap<>();

    private static final RadioStorage INVALID = new RadioStorage() {};

    private final Channel[] channels;

    public static int toSatelliteBand(int satelliteChannel) {
        return satelliteChannel + RADIO_CHANNEL_COUNT;
    }

    public RadioStorage() {
        channels = new Channel[CHANNEL_COUNT];
        for (int i = 0; i < CHANNEL_COUNT; i++) {
            channels[i] = new Channel(i, new LongArrayList(), new ArrayList<>());
        }
    }

    public LongList getReceivingEmitters(int channel) {
        return channels[channel].receivingEmitters();
    }

    public <E extends SoundEmitter & RadioDevice.Receiver> void addReceivingEmitter(int channel, E receiver) {
        var list = getReceivingEmitters(channel);
        long id = receiver.emitterId();

        if (!list.contains(id)) {
            list.add(id);
        }
    }

    public void removeReceivingEmitter(int channel, long emitterId) {
        getReceivingEmitters(channel).rem(emitterId);
    }

    public List<SoundSource> getReceivingSources(int channel) {
        return channels[channel].receivingSources();
    }

    public void addReceivingSource(int channel, SoundSource receiver) {
        var list = getReceivingSources(channel);

        if (!list.contains(receiver)) {
            list.add(receiver);
        }
    }

    public void removeReceivingSource(int channel, SoundSource receiver) {
        getReceivingSources(channel).remove(receiver);
    }

    public static RadioStorage getInstance(World world) {
        if (world.isClient()) {
            if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
                if (CLIENT == null) {
                    CLIENT = new RadioStorage();
                }

                return CLIENT;
            }
        }

        if (world instanceof ServerWorld sWorld) {
            return SERVER.computeIfAbsent(sWorld.getRegistryKey(), w -> new RadioStorage());
        }

        return INVALID;
    }

    public static void serverReset() {
        SERVER.clear();
    }

    public static void clientReset() {
        CLIENT = null;
    }

    public record Channel(int number, LongList receivingEmitters, List<SoundSource> receivingSources) {}

    public static class RadioEmitter implements SoundEmitter {
        public final int channel;

        private final World world;
        private final long emitterId;

        public RadioEmitter(World world, int channel) {
            this.world = world;
            this.channel = channel;
            this.emitterId = UniqueId.ofRadioChannel(channel);
        }

        @Override
        public long emitterId() {
            return this.emitterId;
        }

        public boolean isRadio() {
            return channel < RADIO_CHANNEL_COUNT;
        }

        public boolean isSatellite() {
            return channel >= RADIO_CHANNEL_COUNT;
        }

        @Override
        public void forEachSource(Consumer<SoundSource> action) {
            var radio = RadioStorage.getInstance(world);

            for (var source : radio.getReceivingSources(this.channel)) {
                action.accept(source);
            }

            // TODO: make this less bad
            if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
                var headset = HeadsetSoundSource.INSTANCE;
                if (headset.parentEmitters.contains(this.emitterId) && headset.shouldAccept(action)) {
                    action.accept(headset);
                }
            }
        }

        @Override
        public void forEachChild(LongConsumer action) {
            var emitters = SoundEmitterStorage.getInstance(world);
            var radio = RadioStorage.getInstance(world);

            for (long rec : radio.getReceivingEmitters(this.channel)) if (emitters.isLoaded(rec)) {
                if (action instanceof RadioLongConsumer rlc && emitters.getEmitter(rec) instanceof RadioDevice radioDevice) {
                    rlc.accept(rec, radioDevice.getMetadata());
                } else {
                    action.accept(rec);
                }
            }
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + " {channel#%s %s}".formatted(channel, UniqueId.debugNameOf(emitterId()));
        }
    }

    public static void init() {
        for (int i = 0; i < CHANNEL_COUNT; i++) {
            final int channel = i;
            SoundEmitterStorage.DEFAULT_EMITTERS.add(w -> new RadioEmitter(w, channel));
            RADIO_EMITTERS.add(UniqueId.ofRadioChannel(channel));
        }
    }
}
