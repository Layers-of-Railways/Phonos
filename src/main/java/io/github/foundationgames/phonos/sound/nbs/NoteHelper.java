package io.github.foundationgames.phonos.sound.nbs;

import cz.koca2000.nbs4j.CustomInstrument;
import io.github.foundationgames.phonos.Phonos;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

// Helpers are from Notica by LCLPYT and are licensed under the MIT license
// (https://github.com/LCLPYT/notica)
public class NoteHelper {
    public static final byte
        LOWEST_VANILLA_KEY = 33,
        HIGHEST_VANILLA_KEY = 57,
        OCTAVE_KEYS = 12,
        TWO_OCTAVES_KEYS = OCTAVE_KEYS * 2;
    public static final short
        KEY_PITCH_FACTOR = 100,
        OCTAVE_PITCH = OCTAVE_KEYS * KEY_PITCH_FACTOR;

    private static final Map<CacheKey, SoundEvent> cache = new HashMap<>();
    private static @Nullable Registry<SoundEvent> soundRegistry;

    private record CacheKey(String name, String fileName, int key, boolean shouldPressKey) {
        public CacheKey(CustomInstrument instrument) {
            this(instrument.getName(), instrument.getFileName(), instrument.getKey(), instrument.shouldPressKey());
        }
    }

    @ApiStatus.Internal
    public static void setRegistryManager(@Nullable DynamicRegistryManager registryManager) {
        cache.clear();
        if (registryManager != null) {
            soundRegistry = registryManager.get(RegistryKeys.SOUND_EVENT);
        } else {
            soundRegistry = null;
        }
    }

    @Nullable
    public static SoundEvent getVanillaInstrumentSound(int instrument) {
        return switch (instrument) {
            case 0  -> SoundEvents.BLOCK_NOTE_BLOCK_HARP.value();
            case 1  -> SoundEvents.BLOCK_NOTE_BLOCK_BASS.value();
            case 2  -> SoundEvents.BLOCK_NOTE_BLOCK_BASEDRUM.value();
            case 3  -> SoundEvents.BLOCK_NOTE_BLOCK_SNARE.value();
            case 4  -> SoundEvents.BLOCK_NOTE_BLOCK_HAT.value();
            case 5  -> SoundEvents.BLOCK_NOTE_BLOCK_GUITAR.value();
            case 6  -> SoundEvents.BLOCK_NOTE_BLOCK_FLUTE.value();
            case 7  -> SoundEvents.BLOCK_NOTE_BLOCK_BELL.value();
            case 8  -> SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value();
            case 9  -> SoundEvents.BLOCK_NOTE_BLOCK_XYLOPHONE.value();
            case 10 -> SoundEvents.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE.value();
            case 11 -> SoundEvents.BLOCK_NOTE_BLOCK_COW_BELL.value();
            case 12 -> SoundEvents.BLOCK_NOTE_BLOCK_DIDGERIDOO.value();
            case 13 -> SoundEvents.BLOCK_NOTE_BLOCK_BIT.value();
            case 14 -> SoundEvents.BLOCK_NOTE_BLOCK_BANJO.value();
            case 15 -> SoundEvents.BLOCK_NOTE_BLOCK_PLING.value();
            default -> null;
        };
    }

    @Nullable
    public static SoundEvent getCustomInstrumentSound(CustomInstrument instrument) {
        final CacheKey key = new CacheKey(instrument);

        SoundEvent sound = cache.get(key);

        if (sound != null) {
            return sound;
        }

        sound = fetchCustomSound(instrument);

        if (sound != null) {
            cache.put(key, sound);
        }

        return sound;
    }

    private static final String[] prefixes = new String[] {
        "ambient",
        "block",
        "damage",
        "dig",
        "enchant",
        "entity",
        "event",
        "fire",
        "fireworks",
        "item",
        "liquid",
        "minecart",
        "mob",
        "music",
        "note",
        "portal",
        "random",
        "records",
        "step",
        "tile",
        "ui"
    };

    @Nullable
    private static SoundEvent fetchCustomSound(CustomInstrument instrument) {
        String file = instrument.getFileName();

        if (file.endsWith(".ogg")) {
            file = file.substring(0, file.length() - 4);
        }

        // support for old nbs files that encoded the pling sound as custom instrument
        if (file.equalsIgnoreCase("pling")) {
            return SoundEvents.BLOCK_NOTE_BLOCK_PLING.value();
        }

        if (file.startsWith("minecraft/")) {
            file = file.substring(10);
        }

        if (file.contains("/")) {
            file = file.replace('/', '.');
        }

        file = file.replaceFirst("entity.firework.", "entity.firework_rocket.");

        // try to parse filename as sound id
        Identifier idFromFile = Identifier.tryParse(file);

        if (idFromFile != null && soundRegistry != null) {
            SoundEvent sound = soundRegistry.get(idFromFile);

            if (sound != null) {
                return sound;
            }

            for (String p : prefixes) {
                Identifier prefixedId = Identifier.of(idFromFile.getNamespace(), p + "." + idFromFile.getPath());

                SoundEvent prefixedSound = soundRegistry.get(prefixedId);
                if (prefixedSound != null) {
                    return prefixedSound;
                }
            }
        }

        // try the sound name instead
        String name = instrument.getName().replaceFirst("entity.firework.", "entity.firework_rocket.");
        Identifier idFromName = Identifier.tryParse(name);

        if (idFromName != null && soundRegistry != null) {
            SoundEvent sound = soundRegistry.get(idFromName);

            if (sound != null) {
                return sound;
            }

            for (String p : prefixes) {
                Identifier prefixedId = Identifier.of(idFromName.getNamespace(), p + "." + idFromName.getPath());

                SoundEvent prefixedSound = soundRegistry.get(prefixedId);
                if (prefixedSound != null) {
                    return prefixedSound;
                }
            }
        }

        Phonos.LOG.warn("Could not find sound for custom instrument {} with file name {} and key {}",
            instrument.getName(), instrument.getFileName(), instrument.getKey());

        // fallback for non-vanilla custom sounds
        if (idFromFile != null) {
            return SoundEvent.of(idFromFile);
        }

        if (idFromName != null) {
            return SoundEvent.of(idFromName);
        }

        return null;
    }

    public static float openAlPitch(short pitch) {
        // normalize, so that the lowest vanilla key pitch is mapped to 0
        pitch -= LOWEST_VANILLA_KEY * KEY_PITCH_FACTOR;

        // in openal, a reduction by 50% is equal to -12 semitones and increase by 50% is equal to 12 semitones
        // https://www.openal.org/documentation/openal-1.1-specification.pdf (page 38)

        // lowest vanilla pitch should map to 0.5, highest (2 octaves or 24 semitones above) should map to 2.0
        return (float) Math.pow(2, (double) (pitch - OCTAVE_PITCH) / OCTAVE_PITCH);
    }
}
