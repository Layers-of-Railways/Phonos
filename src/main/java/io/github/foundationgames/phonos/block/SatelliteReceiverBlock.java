package io.github.foundationgames.phonos.block;

import io.github.foundationgames.phonos.block.entity.SatelliteReceiverBlockEntity;
import io.github.foundationgames.phonos.block.entity.SatelliteStationBlockEntity;
import io.github.foundationgames.phonos.util.PhonosUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class SatelliteReceiverBlock extends RadioReceiverBlock {
    public SatelliteReceiverBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected ActionResult onUseFace(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        var stack = player.getStackInHand(hand);

        if (world.getBlockEntity(pos) instanceof SatelliteReceiverBlockEntity be) {
            if (stack.isOf(Items.NAME_TAG) && stack.hasCustomName()) {
                String customName = stack.getName().getString();
                if (SatelliteStationBlockEntity.validateChannel(customName)) {
                    be.setAndUpdateChannel(customName);
                    be.markDirty();
                }
            }
        }

        return ActionResult.CONSUME;
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
