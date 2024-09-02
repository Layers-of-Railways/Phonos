package io.github.foundationgames.phonos.sound.emitter;

public interface ForwardingSoundEmitter extends SoundEmitter {
    boolean forwards();

    default SoundEmitter forward(int connectionIndex) {
        return this;
    }
}
