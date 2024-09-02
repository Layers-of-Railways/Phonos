package io.github.foundationgames.phonos.sound.emitter;

import io.github.foundationgames.phonos.Phonos;
import io.github.foundationgames.phonos.radio.RadioDevice;
import io.github.foundationgames.phonos.radio.RadioLongConsumer;
import io.github.foundationgames.phonos.radio.RadioMetadata;
import io.github.foundationgames.phonos.radio.RadioStorage;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class SoundEmitterTree {
    public final long rootId;
    private final ArrayList<Level> levels;
    private final Int2ObjectOpenHashMap<HashSet<RadioMetadata>> radioSources;

    public SoundEmitterTree(long rootId) {
        this(rootId, new ArrayList<>(), new Int2ObjectOpenHashMap<>());
        levels.add(new Level(new LongArrayList(new long[] {rootId}), new LongArrayList()));
    }

    private SoundEmitterTree(long rootId, ArrayList<Level> levels, Int2ObjectOpenHashMap<HashSet<RadioMetadata>> radioSources) {
        this.rootId = rootId;
        this.levels = levels;
        this.radioSources = radioSources;
    }

    public boolean contains(long value, int upUntil) {
        upUntil = Math.min(this.levels.size() - 1, upUntil);

        for (int i = 0; i < upUntil; i++) {
            var level = this.levels.get(i);
            if (level.active.contains(value) || level.inactive.contains(value)) {
                return true;
            }
        }

        return false;
    }

    // Updates the tree on the server and provides a list of changes to be sent to the client
    // More accurate than the updateClient() method, as far as what the server is aware of (loaded chunks)
    public Delta updateServer(World world) {
        var emitters = SoundEmitterStorage.getInstance(world);
        var delta = new Delta(this.rootId, new Int2ObjectOpenHashMap<>(), new RadioSourceChangeList(new Int2ObjectOpenHashMap<>(), new Int2ObjectOpenHashMap<>()));
        
        var currentCopy = new Int2ObjectOpenHashMap<>(radioSources);
        for (int key : currentCopy.keySet()) {
            radioSources.put(key, new HashSet<>(currentCopy.get(key)));
        }
        var radioChanges = new RadioSourceChangeList(new Int2ObjectOpenHashMap<>(), currentCopy);

        int index = 0;

        while (index < this.levels.size() && !this.levels.get(index).empty()) {
            if (index + 1 == this.levels.size()) {
                this.levels.add(new Level(new LongArrayList(), new LongArrayList()));
            }

            var level = this.levels.get(index);
            var nextLevel = this.levels.get(index + 1);

            var nextLevelChanges = new ChangeList(new LongArrayList(), new LongArrayList(nextLevel.active()));

            for (long emId : level.active()) {
                if (emitters.isLoaded(emId)) {
                    var emitter = emitters.getEmitter(emId);
                    if (emitter instanceof RadioStorage.RadioEmitter radioEmitter && radioEmitter.isRadio())
                        continue;

                    if (emitter instanceof RadioDevice.Transmitter radioTransmitter) {
                        int channel = radioTransmitter.getChannel();
                        RadioMetadata metadata = radioTransmitter.getMetadata();

                        radioChanges.cancelRemove(channel, metadata);
                        radioChanges.add(channel, metadata);
                    }

                    final int searchUntil = index; // fixme should this be index + 1?
                    RadioLongConsumer[] consumerRef = new RadioLongConsumer[1];
                    RadioLongConsumer consumer = (child, metadata) -> {
                        if (emitters.isLoaded(child) && emitters.getEmitter(child) instanceof RadioStorage.RadioEmitter radioEmitter && radioEmitter.isRadio()) {
                            radioEmitter.forEachChild(consumerRef[0]);
                        }

                        if (this.contains(child, searchUntil)) {
                            return;
                        }

                        if (metadata != null && emitter instanceof RadioDevice radioDevice) {
                            RadioMetadata emitterMetadata = radioDevice.getMetadata();
                            if (!emitterMetadata.shouldTransmitTo(metadata)) {
                                return;
                            }
                        }

                        nextLevelChanges.remove().rem(child);

                        if (!nextLevel.active().contains(child)) {
                            nextLevelChanges.add().add(child);
                        }
                    };
                    consumerRef[0] = consumer;
                    emitter.forEachChild(consumer);
                }
            }

            if (!nextLevelChanges.add().isEmpty() || !nextLevelChanges.remove().isEmpty()) {
                nextLevelChanges.apply(nextLevel);
                delta.deltas().put(index + 1, new Level(
                        new LongArrayList(nextLevel.active()),
                        new LongArrayList(nextLevel.inactive())
                ));
            }

            index++;
        }

        radioChanges.apply(this);
        delta.radioDeltas.setFrom(radioChanges);

        return delta;
    }

    // Updates the tree on the client side
    // Not entirely accurate, but enough so that most modifications of sound networks will have
    // immediate effects regardless of server speed/latency
    public void updateClient(World world) {
        var emitters = SoundEmitterStorage.getInstance(world);
        radioSources.clear();

        int index = 0;

        while (index < this.levels.size() && !this.levels.get(index).empty()) {
            if (index + 1 == this.levels.size()) {
                this.levels.add(new Level(new LongArrayList(), new LongArrayList()));
            }

            var level = this.levels.get(index);
            var nextLevel = this.levels.get(index + 1);

            nextLevel.inactive().addAll(nextLevel.active());
            nextLevel.active().clear();
            for (long l : nextLevel.inactive()) if (RadioStorage.RADIO_EMITTERS.contains(l)) {
                nextLevel.active().add(l);
            }
            nextLevel.inactive().removeAll(nextLevel.active());

            for (long emId : level.active()) {
                if (emitters.isLoaded(emId)) {
                    var emitter = emitters.getEmitter(emId);
                    if (emitter instanceof RadioStorage.RadioEmitter radioEmitter && radioEmitter.isRadio())
                        continue;

                    if (emitter instanceof RadioDevice.Transmitter radioTransmitter) {
                        int channel = radioTransmitter.getChannel();
                        RadioMetadata metadata = radioTransmitter.getMetadata();

                        radioSources.computeIfAbsent(channel, k -> new HashSet<>()).add(metadata);
                    }

                    final int searchUntil = index;
                    RadioLongConsumer[] consumerRef = new RadioLongConsumer[1];
                    RadioLongConsumer consumer = (child, metadata) -> {
                        if (emitters.isLoaded(child) && emitters.getEmitter(child) instanceof RadioStorage.RadioEmitter radioEmitter && radioEmitter.isRadio()) {
                            radioEmitter.forEachChild(consumerRef[0]);
                        }

                        if (this.contains(child, searchUntil)) {
                            return;
                        }

                        if (metadata != null && emitter instanceof RadioDevice radioDevice) {
                            RadioMetadata emitterMetadata = radioDevice.getMetadata();
                            if (!emitterMetadata.shouldTransmitTo(metadata)) {
                                return;
                            }
                        }

                        nextLevel.inactive().rem(child);

                        if (!nextLevel.active().contains(child)) {
                            nextLevel.active().add(child);
                        }
                    };
                    consumerRef[0] = consumer;
                    emitter.forEachChild(consumer);
                }
            }

            index++;
        }

        if (index < this.levels.size() && this.levels.get(index).empty()) {
            while (index < this.levels.size()) {
                this.levels.remove(this.levels.size() - 1);
            }
        }
    }

    public class SmartSoundSourceConsumer implements Consumer<SoundSource> {
        private final Consumer<SoundSource> wrapped;
        private final int channel;

        public SmartSoundSourceConsumer(Consumer<SoundSource> wrapped, int channel) {
            this.wrapped = wrapped;
            this.channel = channel;
        }

        public boolean shouldAccept(RadioMetadata deviceMetadata) {
            if (!radioSources.containsKey(channel)) {
                return false;
            }
            for (var metadata : radioSources.get(channel)) {
                if (metadata.shouldTransmitTo(deviceMetadata)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void accept(SoundSource soundSource) {
            if (soundSource instanceof RadioDevice radioDevice) {
                if (radioDevice.getChannel() != channel) {
                    Phonos.LOG.error("RadioDevice channel mismatch");
                }
                if (shouldAccept(radioDevice.getMetadata())) {
                    wrapped.accept(soundSource);
                }
            } else {
                wrapped.accept(soundSource);
            }
        }
    }

    public void forEachSource(World world, Consumer<SoundSource> action) {
        var emitters = SoundEmitterStorage.getInstance(world);

        for (var level : this.levels)
            for (long em : level.active) {
            if (emitters.isLoaded(em)) {
                var emitter = emitters.getEmitter(em);
                emitter.forEachSource(emitter instanceof RadioStorage.RadioEmitter radioEmitter && radioEmitter.isRadio()
                    ? new SmartSoundSourceConsumer(action, radioEmitter.channel)
                    : action
                );
            }
        }
    }

    public record Level(LongArrayList active, LongArrayList inactive) {
        public boolean empty() {
            return active.isEmpty() && inactive.isEmpty();
        }

        public static void toPacket(PacketByteBuf buf, Level level) {
            buf.writeCollection(level.active, PacketByteBuf::writeLong);
            buf.writeCollection(level.inactive, PacketByteBuf::writeLong);
        }

        public static Level fromPacket(PacketByteBuf buf) {
            var active = buf.readCollection(LongArrayList::new, PacketByteBuf::readLong);
            var inactive = buf.readCollection(LongArrayList::new, PacketByteBuf::readLong);

            return new Level(active, inactive);
        }
    }

    public void debugLevels(BiConsumer<Integer, Long> action) {
        for (int i = 0; i < levels.size(); i++) {
            for (long l : levels.get(i).active) {
                action.accept(i, l);
            }
        }
    }

    public void toPacket(PacketByteBuf buf) {
        buf.writeLong(this.rootId);
        buf.writeCollection(this.levels, Level::toPacket);
        buf.writeMap(this.radioSources, PacketByteBuf::writeInt, (b, s) -> {
            b.writeCollection(s, RadioMetadata::write);
        });
    }

    public static SoundEmitterTree fromPacket(PacketByteBuf buf) {
        return new SoundEmitterTree(
            buf.readLong(),
            buf.readCollection(ArrayList::new, Level::fromPacket),
            buf.readMap(Int2ObjectOpenHashMap::new, PacketByteBuf::readInt,
                b -> b.readCollection(HashSet::new, RadioMetadata::new)
            )
        );
    }

    public record Delta(long rootId, Int2ObjectMap<Level> deltas, RadioSourceChangeList radioDeltas) {
        public static void toPacket(PacketByteBuf buf, Delta delta) {
            buf.writeLong(delta.rootId);
            buf.writeMap(delta.deltas, PacketByteBuf::writeInt, Level::toPacket);
            RadioSourceChangeList.toPacket(buf, delta.radioDeltas);
        }

        public static Delta fromPacket(PacketByteBuf buf) {
            var id = buf.readLong();
            var deltas = buf.readMap(Int2ObjectOpenHashMap::new, PacketByteBuf::readInt, Level::fromPacket);
            var radioDeltas = RadioSourceChangeList.fromPacket(buf);

            return new Delta(id, deltas, radioDeltas);
        }

        public boolean hasChanges() {
            return this.deltas.size() > 0;
        }

        public void apply(SoundEmitterTree tree) {
            for (var entry : this.deltas().int2ObjectEntrySet()) {
                int idx = entry.getIntKey();
                if (idx < tree.levels.size()) {
                    tree.levels.set(idx, entry.getValue());
                } else {
                    while (tree.levels.size() < idx - 1) {
                        tree.levels.add(new Level(new LongArrayList(), new LongArrayList()));
                    }
                    tree.levels.add(entry.getValue());
                }
            }
            radioDeltas.apply(tree);
        }
    }

    public record RadioSourceChangeList(Int2ObjectOpenHashMap<HashSet<RadioMetadata>> add, Int2ObjectOpenHashMap<HashSet<RadioMetadata>> remove) {
        public static void toPacket(PacketByteBuf buf, RadioSourceChangeList level) {
            buf.writeMap(level.add, PacketByteBuf::writeInt, (b, s) -> {
                b.writeCollection(s, RadioMetadata::write);
            });
            buf.writeMap(level.remove, PacketByteBuf::writeInt, (b, s) -> {
                b.writeCollection(s, RadioMetadata::write);
            });
        }

        public static RadioSourceChangeList fromPacket(PacketByteBuf buf) {
            var radioAdd = buf.readMap(Int2ObjectOpenHashMap::new, PacketByteBuf::readInt,
                b -> b.readCollection(HashSet::new, RadioMetadata::new)
            );
            var radioRem = buf.readMap(Int2ObjectOpenHashMap::new, PacketByteBuf::readInt,
                b -> b.readCollection(HashSet::new, RadioMetadata::new)
            );

            return new RadioSourceChangeList(radioAdd, radioRem);
        }

        public void cancelRemove(int channel, RadioMetadata radioMetadata) {
            if (this.remove.containsKey(channel)) {
                this.remove.get(channel).remove(radioMetadata);
            }
        }

        public void add(int channel, RadioMetadata radioMetadata) {
            this.add.computeIfAbsent(channel, k -> new HashSet<>()).add(radioMetadata);
        }

        public void apply(SoundEmitterTree tree) {
            for (var entry : this.add.int2ObjectEntrySet()) {
                int channel = entry.getIntKey();
                if (!tree.radioSources.containsKey(channel)) {
                    tree.radioSources.put(channel, entry.getValue());
                } else {
                    tree.radioSources.get(channel).addAll(entry.getValue());
                }
            }

            for (var entry : this.remove.int2ObjectEntrySet()) {
                int channel = entry.getIntKey();
                if (tree.radioSources.containsKey(channel)) {
                    tree.radioSources.get(channel).removeAll(entry.getValue());
                }
            }
        }

        public void setFrom(RadioSourceChangeList radioChanges) {
            add.clear();
            remove.clear();
            for (var entry : radioChanges.add.int2ObjectEntrySet()) {
                add.put(entry.getIntKey(), new HashSet<>(entry.getValue()));
            }
            for (var entry : radioChanges.remove.int2ObjectEntrySet()) {
                remove.put(entry.getIntKey(), new HashSet<>(entry.getValue()));
            }
        }
    }

    public record ChangeList(LongList add, LongList remove) {
        public static void toPacket(PacketByteBuf buf, ChangeList level) {
            buf.writeCollection(level.add, PacketByteBuf::writeLong);
            buf.writeCollection(level.remove, PacketByteBuf::writeLong);
        }

        public static ChangeList fromPacket(PacketByteBuf buf) {
            var add = buf.readCollection(LongArrayList::new, PacketByteBuf::readLong);
            var rem = buf.readCollection(LongArrayList::new, PacketByteBuf::readLong);

            return new ChangeList(add, rem);
        }

        public void apply(Level level) {
            level.active().removeAll(this.remove);
            level.inactive().removeAll(this.remove);

            for (long l : this.add) {
                if (!level.active().contains(l)) {
                    level.active().add(l);
                }
                level.inactive().rem(l);
            }
        }
    }
}
