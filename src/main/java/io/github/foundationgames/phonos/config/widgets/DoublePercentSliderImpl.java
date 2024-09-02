package io.github.foundationgames.phonos.config.widgets;

import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.controller.ControllerBuilder;
import dev.isxander.yacl3.api.controller.DoubleSliderControllerBuilder;
import dev.isxander.yacl3.config.v2.api.ConfigField;
import dev.isxander.yacl3.config.v2.api.autogen.OptionAccess;
import dev.isxander.yacl3.config.v2.api.autogen.SimpleOptionFactory;
import net.minecraft.text.Text;
import net.minecraft.util.Language;

class DoublePercentSliderImpl extends SimpleOptionFactory<DoublePercentSlider, Double> {
    @Override
    protected ControllerBuilder<Double> createController(DoublePercentSlider annotation, ConfigField<Double> field, OptionAccess storage, Option<Double> option) {
        return DoubleSliderControllerBuilder.create(option)
            .formatValue(v -> {
                String key = null;
                if (v == annotation.min())
                    key = getTranslationKey(field, "fmt.min");
                else if (v == annotation.max())
                    key = getTranslationKey(field, "fmt.max");
                if (key != null && Language.getInstance().hasTranslation(key))
                    return Text.translatable(key);
                key = getTranslationKey(field, "fmt");
                if (Language.getInstance().hasTranslation(key))
                    return Text.translatable(key, v*100);
                return Text.translatable(String.format(annotation.format(), v*100));
            })
            .range(annotation.min(), annotation.max())
            .step(annotation.step());
    }
}
