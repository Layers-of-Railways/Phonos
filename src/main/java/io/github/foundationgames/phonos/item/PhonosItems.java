package io.github.foundationgames.phonos.item;

import io.github.foundationgames.phonos.Phonos;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.DyeColor;

public class PhonosItems {
    public static final Item SATELLITE = register("satellite", new Item(new Item.Settings()));
    public static final AudioCableItem AUDIO_CABLE = register("audio_cable", new AudioCableItem(null, new Item.Settings()));
    public static final AudioCableItem WHITE_AUDIO_CABLE = register("white_audio_cable", new AudioCableItem(DyeColor.WHITE, new Item.Settings()));
    public static final AudioCableItem ORANGE_AUDIO_CABLE = register("orange_audio_cable", new AudioCableItem(DyeColor.ORANGE, new Item.Settings()));
    public static final AudioCableItem MAGENTA_AUDIO_CABLE = register("magenta_audio_cable", new AudioCableItem(DyeColor.MAGENTA, new Item.Settings()));
    public static final AudioCableItem LIGHT_BLUE_AUDIO_CABLE = register("light_blue_audio_cable", new AudioCableItem(DyeColor.LIGHT_BLUE, new Item.Settings()));
    public static final AudioCableItem YELLOW_AUDIO_CABLE = register("yellow_audio_cable", new AudioCableItem(DyeColor.YELLOW, new Item.Settings()));
    public static final AudioCableItem LIME_AUDIO_CABLE = register("lime_audio_cable", new AudioCableItem(DyeColor.LIME, new Item.Settings()));
    public static final AudioCableItem PINK_AUDIO_CABLE = register("pink_audio_cable", new AudioCableItem(DyeColor.PINK, new Item.Settings()));
    public static final AudioCableItem GRAY_AUDIO_CABLE = register("gray_audio_cable", new AudioCableItem(DyeColor.GRAY, new Item.Settings()));
    public static final AudioCableItem LIGHT_GRAY_AUDIO_CABLE = register("light_gray_audio_cable", new AudioCableItem(DyeColor.LIGHT_GRAY, new Item.Settings()));
    public static final AudioCableItem CYAN_AUDIO_CABLE = register("cyan_audio_cable", new AudioCableItem(DyeColor.CYAN, new Item.Settings()));
    public static final AudioCableItem PURPLE_AUDIO_CABLE = register("purple_audio_cable", new AudioCableItem(DyeColor.PURPLE, new Item.Settings()));
    public static final AudioCableItem BLUE_AUDIO_CABLE = register("blue_audio_cable", new AudioCableItem(DyeColor.BLUE, new Item.Settings()));
    public static final AudioCableItem BROWN_AUDIO_CABLE = register("brown_audio_cable", new AudioCableItem(DyeColor.BROWN, new Item.Settings()));
    public static final AudioCableItem GREEN_AUDIO_CABLE = register("green_audio_cable", new AudioCableItem(DyeColor.GREEN, new Item.Settings()));
    public static final AudioCableItem RED_AUDIO_CABLE = register("red_audio_cable", new AudioCableItem(DyeColor.RED, new Item.Settings()));
    public static final AudioCableItem BLACK_AUDIO_CABLE = register("black_audio_cable", new AudioCableItem(DyeColor.BLACK, new Item.Settings()));
    public static final Item HEADSET = register("headset", new HeadsetItem(new Item.Settings().maxCount(1)));
    public static final Item PORTABLE_RADIO = register("portable_radio", new PortableRadioItem(new Item.Settings().maxCount(1)));
    public static final Item PORTABLE_SATELLITE_RADIO = register("portable_satellite_radio", new PortableSatelliteRadioItem(new Item.Settings().maxCount(1)));
    public static final Item PORTABLE_RECORD_PLAYER = register("portable_record_player", new PortableRecordPlayerItem(new Item.Settings().maxCount(1)));

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

    public static <T extends Item> T register(String name, T item) {
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
