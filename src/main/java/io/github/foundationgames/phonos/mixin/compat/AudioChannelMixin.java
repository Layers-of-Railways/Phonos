package io.github.foundationgames.phonos.mixin.compat;

import de.maxhenkel.voicechat.voice.client.AudioChannel;
import de.maxhenkel.voicechat.voice.common.LocationSoundPacket;
import de.maxhenkel.voicechat.voice.common.SoundPacket;
import io.github.foundationgames.phonos.util.compat.PhonosVoicechatPlugin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AudioChannel.class)
public class AudioChannelMixin {
    @Inject(method = "writeToSpeaker", at = @At("HEAD"), remap = false)
    private void fixLocation(SoundPacket<?> packet, short[] monoData, CallbackInfo ci) {
        if (packet instanceof LocationSoundPacket locationSoundPacket) {
            PhonosVoicechatPlugin.onLocationalSoundPacket(locationSoundPacket, monoData);
        }
    }
}
