package io.github.foundationgames.phonos.client.model;

import io.github.foundationgames.phonos.Phonos;

public class PhonosPartialModels {
    public static final PartialModel
        MICROPHONE = block("microphone"),
        RADIO_ANTENNA = block("radio_transceiver_antenna"),
        WIRELESS_MICROPHONE_ON = block("wireless_microphone_on"),
        WIRELESS_MICROPHONE_ON_SELF = block("wireless_microphone_on_self")
    ;

    @SuppressWarnings("SameParameterValue")
    private static PartialModel block(String path) {
        return new PartialModel(Phonos.id("block/" + path));
    }

    public static void init() {}
}
