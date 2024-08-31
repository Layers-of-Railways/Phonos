package io.github.foundationgames.phonos.world.sound.data;

import io.github.foundationgames.phonos.world.sound.block.ResumableSoundHolder;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;

public class SoundEventSoundData extends SoundData {
    public final RegistryEntry<SoundEvent> sound;
    private long skippedTicks = 0;
    private final int soundId;
    private final @Nullable WeakReference<ResumableSoundHolder> holder;

    protected SoundEventSoundData(Type<?> type, long id, RegistryEntry<SoundEvent> sound, SoundCategory category, float volume, float pitch, @Nullable ResumableSoundHolder holder) {
        super(type, id, category, volume, pitch);
        this.sound = sound;

        if (holder == null) {
            this.soundId = -1;
            this.holder = null;
        } else {
            this.soundId = holder.getPlayingSoundId();
            this.holder = new WeakReference<>(holder);
        }
    }

    public SoundEventSoundData(Type<?> type, PacketByteBuf buf) {
        super(type, buf);
        this.sound = buf.readRegistryEntry(Registries.SOUND_EVENT.getIndexedEntries(), SoundEvent::fromBuf);
        this.skippedTicks = buf.readVarLong();
        this.soundId = -1;
        this.holder = null;
    }

    public static SoundEventSoundData create(long id, RegistryEntry<SoundEvent> sound, SoundCategory category, float volume, float pitch) {
        return create(id, sound, category, volume, pitch, null);
    }

    public static SoundEventSoundData create(long id, RegistryEntry<SoundEvent> sound, SoundCategory category, float volume, float pitch, @Nullable ResumableSoundHolder holder) {
        return new SoundEventSoundData(SoundDataTypes.SOUND_EVENT, id, sound, category, volume, pitch, holder);
    }

    public long getSkippedTicks() {
        return skippedTicks;
    }

    @Override
    public boolean updateSkippedTicksAndCheckResumable() {
        if (holder == null)
            return false;

        var h = holder.get();
        if (h == null)
            return false;

        skippedTicks = h.getSkippedTicks();

        return h.getPlayingSoundId() == soundId;
    }

    @Override
    public void toPacket(PacketByteBuf buf) {
        super.toPacket(buf);
        buf.writeRegistryEntry(Registries.SOUND_EVENT.getIndexedEntries(), sound, (rbuf, sound) -> sound.writeBuf(rbuf));
        buf.writeVarLong(skippedTicks);
    }
}
