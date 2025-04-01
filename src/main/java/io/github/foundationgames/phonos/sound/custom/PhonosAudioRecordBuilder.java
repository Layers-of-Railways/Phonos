package io.github.foundationgames.phonos.sound.custom;

import java.nio.ByteBuffer;

public interface PhonosAudioRecordBuilder<T extends PhonosAudioRecord<T>> {
    void pushUploadBytes(ByteBuffer buffer);
    int getDataSize();
    T build();

    interface Factory<T extends PhonosAudioRecord<T>> {
        PhonosAudioRecordBuilder<T> create(int initData);

        static <T extends PhonosAudioRecord<T>> Factory<?> cast(Factory<T> factory) {
            return factory;
        }
    }
}
