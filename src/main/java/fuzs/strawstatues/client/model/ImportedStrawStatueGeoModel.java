package fuzs.strawstatues.client.model;

import fuzs.strawstatues.client.importmodel.ImportedModelRegistry;
import fuzs.strawstatues.importmodel.ImportedModelData;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import fuzs.strawstatues.world.entity.decoration.ImportedStrawStatue;

/**
 * GeoModel that resolves model/texture/animation resources from the
 * {@link ImportedModelRegistry} based on the entity's current model ID.
 */
public class ImportedStrawStatueGeoModel extends GeoModel<ImportedStrawStatue> {

    // Fallback resource locations when the model is not loaded
    private static final ResourceLocation FALLBACK_MODEL = new ResourceLocation("strawstatues_imported", "geo/placeholder.geo.json");
    private static final ResourceLocation FALLBACK_TEXTURE = new ResourceLocation("strawstatues_imported", "textures/entity/placeholder.png");

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
        return FALLBACK_MODEL;
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
        return FALLBACK_TEXTURE;
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
        // Return a dummy location even without animation - GeckoLib handles missing gracefully
        return FALLBACK_MODEL;
    }
}
