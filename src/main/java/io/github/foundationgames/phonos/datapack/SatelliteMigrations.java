package io.github.foundationgames.phonos.datapack;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import io.github.foundationgames.phonos.Phonos;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SinglePreparationResourceReloader;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.profiler.Profiler;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SatelliteMigrations {
    private static final Int2ObjectMap<String> MIGRATIONS = new Int2ObjectOpenHashMap<>();

    public static @Nullable String getMigration(int legacyChannel) {
        return MIGRATIONS.get(legacyChannel);
    }

    public static class ReloadListener extends SinglePreparationResourceReloader<List<JsonElement>> implements IdentifiableResourceReloadListener {
        private static final Gson GSON = new Gson();
        public static final ReloadListener INSTANCE = new ReloadListener();
        public static final String ID = "satellite_migrations";

        @Override
        public Identifier getFabricId() {
            return Phonos.id(ID);
        }

        @Override
        protected List<JsonElement> prepare(ResourceManager manager, Profiler profiler) {
            List<JsonElement> elements = new ArrayList<>();
            final String filePath = ID + ".json";

            for (var resource : manager.getAllResources(Phonos.id(filePath))) {
                try {
                    try (BufferedReader reader = resource.getReader()) {
                        elements.add(JsonHelper.deserialize(GSON, reader, JsonElement.class));
                    }
                } catch (JsonParseException | IOException | IllegalArgumentException exception) {
                    Identifier id = Identifier.of(resource.getPack().getId(), "phonos/"+filePath);
                    Phonos.LOG.error("Couldn't parse satellite migrations file {}", id, exception);
                }
            }
            return elements;
        }

        @Override
        protected void apply(List<JsonElement> prepared, ResourceManager manager, Profiler profiler) {
            MIGRATIONS.clear();
            for (JsonElement element : prepared) {
                element.getAsJsonObject().entrySet().forEach(entry -> {
                    MIGRATIONS.put(Integer.parseInt(entry.getKey()), entry.getValue().getAsString());
                });
            }
        }
    }
}
