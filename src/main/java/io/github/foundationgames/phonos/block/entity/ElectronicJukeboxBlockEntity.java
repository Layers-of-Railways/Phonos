package io.github.foundationgames.phonos.block.entity;

import io.github.foundationgames.phonos.block.PhonosBlocks;
import io.github.foundationgames.phonos.client.render.CableVBOContainer;
import io.github.foundationgames.phonos.network.PayloadPackets;
import io.github.foundationgames.phonos.sound.SoundStorage;
import io.github.foundationgames.phonos.sound.emitter.SoundEmitterTree;
import io.github.foundationgames.phonos.sound.emitter.SoundSource;
import io.github.foundationgames.phonos.util.UniqueId;
import io.github.foundationgames.phonos.world.sound.InputPlugPoint;
import io.github.foundationgames.phonos.world.sound.block.BlockConnectionLayout;
import io.github.foundationgames.phonos.world.sound.block.BlockEntityOutputs;
import io.github.foundationgames.phonos.world.sound.block.OutputBlockEntity;
import io.github.foundationgames.phonos.world.sound.block.ResumableSoundHolder;
import io.github.foundationgames.phonos.world.sound.data.SoundEventSoundData;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.JukeboxBlock;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.JukeboxBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.MusicDiscItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.LongConsumer;

public class ElectronicJukeboxBlockEntity extends JukeboxBlockEntity implements Syncing, Ticking, OutputBlockEntity, ResumableSoundHolder {
    public static final BlockConnectionLayout OUTPUT_LAYOUT = new BlockConnectionLayout()
            .addPoint(-8, -4, 0, Direction.WEST)
            .addPoint(8, -4, 0, Direction.EAST)
            .addPoint(0, -4, 8, Direction.SOUTH)
            .addPoint(0, -4, -8, Direction.NORTH);

    public final BlockEntityOutputs outputs;

    private final BlockEntityType<?> type;
    private @Nullable NbtCompound pendingNbt = null;
    private final long emitterId;
    private @Nullable SoundEmitterTree playingSound = null;
    private int playingSoundId = 1;

    private CableVBOContainer vboContainer;

    public ElectronicJukeboxBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(pos, state);
        this.type = type;
        this.emitterId = UniqueId.ofBlock(pos);

