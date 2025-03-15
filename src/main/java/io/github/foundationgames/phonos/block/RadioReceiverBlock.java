package io.github.foundationgames.phonos.block;

import com.mojang.serialization.MapCodec;
import io.github.foundationgames.phonos.block.entity.AbstractConnectionHubBlockEntity;
import io.github.foundationgames.phonos.util.PhonosUtil;
import io.github.foundationgames.phonos.world.sound.block.InputBlock;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public abstract class RadioReceiverBlock extends HorizontalFacingBlock implements BlockEntityProvider {
    private static final VoxelShape SHAPE = createCuboidShape(0, 0, 0, 16, 7, 16);

    public RadioReceiverBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    protected abstract MapCodec<? extends RadioReceiverBlock> getCodec();

    protected abstract ItemActionResult onUseFace(BlockState state, World world, BlockPos pos, PlayerEntity player, ItemStack stack, BlockHitResult hit);

    @Override
    protected ItemActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        var side = hit.getSide();
        var facing = state.get(FACING);

        if (player.canModifyBlocks() && side == facing) {
            if (!world.isClient()) {
                var result = onUseFace(state, world, pos, player, stack, hit);
                if (result.isAccepted())
                    return result;
            }

            return ItemActionResult.SUCCESS;
        }

        return super.onUseWithItem(stack, state, world, pos, player, hand, hit);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        var side = hit.getSide();
        var facing = state.get(FACING);

        if (side == Direction.DOWN || side == Direction.UP) {
            return ActionResult.PASS;
        }

        if (player.canModifyBlocks()) {
            if (side == facing) {
                if (!world.isClient()) {
                    var result = onUseFace(state, world, pos, player, ItemStack.EMPTY, hit);
                    if (result.isAccepted()) {
                        return result.toActionResult();
                    }
                }

                return ActionResult.SUCCESS;
            }

            if (!world.isClient() && world.getBlockEntity(pos) instanceof AbstractConnectionHubBlockEntity be) {
                if (PhonosUtil.holdingAudioCable(player)) {
                    return ActionResult.PASS;
                }

                if (side != facing.getOpposite()) {
                    if (be.outputs.tryRemoveConnection(world, hit, !player.isCreative())) {
                        be.sync();
                        return ActionResult.SUCCESS;
                    }
                } else if (this instanceof InputBlock inputBlock) {
                    return inputBlock.tryRemoveConnection(state, world, pos, hit);
                }
            }

            return ActionResult.success(side == facing.getOpposite());
        }

        return ActionResult.PASS;
    }

    @Override
    protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!newState.isOf(this) && world.getBlockEntity(pos) instanceof AbstractConnectionHubBlockEntity be) {
            be.onDestroyed();
        }

        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    public Direction getRotation(BlockState state) {
        return state.get(FACING);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);

        builder.add(FACING);
    }

    @Nullable
    @Override
    public abstract BlockEntity createBlockEntity(BlockPos pos, BlockState state);

    @Nullable
    @Override
    public abstract <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type);
}
