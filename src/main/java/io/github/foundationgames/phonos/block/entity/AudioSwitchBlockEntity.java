package io.github.foundationgames.phonos.block.entity;

import io.github.foundationgames.phonos.block.AudioSwitchBlock;
import io.github.foundationgames.phonos.block.PhonosBlocks;
import io.github.foundationgames.phonos.block.RadioTransceiverBlock;
import io.github.foundationgames.phonos.sound.emitter.SecondaryEmitterHolder;
import io.github.foundationgames.phonos.sound.emitter.SoundEmitter;
import io.github.foundationgames.phonos.sound.emitter.SoundSource;
import io.github.foundationgames.phonos.util.UniqueId;
import io.github.foundationgames.phonos.world.sound.InputPlugPoint;
import io.github.foundationgames.phonos.world.sound.block.BlockConnectionLayout;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.LongConsumer;

public class AudioSwitchBlockEntity extends AbstractConnectionHubBlockEntity implements SecondaryEmitterHolder {
    public static final BlockConnectionLayout OUTPUT_LAYOUT = new BlockConnectionLayout()
        .addPoint(8, -5, 5, Direction.EAST)
        .addPoint(8, -5, -5, Direction.EAST);

    protected final long secondaryEmitterId;
    protected final SoundEmitter secondaryEmitter = new SecondaryEmitter();

    public AudioSwitchBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, OUTPUT_LAYOUT, new boolean[2]);
        this.secondaryEmitterId = UniqueId.obf(emitterId());
    }

    public AudioSwitchBlockEntity(BlockPos pos, BlockState state) {
        this(PhonosBlocks.AUDIO_SWITCH_ENTITY, pos, state);
    }

    @Override
    public boolean canConnect(ItemUsageContext ctx) {
        var side = ctx.getSide();
        var facing = getRotation();

        if (side != Direction.UP && side != Direction.DOWN && side == facing.rotateYClockwise()) {
            return !this.outputs.isOutputPluggedIn(OUTPUT_LAYOUT.getClosestIndexClicked(ctx.getHitPos(), this.getPos(), facing));
        }

        return false;
    }

    @Override
    public boolean addConnection(Vec3d hitPos, @Nullable DyeColor color, InputPlugPoint destInput, ItemStack cable) {
        int index = OUTPUT_LAYOUT.getClosestIndexClicked(hitPos, this.getPos(), getRotation());

        if (this.outputs.tryPlugOutputIn(index, color, destInput, cable)) {
            this.markDirty();
            this.sync();
            return true;
        }

        return false;
    }

    @Override
    public boolean forwards() {
        return true;
    }

    @Override
    public SoundEmitter forward(int connectionIndex) {
        return connectionIndex == 0 ? this : secondaryEmitter;
    }

    @Override
    public SoundEmitter getSecondaryEmitter() {
        return secondaryEmitter;
    }

    @Override
    public Direction getRotation() {
        if (this.getCachedState().getBlock() instanceof AudioSwitchBlock block) {
            return block.getRotation(this.getCachedState());
        }

        return Direction.NORTH;
    }

    protected int getPhase() {
        return getCachedState().get(AudioSwitchBlock.POWERED) ? 1 : 0;
    }

    @Override
    public void forEachSource(Consumer<SoundSource> action) {
        this.outputs.forEach((i, conn) -> {
            if (i % 2 != getPhase()) return;
            var src = conn.end.asSource(this.world);

            if (src != null) {
                action.accept(src);
            }
        });
    }

    @Override
    public void forEachChild(LongConsumer action) {
        this.outputs.forEach((i, conn) -> {
            if (i % 2 != getPhase()) return;
            var emitter = conn.end.forward(this.world);

            if (emitter != null) {
                action.accept(emitter.emitterId());
            }
        });
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " {(%s) %s}".formatted(getPos().toShortString(), UniqueId.debugNameOf(emitterId()));
    }

    private class SecondaryEmitter implements SoundEmitter {

        @Override
        public long emitterId() {
            return secondaryEmitterId;
        }

        @Override
        public void forEachSource(Consumer<SoundSource> action) {
            outputs.forEach((i, conn) -> {
                if (i % 2 == getPhase()) return;
                var src = conn.end.asSource(world);

                if (src != null) {
                    action.accept(src);
                }
            });
        }

        @Override
        public void forEachChild(LongConsumer action) {
            outputs.forEach((i, conn) -> {
                if (i % 2 == getPhase()) return;
                var emitter = conn.end.forward(world);

                if (emitter != null) {
                    action.accept(emitter.emitterId());
                }
            });
        }
    }
}
