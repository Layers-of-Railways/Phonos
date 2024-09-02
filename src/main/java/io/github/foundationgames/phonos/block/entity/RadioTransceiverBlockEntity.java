package io.github.foundationgames.phonos.block.entity;

import io.github.foundationgames.phonos.block.PhonosBlocks;
import io.github.foundationgames.phonos.config.PhonosServerConfig;
import io.github.foundationgames.phonos.radio.RadioDevice;
import io.github.foundationgames.phonos.radio.RadioMetadata;
import io.github.foundationgames.phonos.util.PhonosTags;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.function.LongConsumer;

public class RadioTransceiverBlockEntity extends RadioReceiverBlockEntity implements RadioDevice.Transmitter {
    private int transmissionTowerHeight = 0;

    public RadioTransceiverBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, new boolean[2]);
    }

    public RadioTransceiverBlockEntity(BlockPos pos, BlockState state) {
        this(PhonosBlocks.RADIO_TRANSCEIVER_ENTITY, pos, state);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);

        this.transmissionTowerHeight = nbt.getInt("transmissionTowerHeight");
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);

        nbt.putInt("transmissionTowerHeight", this.transmissionTowerHeight);
    }

    @Override
    public void forEachChild(LongConsumer action) {
        Transmitter.super.forEachChild(action);

        super.forEachChild(action);
    }

    @Override
    public RadioMetadata getMetadata() {
        PhonosServerConfig conf = PhonosServerConfig.get(world);
        BlockPos pos = getPos();

        int relativeToSeaLevel = pos.getY() - (world == null ? 63 : world.getSeaLevel());

        int range = conf.radioBaseRange;
        range += Math.round(relativeToSeaLevel * conf.worldHeightMultiplier);
        range += Math.round(transmissionTowerHeight * conf.transmissionTowerMultiplier);

        return new RadioMetadata(pos, Math.max(0, range));
    }

    private static final int lazyTickRate = 100;
    private int lazyTickCounter = 0;

    @Override
    public void tick(World world, BlockPos pos, BlockState state) {
        super.tick(world, pos, state);

        if (lazyTickCounter++ >= lazyTickRate) {
            lazyTickCounter = 0;
            lazyTick();
        }
    }

    private void lazyTick() {
        if (world == null || world.isClient)
            return;

        int lastHeight = transmissionTowerHeight;

        BlockPos.Mutable pos = getPos().mutableCopy();
        for (transmissionTowerHeight = 0; transmissionTowerHeight < PhonosServerConfig.get(world).maxTransmissionTowerHeight; transmissionTowerHeight++) {
            pos.move(Direction.UP);
            BlockState state = world.getBlockState(pos);
            if (!state.isIn(PhonosTags.TRANSMISSION_TOWERS))
                break;
        }

        if (transmissionTowerHeight != lastHeight) {
            this.markDirty();
            sync();
        }
    }

    @Override
    public int getTransmissionTowerHeight() {
        return transmissionTowerHeight;
    }

    @Override
    public boolean isSatellite() {
        return false;
    }
}
