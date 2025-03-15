package io.github.foundationgames.phonos.item;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Unit;

public interface GlowableItem {
    default void setGlowing(ItemStack stack, boolean glowing) {
        if (glowing) {
            stack.set(PhonosDataComponents.GLOWING, Unit.INSTANCE);
        } else {
            stack.remove(PhonosDataComponents.GLOWING);
        }
    }

    default boolean isGlowing(ItemStack stack) {
        return stack.contains(PhonosDataComponents.GLOWING);
    }
}
