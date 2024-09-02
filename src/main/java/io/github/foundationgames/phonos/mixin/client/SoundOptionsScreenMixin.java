package io.github.foundationgames.phonos.mixin.client;

import io.github.foundationgames.phonos.config.PhonosClientConfig;
import io.github.foundationgames.phonos.config.widgets.PhonosSoundVolumeOption;
import net.minecraft.client.gui.screen.option.SoundOptionsScreen;
import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(SoundOptionsScreen.class)
public class SoundOptionsScreenMixin {
    @ModifyArg(
            method = "init",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/widget/OptionListWidget;addAll([Lnet/minecraft/client/option/SimpleOption;)V",
                    ordinal = 0
            ),
            index = 0
    )
    private SimpleOption<?>[] phonos$addVolumeSliders(SimpleOption<?>[] old) {
        var options = new SimpleOption<?>[old.length + 3];
        System.arraycopy(old, 0, options, 0, old.length);

        var config = PhonosClientConfig.get();

        options[options.length - 3] = PhonosSoundVolumeOption.create(
                "phonosMasterVolume", config.phonosMasterVolume,
                val -> {
                    config.phonosMasterVolume = val;
                    PhonosClientConfig.save();
                });
        options[options.length - 2] = PhonosSoundVolumeOption.create(
                "streamVolume", config.streamVolume,
                val -> {
                    config.streamVolume = val;
                    PhonosClientConfig.save();
                });
        options[options.length - 1] = PhonosSoundVolumeOption.create(
                "ownVoiceVolume", config.ownVoiceVolume,
                val -> {
                    config.ownVoiceVolume = val;
                    PhonosClientConfig.save();
                });

        return options;
    }
}
