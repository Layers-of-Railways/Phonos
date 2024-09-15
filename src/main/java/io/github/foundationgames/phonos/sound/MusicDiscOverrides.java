package io.github.foundationgames.phonos.sound;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.foundationgames.phonos.Phonos;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.item.MusicDiscItem;
import net.minecraft.registry.Registries;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class MusicDiscOverrides {
    private static final Map<Identifier, Integer> TICK_LENGTHS = new HashMap<>();

    public static @Nullable Integer getTickLength(MusicDiscItem item) {
        Identifier id = Registries.ITEM.getId(item);
        return TICK_LENGTHS.getOrDefault(id, null);
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
            TICK_LENGTHS.clear();
            prepared.forEach((id, json) -> {
                JsonObject obj = json.getAsJsonObject();
                int length = obj.get("length").getAsInt();
                Identifier itemId = Identifier.tryParse(id.getPath().replaceFirst("/", ":"));
                TICK_LENGTHS.put(itemId, length);
            });
        }
    }
}
