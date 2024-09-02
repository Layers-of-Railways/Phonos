package io.github.foundationgames.phonos.block;

import io.github.foundationgames.phonos.block.entity.SatelliteReceiverBlockEntity;
import io.github.foundationgames.phonos.util.PhonosUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class SatelliteReceiverBlock extends RadioReceiverBlock {
    public SatelliteReceiverBlock(Settings settings) {
        super(settings);
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new SatelliteReceiverBlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return PhonosUtil.blockEntityTicker(type, PhonosBlocks.SATELLITE_RECEIVER_ENTITY);
    }
}
