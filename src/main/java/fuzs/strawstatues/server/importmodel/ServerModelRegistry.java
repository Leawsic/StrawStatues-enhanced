package fuzs.strawstatues.server.importmodel;

import fuzs.strawstatues.StrawStatues;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class ServerModelRegistry {
    private static final Path SERVER_DIR = Paths.get("config", StrawStatues.MOD_ID, "server_models");
    private static final Set<String> AVAILABLE = ConcurrentHashMap.newKeySet();
    private ServerModelRegistry() {}

    public static void init() {
        try { Files.createDirectories(SERVER_DIR); } catch (IOException ignored) {}
        rescan();
    }

    public static void rescan() {
        AVAILABLE.clear();
        if (!Files.isDirectory(SERVER_DIR)) return;
        try (var dirStream = Files.newDirectoryStream(SERVER_DIR)) {
            for (Path dir : dirStream) {
                if (!Files.isDirectory(dir)) continue;
                String id = dir.getFileName().toString();
                if (id.isEmpty() || id.startsWith(".")) continue;
                if (Files.exists(dir.resolve("model.geo.json")) && Files.exists(dir.resolve("texture.png"))) {
                    AVAILABLE.add(id);
                }
            }
        } catch (IOException ignored) {}
    }

    public static Set<String> getAvailableModelIds() { return Collections.unmodifiableSet(AVAILABLE); }
    public static boolean isModelAvailable(String id) { return AVAILABLE.contains(id); }

    public static boolean saveUploadedModel(String modelId, Map<String, byte[]> files) {
        Path dir = SERVER_DIR.resolve(modelId);
        try {
            Files.createDirectories(dir);
            for (var entry : files.entrySet()) {
                Files.write(dir.resolve(entry.getKey()), entry.getValue());
            }
            AVAILABLE.add(modelId);
            return true;
        } catch (IOException e) {
            StrawStatues.LOGGER.warn("Failed to save uploaded model '{}'", modelId, e);
            return false;
        }
    }

    public static byte[] readModelFile(String modelId, String fileName) {
        Path f = SERVER_DIR.resolve(modelId).resolve(fileName);
        if (!Files.exists(f)) return null;
        try { return Files.readAllBytes(f); } catch (IOException e) { return null; }
    }

    public static List<String> listModelFiles(String modelId) {
        Path dir = SERVER_DIR.resolve(modelId);
        if (!Files.isDirectory(dir)) return List.of();
        try (var s = Files.list(dir)) {
            return s.map(p -> p.getFileName().toString()).toList();
        } catch (IOException e) { return List.of(); }
    }
}
