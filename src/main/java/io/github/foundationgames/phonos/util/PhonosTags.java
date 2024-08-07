package io.github.foundationgames.phonos.util;

import io.github.foundationgames.phonos.Phonos;
import net.minecraft.block.Block;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;

public class PhonosTags {
    public static final TagKey<Block> TRANSMISSION_TOWERS = TagKey.of(RegistryKeys.BLOCK, Phonos.id("transmission_towers"));

    public static void init() {}
}
