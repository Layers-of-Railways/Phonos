package io.github.foundationgames.phonos.item;

import io.github.foundationgames.phonos.Phonos;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.Equipment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.world.World;

import java.util.List;

public class HeadsetItem extends Item implements GlowableItem, Equipment {
    public static final Text NOISE_CANCELLING = Text.translatable("tooltip.phonos.item.noise_cancelling").formatted(Formatting.YELLOW);
    public static final Text GLOWING = Text.translatable("tooltip.phonos.item.glowing").formatted(Formatting.GRAY, Formatting.ITALIC);

    public static final Identifier DEFAULT_TEXTURE = Phonos.id("textures/entity/headset.png");
    public static final Identifier NOISE_CANCELLING_TEXTURE = Phonos.id("textures/entity/headset_noise_cancelling.png");

    public HeadsetItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return this.equipAndSwap(this, world, user, hand);
    }

    public boolean isNoiseCancelling(ItemStack stack) {
        return stack.contains(PhonosDataComponents.NOISE_CANCELLING);
    }

    public void setNoiseCancelling(ItemStack stack, boolean noiseCancelling) {
        if (noiseCancelling) {
            stack.set(PhonosDataComponents.NOISE_CANCELLING, Unit.INSTANCE);
        } else {
            stack.remove(PhonosDataComponents.NOISE_CANCELLING);
        }
    }

    @Override
    public boolean onClicked(ItemStack stack, ItemStack otherStack, Slot slot, ClickType clickType, PlayerEntity player, StackReference cursorStackReference) {
        if (clickType == ClickType.RIGHT && otherStack.isEmpty()) {
            setNoiseCancelling(stack, !isNoiseCancelling(stack));

            return true;
        }

        return super.onClicked(stack, otherStack, slot, clickType, player, cursorStackReference);
    }

    @Override
    public EquipmentSlot getSlotType() {
        return EquipmentSlot.HEAD;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);

        if (isNoiseCancelling(stack)) {
            tooltip.add(NOISE_CANCELLING);
        }

        if (isGlowing(stack)) {
            tooltip.add(GLOWING);
        }
    }

    public Identifier getTexture(ItemStack stack) {
        return isNoiseCancelling(stack) ? NOISE_CANCELLING_TEXTURE : DEFAULT_TEXTURE;
    }

    public int getColor(ItemStack stack) {
        return DyedColorComponent.getColor(stack, DyedColorComponent.DEFAULT_COLOR);
    }
}
