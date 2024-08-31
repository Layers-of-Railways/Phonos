package io.github.foundationgames.phonos.block.entity;

import io.github.foundationgames.phonos.client.render.CableVBOContainer;
import io.github.foundationgames.phonos.sound.emitter.SoundEmitterStorage;
import io.github.foundationgames.phonos.sound.emitter.SoundEmitterTree;
import io.github.foundationgames.phonos.sound.emitter.SoundSource;
import io.github.foundationgames.phonos.util.UniqueId;
import io.github.foundationgames.phonos.world.sound.block.BlockConnectionLayout;
import io.github.foundationgames.phonos.world.sound.block.BlockEntityOutputs;
import io.github.foundationgames.phonos.world.sound.block.OutputBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.LongConsumer;

public abstract class AbstractOutputBlockEntity extends BlockEntity implements Syncing, Ticking, OutputBlockEntity {
    public final BlockEntityOutputs outputs;
    protected @Nullable NbtCompound pendingNbt = null;
    protected final long emitterId;

    private CableVBOContainer vboContainer;

    public AbstractOutputBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, BlockConnectionLayout outputLayout) {
        super(type, pos, state);

        this.emitterId = UniqueId.ofBlock(pos);
        this.outputs = new BlockEntityOutputs(outputLayout, this);
        this.vboContainer = null;
    }

    @Override
    public void tick(World world, BlockPos pos, BlockState state) {
        if (this.pendingNbt != null) {
            this.pendingNbt = this.outputs.consumeNbt(this.pendingNbt);
        }

        if (!world.isClient()) {
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

    protected void writeClientNbt(NbtCompound nbt) {}

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        NbtCompound nbt = new NbtCompound();
        this.writeNbt(nbt);
        this.writeClientNbt(nbt);
        return nbt;
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return this.getPacket();
    }

    @Override
    public long emitterId() {
        return this.emitterId;
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

    @ApiStatus.Internal
    public void debugNetwork(World world, Consumer<String> feedback) {
        SoundEmitterStorage emitterStorage = SoundEmitterStorage.getInstance(world);

        feedback.accept("%s %s has the following children: ".formatted(getClass().getSimpleName(), UniqueId.debugNameOf(emitterId())));

        var tree = new SoundEmitterTree(emitterId());
        if (world.isClient) {
            tree.updateClient(world);
        } else {
            tree.updateServer(world);
        }

        tree.debugLevels((level, id) -> {
            feedback.accept("%s- %s: %s".formatted("  ".repeat(level + 1), UniqueId.debugNameOf(id), emitterStorage.getEmitter(id)));
        });

        feedback.accept("");
        feedback.accept("Sources:");
        tree.forEachSource(world, source -> {
            feedback.accept("  - (%s, %s, %s) %s".formatted(source.x(), source.y(), source.z(), source));
        });
    }
}
