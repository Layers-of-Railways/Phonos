package io.github.foundationgames.phonos.block;

import io.github.foundationgames.phonos.block.entity.RadioTransceiverBlockEntity;
import io.github.foundationgames.phonos.radio.RadioStorage;
import io.github.foundationgames.phonos.util.PhonosUtil;
import io.github.foundationgames.phonos.world.RadarPoints;
import io.github.foundationgames.phonos.world.sound.block.BlockConnectionLayout;
import io.github.foundationgames.phonos.world.sound.block.InputBlock;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class RadioTransceiverBlock extends RadioReceiverBlock implements BlockEntityProvider, InputBlock {
    public final BlockConnectionLayout inputLayout = new BlockConnectionLayout()
            .addPoint(-4.5, -4.5, 8, Direction.SOUTH)
            .addPoint(4.5, -4.5, 8, Direction.SOUTH);

    public RadioTransceiverBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected ActionResult onUseFace(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        var stack = player.getStackInHand(hand);

        int inc = player.isSneaking() ? -1 : 1;
        if (world.getBlockEntity(pos) instanceof RadioTransceiverBlockEntity be) {
            int goal = be.getChannel() + inc;

            if (stack.isOf(Items.NAME_TAG) && stack.hasCustomName()) {
                String customName = stack.getName().getString();
                if (customName.matches("\\d+")) {
                    goal = Integer.parseInt(customName);
                    goal = MathHelper.clamp(goal, 0, RadioStorage.CHANNEL_COUNT -1);
                }
            }

            be.setAndUpdateChannel(goal);
            be.markDirty();
        }

        return ActionResult.CONSUME;
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
        if (world.getBlockEntity(pos) instanceof RadioTransceiverBlockEntity be) {
            inputIndex = MathHelper.clamp(inputIndex, 0, be.inputs.length - 1);

            return be.inputs[inputIndex];
        }

        return false;
    }

    @Override
    public void setInputPluggedIn(int inputIndex, boolean pluggedIn, BlockState state, World world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof RadioTransceiverBlockEntity be) {
            inputIndex = MathHelper.clamp(inputIndex, 0, be.inputs.length - 1);
            be.inputs[inputIndex] = pluggedIn;

            if (world instanceof ServerWorld sWorld) {
                if (pluggedIn) {
                    RadarPoints.get(sWorld).add(be.getChannel(), pos);
                } else {
                    boolean remove = true;
                    for (boolean in : be.inputs) if (in) {
                        remove = false;
                        break;
                    }

                    if (remove) {
                        RadarPoints.get(sWorld).remove(be.getChannel(), pos);
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
        return new RadioTransceiverBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return PhonosUtil.blockEntityTicker(type, PhonosBlocks.RADIO_TRANSCEIVER_ENTITY);
    }

    @Override
    public Direction getRotation(BlockState state) {
        return super.getRotation(state);
    }
}
