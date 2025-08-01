package io.github.foundationgames.phonos.datagen.providers;

import io.github.foundationgames.phonos.Phonos;
import io.github.foundationgames.phonos.block.AudioSwitchBlock;
import io.github.foundationgames.phonos.block.PhonosBlocks;
import io.github.foundationgames.phonos.item.AudioCableItem;
import io.github.foundationgames.phonos.item.PhonosItems;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.minecraft.data.client.*;
import net.minecraft.util.Identifier;

import java.util.Optional;

import static net.minecraft.data.client.BlockStateModelGenerator.createNorthDefaultHorizontalRotationStates;
import static net.minecraft.data.client.BlockStateModelGenerator.createSingletonBlockState;
import static net.minecraft.data.client.ModelIds.getBlockModelId;
import static net.minecraft.data.client.ModelIds.getBlockSubModelId;

public class PhonosModelProvider extends FabricModelProvider {
    public PhonosModelProvider(FabricDataOutput output) {
        super(output);
    }

    private static BlockStateVariant modelVariant(Identifier id) {
        return BlockStateVariant.create().put(VariantSettings.MODEL, id);
    }

    @Override
    public void generateBlockStateModels(BlockStateModelGenerator gen) {
        final Identifier stoneBase = Phonos.id("block/stone_base");
        final Identifier speakerWood = Phonos.id("block/speaker_wood");

        // Audio Switch
        gen.registerParentedItemModel(PhonosBlocks.AUDIO_SWITCH, getBlockSubModelId(PhonosBlocks.AUDIO_SWITCH, "_straight"));
        gen.blockStateCollector.accept(
            VariantsBlockStateSupplier.create(PhonosBlocks.AUDIO_SWITCH)
                .coordinate(createNorthDefaultHorizontalRotationStates())
                .coordinate(
                    BlockStateVariantMap.create(AudioSwitchBlock.POWERED)
                        .register(false, modelVariant(getBlockSubModelId(PhonosBlocks.AUDIO_SWITCH, "_straight")))
                        .register(true, modelVariant(getBlockSubModelId(PhonosBlocks.AUDIO_SWITCH, "_cross")))
                )
        );

        // Connection Hub
        gen.blockStateCollector.accept(
            createSingletonBlockState(PhonosBlocks.CONNECTION_HUB, getBlockModelId(PhonosBlocks.CONNECTION_HUB))
                .coordinate(gen.createUpDefaultFacingVariantMap())
        );

        // Electronic Jukebox
        gen.registerSingleton(
            PhonosBlocks.ELECTRONIC_JUKEBOX,
            TextureMap
                .sideAndTop(PhonosBlocks.ELECTRONIC_JUKEBOX)
                .put(TextureKey.BOTTOM, stoneBase),
            Models.CUBE_BOTTOM_TOP
        );

        // Electronic Note Block
        gen.excludeFromSimpleItemModelGeneration(PhonosBlocks.ELECTRONIC_NOTE_BLOCK); // totally custom
        TextureMap electronicNoteBlockTextures = new TextureMap()
            .put(TextureKey.SIDE, TextureMap.getSubId(PhonosBlocks.ELECTRONIC_NOTE_BLOCK, "_side"))
            .put(TextureKey.TOP, speakerWood)
            .put(TextureKey.BOTTOM, stoneBase);
        Models.CUBE_BOTTOM_TOP.upload(
            ModelIds.getItemModelId(PhonosBlocks.ELECTRONIC_NOTE_BLOCK.asItem()),
            electronicNoteBlockTextures,
            gen.modelCollector
        );
        gen.registerSingleton(
            PhonosBlocks.ELECTRONIC_NOTE_BLOCK,
            electronicNoteBlockTextures.copyAndAdd(CustomTextureKey.OVERLAY, TextureMap.getSubId(PhonosBlocks.ELECTRONIC_NOTE_BLOCK, "_overlay")),
            CustomModels.TEMPLATE_TINTED_COLUMN
        );

        // Ender Music Box
        gen.registerSingleton(
            PhonosBlocks.ENDER_MUSIC_BOX,
            new TextureMap()
                .put(TextureKey.SIDE, TextureMap.getId(PhonosBlocks.ENDER_MUSIC_BOX))
                .put(TextureKey.TOP, TextureMap.getSubId(PhonosBlocks.ENDER_MUSIC_BOX, "_top"))
                .put(TextureKey.BOTTOM, stoneBase)
                .put(TextureKey.PARTICLE, stoneBase),
            Models.CUBE_BOTTOM_TOP
        );

        // Loudspeaker
        gen.blockStateCollector.accept(
            createSingletonBlockState(
                PhonosBlocks.LOUDSPEAKER, Models.CUBE.upload(
                    PhonosBlocks.LOUDSPEAKER,
                    new TextureMap()
                        .put(TextureKey.PARTICLE, TextureMap.getId(PhonosBlocks.LOUDSPEAKER))
                        .put(TextureKey.NORTH, TextureMap.getId(PhonosBlocks.LOUDSPEAKER))
                        .put(TextureKey.SOUTH, TextureMap.getSubId(PhonosBlocks.LOUDSPEAKER, "_back"))
                        .put(TextureKey.EAST, TextureMap.getSubId(PhonosBlocks.LOUDSPEAKER, "_east"))
                        .put(TextureKey.WEST, TextureMap.getSubId(PhonosBlocks.LOUDSPEAKER, "_west"))
                        .put(TextureKey.UP, speakerWood)
                        .put(TextureKey.DOWN, stoneBase),
                    gen.modelCollector
                ))
                .coordinate(createNorthDefaultHorizontalRotationStates())
        );

        // Microphone Base
        gen.registerParentedItemModel(PhonosBlocks.MICROPHONE_BASE, Phonos.id("block/microphone_base_reference"));
        gen.registerNorthDefaultHorizontalRotation(PhonosBlocks.MICROPHONE_BASE);

        // Radio Loudspeaker
        gen.registerNorthDefaultHorizontalRotation(PhonosBlocks.RADIO_LOUDSPEAKER);

        // Radio Transceiver
        gen.excludeFromSimpleItemModelGeneration(PhonosBlocks.RADIO_TRANSCEIVER); // totally custom
        gen.registerNorthDefaultHorizontalRotation(PhonosBlocks.RADIO_TRANSCEIVER);

        // Satellite Receiver
        gen.registerNorthDefaultHorizontalRotation(PhonosBlocks.SATELLITE_RECEIVER);

        // Satellite Station
        gen.registerNorthDefaultHorizontalRotation(PhonosBlocks.SATELLITE_STATION);

        // Wireless Microphone Base
        gen.registerNorthDefaultHorizontalRotation(PhonosBlocks.WIRELESS_MICROPHONE_BASE);
    }

