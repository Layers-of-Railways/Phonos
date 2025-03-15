package io.github.foundationgames.phonos.block;

import com.mojang.serialization.MapCodec;
import io.github.foundationgames.phonos.block.entity.SatelliteReceiverBlockEntity;
import io.github.foundationgames.phonos.block.entity.SatelliteStationBlockEntity;
import io.github.foundationgames.phonos.util.PhonosUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class SatelliteReceiverBlock extends RadioReceiverBlock {
    public static final MapCodec<SatelliteReceiverBlock> CODEC = createCodec(SatelliteReceiverBlock::new);

    public SatelliteReceiverBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends RadioReceiverBlock> getCodec() {
        return CODEC;
    }

    @Override
    protected ItemActionResult onUseFace(BlockState state, World world, BlockPos pos, PlayerEntity player, ItemStack stack, BlockHitResult hit) {
        if (world.getBlockEntity(pos) instanceof SatelliteReceiverBlockEntity be) {
            if (stack.isOf(Items.NAME_TAG) && stack.contains(DataComponentTypes.CUSTOM_NAME)) {
                String customName = stack.getName().getString();
                if (SatelliteStationBlockEntity.validateChannel(customName)) {
                    be.setAndUpdateChannel(customName);
                    be.markDirty();
                }
            }
        }

        return ItemActionResult.CONSUME;
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
