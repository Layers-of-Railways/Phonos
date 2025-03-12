package io.github.foundationgames.phonos.satellite_radio;

import io.github.foundationgames.phonos.Phonos;
import io.github.foundationgames.phonos.datapack.SatelliteMigrations;
import io.github.foundationgames.phonos.sound.emitter.SoundEmitter;
import io.github.foundationgames.phonos.sound.emitter.SoundEmitterStorage;
import io.github.foundationgames.phonos.sound.emitter.SoundSource;
import io.github.foundationgames.phonos.util.UniqueId;
import io.github.foundationgames.phonos.world.sound.entity.HeadsetSoundSource;
import it.unimi.dsi.fastutil.longs.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import java.util.function.Supplier;

public class SatelliteRadioStorage {
    private static SatelliteRadioStorage CLIENT;
    private static final Map<RegistryKey<World>, SatelliteRadioStorage> SERVER = new HashMap<>();

    private static final SatelliteRadioStorage INVALID = new SatelliteRadioStorage(null) {};

    private final Map<String, Channel> channels;
    private final World world;

    private SatelliteRadioStorage(World world) {
        channels = new HashMap<>();
        this.world = world;
    }

    public void gc() {
        var toRemove = new ArrayList<String>();
        for (var entry : channels.entrySet()) {
            if (entry.getValue().receivingEmitters().isEmpty() && entry.getValue().receivingSources().isEmpty() && !entry.getValue().isForcedAlive()) {
                toRemove.add(entry.getKey());
            }
            entry.getValue().decrementKeepAlive();
        }
        toRemove.forEach(this::removeChannel);
    }

    public void keepAlive(String channel) {
        getOrCreateChannel(channel).keepAlive();
    }

    public LongList getReceivingEmitters(String channel) {
        return getOrCreateChannel(channel).receivingEmitters();
    }

    public <E extends SoundEmitter & SatelliteRadioDevice.Receiver> void addReceivingEmitter(String channel, E receiver) {
        var list = getReceivingEmitters(channel);
        long id = receiver.emitterId();

        if (!list.contains(id)) {
            list.add(id);
        }
    }

    public void removeReceivingEmitter(String channel, long emitterId) {
        getReceivingEmitters(channel).rem(emitterId);
    }

    public List<SoundSource> getReceivingSources(String channel) {
        return getOrCreateChannel(channel).receivingSources();
    }

    public void addReceivingSource(String channel, SoundSource receiver) {
        var list = getReceivingSources(channel);

        if (!list.contains(receiver)) {
            list.add(receiver);
        }
    }

    public void removeReceivingSource(String channel, SoundSource receiver) {
        getReceivingSources(channel).remove(receiver);
    }

    public static SatelliteRadioStorage getInstance(World world) {
        if (world == null) {
            return INVALID;
        }

        if (world.isClient) {
            if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
                if (CLIENT == null) {
                    CLIENT = new SatelliteRadioStorage(world);
                }

                return CLIENT;
            }
        }

        if (world instanceof ServerWorld sWorld) {
            return SERVER.computeIfAbsent(sWorld.getRegistryKey(), w -> new SatelliteRadioStorage(world));
        }

        return INVALID;
    }

    public static Supplier<SatelliteRadioStorage> getInstanceSupplier(World world) {
        if (world == null) {
            return () -> INVALID;
        }

        if (world.isClient) {
            if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
                return () -> {
                    if (CLIENT == null) {
                        CLIENT = new SatelliteRadioStorage(world);
                    }

                    return CLIENT;
                };
            }
        }

        if (world instanceof ServerWorld sWorld) {
            return () -> SERVER.computeIfAbsent(sWorld.getRegistryKey(), w -> new SatelliteRadioStorage(sWorld));
        }

        return () -> INVALID;
    }

    public static void serverReset() {
        SERVER.clear();
    }

    public static void clientReset() {
        CLIENT = null;
    }

    private Channel getOrCreateChannel(String channel) {
        return channels.computeIfAbsent(channel, k -> {
            SoundEmitterStorage.getInstance(world).addEmitter(new SatelliteRadioEmitter(channel));
            return new Channel(k, new LongArrayList(), new ArrayList<>());
        });
    }

    private void removeChannel(String channel) {
        var chan = channels.remove(channel);
        if (chan != null) {
            Phonos.LOG.info("Removing satellite radio channel: {}", channel);
            SoundEmitterStorage.getInstance(world).removeEmitter(chan.emitterId());
        }
    }

    public boolean hasChannelEmitter(long emitterId) {
        return channels.values().stream().anyMatch(ch -> ch.emitterId() == emitterId);
    }

    public static final class Channel {
        private final String channel;
        private final LongList receivingEmitters;
        private final List<SoundSource> receivingSources;
        private int keepAliveCounter;
        private final long emitterId;

        public Channel(String channel, LongList receivingEmitters, List<SoundSource> receivingSources) {
            this.channel = channel;
            this.receivingEmitters = receivingEmitters;
            this.receivingSources = receivingSources;
            this.keepAliveCounter = 0;
            this.emitterId = UniqueId.ofSatelliteChannel(channel);
        }

        public String channel() {
            return channel;
        }

        public LongList receivingEmitters() {
            return receivingEmitters;
        }

        public List<SoundSource> receivingSources() {
            return receivingSources;
        }

        public void keepAlive() {
            keepAliveCounter = 2;
        }

        public boolean isForcedAlive() {
            return keepAliveCounter > 0;
        }

        public void decrementKeepAlive() {
            if (keepAliveCounter > 0) {
                keepAliveCounter--;
            }
        }

        public long emitterId() {
            return emitterId;
        }

        @Override
        public String toString() {
            return "Channel[" +
                "channel=" + channel + ", " +
                "receivingEmitters=" + receivingEmitters + ", " +
                "receivingSources=" + receivingSources + ", " +
                "keepAliveCounter=" + keepAliveCounter + ", " +
                "emitterId=" + emitterId + ']';
        }
    }

    public static @Nullable String convertLegacyChannel(String channel) {
        if (channel.startsWith("N")) {
            try {
                int num = Integer.parseInt(channel.substring(1));
                return SatelliteMigrations.getMigration(num);
            } catch (NumberFormatException ignored) {}
        }

        return null;
    }

    public class SatelliteRadioEmitter implements SoundEmitter {
        public final String channel;

        private final long emitterId;

        public SatelliteRadioEmitter(String channel) {
            this.channel = channel;
            this.emitterId = UniqueId.ofSatelliteChannel(channel);
        }

        @Override
        public long emitterId() {
            return this.emitterId;
        }

        @Override
        public void forEachSource(Consumer<SoundSource> action) {
            var satellite = SatelliteRadioStorage.this;

            for (var source : satellite.getReceivingSources(channel)) {
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
            var satellite = SatelliteRadioStorage.this;

            for (long rec : satellite.getReceivingEmitters(this.channel)) if (emitters.isLoaded(rec)) {
                action.accept(rec);
            }
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + " {channel='%s' %s}".formatted(channel, UniqueId.debugNameOf(emitterId()));
        }
    }
}
