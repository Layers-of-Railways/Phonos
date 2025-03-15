package io.github.foundationgames.phonos.datapack;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.foundationgames.phonos.Phonos;
import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.block.jukebox.JukeboxSong;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class MusicDiscOverrides {
    private static final Object2FloatMap<Identifier> SECOND_LENGTHS = new Object2FloatOpenHashMap<>();

    public static @Nullable Float getSecondLength(JukeboxSong song) {
        Identifier id = song.soundEvent().value().getId();
        return SECOND_LENGTHS.containsKey(id) ? SECOND_LENGTHS.getFloat(id) : null;
    }

    public static class ReloadListener extends JsonDataLoader implements IdentifiableResourceReloadListener {
        private static final Gson GSON = new Gson();
        public static final ReloadListener INSTANCE = new ReloadListener();
        public static final String ID = "sound_length_overrides";

        protected ReloadListener() {
            super(GSON, ID);
        }

        @Override
        public Identifier getFabricId() {
            return Phonos.id(ID);
        }

        @Override
        protected void apply(Map<Identifier, JsonElement> prepared, ResourceManager manager, Profiler profiler) {
            SECOND_LENGTHS.clear();
            prepared.forEach((id, json) -> {
                JsonObject obj = json.getAsJsonObject();
                float length = obj.get("length_seconds").getAsFloat();
                Identifier songId = Identifier.tryParse(id.getPath().replaceFirst("/", ":"));
                SECOND_LENGTHS.put(songId, length);
            });
        }
    }
}
