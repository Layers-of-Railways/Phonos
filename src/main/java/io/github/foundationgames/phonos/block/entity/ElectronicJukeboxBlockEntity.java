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
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.jukebox.JukeboxSong;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.ItemEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SingleStackInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Clearable;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

public class ElectronicJukeboxBlockEntity extends BlockEntity implements Syncing, Ticking, OutputBlockEntity, ResumableSoundHolder, Clearable, SingleStackInventory.SingleStackBlockEntityInventory {
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

    private ItemStack recordStack = ItemStack.EMPTY;
    private long ticksSinceSongStarted;
    @Nullable
    private RegistryEntry<JukeboxSong> song;

    public ElectronicJukeboxBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
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
        return this.ticksSinceSongStarted;
    }

    public void startPlaying(RegistryEntry<JukeboxSong> song) {
        if (world == null) return;

        this.song = song;
        this.ticksSinceSongStarted = 0L;

        if (!world.isClient()) {
            this.playingSound = new SoundEmitterTree(this.emitterId);

            SoundStorage.getInstance(world).play(world, SoundEventSoundData.create(
                    emitterId, song.value().soundEvent(), SoundCategory.RECORDS, 2, 1, this),
                    this.playingSound);
            sync();
        }

        this.world.updateNeighborsAlways(this.getPos(), this.getCachedState().getBlock());
        this.world.emitGameEvent(GameEvent.JUKEBOX_PLAY, this.getPos(), GameEvent.Emitter.of(this.getCachedState()));

        this.markDirty();
    }

    protected void stopPlaying() {
        if (world == null) return;

        if (song != null) {
            song = null;
            ticksSinceSongStarted = 0L;
        }

        if (!world.isClient()) {
            this.playingSound = null;

            SoundStorage.getInstance(world).stop(world, emitterId);
            playingSoundId++;
            sync();
        }

        this.world.emitGameEvent(GameEvent.JUKEBOX_STOP_PLAY, this.getPos(), GameEvent.Emitter.of(this.getCachedState()));
        this.world.updateNeighborsAlways(this.getPos(), this.getCachedState().getBlock());

        this.markDirty();
    }

    private void onRecordStackChanged(boolean hasRecord) {
        if (this.world != null && this.world.getBlockState(this.getPos()) == this.getCachedState()) {
            this.world.setBlockState(this.getPos(), this.getCachedState().with(JukeboxBlock.HAS_RECORD, hasRecord), Block.NOTIFY_LISTENERS);
            this.world.emitGameEvent(GameEvent.BLOCK_CHANGE, this.getPos(), GameEvent.Emitter.of(this.getCachedState()));
        }
    }

    @Override
    public void tick(World world, BlockPos pos, BlockState state) {
        if (this.pendingNbt != null) {
            this.pendingNbt = this.outputs.consumeNbt(this.pendingNbt);
        }

        if (!world.isClient()) {
            if (song != null) {
                if (song.value().shouldStopPlaying(ticksSinceSongStarted)) {
                    stopPlaying();
                } else {
                    if (ticksSinceSongStarted % 20L == 0L) {
                        world.emitGameEvent(GameEvent.JUKEBOX_PLAY, getPos(), GameEvent.Emitter.of(getCachedState()));
                        spawnNoteParticles(world, getPos());
                    }

                    ticksSinceSongStarted++;
                }
            }

            if (this.song != null && this.getStack().isEmpty() && this.getSkippedTicks() > 5) {
                this.onRecordStackChanged(false);
                this.stopPlaying();
            } else if (song == null && this.getStack().isEmpty() && getCachedState().get(JukeboxBlock.HAS_RECORD)) {
                this.onRecordStackChanged(false);
            }
            this.markDirty();

            if (this.playingSound == null && song != null) {
                this.playingSound = new SoundEmitterTree(this.emitterId);

                playingSoundId++;
                SoundStorage.getInstance(world).play(world, SoundEventSoundData.create(
                        emitterId, song.value().soundEvent(), SoundCategory.RECORDS, 2, 1, this),
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

    public boolean isPlaying() {
        return playingSound != null;
    }

    private static void spawnNoteParticles(WorldAccess world, BlockPos pos) {
        if (world instanceof ServerWorld serverWorld) {
            Vec3d vec3d = Vec3d.ofBottomCenter(pos).add(0.0, 1.2F, 0.0);
            float f = (float)world.getRandom().nextInt(4) / 24.0F;
            serverWorld.spawnParticles(ParticleTypes.NOTE, vec3d.getX(), vec3d.getY(), vec3d.getZ(), 0, (double)f, 0.0, 0.0, 1.0);
        }
    }

    public void onDestroyed() {
        this.outputs.forEach((index, conn) -> {
            this.outputs.dropConnectionItem(world, conn, false);
            conn.end.setConnected(this.getWorld(), false);
        });
    }

    public void dropRecord() {
        if (this.world != null && !this.world.isClient) {
            BlockPos pos = getPos();
            ItemStack stack = getStack();

            if (!stack.isEmpty()) {
                emptyStack();
                Vec3d dropPos = Vec3d.add(pos, 0.5, 1.01, 0.5).addRandom(world.random, 0.7f);
                ItemStack stack2 = stack.copy();
                ItemEntity itemEntity = new ItemEntity(world, dropPos.getX(), dropPos.getY(), dropPos.getZ(), stack2);
                itemEntity.setToDefaultPickupDelay();
                world.spawnEntity(itemEntity);
            }
        }
    }

    public int getComparatorOutput() {
        if (world == null) return 0;

        return JukeboxSong.getSongEntryFromStack(world.getRegistryManager(), recordStack)
            .map(RegistryEntry::value)
            .map(JukeboxSong::comparatorOutput)
            .orElse(0);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);

        if (nbt.contains("RecordItem", NbtElement.COMPOUND_TYPE)) {
            this.recordStack = ItemStack.fromNbtOrEmpty(registryLookup, nbt.getCompound("RecordItem"));
        } else {
            this.recordStack = ItemStack.EMPTY;
        }

        if (nbt.contains("ticks_since_song_started", NbtElement.LONG_TYPE)) {
            JukeboxSong.getSongEntryFromStack(registryLookup, this.recordStack).ifPresent(song -> {
                long ticksSinceSongStarted = nbt.getLong("ticks_since_song_started");
                if (!song.value().shouldStopPlaying(ticksSinceSongStarted)) {
                    this.song = song;
                    this.ticksSinceSongStarted = ticksSinceSongStarted;
                }
            });
        }

        this.pendingNbt = nbt.getCompound("Outputs").copy();

        if (this.world != null) {
            this.pendingNbt = this.outputs.consumeNbt(this.pendingNbt);
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);

        if (!this.recordStack.isEmpty()) {
            nbt.put("RecordItem", this.recordStack.encode(registryLookup));
        }

        if (song != null) {
            nbt.putLong("ticks_since_song_started", ticksSinceSongStarted);
        }

        var outputsNbt = this.pendingNbt != null ? this.pendingNbt.copy() : new NbtCompound();
        outputs.writeNbt(outputsNbt, registryLookup);
        nbt.put("Outputs", outputsNbt);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registryLookup) {
        NbtCompound nbt = new NbtCompound();
        this.writeNbt(nbt, registryLookup);
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

    @Override
    public ItemStack getStack() {
        return recordStack;
    }

    @Override
    public ItemStack decreaseStack(int count) {
        ItemStack stack = recordStack;
        setStack(ItemStack.EMPTY);
        return stack;
    }

    @Override
    public void setStack(ItemStack stack) {
        if (world == null) {
            return;
        }

        this.recordStack = stack;
        boolean hasRecord = !recordStack.isEmpty();
        Optional<RegistryEntry<JukeboxSong>> optional = JukeboxSong.getSongEntryFromStack(world.getRegistryManager(), recordStack);
        onRecordStackChanged(hasRecord);

        if (hasRecord && optional.isPresent()) {
            startPlaying(optional.get());
        } else {
            stopPlaying();
        }
    }

    @Override
    public int getMaxCountPerStack() {
        return 1;
    }

    @Override
    public BlockEntity asBlockEntity() {
        return this;
    }

    @Override
    public boolean isValid(int slot, ItemStack stack) {
        return stack.contains(DataComponentTypes.JUKEBOX_PLAYABLE) && getStack(slot).isEmpty();
    }

    @Override
    public boolean canTransferTo(Inventory hopperInventory, int slot, ItemStack stack) {
        return hopperInventory.containsAny(ItemStack::isEmpty);
    }
}
