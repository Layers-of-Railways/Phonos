package io.github.foundationgames.phonos.block;

import com.mojang.serialization.MapCodec;
import io.github.foundationgames.phonos.block.entity.ElectronicJukeboxBlockEntity;
import io.github.foundationgames.phonos.util.PhonosUtil;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.JukeboxBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

// fixme have to do separate block because ffs
public class ElectronicJukeboxBlock extends JukeboxBlock implements BlockEntityProvider {
    public static final MapCodec<JukeboxBlock> CODEC = createCodec(ElectronicJukeboxBlock::new);

    public ElectronicJukeboxBlock(Settings settings) {
        super(settings);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new ElectronicJukeboxBlockEntity(pos, state);
    }

    @Override
    public MapCodec<JukeboxBlock> getCodec() {
        return CODEC;
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        var side = hit.getSide();

        if (side.getAxis().isVertical()) {
            return super.onUse(state, world, pos, player, hit);
        }

        if (player.canModifyBlocks()) {
            if (!world.isClient() && world.getBlockEntity(pos) instanceof ElectronicJukeboxBlockEntity be) {
                if (!PhonosUtil.holdingAudioCable(player) && be.outputs.tryRemoveConnection(world, hit, !player.isCreative())) {
                    be.sync();
                    return ActionResult.SUCCESS;
                }
            }

            return ActionResult.SUCCESS;
        }

        return ActionResult.PASS;
    }

    @Override
    protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!newState.isOf(this) && world.getBlockEntity(pos) instanceof ElectronicJukeboxBlockEntity jukebox) {
            jukebox.onDestroyed();
        }

        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return PhonosUtil.blockEntityTicker(type, PhonosBlocks.ELECTRONIC_JUKEBOX_ENTITY);
    }
}
