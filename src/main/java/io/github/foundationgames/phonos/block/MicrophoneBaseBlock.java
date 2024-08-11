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
import org.jetbrains.annotations.Nullable;

public class MicrophoneBaseBlock extends HorizontalFacingBlock implements BlockEntityProvider {

    private static final VoxelShaper SHAPE = VoxelShaper.Builder.create(5, 0, 5, 11, 6, 11)
        .add(7, 6, 7, 9, 12, 9)
        .add(6, 11, 6, 10, 14, 11)
        .forHorizontal(Direction.NORTH);

    public MicrophoneBaseBlock(Settings settings) {
        super(settings);

        setDefaultState(getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder.add(FACING));
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

                if (be.isPlaying()) {
                    be.stop();
                    return ActionResult.SUCCESS;
                } else if (player instanceof ServerPlayerEntity serverPlayer) {
                    if (player.getMainHandStack().isEmpty())
                        be.start(serverPlayer);
                    return ActionResult.SUCCESS;
                }
            }
        }

        return super.onUse(state, world, pos, player, hand, hit);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!newState.isOf(this) && world.getBlockEntity(pos) instanceof MicrophoneBaseBlockEntity be) {
            be.onDestroyed();
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
