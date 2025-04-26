package io.github.foundationgames.phonos.client;

import io.github.foundationgames.phonos.config.PhonosClientConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;

public class PhonosKeybinds {
    private static KeyBinding TOGGLE_STREAMER_MODE;

    public static void initClient() {
        TOGGLE_STREAMER_MODE = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.phonos.toggle_streamer_mode",
            InputUtil.Type.KEYSYM,
            InputUtil.UNKNOWN_KEY.getCode(),
            "category.phonos.keybinds"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (TOGGLE_STREAMER_MODE.wasPressed()) {
                var config = PhonosClientConfig.get();
                config.muteNonPhonosSVC = !config.muteNonPhonosSVC;
                PhonosClientConfig.save();

                if (client.player != null) {
                    client.player.sendMessage(Text.translatable("key.phonos.toggle_streamer_mode." + (config.muteNonPhonosSVC ? "on" : "off")), true);
                }
            }
        });
    }
}
