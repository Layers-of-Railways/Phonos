package io.github.foundationgames.phonos.block;

import io.github.foundationgames.phonos.Phonos;
import io.github.foundationgames.phonos.block.entity.*;
import io.github.foundationgames.phonos.item.PhonosItems;
import net.minecraft.block.AbstractBlock.Settings;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;

import java.util.function.Function;

public class PhonosBlocks {
    public static final Block LOUDSPEAKER = register("loudspeaker", LoudspeakerBlock::new, Settings.copy(Blocks.NOTE_BLOCK));
    public static final Block ELECTRONIC_NOTE_BLOCK = register("electronic_note_block", ElectronicNoteBlock::new, Settings.copy(Blocks.NOTE_BLOCK));
    public static final Block ELECTRONIC_JUKEBOX = register("electronic_jukebox", ElectronicJukeboxBlock::new, Settings.copy(Blocks.JUKEBOX));
    public static final Block CONNECTION_HUB = register("connection_hub", ConnectionHubBlock::new, Settings.copy(Blocks.OAK_PLANKS));
    public static final Block RADIO_TRANSCEIVER = register("radio_transceiver", RadioTransceiverBlock::new, Settings.copy(Blocks.OAK_SLAB));
    public static final Block SATELLITE_RECEIVER = register("satellite_receiver", SatelliteReceiverBlock::new, Settings.copy(Blocks.OAK_SLAB));
    public static final Block RADIO_LOUDSPEAKER = register("radio_loudspeaker", RadioLoudspeakerBlock::new, Settings.copy(Blocks.NOTE_BLOCK));
    public static final Block SATELLITE_STATION = register("satellite_station", SatelliteStationBlock::new, Settings.copy(Blocks.OAK_SLAB));
    public static final Block AUDIO_SWITCH = register("audio_switch", AudioSwitchBlock::new, Settings.copy(Blocks.OAK_SLAB));
    public static final Block ENDER_MUSIC_BOX = register("ender_music_box", EnderMusicBoxBlock::new, Settings.copy(Blocks.NOTE_BLOCK));
    public static final Block MICROPHONE_BASE = register("microphone_base", MicrophoneBaseBlock::new, Settings.copy(Blocks.OAK_PLANKS).nonOpaque());
    public static final Block WIRELESS_MICROPHONE_BASE = register("wireless_microphone_base", WirelessMicrophoneBaseBlock::new, Settings.copy(Blocks.OAK_PLANKS).nonOpaque());

    public static BlockEntityType<ElectronicNoteBlockEntity> ELECTRONIC_NOTE_BLOCK_ENTITY = register("electronic_note_block", ElectronicNoteBlockEntity::new, ELECTRONIC_NOTE_BLOCK);
    public static BlockEntityType<ElectronicJukeboxBlockEntity> ELECTRONIC_JUKEBOX_ENTITY = register("electronic_jukebox", ElectronicJukeboxBlockEntity::new, ELECTRONIC_JUKEBOX);
    public static BlockEntityType<ConnectionHubBlockEntity> CONNECTION_HUB_ENTITY = register("connection_hub", ConnectionHubBlockEntity::new, CONNECTION_HUB);
    public static BlockEntityType<RadioTransceiverBlockEntity> RADIO_TRANSCEIVER_ENTITY = register("radio_transceiver", RadioTransceiverBlockEntity::new, RADIO_TRANSCEIVER);
    public static BlockEntityType<SatelliteReceiverBlockEntity> SATELLITE_RECEIVER_ENTITY = register("radio_receiver", SatelliteReceiverBlockEntity::new, SATELLITE_RECEIVER);
    public static BlockEntityType<RadioLoudspeakerBlockEntity> RADIO_LOUDSPEAKER_ENTITY = register("radio_loudspeaker", RadioLoudspeakerBlockEntity::new, RADIO_LOUDSPEAKER);
    public static BlockEntityType<SatelliteStationBlockEntity> SATELLITE_STATION_ENTITY = register("satellite_station", SatelliteStationBlockEntity::new, SATELLITE_STATION);
    public static BlockEntityType<AudioSwitchBlockEntity> AUDIO_SWITCH_ENTITY = register("audio_switch", AudioSwitchBlockEntity::new, AUDIO_SWITCH);
    public static BlockEntityType<EnderMusicBoxBlockEntity> ENDER_MUSIC_BOX_ENTITY = register("ender_music_box", EnderMusicBoxBlockEntity::new, ENDER_MUSIC_BOX);
    public static BlockEntityType<MicrophoneBaseBlockEntity> MICROPHONE_BASE_ENTITY = register("microphone_base", MicrophoneBaseBlockEntity::new, MICROPHONE_BASE);
    public static BlockEntityType<WirelessMicrophoneBaseBlockEntity> WIRELESS_MICROPHONE_BASE_ENTITY = register("wireless_microphone_base", WirelessMicrophoneBaseBlockEntity::new, WIRELESS_MICROPHONE_BASE);

    private static <T extends Block> T register(String name, Function<Settings, T> factory, Settings settings) {
        var block = factory.apply(settings);

        PhonosItems.register(name, itemSettings -> new BlockItem(block, itemSettings));

        return Registry.register(Registries.BLOCK, Phonos.id(name), block);
    }

    private static <T extends BlockEntity> BlockEntityType<T> register(String name, BlockEntityType.BlockEntityFactory<T> factory, Block... blocks) {
        return Registry.register(
            Registries.BLOCK_ENTITY_TYPE, Phonos.id(name),
            BlockEntityType.Builder.create(factory, blocks).build(null)
        );
    }

    public static void init() {
    }

    public static class Tags {
        public static final TagKey<Block> TRANSMISSION_TOWERS = phonosTag("transmission_towers");

        @SuppressWarnings("SameParameterValue")
        private static TagKey<Block> phonosTag(String name) {
            return TagKey.of(RegistryKeys.BLOCK, Phonos.id(name));
        }
    }
}
