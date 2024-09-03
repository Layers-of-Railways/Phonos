package io.github.foundationgames.phonos.mixin_interfaces;

import java.io.IOException;

public interface ISeekableAudioStream {
    void phonos$seekForwardFromHere(float seconds) throws IOException;
}
