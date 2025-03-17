package io.github.foundationgames.phonos.mixin;

import io.github.foundationgames.phonos.block.ElectronicJukeboxBlock;
import io.github.foundationgames.phonos.block.PhonosBlocks;
import io.github.foundationgames.phonos.block.entity.ElectronicJukeboxBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.JukeboxPlayableComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.stat.Stats;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(JukeboxPlayableComponent.class)
public class MixinJukeboxPlayableComponent {
    @Inject(method = "tryPlayStack", at = @At(value = "RETURN"), cancellable = true)
    private static void playInElectronicJukebox(World world, BlockPos pos, ItemStack stack, PlayerEntity player, CallbackInfoReturnable<ItemActionResult> cir) {
        JukeboxPlayableComponent jukeboxPlayableComponent = stack.get(DataComponentTypes.JUKEBOX_PLAYABLE);
        if (jukeboxPlayableComponent == null) return;
        if (cir.getReturnValue() != ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION) return;

        BlockState blockState = world.getBlockState(pos);
        if (blockState.isOf(PhonosBlocks.ELECTRONIC_JUKEBOX) && !blockState.get(ElectronicJukeboxBlock.HAS_RECORD)) {
            if (!world.isClient) {
                ItemStack itemStack = stack.splitUnlessCreative(1, player);
                if (world.getBlockEntity(pos) instanceof ElectronicJukeboxBlockEntity be) {
                    be.setStack(itemStack);
                    world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(player, blockState));
                }

                player.incrementStat(Stats.PLAY_RECORD);
            }

            cir.setReturnValue(ItemActionResult.success(world.isClient));
        }
    }
}
