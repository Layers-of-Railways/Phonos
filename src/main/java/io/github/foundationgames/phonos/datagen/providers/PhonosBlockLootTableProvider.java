package io.github.foundationgames.phonos.datagen.providers;

import io.github.foundationgames.phonos.block.PhonosBlocks;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;

public class PhonosBlockLootTableProvider extends FabricBlockLootTableProvider {
    public PhonosBlockLootTableProvider(FabricDataOutput dataOutput) {
        super(dataOutput);
    }

    @Override
    public void generate() {
        addDrop(PhonosBlocks.LOUDSPEAKER);
        addDrop(PhonosBlocks.ELECTRONIC_NOTE_BLOCK);
        addDrop(PhonosBlocks.ELECTRONIC_JUKEBOX);
        addDrop(PhonosBlocks.CONNECTION_HUB);
        addDrop(PhonosBlocks.RADIO_TRANSCEIVER);
        addDrop(PhonosBlocks.SATELLITE_RECEIVER);
        addDrop(PhonosBlocks.RADIO_LOUDSPEAKER);
        addDrop(PhonosBlocks.SATELLITE_STATION);
        addDrop(PhonosBlocks.AUDIO_SWITCH);
        addDrop(PhonosBlocks.ENDER_MUSIC_BOX);
        addDrop(PhonosBlocks.MICROPHONE_BASE);
        addDrop(PhonosBlocks.WIRELESS_MICROPHONE_BASE);
    }
}
