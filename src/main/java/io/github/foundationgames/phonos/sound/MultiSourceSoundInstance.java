package io.github.foundationgames.phonos.sound;

import com.google.common.util.concurrent.AtomicDouble;
import io.github.foundationgames.phonos.config.PhonosClientConfig;
import io.github.foundationgames.phonos.item.HeadsetItem;
import io.github.foundationgames.phonos.sound.emitter.SoundEmitterTree;
import io.github.foundationgames.phonos.world.sound.entity.HeadsetSoundSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.AbstractSoundInstance;
import net.minecraft.client.sound.TickableSoundInstance;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import org.joml.Vector3d;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class MultiSourceSoundInstance extends AbstractSoundInstance implements TickableSoundInstance, SkippableSoundInstance {
    public final AtomicReference<SoundEmitterTree> emitters;
    private double camX, camZ;
    private double x, y, z;
    protected float volMultiplier = 1;

    private long skippedTicks = 0;

    private boolean done;

    protected MultiSourceSoundInstance(SoundEmitterTree tree, Identifier sound, SoundCategory category, Random random, float volume, float pitch) {
        super(sound, category, random);

        this.emitters = new AtomicReference<>(tree);
        this.volume = volume;
        this.pitch = pitch;

        this.updatePosition();
    }

    public MultiSourceSoundInstance(SoundEmitterTree tree, SoundEvent sound, SoundCategory category, Random random, float volume, float pitch) {
        this(tree, sound.getId(), category, random, volume, pitch);
    }

    @Override
    public float getVolume() {
        return (float) (super.getVolume() * PhonosClientConfig.get().phonosMasterVolume * volMultiplier);
    }

    @Override
    public boolean shouldAlwaysPlay() {
        return true;
    }

    @Override
    public double getX() {
        return x;
    }

    @Override
    public double getY() {
        return y;
    }

    @Override
    public double getZ() {
        return z;
    }

    private void updatePosition() {
        var mc = MinecraftClient.getInstance();
        var camPos = mc.gameRenderer.getCamera().getPos();

        boolean ncTemp = false;
        if (mc.player != null) {
            var headset = mc.player.getEquippedStack(EquipmentSlot.HEAD);

            if (headset.getItem() instanceof HeadsetItem item && item.isNoiseCancelling(headset)) {
                ncTemp = true;
            }
        }
        final boolean noiseCancelling = ncTemp;

        var pos = new Vector3d();

        AtomicDouble srcWeights = new AtomicDouble(0);
        AtomicDouble minDist = new AtomicDouble(Double.POSITIVE_INFINITY);
        AtomicBoolean foundSources = new AtomicBoolean(false);

        var srcPos = new Vector3d();

        this.x = 0;
        this.y = camPos.y + 999;
        this.z = 0;

        this.emitters.get().forEachSource(mc.world, src -> {
            if (noiseCancelling && !(src instanceof HeadsetSoundSource)) {
                return;
            }

            double weight;

            foundSources.set(true);
            srcPos.set(src.x(), src.y(), src.z());

            double dist = srcPos.distance(camPos.x, camPos.y, camPos.z);

            if (dist < minDist.get()) {
                minDist.set(dist);
            }

            if (dist <= 1.4135) {
                weight = -0.05 * Math.pow(dist, 4) + 1;
            } else {
                weight = Math.pow(2, -(dist - 1.0923));
            }

            pos.add(srcPos.mul(weight));
            srcWeights.addAndGet(weight);
        });

        if (foundSources.get()) {
            pos.div(srcWeights.get());

            // Avoid headache by biasing the sound into the player head when moving fast
            double camVel = Vector3d.length(camPos.x - this.camX, 0, camPos.z - this.camZ);

            if (camVel > 0.3) {
                float camWeight = MathHelper.clamp((float) ((-0.0333 / (camVel - 0.2666)) + 1), 0, 1);

                pos.mul(1 - camWeight);
                pos.add(camWeight * camPos.x, camWeight * camPos.y + 2, camWeight * camPos.z);
            }

            this.x = pos.x();
            this.y = pos.y();
            this.z = pos.z();

            double errorDist = minDist.get() - pos.distance(camPos.x, camPos.y, camPos.z);

            if (errorDist > 0) {
                float range = this.volume * 16;
                errorDist = Math.min(errorDist, range);

                //        Fake distance fade, I don't have a good reason for this function other than it works well enough
                this.volMultiplier = (float) MathHelper.clamp(1 - (Math.pow(errorDist / range, 0.25)), 0, 1);
            } else {
                this.volMultiplier = 1;
            }

            this.camX = camPos.x;
            this.camZ = camPos.z;
        } else {
            this.volMultiplier = 0;
        }
    }

    @Override
    public boolean isDone() {
        return done;
    }

    protected final void setDone() {
        this.done = true;
        this.repeat = false;
    }

    @Override
    public void tick() {
        updatePosition();
    }

    public MultiSourceSoundInstance withSkippedTicks(long skippedTicks) {
        this.skippedTicks = skippedTicks;
        return this;
    }

    @Override
    public long getSkippedTicks() {
        /* for stress-testing seeking

        int minutes = 30;
        int seconds = minutes * 60;

        //seconds = 5;

        return seconds * 20;*/

        return skippedTicks;
    }
}