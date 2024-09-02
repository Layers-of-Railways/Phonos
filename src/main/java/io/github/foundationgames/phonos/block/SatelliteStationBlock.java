package io.github.foundationgames.phonos.block;

import io.github.foundationgames.phonos.block.entity.SatelliteStationBlockEntity;
import io.github.foundationgames.phonos.item.PhonosItems;
import io.github.foundationgames.phonos.radio.RadioStorage;
import io.github.foundationgames.phonos.util.PhonosUtil;
import io.github.foundationgames.phonos.world.RadarPoints;
import io.github.foundationgames.phonos.world.sound.block.BlockConnectionLayout;
import io.github.foundationgames.phonos.world.sound.block.InputBlock;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class SatelliteStationBlock extends HorizontalFacingBlock implements BlockEntityProvider, InputBlock {
    private static final VoxelShape SHAPE = createCuboidShape(0, 0, 0, 16, 7, 16);

    public final BlockConnectionLayout inputLayout = new BlockConnectionLayout()
        .addPoint(-4.5, -4.5, 8, Direction.SOUTH)
        .addPoint(4.5, -4.5, 8, Direction.SOUTH);

    public SatelliteStationBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    @SuppressWarnings("deprecation")
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        var side = hit.getSide();
        var facing = state.get(FACING);

        if (side == Direction.DOWN) {
            return ActionResult.PASS;
        }

        if (player.canModifyBlocks() && world.getBlockEntity(pos) instanceof SatelliteStationBlockEntity be) {
            if (side == Direction.UP) {
                var holding = player.getStackInHand(hand);
                if (holding.getItem() == PhonosItems.SATELLITE) {
                    if (world.isClient()) {
                        return ActionResult.CONSUME;
                    } else {
                        if (be.addRocket() && !player.isCreative()) {
                            holding.decrement(1);
                        }

                        return ActionResult.SUCCESS;
                    }
                } else if (player instanceof ServerPlayerEntity sPlayer) {
                    be.tryOpenScreen(sPlayer);
                }

                return ActionResult.CONSUME;
            }

            if (side == facing) {
                if (!world.isClient()) {
                    int inc = player.isSneaking() ? -1 : 1;
                    be.setAndUpdateChannel(be.getChannel() + inc);
                    be.markDirty();

                    return ActionResult.CONSUME;
                }

                return ActionResult.SUCCESS;
            }

            if (!world.isClient()) {
                if (PhonosUtil.holdingAudioCable(player)) {
                    return ActionResult.PASS;
                }

                return tryRemoveConnection(state, world, pos, hit);
            }
        }

        return super.onUse(state, world, pos, player, hand, hit);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!newState.isOf(this) && world.getBlockEntity(pos) instanceof SatelliteStationBlockEntity be) {
            be.onDestroyed();
        }

        super.onStateReplaced(state, world, pos, newState, moved);
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

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    public Direction getRotation(BlockState state) {
        return state.get(FACING);
    }

    @Override
    public boolean canInputConnect(ItemUsageContext ctx) {
        var world = ctx.getWorld();
        var pos = ctx.getBlockPos();
        var state = world.getBlockState(pos);
        var facing = state.get(FACING);
        var side = ctx.getSide();

        if (side == facing.getOpposite()) {
            int index = this.getInputLayout().getClosestIndexClicked(ctx.getHitPos(), pos, getRotation(state));

            return !this.isInputPluggedIn(index, state, world, pos);
        }

        return false;
    }

    @Override
    public boolean playsSound(World world, BlockPos pos) {
        return false;
    }

    @Override
    public boolean isInputPluggedIn(int inputIndex, BlockState state, World world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof SatelliteStationBlockEntity be) {
            inputIndex = MathHelper.clamp(inputIndex, 0, be.inputs.length - 1);

            return be.inputs[inputIndex];
        }

        return false;
    }

    @Override
    public void setInputPluggedIn(int inputIndex, boolean pluggedIn, BlockState state, World world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof SatelliteStationBlockEntity be) {
            inputIndex = MathHelper.clamp(inputIndex, 0, be.inputs.length - 1);
            be.inputs[inputIndex] = pluggedIn;

            if (world instanceof ServerWorld sWorld) {
                if (pluggedIn) {
                    RadarPoints.get(sWorld).add(RadioStorage.toSatelliteBand(be.getChannel()), pos);
                } else {
                    boolean remove = true;
                    for (boolean in : be.inputs) if (in) {
                        remove = false;
                        break;
                    }

                    if (remove) {
                        RadarPoints.get(sWorld).remove(RadioStorage.toSatelliteBand(be.getChannel()), pos);
                    }
                }
            }

            be.sync();
            be.markDirty();
        }
    }

    @Override
    public BlockConnectionLayout getInputLayout() {
        return inputLayout;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new SatelliteStationBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return PhonosUtil.blockEntityTicker(type, PhonosBlocks.SATELLITE_STATION_ENTITY);
    }
}
