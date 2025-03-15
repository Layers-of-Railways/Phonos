package io.github.foundationgames.phonos.item;

import io.github.foundationgames.phonos.client.screen.ConfigurePortableSatelliteRadioScreen;
import io.github.foundationgames.phonos.util.PhonosUtil;
import io.github.foundationgames.phonos.util.UniqueId;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

public class PortableSatelliteRadioItem extends Item implements SoundEmitterItem {

    public PortableSatelliteRadioItem(Settings settings) {
        super(settings);
    }

    @Environment(EnvType.CLIENT)
    private static void openConfigureScreen(PlayerEntity player) {
        MinecraftClient.getInstance().setScreen(new ConfigurePortableSatelliteRadioScreen(player.getInventory().selectedSlot));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient()) {
            PhonosUtil.runIfClient(() -> () -> openConfigureScreen(user));

            return TypedActionResult.consume(user.getStackInHand(hand));
        }

        return super.use(world, user, hand);
    }

    public String getChannel(ItemStack stack) {
        return stack.getOrDefault(PhonosDataComponents.SATELLITE_CHANNEL, "");
    }

    public void setChannel(ItemStack stack, String channel) {
        stack.set(PhonosDataComponents.SATELLITE_CHANNEL, channel);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);

        tooltip.add(TOOLTIP_HINT);
        String channel = getChannel(stack);
        if (channel.isEmpty()) {
            channel = "Unset";
        }
        tooltip.add(Text.translatable("tooltip.phonos.item.channel", channel).formatted(Formatting.BLUE));
    }

    @Override
    public long getParentEmitter(ItemStack stack) {
        return UniqueId.ofSatelliteChannel(getChannel(stack));
    }
}
