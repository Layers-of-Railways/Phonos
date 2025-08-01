package io.github.foundationgames.phonos.item;

import io.github.foundationgames.phonos.Phonos;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.DyeColor;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Function;

public class PhonosItems {
    public static final Item SATELLITE = register("satellite", Item::new);
    public static final AudioCableItem AUDIO_CABLE = register("audio_cable", AudioCableItem.create(null));
    public static final AudioCableItem WHITE_AUDIO_CABLE = register("white_audio_cable", AudioCableItem.create(DyeColor.WHITE));
    public static final AudioCableItem ORANGE_AUDIO_CABLE = register("orange_audio_cable", AudioCableItem.create(DyeColor.ORANGE));
    public static final AudioCableItem MAGENTA_AUDIO_CABLE = register("magenta_audio_cable", AudioCableItem.create(DyeColor.MAGENTA));
    public static final AudioCableItem LIGHT_BLUE_AUDIO_CABLE = register("light_blue_audio_cable", AudioCableItem.create(DyeColor.LIGHT_BLUE));
    public static final AudioCableItem YELLOW_AUDIO_CABLE = register("yellow_audio_cable", AudioCableItem.create(DyeColor.YELLOW));
    public static final AudioCableItem LIME_AUDIO_CABLE = register("lime_audio_cable", AudioCableItem.create(DyeColor.LIME));
    public static final AudioCableItem PINK_AUDIO_CABLE = register("pink_audio_cable", AudioCableItem.create(DyeColor.PINK));
    public static final AudioCableItem GRAY_AUDIO_CABLE = register("gray_audio_cable", AudioCableItem.create(DyeColor.GRAY));
    public static final AudioCableItem LIGHT_GRAY_AUDIO_CABLE = register("light_gray_audio_cable", AudioCableItem.create(DyeColor.LIGHT_GRAY));
    public static final AudioCableItem CYAN_AUDIO_CABLE = register("cyan_audio_cable", AudioCableItem.create(DyeColor.CYAN));
    public static final AudioCableItem PURPLE_AUDIO_CABLE = register("purple_audio_cable", AudioCableItem.create(DyeColor.PURPLE));
    public static final AudioCableItem BLUE_AUDIO_CABLE = register("blue_audio_cable", AudioCableItem.create(DyeColor.BLUE));
    public static final AudioCableItem BROWN_AUDIO_CABLE = register("brown_audio_cable", AudioCableItem.create(DyeColor.BROWN));
    public static final AudioCableItem GREEN_AUDIO_CABLE = register("green_audio_cable", AudioCableItem.create(DyeColor.GREEN));
    public static final AudioCableItem RED_AUDIO_CABLE = register("red_audio_cable", AudioCableItem.create(DyeColor.RED));
    public static final AudioCableItem BLACK_AUDIO_CABLE = register("black_audio_cable", AudioCableItem.create(DyeColor.BLACK));
    public static final Item HEADSET = register("headset", HeadsetItem::new, new Item.Settings().maxCount(1));
    public static final Item PORTABLE_RADIO = register("portable_radio", PortableRadioItem::new, new Item.Settings().maxCount(1));
    public static final Item PORTABLE_SATELLITE_RADIO = register("portable_satellite_radio", PortableSatelliteRadioItem::new, new Item.Settings().maxCount(1));
    public static final Item PORTABLE_RECORD_PLAYER = register("portable_record_player", PortableRecordPlayerItem::new, new Item.Settings().maxCount(1));

    public static final AudioCableItem[] ALL_AUDIO_CABLES = new AudioCableItem[] {
        AUDIO_CABLE,
        RED_AUDIO_CABLE,
        ORANGE_AUDIO_CABLE,
        YELLOW_AUDIO_CABLE,
        LIME_AUDIO_CABLE,
        GREEN_AUDIO_CABLE,
        CYAN_AUDIO_CABLE,
        LIGHT_BLUE_AUDIO_CABLE,
        BLUE_AUDIO_CABLE,
        PURPLE_AUDIO_CABLE,
        MAGENTA_AUDIO_CABLE,
        PINK_AUDIO_CABLE,
        WHITE_AUDIO_CABLE,
        LIGHT_GRAY_AUDIO_CABLE,
        GRAY_AUDIO_CABLE,
        BLACK_AUDIO_CABLE,
        BROWN_AUDIO_CABLE,
    };

    @ApiStatus.Internal
    public static <T extends Item> T register(String name, Function<Item.Settings, T> factory) {
        return register(name, factory, new Item.Settings());
    }

    @ApiStatus.Internal
    public static <T extends Item> T register(String name, Function<Item.Settings, T> factory, Item.Settings settings) {
        var item = factory.apply(settings);
        var entry = Registry.register(Registries.ITEM, Phonos.id(name), item);
        Phonos.PHONOS_ITEMS.queue(entry);
        return entry;
    }

    public static void init() {
    }

    public static class Tags {
        public static final TagKey<Item> AUDIO_CABLES = phonosTag("audio_cables");

        @SuppressWarnings("SameParameterValue")
        private static TagKey<Item> phonosTag(String name) {
            return TagKey.of(RegistryKeys.ITEM, Phonos.id(name));
        }
    }
}
