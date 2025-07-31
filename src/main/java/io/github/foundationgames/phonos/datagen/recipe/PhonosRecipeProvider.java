package io.github.foundationgames.phonos.datagen.recipe;

import io.github.foundationgames.phonos.Phonos;
import io.github.foundationgames.phonos.block.PhonosBlocks;
import io.github.foundationgames.phonos.item.AudioCableItem;
import io.github.foundationgames.phonos.item.PhonosItems;
import io.github.foundationgames.phonos.util.PhonosUtil;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.data.server.recipe.RecipeJsonProvider;
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder;
import net.minecraft.data.server.recipe.ShapelessRecipeJsonBuilder;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.recipe.book.RecipeCategory;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.registry.tag.TagKey;

import java.util.function.Consumer;

public class PhonosRecipeProvider extends FabricRecipeProvider {
    public PhonosRecipeProvider(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void generate(Consumer<RecipeJsonProvider> exporter) {
        /* Routing */

        ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, PhonosBlocks.CONNECTION_HUB)
            .input(I.AUDIO_CABLES)
            .input(I.AUDIO_CABLES)
            .input(ItemTags.WOODEN_SLABS)
            .criterion("has_audio_cables", conditionsFromTag(I.AUDIO_CABLES))
            .offerTo(exporter);

        ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, PhonosBlocks.AUDIO_SWITCH)
            .input(PhonosBlocks.CONNECTION_HUB)
            .input(Items.REDSTONE)
            .criterion("has_connection_hub", conditionsFromItem(PhonosBlocks.CONNECTION_HUB))
            .offerTo(exporter);

        ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, PhonosItems.AUDIO_CABLE, 3)
            .group("phonos_audio_cable")
            .input('_', Items.COPPER_INGOT)
            .input('*', Items.REDSTONE)
            .input('/', Items.GOLD_INGOT)
            .pattern("_*/")
            .criterion("has_copper_ingot", conditionsFromItem(Items.COPPER_INGOT))
            .offerTo(exporter, Phonos.id("audio_cable/base"));

        for (AudioCableItem cable : PhonosItems.ALL_AUDIO_CABLES) {
            if (cable.color == null) continue;

            Item dye = PhonosUtil.getDyeColorDyeItem(cable.color);

            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, cable)
                .group("phonos_audio_cable")
                .input(I.AUDIO_CABLES)
                .input(dye)
                .criterion("has_audio_cable", conditionsFromItem(PhonosItems.AUDIO_CABLE))
                .offerTo(exporter, Phonos.id("audio_cable/dye_1/" + cable.color.getName()));

            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, cable, 8)
                .group("phonos_audio_cable")
                .input('-', I.AUDIO_CABLES)
                .input('d', dye)
                .pattern("---")
                .pattern("-d-")
                .pattern("---")
                .criterion("has_audio_cable", conditionsFromItem(PhonosItems.AUDIO_CABLE))
                .offerTo(exporter, Phonos.id("audio_cable/dye_8/" + cable.color.getName()));
        }

        /* Sources */

        ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, PhonosBlocks.ELECTRONIC_JUKEBOX)
            .input(PhonosBlocks.CONNECTION_HUB)
            .input(Items.JUKEBOX)
            .criterion("has_connection_hub", conditionsFromItem(PhonosBlocks.CONNECTION_HUB))
            .offerTo(exporter);

        ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, PhonosBlocks.ELECTRONIC_NOTE_BLOCK)
            .input(PhonosBlocks.CONNECTION_HUB)
            .input(Items.NOTE_BLOCK)
            .criterion("has_connection_hub", conditionsFromItem(PhonosBlocks.CONNECTION_HUB))
            .offerTo(exporter);

        ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, PhonosBlocks.MICROPHONE_BASE)
            .input('i', Items.IRON_INGOT)
            .input('g', Items.GOLD_INGOT)
            .input('c', PhonosBlocks.CONNECTION_HUB)
            .pattern("i")
            .pattern("g")
            .pattern("c")
            .criterion("has_connection_hub", conditionsFromItem(PhonosBlocks.CONNECTION_HUB))
            .offerTo(exporter);

        ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, PhonosBlocks.WIRELESS_MICROPHONE_BASE)
            .input('i', Items.IRON_INGOT)
            .input('g', Items.GOLD_INGOT)
            .input('c', PhonosBlocks.RADIO_TRANSCEIVER)
            .pattern("i")
            .pattern("g")
            .pattern("c")
            .criterion("has_radio_transceiver", conditionsFromItem(PhonosBlocks.RADIO_TRANSCEIVER))
            .offerTo(exporter);

        /* Emitters */

        ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, PhonosBlocks.LOUDSPEAKER)
            .input('=', ItemTags.PLANKS)
            .input('#', ItemTags.STONE_CRAFTING_MATERIALS)
            .input('c', PhonosBlocks.CONNECTION_HUB)
            .pattern("===")
            .pattern("#c#")
            .pattern("###")
            .criterion("has_connection_hub", conditionsFromItem(PhonosBlocks.CONNECTION_HUB))
            .offerTo(exporter);

        ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, PhonosBlocks.RADIO_LOUDSPEAKER)
            .input(PhonosBlocks.LOUDSPEAKER)
            .input(PhonosBlocks.RADIO_TRANSCEIVER)
            .criterion("has_loudspeaker", conditionsFromItem(PhonosBlocks.LOUDSPEAKER))
            .offerTo(exporter);

        /* Remote */

        ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, PhonosBlocks.RADIO_TRANSCEIVER)
            .input('I', Items.IRON_BARS)
            .input('c', PhonosBlocks.CONNECTION_HUB)
            .pattern("I")
            .pattern("I")
            .pattern("c")
            .criterion("has_connection_hub", conditionsFromItem(PhonosBlocks.CONNECTION_HUB))
            .offerTo(exporter);

        ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, PhonosItems.SATELLITE)
            .input('=', Items.LAPIS_LAZULI)
            .input('S', Items.ECHO_SHARD)
            .input('f', Items.FIREWORK_ROCKET)
            .pattern("=S=")
            .pattern("=f=")
            .pattern("= =")
            .criterion("has_satellite_station", conditionsFromItem(PhonosBlocks.SATELLITE_STATION))
            .offerTo(exporter);

        ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, PhonosBlocks.SATELLITE_STATION)
            .input(PhonosBlocks.CONNECTION_HUB)
            .input(PhonosBlocks.SATELLITE_RECEIVER)
            .criterion("has_radio_transceiver", conditionsFromItem(PhonosBlocks.RADIO_TRANSCEIVER))
            .offerTo(exporter);

        ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, PhonosBlocks.SATELLITE_RECEIVER)
            .input(Items.ENDER_EYE)
            .input(PhonosBlocks.RADIO_TRANSCEIVER)
            .criterion("has_radio_transceiver", conditionsFromItem(PhonosBlocks.RADIO_TRANSCEIVER))
            .offerTo(exporter);

        /* Portable Audio */

        ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, PhonosItems.HEADSET)
            .input('-', Items.IRON_BARS)
            .input('s', PhonosBlocks.LOUDSPEAKER)
            .pattern(" - ")
            .pattern("s s")
            .criterion("has_loudspeaker", conditionsFromItem(PhonosBlocks.LOUDSPEAKER))
            .offerTo(exporter);

        ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, PhonosItems.PORTABLE_RECORD_PLAYER)
            .input(PhonosBlocks.LOUDSPEAKER)
            .input(PhonosBlocks.ELECTRONIC_JUKEBOX)
            .criterion("has_loudspeaker", conditionsFromItem(PhonosBlocks.LOUDSPEAKER))
            .offerTo(exporter);

        ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, PhonosItems.PORTABLE_RADIO)
            .input(PhonosBlocks.RADIO_TRANSCEIVER)
            .input(PhonosItems.PORTABLE_RECORD_PLAYER)
            .criterion("has_radio_transceiver", conditionsFromItem(PhonosBlocks.RADIO_TRANSCEIVER))
            .offerTo(exporter);

        ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, PhonosItems.PORTABLE_SATELLITE_RADIO)
            .input(PhonosBlocks.SATELLITE_RECEIVER)
            .input(PhonosItems.PORTABLE_RECORD_PLAYER)
            .criterion("has_satellite_receiver", conditionsFromItem(PhonosBlocks.SATELLITE_RECEIVER))
            .offerTo(exporter);
    }

    @SuppressWarnings("SameParameterValue")
    private static TagKey<Item> phonosTag(String name) {
        return TagKey.of(RegistryKeys.ITEM, Phonos.id(name));
    }

    private interface I {
        TagKey<Item> AUDIO_CABLES = phonosTag("audio_cables");
    }
}
