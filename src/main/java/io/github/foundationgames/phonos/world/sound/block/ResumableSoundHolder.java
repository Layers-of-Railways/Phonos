package io.github.foundationgames.phonos.world.sound.block;

public interface ResumableSoundHolder {
    int getPlayingSoundId();
    long getSkippedTicks();
}
