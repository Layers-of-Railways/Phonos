package io.github.foundationgames.phonos.config.widgets;

import dev.isxander.yacl3.config.v2.impl.autogen.OptionFactoryRegistry;

public class PhonosOptionRegistry {
    static {
        OptionFactoryRegistry.registerOptionFactory(DoublePercentSlider.class, new DoublePercentSliderImpl());
    }

    public static void init() {}
}
