package io.github.foundationgames.phonos.sound;

import io.github.foundationgames.phonos.network.PayloadPackets;
import io.github.foundationgames.phonos.sound.emitter.SoundEmitterTree;
import io.github.foundationgames.phonos.world.sound.data.SoundData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

import java.util.*;

public class ServerSoundStorage extends SoundStorage {
    private final Map<SoundEmitterTree, SoundData> resumableSounds = new WeakHashMap<>();
    private final Set<ServerPlayerEntity> playersWaitingForResume = new HashSet<>();

    @Override
    public void registerPlayerWaitingForResume(ServerPlayerEntity player) {
        playersWaitingForResume.add(player);
    }

    @Override
    public void play(World world, SoundData data, SoundEmitterTree tree) {
        tree.updateServer(world);

        if (data.type.resumable()) {
            data.updateSkippedTicksAndCheckResumable();
            resumableSounds.put(tree, data);
        }

        if (world instanceof ServerWorld sWorld) for (var player : sWorld.getPlayers()) {
            PayloadPackets.sendSoundPlay(player, data, tree);
        }

        this.notifySoundSourcesPlayed(world, data, tree);
    }

    @Override
    public void stop(World world, long soundUniqueId) {
        if (world instanceof ServerWorld sWorld) for (var player : sWorld.getPlayers()) {
            PayloadPackets.sendSoundStop(player, soundUniqueId);
        }
    }

    @Override
    public void update(SoundEmitterTree.Delta delta) {
    }

    @Override
    public void tick(World world) {
        // if no players need to resume, AND either no sounds need to be checked OR the current tick is not a lazy tick, return
        if (playersWaitingForResume.isEmpty() && (resumableSounds.isEmpty() || world.getTime() % 20 != 0)) return;

        var iterator = resumableSounds.entrySet().iterator();

        while (iterator.hasNext()) {
            var entry = iterator.next();
            var tree = entry.getKey();
            var data = entry.getValue();

            if (data.updateSkippedTicksAndCheckResumable()) {
                if (!playersWaitingForResume.isEmpty()) {
                    tree.updateServer(world);

                    for (var player : playersWaitingForResume) {
                        if (player.isRemoved() || player.getWorld() != world)
                            continue;
                        PayloadPackets.sendSoundPlay(player, data, tree);
                        data.onResumedToPlayer(player);
                    }
                }
            } else {
                iterator.remove();
            }
        }

        playersWaitingForResume.clear();
    }
}
