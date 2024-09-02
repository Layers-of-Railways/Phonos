package io.github.foundationgames.phonos.config;

import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.autogen.AutoGen;
import dev.isxander.yacl3.config.v2.api.autogen.MasterTickBox;
import dev.isxander.yacl3.config.v2.api.autogen.TickBox;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import io.github.foundationgames.phonos.Phonos;
import io.github.foundationgames.phonos.config.widgets.DoublePercentSlider;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;
import org.jetbrains.annotations.ApiStatus;

public class PhonosClientConfig {
    private static final ConfigClassHandler<PhonosClientConfig> HANDLER = ConfigClassHandler.createBuilder(PhonosClientConfig.class)
        .id(Phonos.id("client_config"))
        .serializer(config -> GsonConfigSerializerBuilder.create(config)
            .setPath(FabricLoader.getInstance().getConfigDir().resolve("phonos.json5"))
            .setJson5(true)
            .build())
        .build();

    @SerialEntry(comment = "Loudspeaker Master Volume [0, 1]")
    @AutoGen(category = "audio")
    @DoublePercentSlider(min = 0, max = 1, step = 0.01f)
    public double phonosMasterVolume = 1;

    @SerialEntry(comment = "Satellite Audio Volume [0, 1]")
    @AutoGen(category = "audio")
    @DoublePercentSlider(min = 0, max = 1, step = 0.01f)
    public double streamVolume = 1;

    @SerialEntry(comment = "Microphone Volume (Own Voice) [0, 1]")
    @AutoGen(category = "audio")
    @DoublePercentSlider(min = 0, max = 1, step = 0.01f)
    public double ownVoiceVolume = 1;


    @SerialEntry(comment = "Cull Audio Cables not on-screen")
    @AutoGen(category = "rendering")
    @TickBox
    public boolean cableCulling = true;

    @SerialEntry(comment = "Render Audio Cables with VBOs")
    @AutoGen(category = "rendering")
    @TickBox
    public boolean cableVBOs = false;

    @SerialEntry(comment = "Render Audio Cables with Detail Levels")
    @AutoGen(category = "rendering")
    @MasterTickBox(value = {"cableLODNearDetail", "cableLODFarDetail"})
    public boolean cableLODs = true;

    @SerialEntry(comment = "Highest Audio Cable Detail Level [0.1, 1]")
    @AutoGen(category = "rendering")
    @DoublePercentSlider(min = 0.1, max = 1, step = 0.01f)
    public double cableLODNearDetail = 1;

    @SerialEntry(comment = "Lowest Audio Cable Detail Level [0.1, 1]")
    @AutoGen(category = "rendering")
    @DoublePercentSlider(min = 0.1, max = 1, step = 0.01f)
    public double cableLODFarDetail = 0.25;

    @ApiStatus.Internal
    public PhonosClientConfig() {
    }

    public static PhonosClientConfig get() {
        return HANDLER.instance();
    }

    public static boolean load() {
        return HANDLER.load();
    }

    public static void save() {
        HANDLER.save();
    }

    @Environment(EnvType.CLIENT)
    public static Screen createScreen(Screen parent) {
        return HANDLER.generateGui()
            .generateScreen(parent);
    }
}
