package io.github.foundationgames.phonos.item;

import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

public interface SoundEmitterItem {

    Text TOOLTIP_HINT = Text.translatable("tooltip.phonos.item.how_to_listen").formatted(Formatting.GRAY);

    default boolean hasParentEmitter(ItemStack stack) {
        return true;
    }

    default boolean createsEmitter(ItemStack stack) {
        return false;
    }

    long getParentEmitter(ItemStack stack);
}
