package io.github.foundationgames.phonos.sound;

import com.mojang.logging.LogUtils;
import cz.koca2000.nbs4j.CustomInstrument;
import cz.koca2000.nbs4j.Layer;
import cz.koca2000.nbs4j.Note;
import cz.koca2000.nbs4j.Song;
import io.github.foundationgames.phonos.Phonos;
import io.github.foundationgames.phonos.mixin_interfaces.IMonoForceableAudioStream;
import io.github.foundationgames.phonos.sound.emitter.SoundEmitterTree;
import io.github.foundationgames.phonos.sound.nbs.NoteHelper;
import io.github.foundationgames.phonos.sound.nbs.stream.ClientIncomingNBSStreamHandler;
import io.github.foundationgames.phonos.sound.nbs.stream.SynchronizedSong;
import net.fabricmc.fabric.api.client.sound.v1.FabricSoundInstance;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.*;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class NBSStreamMultiSoundInstance extends MultiSourceSoundInstance implements FabricSoundInstance {
    public final long streamId;
    private final Object mutex = new Object();
    @Nullable
    private PlayerThread playerThread;
    private boolean audible = true;

    private static final Logger LOGGER = LogUtils.getLogger();

    protected NBSStreamMultiSoundInstance(SoundEmitterTree tree, long streamId, SoundCategory category, Random random, float volume, float pitch) {
        super(tree, Phonos.STREAMED_SOUND, category, random, volume, pitch);
        this.streamId = streamId;
    }

    @Override
    public CompletableFuture<AudioStream> getAudioStream(SoundLoader loader, Identifier id, boolean repeatInstantly) {
        return ClientIncomingNBSStreamHandler.getStream(this.streamId).thenApply(song -> {
            // start player thread
            synchronized (mutex) {
                this.playerThread = new PlayerThread(song);
                this.playerThread.start();
            }

            return new InfinitelyExtendingAudioStream(null);
        });
    }

    @Override
    public void tick() {
        super.tick();

        var mc = MinecraftClient.getInstance();
        var camPos = mc.gameRenderer.getCamera().getPos();

        audible = camPos.squaredDistanceTo(getX(), getY(), getZ()) <= 34 * 34;
    }

    // Waiting logic is from Notica by LCLPYT and is licensed under the MIT license
    // (https://github.com/LCLPYT/notica/blob/1.21/src/api/java/work/lclpnet/notica/api/SongPlayback.java)
    private class PlayerThread extends Thread {
        private final SynchronizedSong song;
        private boolean receivedAnyData = false;
        private int tick;

        // timing
        private final int period;
        private final double remainder;
        private double extraMs = 0;

        private PlayerThread(SynchronizedSong song) {
            this.song = song;
            this.setName("Phonos NBS Player Thread " + NBSStreamMultiSoundInstance.this.streamId);
            this.setDaemon(true);

            float notesPerSecond = song.apply(s -> s.getTempo(0));

            this.tick = (int) (notesPerSecond / 20. * NBSStreamMultiSoundInstance.this.getSkippedTicks());

            double exactTempo = 1000. / notesPerSecond;
            this.period = (int) Math.ceil(exactTempo);
            this.remainder = Math.max(0, period - exactTempo);

            LOGGER.debug("Created player thread for stream {} with tempo {} bps ({} ms per tick)", NBSStreamMultiSoundInstance.this.streamId, notesPerSecond, period);
        }

        @Override
        @SuppressWarnings("BusyWait")
        public void run() {
            MinecraftClient mc = MinecraftClient.getInstance();

            int remainingLoops;
            final boolean loopForever;
            final int songEndTick;
            final int loopStartTick;
            {
                var metadata = this.song.apply(Song::getMetadata);
                int $remainingLoops = metadata.getLoopMaxCount() & 0xff;
                boolean $loopForever = $remainingLoops == 0;
                if (!metadata.isLoop()) {
                    $remainingLoops = 0;
                    $loopForever = false;
                }
                int $interval = Math.max(2, Math.min(metadata.getTimeSignature() & 0xff, 8)) * 4;
                int $songEndTick = this.song.apply(Song::getSongLength);
                $songEndTick += $interval - ($songEndTick % $interval);

                remainingLoops = $remainingLoops;
                loopForever = $loopForever;
                songEndTick = $songEndTick;
                loopStartTick = metadata.getLoopStartTick() & 0xffff;
            }

            LOGGER.debug("Starting stream player thread for stream {} with loop start tick {}, end tick {}, remaining loops: {}, loop forever: {}",
                    NBSStreamMultiSoundInstance.this.streamId, loopStartTick, songEndTick, remainingLoops, loopForever);

            while (true) {
                if (!this.receivedAnyData) {
                    if (this.song.apply(s -> s.getNextNonEmptyTick(-1)) == -1) {
                        try {
                            Thread.onSpinWait();
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            LOGGER.debug("Player thread for stream {} was interrupted while waiting for data", NBSStreamMultiSoundInstance.this.streamId);
                            return;
                        }
                    } else {
                        LOGGER.debug("Received initial data for stream {}, starting playback", NBSStreamMultiSoundInstance.this.streamId);
                        this.receivedAnyData = true;
                    }
                } else {
                    long start = System.currentTimeMillis();

                    List<ChildSound> toPlay = this.song.apply(s -> {
                        List<ChildSound> sounds = new ArrayList<>();
                        for (int i = 0; i < s.getLayersCount(); i++) {
                            Layer layer = s.getLayer(i);
                            Note note = layer.getNote(this.tick);
                            if (note != null) {
                                int key = note.getKey();

                                var instrument = note.getInstrument();
                                SoundEvent soundEvent;
                                if (!note.isCustomInstrument()) {
                                    soundEvent = NoteHelper.getVanillaInstrumentSound(instrument);
                                } else {
                                    CustomInstrument customInstrument = s.getCustomInstrument(instrument);
                                    soundEvent = NoteHelper.getCustomInstrumentSound(customInstrument);

                                    int keyOffset = customInstrument.getKey() - 45;
                                    key -= keyOffset;
                                }
                                if (soundEvent == null) continue;

                                ChildSound child = new ChildSound(soundEvent.getId());
                                child.setVolume((layer.getVolume() / 100f) * (note.getVolume() / 100.f));
                                child.setPitch(NoteHelper.openAlPitch((short) (key * 100 + note.getPitch())));

                                sounds.add(child);
                            }
                        }
                        return sounds;
                    });

                    mc.executeSync(() -> {
                        if (audible) {
                            SoundManager soundManager = mc.getSoundManager();

                            for (ChildSound sound : toPlay) {
                                sound.initActualSound(soundManager);
                                soundManager.play(sound);
                            }
                        }

                        if (mc.world == null) {
                            LOGGER.debug("World is null, marking stream {} as done", NBSStreamMultiSoundInstance.this.streamId);
                            NBSStreamMultiSoundInstance.this.setDone();
                        }
                    });

                    int nextNonEmptyTick = this.song.apply(s -> s.getNextNonEmptyTick(this.tick));
                    long sleepTicks;
                    if (nextNonEmptyTick == -1) {
                        if (!song.isComplete()) {
                            sleepTicks = 1;
                            this.tick++;
                        } else if (loopForever || remainingLoops-- > 0) {
                            sleepTicks = songEndTick - this.tick;
                            this.tick = loopStartTick;
                            var loopDesc = loopForever ? "∞" : String.valueOf(remainingLoops);
                            LOGGER.info("Looping song {} to tick {}, remaining loops: {}", NBSStreamMultiSoundInstance.this.streamId, this.tick, loopDesc);
                        } else {
                            mc.execute(NBSStreamMultiSoundInstance.this::setDone);
                            LOGGER.debug("Song {} has ended and will not loop", NBSStreamMultiSoundInstance.this.streamId);
                            return;
                        }
                    } else {
                        sleepTicks = nextNonEmptyTick - this.tick;
                        this.tick = nextNonEmptyTick;
                    }

                    // calculate time to sleep
                    long elapsed = System.currentTimeMillis() - start;

                    if (extraMs >= 1.0) {
                        int w = (int) Math.floor(extraMs);
                        elapsed += w;
                        extraMs -= w;
                    }

                    long sleepTime = (sleepTicks * period) - elapsed;
                    extraMs += remainder;

                    if (sleepTime <= 0) continue;
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException e) {
                        LOGGER.debug("Player thread for stream {} was interrupted during sleep", NBSStreamMultiSoundInstance.this.streamId);
                        return;
                    }
                }

                if (this.isInterrupted() || NBSStreamMultiSoundInstance.this.isDone()) {
                    LOGGER.debug("Player thread for stream {} was interrupted or Instance was marked done", NBSStreamMultiSoundInstance.this.streamId);
                    return;
                }
            }
        }
    }

    @Override
    protected void onDone() {
        super.onDone();

        synchronized (mutex) {
            if (playerThread != null && playerThread.isAlive()) {
                LOGGER.debug("Interrupting player thread for stream {} because Instance was marked done", streamId);
                playerThread.interrupt();
                playerThread = null;
            }
        }
    }

    private class ChildSound extends AbstractSoundInstance implements RemoveNotifiedTickableSoundInstance, UnlimitedPitchSoundInstance, FabricSoundInstance {
        private int doneTicks = 0;
        private final Identifier actualSoundId;
        private @Nullable Sound actualSound;

        private static final Set<Identifier> monoCache = Collections.synchronizedSet(new HashSet<>());

        // only do streaming if the sound is custom, because it is slower
        private static Identifier maybeMakeStreamed(Identifier soundId) {
            if (monoCache.contains(soundId)) {
                return soundId;
            }

            if (soundId.getNamespace().equals("minecraft") && soundId.getPath().startsWith("block.note_block.")) {
                return soundId;
            } else {
                return Phonos.STREAMED_SOUND;
            }
        }

        protected ChildSound(Identifier soundId) {
            super(maybeMakeStreamed(soundId), NBSStreamMultiSoundInstance.this.category, NBSStreamMultiSoundInstance.this.random);
            this.actualSoundId = soundId;
        }

        protected void initActualSound(SoundManager soundManager) {
            if (this.actualSound != null) return;
            if (id != Phonos.STREAMED_SOUND) return;

            if (this.actualSoundId.equals(SoundManager.INTENTIONALLY_EMPTY_ID)) {
                this.actualSound = SoundManager.INTENTIONALLY_EMPTY_SOUND;
            } else {
                WeightedSoundSet weightedSoundSet = soundManager.get(this.actualSoundId);
                if (weightedSoundSet == null) {
                    this.actualSound = SoundManager.MISSING_SOUND;
                } else {
                    this.actualSound = weightedSoundSet.getSound(this.random);
                }
            }
        }

        public void setPitch(float pitch) {
            this.pitch = pitch;
        }

        public void setVolume(float volume) {
            this.volume = volume;
        }

        @Override
        public double getX() {
            return NBSStreamMultiSoundInstance.this.getX();
        }

        @Override
        public double getY() {
            return NBSStreamMultiSoundInstance.this.getY();
        }

        @Override
        public double getZ() {
            return NBSStreamMultiSoundInstance.this.getZ();
        }

        @Override
        public float getVolume() {
            return NBSStreamMultiSoundInstance.this.getVolume() * super.getVolume();
        }

        @Override
        public boolean isDone() {
            // the sound engine will automatically cull this sound if its source stops (even if we return false here),
            // so this is safe
            return doneTicks >= 20; // give a bit of time for the sound to finish, if at the end of a song
        }

        @Override
        public void tick() {
            if (doneTicks > 0 || NBSStreamMultiSoundInstance.this.isDone()) {
                doneTicks++;
            }
        }

        @Override
        public boolean shouldAlwaysPlay() {
            return true;
        }

        @Override
        public CompletableFuture<AudioStream> getAudioStream(SoundLoader loader, Identifier id, boolean repeatInstantly) {
            if (actualSound == null) {
                initActualSound(MinecraftClient.getInstance().getSoundManager());
            }

            if (actualSound == SoundManager.MISSING_SOUND) {
                //Phonos.LOG.warn("Missing sound for note: {}", actualSoundId);
                return CompletableFuture.completedFuture(new InstantaneousAudioStream());
            }

            return loader.loadStreamed(actualSound.getLocation(), repeatInstantly).thenApply(stream -> {
                if (stream.getFormat().getChannels() == 1) {
                    monoCache.add(actualSoundId);
                }
                if (stream instanceof IMonoForceableAudioStream monoStream) {
                    monoStream.phonos$forceMono();
                }
                return stream;
            });
        }

        @Override
        public void setDone() {
            this.doneTicks = 20;
        }
    }
}
