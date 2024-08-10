package io.github.foundationgames.phonos.client.model;

import io.github.foundationgames.phonos.Phonos;
import net.fabricmc.fabric.api.client.model.BakedModelManagerHelper;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceReloadListenerKeys;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedModelManager;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SynchronousResourceReloader;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class PartialModel {

    private static final List<PartialModel> ALL = new ArrayList<>();
    private static boolean tooLate = false;

    protected final Identifier modelLocation;
    protected BakedModel bakedModel;

    public PartialModel(Identifier modelLocation) {
        if (tooLate) throw new RuntimeException("PartialModel '" + modelLocation + "' loaded after ModelEvent.RegisterAdditional");

        this.modelLocation = modelLocation;
        ALL.add(this);
    }

    public static void onModelRegistry(ResourceManager manager, Consumer<Identifier> out) {
        for (PartialModel partial : ALL)
            out.accept(partial.getLocation());

        tooLate = true;
    }

    public static void onModelBake(BakedModelManager manager) {
        for (PartialModel partial : ALL)
            partial.set(BakedModelManagerHelper.getModel(manager, partial.getLocation()));
    }

    protected void set(BakedModel bakedModel) {
        this.bakedModel = bakedModel;
    }

    public Identifier getLocation() {
        return modelLocation;
    }

    public BakedModel get() {
        return bakedModel;
    }

    public static class ResourceReloadListener implements SynchronousResourceReloader, IdentifiableResourceReloadListener {
        public static final ResourceReloadListener INSTANCE = new ResourceReloadListener();

        public static final Identifier ID = Phonos.id("partial_models");
        public static final List<Identifier> DEPENDENCIES = List.of(ResourceReloadListenerKeys.MODELS);

        @Override
        public void reload(ResourceManager resourceManager) {
            onModelBake(MinecraftClient.getInstance().getBakedModelManager());
        }

        @Override
        public Identifier getFabricId() {
            return ID;
        }

        @Override
        public List<Identifier> getFabricDependencies() {
            return DEPENDENCIES;
        }
    }

}

