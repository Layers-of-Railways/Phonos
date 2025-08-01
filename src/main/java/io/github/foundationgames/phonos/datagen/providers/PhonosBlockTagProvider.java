package io.github.foundationgames.phonos.datagen.providers;

import io.github.foundationgames.phonos.block.PhonosBlocks;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.BlockTags;

import java.util.concurrent.CompletableFuture;

public class PhonosBlockTagProvider extends FabricTagProvider<Block> {
    public PhonosBlockTagProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, RegistryKeys.BLOCK, registriesFuture);
    }

    @Override
    protected void configure(RegistryWrapper.WrapperLookup arg) {
        getOrCreateTagBuilder(BlockTags.AXE_MINEABLE)
            .add(
                PhonosBlocks.LOUDSPEAKER,
                PhonosBlocks.ELECTRONIC_NOTE_BLOCK,
                PhonosBlocks.ELECTRONIC_JUKEBOX,
                PhonosBlocks.CONNECTION_HUB,
                PhonosBlocks.RADIO_TRANSCEIVER,
                PhonosBlocks.SATELLITE_RECEIVER,
                PhonosBlocks.RADIO_LOUDSPEAKER,
                PhonosBlocks.SATELLITE_STATION,
                PhonosBlocks.AUDIO_SWITCH,
                PhonosBlocks.ENDER_MUSIC_BOX,
                PhonosBlocks.MICROPHONE_BASE,
                PhonosBlocks.WIRELESS_MICROPHONE_BASE
            );

        getOrCreateTagBuilder(PhonosBlocks.Tags.TRANSMISSION_TOWERS)
            .add(Blocks.IRON_BARS);
    }
}