        this.outputs = new BlockEntityOutputs(OUTPUT_LAYOUT, this);
        this.vboContainer = null;
    }

    public ElectronicJukeboxBlockEntity(BlockPos pos, BlockState state) {
        this(PhonosBlocks.ELECTRONIC_JUKEBOX_ENTITY, pos, state);
    }

    @Override
    public BlockEntityType<?> getType() {
        return this.type;
    }

    @Override
    public int getPlayingSoundId() {
        return this.playingSoundId;
    }

    @Override
    public long getSkippedTicks() {
        return this.tickCount - this.recordStartTick;
    }

    @Override
    public void startPlaying() {
        this.recordStartTick = this.tickCount;
        this.isPlaying = true;
        this.world.updateNeighborsAlways(this.getPos(), this.getCachedState().getBlock());

        if (this.getStack().getItem() instanceof MusicDiscItem disc && !world.isClient()) {
            this.playingSound = new SoundEmitterTree(this.emitterId);

            SoundStorage.getInstance(world).play(world, SoundEventSoundData.create(
                    emitterId, Registries.SOUND_EVENT.getEntry(disc.getSound()), SoundCategory.RECORDS, 2, 1, this),
                    this.playingSound);
            sync();
        }

        this.markDirty();
    }

    @Override
    protected void stopPlaying() {
        this.isPlaying = false;
        this.world.emitGameEvent(GameEvent.JUKEBOX_STOP_PLAY, this.getPos(), GameEvent.Emitter.of(this.getCachedState()));
        this.world.updateNeighborsAlways(this.getPos(), this.getCachedState().getBlock());

        if (!world.isClient()) {
            this.playingSound = null;

            SoundStorage.getInstance(world).stop(world, emitterId);
            playingSoundId++;
            sync();
        }

        this.markDirty();
    }

    private void updateState(@Nullable Entity entity, boolean hasRecord) {
        if (this.world.getBlockState(this.getPos()) == this.getCachedState()) {
            this.world.setBlockState(this.getPos(), (BlockState)this.getCachedState().with(JukeboxBlock.HAS_RECORD, hasRecord), Block.NOTIFY_LISTENERS);
            this.world.emitGameEvent(GameEvent.BLOCK_CHANGE, this.getPos(), GameEvent.Emitter.of(entity, this.getCachedState()));
        }
    }

    @Override
    public void tick(World world, BlockPos pos, BlockState state) {
        if (this.pendingNbt != null) {
            this.pendingNbt = this.outputs.consumeNbt(this.pendingNbt);
        }

        if (!world.isClient()) {
            super.tick(world, pos, state);
            if (this.isPlaying && this.getStack().isEmpty() && this.getSkippedTicks() > 5) {
                this.updateState(null, false);
                this.stopPlaying();
            } else if (!this.isPlaying && this.getStack().isEmpty() && getCachedState().get(JukeboxBlock.HAS_RECORD)) {
                this.updateState(null, false);
            }
            this.markDirty();

            if (this.playingSound == null && this.isPlayingRecord() && this.getStack().getItem() instanceof MusicDiscItem disc) {
                this.playingSound = new SoundEmitterTree(this.emitterId);

                playingSoundId++;
                SoundStorage.getInstance(world).play(world, SoundEventSoundData.create(
                        emitterId, Registries.SOUND_EVENT.getEntry(disc.getSound()), SoundCategory.RECORDS, 2, 1, this),
                    this.playingSound);
                sync();
            }

            if (this.playingSound != null) {
                var delta = this.playingSound.updateServer(world);

                if (delta.hasChanges() && world instanceof ServerWorld sWorld) for (var player : sWorld.getPlayers()) {
                    PayloadPackets.sendSoundUpdate(player, delta);
                }
            }

            if (this.outputs.purge(conn -> this.outputs.dropConnectionItem(world, conn, true))) {
                sync();
                markDirty();
            }
        }
    }

    public void onDestroyed() {
        this.outputs.forEach((index, conn) -> {
            this.outputs.dropConnectionItem(world, conn, false);
            conn.end.setConnected(this.getWorld(), false);
        });
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);

        this.pendingNbt = nbt.getCompound("Outputs").copy();

        if (this.world != null) {
            this.pendingNbt = this.outputs.consumeNbt(this.pendingNbt);
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);

        var outputsNbt = this.pendingNbt != null ? this.pendingNbt.copy() : new NbtCompound();
        outputs.writeNbt(outputsNbt);
        nbt.put("Outputs", outputsNbt);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        NbtCompound nbt = new NbtCompound();
        this.writeNbt(nbt);
        return nbt;
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return this.getPacket();
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

    @Override
    public BlockEntityOutputs getOutputs() {
        return this.outputs;
    }

    @Override
    public void enforceVBOState(boolean enabled) {
        if (this.vboContainer != null && !enabled) {
            this.vboContainer.close();

            this.vboContainer = null;
        }
    }

    @Override
    public CableVBOContainer getOrCreateVBOContainer() {
        if (this.vboContainer == null) {
            this.vboContainer = new CableVBOContainer();
        }

        this.vboContainer.refresh(this.outputs);
        return this.vboContainer;
    }

    @Override
    public void markRemoved() {
        if (this.hasWorld() && this.world.isClient() && this.vboContainer != null) {
            this.vboContainer.rebuild = true;
            this.vboContainer.close();
        }
        super.markRemoved();
    }

    @Override
    public long emitterId() {
        return emitterId;
    }

    @Override
    public void forEachSource(Consumer<SoundSource> action) {
        this.outputs.forEach((i, conn) -> {
            var src = conn.end.asSource(this.world);

            if (src != null) {
                action.accept(src);
            }
        });
    }

    @Override
    public void forEachChild(LongConsumer action) {
        this.outputs.forEach((i, conn) -> {
            var emitter = conn.end.forward(this.world);

            if (emitter != null) {
                action.accept(emitter.emitterId());
            }
        });
    }
}
