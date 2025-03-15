package io.github.foundationgames.phonos.item;

import io.github.foundationgames.phonos.Phonos;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.DyeColor;

public class PhonosItems {
    public static final Item SATELLITE = register("satellite", new Item(new Item.Settings()));
    public static final Item AUDIO_CABLE = register("audio_cable", new AudioCableItem(null, new Item.Settings()));
    public static final Item WHITE_AUDIO_CABLE = register("white_audio_cable", new AudioCableItem(DyeColor.WHITE, new Item.Settings()));
    public static final Item ORANGE_AUDIO_CABLE = register("orange_audio_cable", new AudioCableItem(DyeColor.ORANGE, new Item.Settings()));
    public static final Item MAGENTA_AUDIO_CABLE = register("magenta_audio_cable", new AudioCableItem(DyeColor.MAGENTA, new Item.Settings()));
    public static final Item LIGHT_BLUE_AUDIO_CABLE = register("light_blue_audio_cable", new AudioCableItem(DyeColor.LIGHT_BLUE, new Item.Settings()));
    public static final Item YELLOW_AUDIO_CABLE = register("yellow_audio_cable", new AudioCableItem(DyeColor.YELLOW, new Item.Settings()));
    public static final Item LIME_AUDIO_CABLE = register("lime_audio_cable", new AudioCableItem(DyeColor.LIME, new Item.Settings()));
    public static final Item PINK_AUDIO_CABLE = register("pink_audio_cable", new AudioCableItem(DyeColor.PINK, new Item.Settings()));
    public static final Item GRAY_AUDIO_CABLE = register("gray_audio_cable", new AudioCableItem(DyeColor.GRAY, new Item.Settings()));
    public static final Item LIGHT_GRAY_AUDIO_CABLE = register("light_gray_audio_cable", new AudioCableItem(DyeColor.LIGHT_GRAY, new Item.Settings()));
    public static final Item CYAN_AUDIO_CABLE = register("cyan_audio_cable", new AudioCableItem(DyeColor.CYAN, new Item.Settings()));
    public static final Item PURPLE_AUDIO_CABLE = register("purple_audio_cable", new AudioCableItem(DyeColor.PURPLE, new Item.Settings()));
    public static final Item BLUE_AUDIO_CABLE = register("blue_audio_cable", new AudioCableItem(DyeColor.BLUE, new Item.Settings()));
    public static final Item BROWN_AUDIO_CABLE = register("brown_audio_cable", new AudioCableItem(DyeColor.BROWN, new Item.Settings()));
    public static final Item GREEN_AUDIO_CABLE = register("green_audio_cable", new AudioCableItem(DyeColor.GREEN, new Item.Settings()));
    public static final Item RED_AUDIO_CABLE = register("red_audio_cable", new AudioCableItem(DyeColor.RED, new Item.Settings()));
    public static final Item BLACK_AUDIO_CABLE = register("black_audio_cable", new AudioCableItem(DyeColor.BLACK, new Item.Settings()));
    public static final Item HEADSET = register("headset", new HeadsetItem(new Item.Settings().maxCount(1)));
    public static final Item PORTABLE_RADIO = register("portable_radio", new PortableRadioItem(new Item.Settings().maxCount(1)));
    public static final Item PORTABLE_SATELLITE_RADIO = register("portable_satellite_radio", new PortableSatelliteRadioItem(new Item.Settings().maxCount(1)));
    public static final Item PORTABLE_RECORD_PLAYER = register("portable_record_player", new PortableRecordPlayerItem(new Item.Settings().maxCount(1)));

    public static final Item[] ALL_AUDIO_CABLES = new Item[] {WHITE_AUDIO_CABLE, ORANGE_AUDIO_CABLE, MAGENTA_AUDIO_CABLE,
            LIGHT_BLUE_AUDIO_CABLE, YELLOW_AUDIO_CABLE, LIME_AUDIO_CABLE, PINK_AUDIO_CABLE, GRAY_AUDIO_CABLE,
            LIGHT_GRAY_AUDIO_CABLE, CYAN_AUDIO_CABLE, PURPLE_AUDIO_CABLE, BLUE_AUDIO_CABLE, BROWN_AUDIO_CABLE, GREEN_AUDIO_CABLE,
            RED_AUDIO_CABLE, BLACK_AUDIO_CABLE, AUDIO_CABLE};

    public static <T extends Item> T register(String name, T item) {
        var entry = Registry.register(Registries.ITEM, Phonos.id(name), item);
        Phonos.PHONOS_ITEMS.queue(entry);
        return entry;
    }

    public static void init() {
    }
}
