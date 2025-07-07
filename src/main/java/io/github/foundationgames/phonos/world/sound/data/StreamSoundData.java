package io.github.foundationgames.phonos.world.sound.data;

import io.github.foundationgames.phonos.sound.nbs.stream.ServerOutgoingNBSStreamHandler;
import io.github.foundationgames.phonos.sound.stream.ServerOutgoingStreamHandler;
import io.github.foundationgames.phonos.util.compat.PhonosVoicechatProxy;
import io.github.foundationgames.phonos.world.sound.block.ResumableSoundHolder;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;

public class StreamSoundData extends SoundData {
    public final long streamId;
    private long skippedTicks = 0;
    private final int soundId;
    private final @Nullable WeakReference<ResumableSoundHolder> holder;

    public StreamSoundData(Type<?> type, long emitterId, long streamId, SoundCategory category, float volume, float pitch) {
        this(type, emitterId, streamId, category, volume, pitch, null);
    }

    public StreamSoundData(Type<?> type, long emitterId, long streamId, SoundCategory category, float volume, float pitch, @Nullable ResumableSoundHolder holder) {
        super(type, emitterId, category, volume, pitch);

        this.streamId = streamId;
        if(holder == null) {
            this.soundId = -1;
            this.holder = null;
        } else {
            this.soundId = holder.getPlayingSoundId();
            this.holder = new WeakReference<>(holder);
        }
    }

    public StreamSoundData(Type<?> type, PacketByteBuf buf) {
        super(type, buf);

        this.streamId = buf.readLong();
        this.skippedTicks = buf.readVarLong();
        this.soundId = -1;
        this.holder = null;
    }

    public static StreamSoundData create(long id, long streamId, SoundCategory category, float volume, float pitch) {
        return new StreamSoundData(SoundDataTypes.STREAM, id, streamId, category, volume, pitch);
    }

    public static StreamSoundData createMicrophone(long id, long streamId, SoundCategory category) {
        return new StreamSoundData(SoundDataTypes.SVC_MICROPHONE, id, streamId, category, 1.0f, 1.0f);
    }

    public static StreamSoundData createNBS(long id, long streamId, SoundCategory category, float volume, float pitch, @NotNull ResumableSoundHolder holder) {
        return new StreamSoundData(SoundDataTypes.NBS_STREAM, id, streamId, category, volume, pitch, holder);
    }

    public long getSkippedTicks() {
        return skippedTicks;
    }

    @Override
    public boolean updateSkippedTicksAndCheckResumable() {
        if (!this.type.resumable())
            return false;

        if (holder != null) {
            var h = holder.get();
            if (h != null && h.getPlayingSoundId() == this.soundId) {
                skippedTicks = h.getSkippedTicks();
            }
        }

        if (this.type == SoundDataTypes.SVC_MICROPHONE) {
            return PhonosVoicechatProxy.isStreaming(this.streamId);
        } else if (this.type == SoundDataTypes.STREAM) {
            return ServerOutgoingStreamHandler.STREAMS.containsKey(this.streamId);
        } else if (this.type == SoundDataTypes.NBS_STREAM) {
            return ServerOutgoingNBSStreamHandler.STREAMS.containsKey(this.streamId);
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
        } else if (this.type == SoundDataTypes.NBS_STREAM) {
            ServerOutgoingNBSStreamHandler.resumeStream(player, this.streamId);
        }
    }

    @Override
    public void toPacket(PacketByteBuf buf) {
        super.toPacket(buf);

        buf.writeLong(this.streamId);
        buf.writeVarLong(this.skippedTicks);
    }
}
