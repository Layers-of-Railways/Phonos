package io.github.foundationgames.phonos.world;

import io.github.foundationgames.phonos.radio.RadioStorage;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class RadarPoints extends PersistentState {
    private final Int2ObjectMap<LongSet> channelToSources = new Int2ObjectOpenHashMap<>();
    private final Map<String, LongSet> satelliteChannelToSources = new HashMap<>();

    public Collection<String> getSatelliteChannels() {
        return satelliteChannelToSources.keySet();
    }

    public void add(int channel, BlockPos pos) {
        var set = channelToSources.computeIfAbsent(channel, $ -> new LongOpenHashSet());
        set.add(pos.asLong());
        markDirty();
    }

    public void add(String channel, BlockPos pos) {
        var set = satelliteChannelToSources.computeIfAbsent(channel, $ -> new LongOpenHashSet());
        set.add(pos.asLong());
        markDirty();
    }

    public void remove(int channel, BlockPos pos) {
        if (channelToSources.containsKey(channel)) {
            channelToSources.get(channel).remove(pos.asLong());
            markDirty();
        }
    }

    public void remove(String channel, BlockPos pos) {
        if (satelliteChannelToSources.containsKey(channel)) {
            satelliteChannelToSources.get(channel).remove(pos.asLong());
            markDirty();
        }
    }

    public LongSet getPoints(int channel) {
        return channelToSources.get(channel);
    }

    public LongSet getPoints(String channel) {
        return satelliteChannelToSources.get(channel);
    }

    public static RadarPoints get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(RadarPoints::readNbt, RadarPoints::new, "phonos_radar_points");
    }

    private static int[] packPosSet(LongSet posSet) {
        var packedPosSet = new int[posSet.size() * 2];

        int idx = 0;
        for (long pos : posSet) {
            packedPosSet[idx] = (int) (pos >> 32);
            packedPosSet[idx + 1] = (int) pos;

            idx += 2;
        }

        return packedPosSet;
    }

    private static LongSet unpackPosSet(int[] packed) {
        var posSet = new LongOpenHashSet();

        for (int i = 0; i < packed.length; i += 2) {
            long upper = packed[i];
            long lower = packed[i + 1];

            posSet.add(lower | (upper << 32));
        }

        return posSet;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        for (var entry : channelToSources.int2ObjectEntrySet()) if (!entry.getValue().isEmpty()) {
            nbt.putIntArray("ch" + entry.getIntKey(), packPosSet(entry.getValue()));
        }

        NbtCompound sat = new NbtCompound();
        for (var entry : satelliteChannelToSources.entrySet()) if (!entry.getValue().isEmpty()) {
            sat.putIntArray(entry.getKey(), packPosSet(entry.getValue()));
        }
        nbt.put("sat", sat);

        return nbt;
    }

    public static RadarPoints readNbt(NbtCompound nbt) {
        var state = new RadarPoints();

        for (int ch = 0; ch < RadioStorage.CHANNEL_COUNT; ch++) {
            var key = "ch" + ch;

            if (nbt.contains(key)) {
                state.channelToSources.put(ch, unpackPosSet(nbt.getIntArray(key)));
            }
        }

        if (nbt.contains("sat")) {
            var sat = nbt.getCompound("sat");
            for (String key : sat.getKeys()) {
                state.satelliteChannelToSources.put(key, unpackPosSet(sat.getIntArray(key)));
            }
        }

        return state;
    }
}
