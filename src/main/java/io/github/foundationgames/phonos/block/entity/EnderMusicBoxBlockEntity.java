package io.github.foundationgames.phonos.block.entity;

import io.github.foundationgames.phonos.Phonos;
import io.github.foundationgames.phonos.block.EnderMusicBoxBlock;
import io.github.foundationgames.phonos.block.PhonosBlocks;
import io.github.foundationgames.phonos.config.PhonosServerConfig;
import io.github.foundationgames.phonos.network.PayloadPackets;
import io.github.foundationgames.phonos.sound.SoundStorage;
import io.github.foundationgames.phonos.sound.custom.PhonosAudioRecord;
import io.github.foundationgames.phonos.sound.custom.ServerCustomAudio;
import io.github.foundationgames.phonos.sound.emitter.SoundEmitterTree;
import io.github.foundationgames.phonos.util.UniqueId;
import io.github.foundationgames.phonos.world.sound.InputPlugPoint;
import io.github.foundationgames.phonos.world.sound.block.BlockConnectionLayout;
import io.github.foundationgames.phonos.world.sound.block.ResumableSoundHolder;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

public class EnderMusicBoxBlockEntity extends AbstractConnectionHubBlockEntity implements ResumableSoundHolder {
    public static final BlockConnectionLayout OUTPUT_LAYOUT = new BlockConnectionLayout()
        .addPoint(-8, -4, 0, Direction.WEST)
        .addPoint(8, -4, 0, Direction.EAST)
        .addPoint(0, -4, 8, Direction.SOUTH)
        .addPoint(0, -4, -8, Direction.NORTH);

    private final LongList streamIds = new LongArrayList(15);
    private final List<String> streamNames = new ArrayList<>(15);
    private final long s2cStreamId;

    private @Nullable SoundEmitterTree playingSound = null;
    private @Nullable PhonosAudioRecord<?> playingRecord = null;
    private int playDuration = 0;
    private int playingTimer = 0;
    private int playingIndex = 0;

    private int playingSoundId = 1;

    private int deleteCooldown = 0;

    private int lastPowered = 0;

    private int lastComparatorOutput = -1;

    @ApiStatus.Internal
    public boolean playingClient = false;

    public EnderMusicBoxBlockEntity(BlockPos pos, BlockState state) {
        super(PhonosBlocks.ENDER_MUSIC_BOX_ENTITY, pos, state, OUTPUT_LAYOUT, new boolean[0]);

        this.s2cStreamId = UniqueId.obf(this.emitterId());
    }

    @Override
    public int getPlayingSoundId() {
        return playingSoundId;
    }

    @Override
    public long getSkippedTicks() {
        return playDuration - playingTimer;
    }

    public void play(int index) {
        if (index < 0 || index >= this.streamIds.size()) return;
        long streamId = this.streamIds.getLong(index);

        if (world instanceof ServerWorld sWorld && ServerCustomAudio.loaded() && ServerCustomAudio.hasSaved(streamId)) {
            if (this.playingSound != null) {
                this.stop();
            }

            var aud = Objects.requireNonNull(ServerCustomAudio.loadSaved(streamId));
            var soundData = aud.startPlaying(this.emitterId(), this.s2cStreamId, SoundCategory.MASTER, 2, 1, sWorld.getServer(), this);

            this.playingSound = new SoundEmitterTree(this.emitterId);
            SoundStorage.getInstance(this.world).play(this.world, soundData, this.playingSound);

            this.playingTimer = this.playDuration = aud.getPlayTicks();
            this.playingRecord = aud;

            world.updateComparators(pos, getCachedState().getBlock());
            sync();
        }
    }

    public void stop() {
        if (world instanceof ServerWorld sWorld && playingSound != null && playingRecord != null) {
            this.playingRecord.stopPlaying(this.s2cStreamId, sWorld.getServer());
            SoundStorage.getInstance(this.world).stop(this.world, this.emitterId());
            this.playingSound = null;
            this.playingRecord = null;

            world.updateComparators(pos, getCachedState().getBlock());
            sync();
        }

        this.playDuration = this.playingTimer = 0;
        this.playingSoundId++;
    }

    public void requestPlay(int power) {
        if (power > 0) {
            int targetIndex = MathHelper.clamp(power - 1, 0, this.streamIds.size() - 1);
            if (targetIndex != this.playingIndex) {
                this.playingIndex = targetIndex;
                sync();
                markDirty();

                if (getCachedState().get(EnderMusicBoxBlock.POWERED)>0) {
                    this.play(targetIndex);
                }
            }
        }
    }