    @Override
    public void generateItemModels(ItemModelGenerator gen) {
        // templates: dyed_audio_cable

        // totally custom: PhonosItems.HEADSET
        gen.register(PhonosItems.PORTABLE_RADIO, Models.GENERATED);
        // totally custom: PhonosItems.PORTABLE_RECORD_PLAYER
        gen.register(PhonosItems.PORTABLE_SATELLITE_RADIO, Models.GENERATED);
        gen.register(PhonosItems.SATELLITE, Models.GENERATED);

        for (AudioCableItem cable : PhonosItems.ALL_AUDIO_CABLES) {
            if (cable.color != null) {
                gen.register(cable, CustomModels.DYED_AUDIO_CABLE);
            } else {
                gen.register(cable, Models.GENERATED);
            }
        }
    }

    private interface CustomTextureKey {
        TextureKey OVERLAY = TextureKey.of("overlay");
    }

    private interface CustomModels {
        Model DYED_AUDIO_CABLE = item("dyed_audio_cable");
        Model TEMPLATE_TINTED_COLUMN = block("template_tinted_column", TextureKey.BOTTOM, TextureKey.SIDE, TextureKey.TOP, CustomTextureKey.OVERLAY);

        private static Model item(String parent, TextureKey... requiredTextureKeys) {
            return new Model(Optional.of(Phonos.id("item/" + parent)), Optional.empty(), requiredTextureKeys);
        }

        private static Model block(String parent, TextureKey... requiredTextureKeys) {
            return new Model(Optional.of(Phonos.id("block/" + parent)), Optional.empty(), requiredTextureKeys);
        }

        private static Model block(String parent, String variant, TextureKey... requiredTextureKeys) {
            return new Model(Optional.of(Phonos.id("block/" + parent)), Optional.of(variant), requiredTextureKeys);
        }
    }
}
