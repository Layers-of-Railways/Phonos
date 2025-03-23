package io.github.foundationgames.phonos.item;

import io.github.foundationgames.phonos.block.entity.SatelliteStationBlockEntity;
import io.github.foundationgames.phonos.client.screen.ConfigurePortableSatelliteRadioScreen;
import io.github.foundationgames.phonos.satellite_radio.SatelliteRadioStorage;
import io.github.foundationgames.phonos.util.PhonosUtil;
import io.github.foundationgames.phonos.util.UniqueId;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

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
        NbtCompound nbt = stack.getNbt();
        if (nbt != null) {
            if (nbt.contains("channel", NbtElement.INT_TYPE)) {
                String channel = "N" + nbt.getInt("channel");
                String migrated = SatelliteRadioStorage.convertLegacyChannel(channel);
                if (migrated != null) {
                    nbt.remove("channel");
                    nbt.putString("channel", migrated);
                    return migrated;
                } else {
                    return channel;
                }
            } else if (nbt.contains("channel", NbtElement.STRING_TYPE)) {
                return nbt.getString("channel");
            }
        }

        return "";
    }

    public void setChannel(ItemStack stack, String channel) {
        stack.getOrCreateNbt().putString("channel", channel);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);

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

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        String channel = getChannel(stack);
        if (world != null && SatelliteStationBlockEntity.validateChannel(channel)) {
            SatelliteRadioStorage.getInstance(world).keepAlive(channel);
        }
    }
}
