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
import java.nio.charset.StandardCharsets;
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

    // Built-in placeholder model resource locations (always available)
    public static final ResourceLocation DEFAULT_MODEL_LOC = new ResourceLocation(NAMESPACE, "geo/default.geo.json");
    public static final ResourceLocation DEFAULT_TEXTURE_LOC = new ResourceLocation(NAMESPACE, "textures/entity/default.png");

    private static final Map<String, ModelEntry> REGISTRY = new ConcurrentHashMap<>();
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

    public static Set<String> getAvailableModelIds() {
        return REGISTRY.keySet();
    }

    public static boolean isModelLoaded(String modelId) {
        return REGISTRY.containsKey(modelId);
    }

    public static Optional<ModelEntry> getModel(String modelId) {
        return Optional.ofNullable(REGISTRY.get(modelId));
    }

    // ── Initialisation ──────────────────────────────────────

    /**
     * Register the built-in default model (always available).
     * Called once during client setup.
     */
    public static void registerDefaultModel() {
        registerDefaultModelNow(DEFAULT_MODEL_LOC);
        StrawStatues.LOGGER.info("Registered default placeholder model");
    }

    // ── Scanning & Loading ─────────────────────────────────

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

    private static void loadModelFromDir(String modelId, Path dir) {
        // Scan directory for any .geo.json, .png, and .animation.json files
        Path geoJson = null;
        Path texture = null;
        Path animation = null;

        try (var files = Files.list(dir)) {
            var fileList = files.toList();
            for (Path f : fileList) {
                String name = f.getFileName().toString().toLowerCase();
                if (name.endsWith(".geo.json") && geoJson == null) {
                    geoJson = f;
                } else if (name.endsWith(".png") && texture == null) {
                    texture = f;
                } else if (name.endsWith(".animation.json") && animation == null) {
                    animation = f;
                }
            }
        } catch (IOException e) {
            StrawStatues.LOGGER.debug("Cannot list directory '{}'", modelId);
            return;
        }

        if (geoJson == null || texture == null) {
            StrawStatues.LOGGER.debug("Skipping '{}': no .geo.json or .png file found in directory", modelId);
            return;
        }

        String safeId = modelId.toLowerCase(Locale.ROOT);
        ResourceLocation modelLoc = new ResourceLocation(NAMESPACE, "geo/" + safeId + ".geo.json");
        ResourceLocation texLoc = new ResourceLocation(NAMESPACE, "textures/entity/" + safeId + ".png");
        ResourceLocation animLoc = new ResourceLocation(NAMESPACE, "animations/" + safeId + ".animation.json");

        // Try to parse geo.json (required)
        try {
            injectGeoModel(geoJson, modelLoc);
        } catch (Exception e) {
            StrawStatues.LOGGER.warn("Failed to load geo.json for '{}': {}", modelId, e.getMessage());
            return;
        }
        // Try to register texture (required)
        try {
            registerTexture(texture, texLoc);
        } catch (Exception e) {
            StrawStatues.LOGGER.warn("Failed to load texture for '{}': {}", modelId, e.getMessage());
            return;
        }
        // Try to load animation (optional, failure is non-fatal)
        Optional<Path> animPath = Optional.empty();
        if (animation != null && Files.exists(animation)) {
            try {
                injectAnimation(animation, animLoc);
                animPath = Optional.of(animation);
            } catch (Exception e) {
                StrawStatues.LOGGER.debug("Skipping animation for '{}': {}", modelId, e.getMessage());
            }
        }

        REGISTRY.put(modelId, new ModelEntry(modelId, geoJson, texture, animPath, modelLoc, texLoc, animLoc));
        StrawStatues.LOGGER.info("Loaded imported model: {} (geo: {}, tex: {})", modelId,
                geoJson.getFileName(), texture.getFileName());
    }

    // ── GeoJSON injection ──────────────────────────────────

    private static void injectGeoModel(Path geoJsonPath, ResourceLocation location) throws IOException {
        String content = Files.readString(geoJsonPath, StandardCharsets.UTF_8);
        JsonObject jsonObj = net.minecraft.util.GsonHelper.fromJson(GEO_GSON, content, JsonObject.class);
        Model model = GEO_GSON.fromJson(jsonObj, Model.class);
        GeometryTree geometryTree = GeometryTree.fromModel(model);
        var bakedModel = BakedModelFactory.DEFAULT_FACTORY.constructGeoModel(geometryTree);
        GeckoLibCache.getBakedModels().put(location, bakedModel);
    }

    private static void injectAnimation(Path animPath, ResourceLocation location) throws IOException {
        String content = Files.readString(animPath, StandardCharsets.UTF_8);
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

    /**
     * Re-register the default model at a given location (called when GeckoLib cache is cleared by reload).
     */
    public static void registerDefaultModelNow(ResourceLocation location) {
        String defaultGeo = """
                {"format_version":"1.12.0","minecraft:geometry":[{"description":{"identifier":"geometry.default","texture_width":64,"texture_height":64,"visible_bounds_offset":[0,0,0],"visible_bounds_width":2,"visible_bounds_height":2},"bones":[{"name":"body","pivot":[0,0,0],"cubes":[{"origin":[-8,0,-8],"size":[16,16,16],"uv":[0,0]}]}]}]}""";
        try {
            JsonObject jsonObj = net.minecraft.util.GsonHelper.fromJson(GEO_GSON, defaultGeo, JsonObject.class);
            Model model = GEO_GSON.fromJson(jsonObj, Model.class);
            GeometryTree tree = GeometryTree.fromModel(model);
            var bakedModel = BakedModelFactory.DEFAULT_FACTORY.constructGeoModel(tree);
            GeckoLibCache.getBakedModels().put(location, bakedModel);
        } catch (Exception ignored) {}
    }

    /**
     * Reload a single model by ID (re-injects into GeckoLib cache).
     */
    public static void reloadSingle(String modelId) {
        var entry = REGISTRY.get(modelId);
        if (entry == null) return;
        try {
            injectGeoModel(entry.geoJsonPath(), entry.modelLocation());
            if (entry.hasAnimation()) {
                injectAnimation(entry.animationPath().get(), entry.animationLocation());
            }
        } catch (Exception ignored) {}
    }

    // ── Config helpers ─────────────────────────────────────

    public static Path getModelDir(String modelId) {
        return CONFIG_DIR.resolve(modelId);
    }

    public static void reload() {
        GeckoLibCache.getBakedModels().keySet()
                .removeIf(key -> key.getNamespace().equals(NAMESPACE) && !key.equals(DEFAULT_MODEL_LOC));
        GeckoLibCache.getBakedAnimations().keySet()
                .removeIf(key -> key.getNamespace().equals(NAMESPACE));
        scanAndLoad();
    }
}
