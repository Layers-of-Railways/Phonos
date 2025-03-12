package io.github.foundationgames.phonos.satellite_radio;

import io.github.foundationgames.phonos.sound.emitter.SoundEmitter;
import io.github.foundationgames.phonos.util.UniqueId;

import java.util.function.LongConsumer;

public interface SatelliteRadioDevice {
    String getChannel();

    interface Transmitter extends SatelliteRadioDevice, SoundEmitter {
        @Override
        default void forEachChild(LongConsumer action) {
            action.accept(UniqueId.ofSatelliteChannel(getChannel()));
        }
    }

    interface Receiver extends SatelliteRadioDevice {
        void setAndUpdateChannel(String channel);

        void addReceiver();

        void removeReceiver();
    }
}
