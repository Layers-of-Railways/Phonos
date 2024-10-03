package io.github.foundationgames.phonos.block;

import io.github.foundationgames.phonos.block.entity.MicrophoneBaseBlockEntity;
import io.github.foundationgames.phonos.util.PhonosUtil;
import io.github.foundationgames.phonos.util.VoxelShaper;
import io.github.foundationgames.phonos.util.compat.PhonosVoicechatProxy;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

public class MicrophoneBaseBlock extends HorizontalFacingBlock implements BlockEntityProvider {

    public static final BooleanProperty POWERED = Properties.POWERED;

    private static final VoxelShaper SHAPE = VoxelShaper.Builder.create(5, 0, 5, 11, 6, 11)
        .add(7, 6, 7, 9, 12, 9)
        .add(6, 11, 6, 10, 14, 11)
        .forHorizontal(Direction.NORTH);

    public MicrophoneBaseBlock(Settings settings) {
        super(settings);

        setDefaultState(getDefaultState()
            .with(FACING, Direction.NORTH)
            .with(POWERED, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder.add(FACING, POWERED));
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE.get(state.get(FACING));
    }

    @ApiStatus.Internal
    public void updatePower(World world, BlockPos pos) {
        boolean poweredTarget;
        if (world.getBlockEntity(pos) instanceof MicrophoneBaseBlockEntity be) {
            poweredTarget = be.isPlaying();
        } else {
            poweredTarget = false;
        }

        BlockState state = world.getBlockState(pos);

        boolean powered = state.get(POWERED);

        if (powered ^ poweredTarget) {
            world.setBlockState(pos, state.with(POWERED, poweredTarget), Block.NOTIFY_LISTENERS);
            updateNeighbors(world, pos);
        }
    }

    protected void updateNeighbors(World world, BlockPos pos) {
        BlockPos blockPos = pos.down();
        world.updateNeighbor(blockPos, this, pos);
        world.updateNeighborsExcept(blockPos, this, Direction.UP);
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean emitsRedstonePower(BlockState state) {
        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getStrongRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        return state.getWeakRedstonePower(world, pos, direction);
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        if (state.get(POWERED) && direction == Direction.UP) {
            return 15;
        }
        return 0;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        if (state.isOf(oldState.getBlock())) {
            return;
        }
        if (!world.isClient() && state.get(POWERED)) {
            BlockState blockState = state.with(POWERED, false);
            world.setBlockState(pos, blockState, Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
            this.updateNeighbors(world, pos);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!PhonosVoicechatProxy.isLoaded()) {
            if (player instanceof ServerPlayerEntity serverPlayer) {
                serverPlayer.sendMessageToClient(Text.translatable("block.phonos.microphone_base.voicechat_not_installed")
                    .setStyle(Style.EMPTY.withColor(Formatting.RED)), true);
            }
            return ActionResult.FAIL;
        }

        if (player.canModifyBlocks()) {
            if (PhonosUtil.holdingAudioCable(player)) {
                return ActionResult.PASS;
            }

            if (world.isClient()) {
                return ActionResult.SUCCESS;
            }

            if (world.getBlockEntity(pos) instanceof MicrophoneBaseBlockEntity be) {
                if (hit.getSide() != Direction.UP) {
                    var relHitPos = hit.getPos().subtract(pos.toCenterPos());
                    if (relHitPos.getY() < -2/16. && !be.isSpeakingPlayer(player)) {
                        if (be.outputs.tryRemoveConnection(world, hit, !player.isCreative())) {
                            be.sync();
                            return ActionResult.SUCCESS;
                        }
                    }
                }

                if (player instanceof ServerPlayerEntity serverPlayer) {
                    if (be.isPlaying()) {
                        if (be.isSpeakingPlayer(serverPlayer)) {
                            be.stop();
                            updatePower(world, pos);
                            return ActionResult.SUCCESS;
                        } else {
                            serverPlayer.sendMessageToClient(Text.translatable("block.phonos.microphone_base.already_playing")
                                .setStyle(Style.EMPTY.withColor(Formatting.RED)), true);
                            return ActionResult.FAIL;
                        }
                    } else {
                        if (be.canStart(serverPlayer))
                            be.start(serverPlayer);
                        updatePower(world, pos);
                        return ActionResult.SUCCESS;
                    }
                }
            }
        }

        return super.onUse(state, world, pos, player, hand, hit);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (newState.isOf(this)) {
            return;
        }

        if (world.getBlockEntity(pos) instanceof MicrophoneBaseBlockEntity be) {
            be.onDestroyed();
        }

        if (!world.isClient && state.get(POWERED)) {
            this.updateNeighbors(world, pos);
        }

        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new MicrophoneBaseBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return PhonosUtil.blockEntityTicker(type, PhonosBlocks.MICROPHONE_BASE_ENTITY);
    }
}
