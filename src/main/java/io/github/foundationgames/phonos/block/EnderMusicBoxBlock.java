package io.github.foundationgames.phonos.block;

import io.github.foundationgames.phonos.block.entity.EnderMusicBoxBlockEntity;
import io.github.foundationgames.phonos.config.PhonosServerConfig;
import io.github.foundationgames.phonos.network.PayloadPackets;
import io.github.foundationgames.phonos.util.PhonosUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class EnderMusicBoxBlock extends Block implements BlockEntityProvider {
    public static final BooleanProperty POWERED = Properties.POWERED;

    public EnderMusicBoxBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState()
            .with(POWERED, false));
    }

    @Override
    @SuppressWarnings("deprecation")
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        var side = hit.getSide();

        if (side == Direction.DOWN) {
            return ActionResult.PASS;
        }

        if (player.canModifyBlocks()) {
            if (!world.isClient() && world.getBlockEntity(pos) instanceof EnderMusicBoxBlockEntity be) {
                if (side == Direction.UP) {
                    if (!PhonosServerConfig.get(world).restrictEnderMusicBoxUploads || player.hasPermissionLevel(2)) {
                        if (player instanceof ServerPlayerEntity sPlayer) {
                            be.sync();
                            PayloadPackets.sendOpenEnderMusicBoxScreen(sPlayer, pos);
                        }

                        return ActionResult.SUCCESS;
                    } else {
                        return ActionResult.FAIL;
                    }
                }

                if (PhonosUtil.holdingAudioCable(player)) {
                    return ActionResult.PASS;
                }

                if (be.outputs.tryRemoveConnection(world, hit, !player.isCreative())) {
                    be.sync();
                    return ActionResult.SUCCESS;
                }
            }

            return ActionResult.SUCCESS;
        }

        return ActionResult.PASS;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!newState.isOf(this) && world.getBlockEntity(pos) instanceof EnderMusicBoxBlockEntity be) {
            be.onDestroyed();
        }

        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState()
            .with(POWERED, ctx.getWorld().getEmittedRedstonePower(ctx.getBlockPos().down(), Direction.DOWN) > 0);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        super.neighborUpdate(state, world, pos, sourceBlock, sourcePos, notify);

        if (world.isClient)
            return;

        BlockPos offset = sourcePos.subtract(pos);
        Direction direction = Direction.fromVector(offset.getX(), offset.getY(), offset.getZ());
        int power = world.getEmittedRedstonePower(pos.offset(direction), direction);

        if (direction == Direction.DOWN) {
            boolean wasPowered = state.get(POWERED);
            boolean isPowered = power > 0;

            if (wasPowered != isPowered) {
                world.setBlockState(pos, state.with(POWERED, isPowered), Block.NOTIFY_LISTENERS);
            }
        } else {
            if (world.getBlockEntity(pos) instanceof EnderMusicBoxBlockEntity be) {
                for (Direction dir : Direction.values()) {
                    if (dir == Direction.DOWN) continue;
                    int p = world.getEmittedRedstonePower(pos.offset(dir), dir);
                    if (p > power) power = p;
                }

                be.requestPlay(power);
            }
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean hasComparatorOutput(BlockState state) {
        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof EnderMusicBoxBlockEntity be) {
            return be.getComparatorOutput();
        }

        return 0;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder.add(POWERED));
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new EnderMusicBoxBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return PhonosUtil.blockEntityTicker(type, PhonosBlocks.ENDER_MUSIC_BOX_ENTITY);
    }
}
