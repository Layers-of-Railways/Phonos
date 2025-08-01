package io.github.foundationgames.phonos.datagen;

import io.github.foundationgames.phonos.datagen.providers.*;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

public class PhonosDataGenerator implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator gen) {
        FabricDataGenerator.Pack pack = gen.createPack();

        pack.addProvider(PhonosRecipeProvider::new);
        pack.addProvider(PhonosBlockLootTableProvider::new);
        pack.addProvider(PhonosBlockTagProvider::new);
        pack.addProvider(PhonosItemTagProvider::new);

        pack.addProvider(PhonosModelProvider::new);
    }
}
