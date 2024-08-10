package io.github.foundationgames.phonos.client.model;

import io.github.foundationgames.phonos.Phonos;

public class PhonosPartialModels {
    public static final PartialModel MICROPHONE = block("microphone");

    @SuppressWarnings("SameParameterValue")
    private static PartialModel block(String path) {
        return new PartialModel(Phonos.id("block/" + path));
    }

    public static void init() {}
}
