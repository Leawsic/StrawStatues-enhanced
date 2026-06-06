package fuzs.strawstatues.client.model;

import fuzs.strawstatues.client.importmodel.ImportedModelRegistry;
import fuzs.strawstatues.importmodel.ImportedModelData;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import fuzs.strawstatues.world.entity.decoration.ImportedStrawStatue;

import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.cache.object.BakedGeoModel;

/**
 * GeoModel that resolves model/texture/animation resources from the
 * {@link ImportedModelRegistry} based on the entity's current model ID.
 */
public class ImportedStrawStatueGeoModel extends GeoModel<ImportedStrawStatue> {

    @Override
    public BakedGeoModel getBakedModel(ResourceLocation location) {
        // If the default model was cleared by a GeckoLib cache reload, re-register it on the fly
        if (!GeckoLibCache.getBakedModels().containsKey(location)) {
            if (location.equals(ImportedModelRegistry.DEFAULT_MODEL_LOC)) {
                ImportedModelRegistry.registerDefaultModelNow(location);
            } else {
                // Try to reload the model from the registry (for user-imported models)
                for (var entry : ImportedModelRegistry.getAvailableModelIds()) {
                    var modelEntry = ImportedModelRegistry.getModel(entry);
                    if (modelEntry.isPresent() && modelEntry.get().modelLocation().equals(location)) {
                        try {
                            ImportedModelRegistry.reloadSingle(entry);
                        } catch (Exception ignored) {}
                        break;
                    }
                }
            }
        }
        return super.getBakedModel(location);
    }

    @Override
    public ResourceLocation getModelResource(ImportedStrawStatue animatable, @Nullable GeoRenderer<ImportedStrawStatue> renderer) {
        return getModelResource(animatable);
    }

    @Override
    public ResourceLocation getModelResource(ImportedStrawStatue animatable) {
        ImportedModelData data = animatable.getImportedModel();
        if (data != null && data.hasModel()) {
            var entry = ImportedModelRegistry.getModel(data.getModelId());
            if (entry.isPresent()) {
                return entry.get().modelLocation();
            }
        }
        // Built-in default model (always registered in GeckoLibCache)
        return ImportedModelRegistry.DEFAULT_MODEL_LOC;
    }

    @Override
    public ResourceLocation getTextureResource(ImportedStrawStatue animatable, @Nullable GeoRenderer<ImportedStrawStatue> renderer) {
        return getTextureResource(animatable);
    }

    @Override
    public ResourceLocation getTextureResource(ImportedStrawStatue animatable) {
        ImportedModelData data = animatable.getImportedModel();
        if (data != null && data.hasModel()) {
            var entry = ImportedModelRegistry.getModel(data.getModelId());
            if (entry.isPresent()) {
                return entry.get().textureLocation();
            }
        }
        return ImportedModelRegistry.DEFAULT_TEXTURE_LOC;
    }

    @Override
    public ResourceLocation getAnimationResource(ImportedStrawStatue animatable) {
        ImportedModelData data = animatable.getImportedModel();
        if (data != null && data.hasModel()) {
            var entry = ImportedModelRegistry.getModel(data.getModelId());
            if (entry.isPresent() && entry.get().hasAnimation()) {
                return entry.get().animationLocation();
            }
        }
        return ImportedModelRegistry.DEFAULT_MODEL_LOC;
    }
}
