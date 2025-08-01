package io.github.foundationgames.phonos.datagen.providers;

import io.github.foundationgames.phonos.item.PhonosItems;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.ItemTags;

import java.util.concurrent.CompletableFuture;

public class PhonosItemTagProvider extends FabricTagProvider<Item> {
    public PhonosItemTagProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, RegistryKeys.ITEM, registriesFuture);
    }

    @Override
    protected void configure(RegistryWrapper.WrapperLookup arg) {
        getOrCreateTagBuilder(ItemTags.DYEABLE)
            .add(PhonosItems.HEADSET);

        getOrCreateTagBuilder(PhonosItems.Tags.AUDIO_CABLES)
            .add(PhonosItems.ALL_AUDIO_CABLES);
    }
}
