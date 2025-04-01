package io.github.foundationgames.phonos.sound.custom;

import java.nio.ByteBuffer;

public interface PhonosAudioRecordUploader {
    UploadFragment getNextFragment();

    record UploadFragment(int initData, ByteBuffer buffer, boolean last) {}
}
