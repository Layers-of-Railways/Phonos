package io.github.foundationgames.phonos.world.sound.data;

import io.github.foundationgames.phonos.Phonos;

public final class SoundDataTypes {
    public static final SoundData.Type<SoundEventSoundData> SOUND_EVENT = SoundData.register(Phonos.id("sound_event"), true, SoundEventSoundData::new);
    public static final SoundData.Type<NoteBlockSoundData> NOTE_BLOCK = SoundData.register(Phonos.id("note_block"), NoteBlockSoundData::new);
    public static final SoundData.Type<StreamSoundData> STREAM = SoundData.register(Phonos.id("stream"), true, StreamSoundData::new);
    public static final SoundData.Type<StreamSoundData> SVC_MICROPHONE = SoundData.register(Phonos.id("svc_microphone"), true , StreamSoundData::new);

    public static void init() {
    }
}
