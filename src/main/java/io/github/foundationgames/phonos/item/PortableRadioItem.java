package io.github.foundationgames.phonos.item;

import io.github.foundationgames.phonos.radio.RadioStorage;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.List;

public class PortableRadioItem extends Item implements SoundEmitterItem {

    public PortableRadioItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (!world.isClient()) {
            var stack = user.getStackInHand(hand);

            int channel = Math.floorMod(getChannel(stack) + (user.isSneaking() ? -1 : 1), RadioStorage.CHANNEL_COUNT);
            this.setChannel(stack, channel);
            user.sendMessage(Text.translatable("tooltip.phonos.item.channel", channel)
                .formatted(Formatting.RED), true);

            return TypedActionResult.consume(stack);
        }

        return super.use(world, user, hand);
    }

    public int getChannel(ItemStack stack) {
        return stack.getOrDefault(PhonosDataComponents.CHANNEL, 0);
    }

    public void setChannel(ItemStack stack, int channel) {
        stack.set(PhonosDataComponents.CHANNEL, channel);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);

        tooltip.add(TOOLTIP_HINT);
        tooltip.add(Text.translatable("tooltip.phonos.item.channel", getChannel(stack)).formatted(Formatting.RED));
    }

    @Override
    public long getParentEmitter(ItemStack stack) {
        return RadioStorage.RADIO_EMITTERS.getLong(MathHelper.clamp(getChannel(stack), 0, RadioStorage.CHANNEL_COUNT));
    }
}
