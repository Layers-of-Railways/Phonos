package io.github.foundationgames.phonos.block.entity;

import io.github.foundationgames.phonos.block.PhonosBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

public class SatelliteReceiverBlockEntity extends RadioReceiverBlockEntity {
    public SatelliteReceiverBlockEntity(BlockPos pos, BlockState state) {
        super(PhonosBlocks.SATELLITE_RECEIVER_ENTITY, pos, state, new boolean[0]);
    }

    @Override
    public boolean isSatellite() {
        return true;
    }
}
