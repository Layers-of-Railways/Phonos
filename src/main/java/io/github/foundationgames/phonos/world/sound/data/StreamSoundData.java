package io.github.foundationgames.phonos.world.sound.data;

import io.github.foundationgames.phonos.sound.stream.ServerOutgoingStreamHandler;
import io.github.foundationgames.phonos.util.compat.PhonosVoicechatProxy;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;

public class StreamSoundData extends SoundData {
    public final long streamId;

    public StreamSoundData(Type<?> type, long emitterId, long streamId, SoundCategory category, float volume, float pitch) {
        super(type, emitterId, category, volume, pitch);

        this.streamId = streamId;
    }

    public StreamSoundData(Type<?> type, PacketByteBuf buf) {
        super(type, buf);

        this.streamId = buf.readLong();
    }

    public static StreamSoundData create(long id, long streamId, SoundCategory category, float volume, float pitch) {
        return new StreamSoundData(SoundDataTypes.STREAM, id, streamId, category, volume, pitch);
    }

    public static StreamSoundData createMicrophone(long id, long streamId, SoundCategory category) {
        return new StreamSoundData(SoundDataTypes.SVC_MICROPHONE, id, streamId, category, 1.0f, 1.0f);
    }

    @Override
    public boolean updateSkippedTicksAndCheckResumable() {
        if (!this.type.resumable())
            return false;

        if (this.type == SoundDataTypes.SVC_MICROPHONE) {
            return PhonosVoicechatProxy.isStreaming(this.streamId);
        } else if (this.type == SoundDataTypes.STREAM) {
            return ServerOutgoingStreamHandler.STREAMS.containsKey(this.streamId);
        } else {
            return false;
        }
    }

    @Override
    public void onResumedToPlayer(ServerPlayerEntity player) {
        if (this.type == SoundDataTypes.SVC_MICROPHONE) {
            PhonosVoicechatProxy.resumeStream(player, this.streamId);
        } else if (this.type == SoundDataTypes.STREAM) {
            ServerOutgoingStreamHandler.resumeStream(player, this.streamId);
        }
    }

    @Override
    public void toPacket(PacketByteBuf buf) {
        super.toPacket(buf);

        buf.writeLong(this.streamId);
    }
}
