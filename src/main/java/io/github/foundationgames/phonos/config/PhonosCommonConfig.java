package io.github.foundationgames.phonos.config;

import com.google.gson.Gson;
import io.github.foundationgames.phonos.Phonos;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class PhonosCommonConfig {
    private static PhonosCommonConfig config = null;

    public int radioBaseRange = 16;
    public float worldHeightMultiplier = 0.0f;
    public float transmissionTowerMultiplier = 8.0f;
    public int maxTransmissionTowerHeight = 64;

    public PhonosCommonConfig() {
    }

    public PhonosCommonConfig copyTo(PhonosCommonConfig copy) {
        for (var f : PhonosCommonConfig.class.getDeclaredFields()) {
            try {
                f.set(copy, f.get(this));
            } catch (IllegalAccessException ignored) {}
        }

        return copy;
    }

    public static PhonosCommonConfig get() {
        if (config == null) {
            config = new PhonosCommonConfig();

            try {
                config.load();
            } catch (IOException ex) {
                Phonos.LOG.error("Error loading Phonos common config!", ex);
                try {
                    config.save();
                } catch (IOException e) {
                    Phonos.LOG.error("Error saving Phonos common config!", e);
                }
            }
        }

        return config;
    }

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("phonos-common.json");
    }

    public void load() throws IOException {
        var path = configPath();

        try (var in = Files.newBufferedReader(path)) {
            var fileCfg = new Gson().fromJson(in, PhonosCommonConfig.class);
            fileCfg.copyTo(this);
        }
    }

    public void save() throws IOException {
        var path = configPath();

        var gson = new Gson();
        try (var writer = gson.newJsonWriter(Files.newBufferedWriter(path))) {
            writer.setIndent("    ");

            gson.toJson(gson.toJsonTree(this, PhonosCommonConfig.class), writer);
        }
    }
}
