package fuzs.strawstatues.client.importmodel;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import fuzs.strawstatues.StrawStatues;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.loading.json.raw.Model;
import software.bernie.geckolib.loading.object.BakedAnimations;
import software.bernie.geckolib.loading.object.BakedModelFactory;
import software.bernie.geckolib.loading.object.GeometryTree;
import software.bernie.geckolib.util.JsonUtil;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages imported external models stored in the config directory.
 * Scans for .geo.json / .png / .animation.json files and injects them
 * directly into GeckoLib's model cache so they can be rendered without
 * requiring a resource pack reload.
 */
public final class ImportedModelRegistry {

    private static final String NAMESPACE = "strawstatues_imported";
    private static final Path CONFIG_DIR = Path.of("config", StrawStatues.MOD_ID, "imported_models");

    private static final Map<String, ModelEntry> REGISTRY = new ConcurrentHashMap<>();

    // Gson instance matching GeckoLib's setup (used for animation loading)
    private static final Gson GEO_GSON = JsonUtil.GEO_GSON;

    private ImportedModelRegistry() {}

    // ── Model entry ─────────────────────────────────────────

    public record ModelEntry(
            String modelId,
            Path geoJsonPath,
            Path texturePath,
            Optional<Path> animationPath,
            ResourceLocation modelLocation,
            ResourceLocation textureLocation,
            ResourceLocation animationLocation
    ) {
        public boolean hasAnimation() { return animationPath.isPresent(); }
    }

    // ── Public API ─────────────────────────────────────────

    /**
     * Get all available model IDs
     */
    public static Set<String> getAvailableModelIds() {
        return REGISTRY.keySet();
    }

    /**
     * Check if a model is loaded by ID
     */
    public static boolean isModelLoaded(String modelId) {
        return REGISTRY.containsKey(modelId);
    }

    /**
     * Get a model entry by ID
     */
    public static Optional<ModelEntry> getModel(String modelId) {
        return Optional.ofNullable(REGISTRY.get(modelId));
    }

    // ── Scanning & Loading ─────────────────────────────────

    /**
     * Scan the config directory for model directories and load each model.
     * Called during client setup and after importing a new model.
     */
    public static void scanAndLoad() {
        try {
            Files.createDirectories(CONFIG_DIR);
        } catch (IOException ignored) {}

        REGISTRY.clear();

        if (!Files.isDirectory(CONFIG_DIR)) return;

        try (var dirStream = Files.newDirectoryStream(CONFIG_DIR)) {
            for (Path dir : dirStream) {
                if (!Files.isDirectory(dir)) continue;
                String modelId = dir.getFileName().toString();
                if (modelId.isEmpty() || modelId.startsWith(".")) continue;
                loadModelFromDir(modelId, dir);
            }
        } catch (IOException e) {
            StrawStatues.LOGGER.warn("Failed to scan imported models directory", e);
        }

        StrawStatues.LOGGER.info("Loaded {} imported models", REGISTRY.size());
    }

    /**
     * Load a single model from a directory, parse geo.json + png,
     * inject into GeckoLib cache, register texture.
     */
    private static void loadModelFromDir(String modelId, Path dir) {
        Path geoJson = dir.resolve("model.geo.json");
        Path texture = dir.resolve("texture.png");
        Path animation = dir.resolve("animation.animation.json");

        if (!Files.exists(geoJson) || !Files.exists(texture)) {
            StrawStatues.LOGGER.debug("Skipping '{}': missing model.geo.json or texture.png", modelId);
            return;
        }

        ResourceLocation modelLoc = new ResourceLocation(NAMESPACE, "geo/" + modelId + ".geo.json");
        ResourceLocation texLoc = new ResourceLocation(NAMESPACE, "textures/entity/" + modelId + ".png");
        ResourceLocation animLoc = new ResourceLocation(NAMESPACE, "animations/" + modelId + ".animation.json");

        try {
            // 1. Parse geo.json → BakedGeoModel → inject into GeckoLib cache
            injectGeoModel(geoJson, modelLoc);

            // 2. Register texture in TextureManager
            registerTexture(texture, texLoc);

            // 3. Load animation (optional)
            Optional<Path> animPath = Files.exists(animation) ? Optional.of(animation) : Optional.empty();
            if (animPath.isPresent()) {
                injectAnimation(animation, animLoc);
            }

            REGISTRY.put(modelId, new ModelEntry(modelId, geoJson, texture, animPath, modelLoc, texLoc, animLoc));
            StrawStatues.LOGGER.info("Loaded imported model: {}", modelId);

        } catch (Exception e) {
            StrawStatues.LOGGER.warn("Failed to load imported model '{}': {}", modelId, e.getMessage());
        }
    }

    // ── GeoJSON injection into GeckoLibCache ───────────────

    private static void injectGeoModel(Path geoJsonPath, ResourceLocation location) throws IOException {
        String content = Files.readString(geoJsonPath, Charset.defaultCharset());
        JsonObject jsonObj = net.minecraft.util.GsonHelper.fromJson(GEO_GSON, content, JsonObject.class);
        Model model = GEO_GSON.fromJson(jsonObj, Model.class);

        GeometryTree geometryTree = GeometryTree.fromModel(model);
        var bakedModel = BakedModelFactory.DEFAULT_FACTORY.constructGeoModel(geometryTree);

        GeckoLibCache.getBakedModels().put(location, bakedModel);
    }

    private static void injectAnimation(Path animPath, ResourceLocation location) throws IOException {
        String content = Files.readString(animPath, Charset.defaultCharset());
        JsonObject jsonObj = net.minecraft.util.GsonHelper.fromJson(GEO_GSON, content, JsonObject.class);
        BakedAnimations bakedAnimations = GEO_GSON.fromJson(jsonObj, BakedAnimations.class);

        GeckoLibCache.getBakedAnimations().put(location, bakedAnimations);
    }

    // ── Texture registration ───────────────────────────────

    private static void registerTexture(Path texturePath, ResourceLocation location) throws IOException {
        Minecraft mc = Minecraft.getInstance();
        try (NativeImage img = NativeImage.read(texturePath.toUri().toURL().openStream())) {
            DynamicTexture dynTex = new DynamicTexture(img);
            mc.getTextureManager().register(location, dynTex);
        }
    }

    // ─── Config helpers ─────────────────────────────────────

    /**
     * Copies model files from a temporary directory (extracted from zip) to the config directory.
     */
    public static Path getModelDir(String modelId) {
        return CONFIG_DIR.resolve(modelId);
    }

    /**
     * Trigger a full reload after importing a new model.
     */
    public static void reload() {
        // Reset GeckoLib cache for our namespace - remove old entries
        GeckoLibCache.getBakedModels().keySet()
                .removeIf(key -> key.getNamespace().equals(NAMESPACE));
        GeckoLibCache.getBakedAnimations().keySet()
                .removeIf(key -> key.getNamespace().equals(NAMESPACE));

        scanAndLoad();
    }
}
