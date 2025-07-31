package io.github.foundationgames.phonos.sound;

import com.mojang.logging.LogUtils;
import io.github.foundationgames.phonos.Phonos;
import io.github.foundationgames.phonos.sound.custom.SVCSoundMetadata;
import io.github.foundationgames.phonos.sound.emitter.SoundEmitterTree;
import io.github.foundationgames.phonos.util.compat.PhonosVoicechatPlugin;
import net.fabricmc.fabric.api.client.sound.v1.FabricSoundInstance;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.AudioStream;
import net.minecraft.client.sound.SoundLoader;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;

public class SVCMultiSoundInstance extends MultiSourceSoundInstance implements FabricSoundInstance {
    private final CompletableFuture<SVCSoundMetadata> metadata;
    private static final Logger LOGGER = LogUtils.getLogger();

    public SVCMultiSoundInstance(SoundEmitterTree tree, long streamId, SoundCategory category, Random random, float volume, float pitch) {
        super(tree, Phonos.SVC_STREAMED_SOUND, category, random, volume, pitch);

        LOGGER.info("Creating SVC sound instance for stream ID {}", streamId);
        this.metadata = PhonosVoicechatPlugin.getClientMicrophoneChannel(streamId);
        this.metadata.thenAccept(m -> {
            if (m == null) {
                LOGGER.error("Failed to get microphone metadata for stream ID {}", streamId);
                this.setDone();
            } else {
                m.setDistance(32.0f);
                m.setCategory(category.getName());
                LOGGER.info("Acquired microphone metadata for SVC sound instance with stream ID {}: {}", streamId, m);
            }
        });
    }

    @Override
    public void tick() {
        super.tick();

        var metadata = this.metadata.getNow(null);
        if (metadata == null)
            return;

        metadata.setPosition(new Vec3d(this.getX(), this.getY(), this.getZ()));
        metadata.setVolume(getVolume());
//        Phonos.LOG.info("Setting microphone position to {}", metadata.getPosition());
    }

    @Override
    public CompletableFuture<AudioStream> getAudioStream(SoundLoader loader, Identifier id, boolean repeatInstantly) {
        MinecraftClient mc = MinecraftClient.getInstance();
        var sound = mc.getSoundManager().get(Identifier.tryParse("minecraft", "block.note_block.chime"))
            .getSound(mc.player.getRandom());
        return loader.loadStreamed(sound.getLocation(), false)
            .thenApply(InfinitelyExtendingAudioStream::new);
    }
}
