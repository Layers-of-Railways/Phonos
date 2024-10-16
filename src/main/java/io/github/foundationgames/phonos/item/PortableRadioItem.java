package io.github.foundationgames.phonos.item;

import io.github.foundationgames.phonos.radio.RadioStorage;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PortableRadioItem extends Item implements SoundEmitterItem {
    public final boolean isSatellite;

    public PortableRadioItem(Settings settings, boolean isSatellite) {
        super(settings);
        this.isSatellite = isSatellite;
    }

    private int getChannelCount() {
        return isSatellite ? RadioStorage.SATELLITE_CHANNEL_COUNT : RadioStorage.RADIO_CHANNEL_COUNT;
    }

    private int transformChannel(int channel) {
        return isSatellite ? RadioStorage.toSatelliteBand(channel) : channel;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (!world.isClient()) {
            var stack = user.getStackInHand(hand);

            int channel = Math.floorMod(getChannel(stack) + (user.isSneaking() ? -1 : 1), getChannelCount());
            this.setChannel(stack, channel);
            user.sendMessage(Text.translatable("tooltip.phonos.item.channel", channel)
                .formatted(isSatellite ? Formatting.BLUE : Formatting.RED), true);

            return TypedActionResult.consume(stack);
        }

        return super.use(world, user, hand);
    }

    public int getChannel(ItemStack stack) {
        if (stack.hasNbt() && stack.getNbt().contains("channel")) {
            return stack.getNbt().getInt("channel");
        }

        return 0;
    }

    public void setChannel(ItemStack stack, int channel) {
        stack.getOrCreateNbt().putInt("channel", channel);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);

        tooltip.add(TOOLTIP_HINT);
        tooltip.add(Text.translatable("tooltip.phonos.item.channel", getChannel(stack)).formatted(Formatting.RED));
    }

    @Override
    public long getParentEmitter(ItemStack stack) {
        return RadioStorage.RADIO_EMITTERS.getLong(transformChannel(MathHelper.clamp(getChannel(stack), 0, getChannelCount())));
    }
}
