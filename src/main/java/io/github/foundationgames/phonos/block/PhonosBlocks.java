package io.github.foundationgames.phonos.block;

import io.github.foundationgames.phonos.Phonos;
import io.github.foundationgames.phonos.block.entity.*;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public class PhonosBlocks {
    public static final Block LOUDSPEAKER = register(new LoudspeakerBlock(FabricBlockSettings.copy(Blocks.NOTE_BLOCK)), "loudspeaker");
    public static final Block ELECTRONIC_NOTE_BLOCK = register(new ElectronicNoteBlock(FabricBlockSettings.copy(Blocks.NOTE_BLOCK)), "electronic_note_block");
    public static final Block ELECTRONIC_JUKEBOX = register(new ElectronicJukeboxBlock(FabricBlockSettings.copy(Blocks.JUKEBOX)), "electronic_jukebox");
    public static final Block CONNECTION_HUB = register(new ConnectionHubBlock(FabricBlockSettings.copy(Blocks.OAK_PLANKS)), "connection_hub");
    public static final Block RADIO_TRANSCEIVER = register(new RadioTransceiverBlock(FabricBlockSettings.copy(Blocks.OAK_SLAB)), "radio_transceiver");
    public static final Block SATELLITE_RECEIVER = register(new SatelliteReceiverBlock(FabricBlockSettings.copy(Blocks.OAK_SLAB)), "satellite_receiver");
    public static final Block RADIO_LOUDSPEAKER = register(new RadioLoudspeakerBlock(FabricBlockSettings.copy(Blocks.NOTE_BLOCK)), "radio_loudspeaker");
    public static final Block SATELLITE_STATION = register(new SatelliteStationBlock(FabricBlockSettings.copy(Blocks.OAK_SLAB)), "satellite_station");
    public static final Block AUDIO_SWITCH = register(new AudioSwitchBlock(FabricBlockSettings.copy(Blocks.OAK_SLAB)), "audio_switch");
    public static final Block ENDER_MUSIC_BOX = register(new EnderMusicBoxBlock(FabricBlockSettings.copy(Blocks.NOTE_BLOCK)), "ender_music_box");
    public static final Block MICROPHONE_BASE = register(new MicrophoneBaseBlock(FabricBlockSettings.copy(Blocks.OAK_PLANKS).nonOpaque()), "microphone_base");
    public static final Block WIRELESS_MICROPHONE_BASE = register(new WirelessMicrophoneBaseBlock(FabricBlockSettings.copy(Blocks.OAK_PLANKS).nonOpaque()), "wireless_microphone_base");

    public static BlockEntityType<ElectronicNoteBlockEntity> ELECTRONIC_NOTE_BLOCK_ENTITY = Registry.register(
            Registries.BLOCK_ENTITY_TYPE, Phonos.id("electronic_note_block"),
            BlockEntityType.Builder.create(ElectronicNoteBlockEntity::new, ELECTRONIC_NOTE_BLOCK).build(null));
    public static BlockEntityType<ElectronicJukeboxBlockEntity> ELECTRONIC_JUKEBOX_ENTITY = Registry.register(
            Registries.BLOCK_ENTITY_TYPE, Phonos.id("electronic_jukebox"),
            BlockEntityType.Builder.create(ElectronicJukeboxBlockEntity::new, ELECTRONIC_JUKEBOX).build(null));
    public static BlockEntityType<ConnectionHubBlockEntity> CONNECTION_HUB_ENTITY = Registry.register(
            Registries.BLOCK_ENTITY_TYPE, Phonos.id("connection_hub"),
            BlockEntityType.Builder.create(ConnectionHubBlockEntity::new, CONNECTION_HUB).build(null));
    public static BlockEntityType<RadioTransceiverBlockEntity> RADIO_TRANSCEIVER_ENTITY = Registry.register(
        Registries.BLOCK_ENTITY_TYPE, Phonos.id("radio_transceiver"),
        BlockEntityType.Builder.create(RadioTransceiverBlockEntity::new, RADIO_TRANSCEIVER).build(null));
    public static BlockEntityType<SatelliteReceiverBlockEntity> SATELLITE_RECEIVER_ENTITY = Registry.register(
        Registries.BLOCK_ENTITY_TYPE, Phonos.id("radio_receiver"),
        BlockEntityType.Builder.create(SatelliteReceiverBlockEntity::new, SATELLITE_RECEIVER).build(null));
    public static BlockEntityType<RadioLoudspeakerBlockEntity> RADIO_LOUDSPEAKER_ENTITY = Registry.register(
            Registries.BLOCK_ENTITY_TYPE, Phonos.id("radio_loudspeaker"),
            BlockEntityType.Builder.create(RadioLoudspeakerBlockEntity::new, RADIO_LOUDSPEAKER).build(null));
    public static BlockEntityType<SatelliteStationBlockEntity> SATELLITE_STATION_ENTITY = Registry.register(
            Registries.BLOCK_ENTITY_TYPE, Phonos.id("satellite_station"),
            BlockEntityType.Builder.create(SatelliteStationBlockEntity::new, SATELLITE_STATION).build(null));
    public static BlockEntityType<AudioSwitchBlockEntity> AUDIO_SWITCH_ENTITY = Registry.register(
            Registries.BLOCK_ENTITY_TYPE, Phonos.id("audio_switch"),
            BlockEntityType.Builder.create(AudioSwitchBlockEntity::new, PhonosBlocks.AUDIO_SWITCH).build(null));
    public static BlockEntityType<EnderMusicBoxBlockEntity> ENDER_MUSIC_BOX_ENTITY = Registry.register(
            Registries.BLOCK_ENTITY_TYPE, Phonos.id("ender_music_box"),
            BlockEntityType.Builder.create(EnderMusicBoxBlockEntity::new, PhonosBlocks.ENDER_MUSIC_BOX).build(null));
    public static BlockEntityType<MicrophoneBaseBlockEntity> MICROPHONE_BASE_ENTITY = Registry.register(
            Registries.BLOCK_ENTITY_TYPE, Phonos.id("microphone_base"),
            BlockEntityType.Builder.create(MicrophoneBaseBlockEntity::new, MICROPHONE_BASE).build(null));
    public static BlockEntityType<WirelessMicrophoneBaseBlockEntity> WIRELESS_MICROPHONE_BASE_ENTITY = Registry.register(
            Registries.BLOCK_ENTITY_TYPE, Phonos.id("wireless_microphone_base"),
            BlockEntityType.Builder.create(WirelessMicrophoneBaseBlockEntity::new, WIRELESS_MICROPHONE_BASE).build(null));

    private static Block register(Block block, String name) {
        var item = Registry.register(Registries.ITEM, Phonos.id(name), new BlockItem(block, new Item.Settings()));
        Phonos.PHONOS_ITEMS.queue(item);
        return Registry.register(Registries.BLOCK, Phonos.id(name), block);
    }

    public static void init() {
    }
}