    @Override
    public void tick(World world, BlockPos pos, BlockState state) {
        super.tick(world, pos, state);

        if (world.isClient)
            return;

        int powered = state.get(EnderMusicBoxBlock.POWERED);

        playingIndex = MathHelper.clamp(powered-1, 0, this.streamIds.size() - 1);

        if (lastPowered == 0 || powered != lastPowered) {
            lastPowered = powered;

            if (powered>0) {
                play(playingIndex);
            } else {
                stop();
            }
        }

        if (this.playingTimer > 0 && this.playingSound == null) {
            if (playingIndex >= 0)
                this.play(playingIndex);
            else
                this.stop();
        }

        if (this.playingTimer > 0) {
            this.playingTimer--;

            if (this.playingTimer == 0) {
                this.stop();
            }

            int comparatorOutput = getComparatorOutput();
            if (comparatorOutput != lastComparatorOutput) {
                lastComparatorOutput = comparatorOutput;
                markDirty();
            }
        }

        if (this.playingSound != null) {
            var delta = this.playingSound.updateServer(world);

            if (delta.hasChanges() && world instanceof ServerWorld sWorld) for (var player : sWorld.getPlayers()) {
                PayloadPackets.sendSoundUpdate(player, delta);
            }
        }

        if (deleteCooldown > 0) {
            deleteCooldown--;
        } else if (ServerCustomAudio.loaded()) {
            for (int i = 0; i < this.streamIds.size(); i++) {
                long streamId = this.streamIds.getLong(i);

                if (ServerCustomAudio.hasSaved(streamId)) continue;
                if (ServerCustomAudio.UPLOADING.containsKey(streamId)) continue;
                if (ServerCustomAudio.UNINITIALIZED_SESSIONS.contains(streamId)) continue;

                Phonos.LOG.warn("Removing stream id {} from Ender Music Box at ({}), because it is neither saved nor being uploaded", streamId, this.getPos().toShortString());
                this.streamIds.removeLong(i);
                this.streamNames.remove(i);

                if (playingIndex == i) {
                    this.stop();
                    if (powered>0)
                        this.play(playingIndex);
                } else if (playingIndex > i) {
                    playingIndex--;
                }

                i--;

                sync();
                markDirty();
            }
        }
    }

    public boolean canModifyStreams(ServerPlayerEntity player) {
        return player.getPos().squaredDistanceTo(this.getPos().toCenterPos()) < 96 && player.canModifyBlocks()
            && (!PhonosServerConfig.get(world).restrictEnderMusicBoxUploads || player.hasPermissionLevel(2));
    }

    public boolean hasFreeSpace() {
        return this.streamIds.size() < 15;
    }

    public @Nullable Long allocateStreamId(String name) {
        if (world == null || world.isClient) {
            return null;
        }

        if (!this.hasFreeSpace()) {
            return null;
        }

        long id = UniqueId.random();
        this.streamIds.add(id);
        this.streamNames.add(name);

        this.deleteCooldown = 10; // don't try to delete for a little bit, to give upload time to start

        this.markDirty();
        this.sync();

        return id;
    }

    public void deleteStream(long id) {
        if (world instanceof ServerWorld sWorld) {
            int index = this.streamIds.indexOf(id);
            if (index != -1) {
                this.streamIds.removeLong(index);
                this.streamNames.remove(index);

                if (playingIndex == index) {
                    this.stop();
                } else if (playingIndex > index) {
                    playingIndex--;
                }

                ServerCustomAudio.deleteSaved(sWorld.getServer(), id);

                this.markDirty();
                this.sync();
            }
        }
    }

    @ApiStatus.Internal
    public int getComparatorOutput() {
        return this.playingSound != null ? playingIndex + 1 : 0;
    }

    public void forEachStream(BiConsumer<Long, String> consumer) {
        for (int i = 0; i < this.streamIds.size(); i++) {
            consumer.accept(this.streamIds.getLong(i), this.streamNames.get(i));
        }
    }

    @Override
    public void onDestroyed() {
        super.onDestroyed();

        if (world instanceof ServerWorld sWorld) {
            this.stop();

            for (long streamId : this.streamIds) {
                ServerCustomAudio.deleteSaved(sWorld.getServer(), streamId);
            }
            this.streamIds.clear();
        }
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);

        this.playingTimer = nbt.getInt("PlayingTimer");
        this.playDuration = nbt.getInt("PlayDuration");

        this.streamIds.clear();
        this.streamNames.clear();
        NbtList streams = nbt.getList("Streams", NbtElement.COMPOUND_TYPE);
        for (NbtElement $streamData : streams) {
            NbtCompound streamData = (NbtCompound) $streamData;
            this.streamIds.add(streamData.getLong("Id"));
            this.streamNames.add(streamData.getString("Name"));
        }

        if (nbt.contains("PlayingIndex", NbtElement.INT_TYPE)) {
            this.playingIndex = MathHelper.clamp(nbt.getInt("PlayingIndex"), 0, this.streamIds.size() - 1);
        } else {
            this.playingIndex = 0;
        }

        this.playingClient = nbt.getBoolean("PlayingClient");
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);

        nbt.putInt("PlayingTimer", this.playingTimer);
        nbt.putInt("PlayDuration", this.playDuration);
        if (this.playingIndex != -1) {
            nbt.putInt("PlayingIndex", this.playingIndex);
        }

        NbtList streams = new NbtList();
        for (int i = 0; i < this.streamIds.size(); i++) {
            NbtCompound streamData = new NbtCompound();
            streamData.putLong("Id", this.streamIds.getLong(i));
            streamData.putString("Name", this.streamNames.get(i));

            streams.add(streamData);
        }

        nbt.put("Streams", streams);
    }

    @Override
    protected void writeClientNbt(NbtCompound nbt) {
        super.writeClientNbt(nbt);

        nbt.putBoolean("PlayingClient", playingSound != null);
    }

    @Override
    public boolean canConnect(ItemUsageContext ctx) {
        var side = ctx.getSide();
        if (side != Direction.UP && side != Direction.DOWN) {
            return !this.outputs.isOutputPluggedIn(OUTPUT_LAYOUT.getClosestIndexClicked(ctx.getHitPos(), this.getPos()));
        }

        return false;
    }

    @Override
    public boolean addConnection(Vec3d hitPos, @Nullable DyeColor color, InputPlugPoint destInput, ItemStack cable) {
        int index = OUTPUT_LAYOUT.getClosestIndexClicked(hitPos, this.getPos());

        if (this.outputs.tryPlugOutputIn(index, color, destInput, cable)) {
            this.markDirty();
            this.sync();
            return true;
        }

        return false;
    }

    @Override
    public boolean forwards() {
        return false;
    }
}
