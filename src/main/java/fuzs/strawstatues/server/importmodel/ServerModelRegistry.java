package fuzs.strawstatues.server.importmodel;

import fuzs.strawstatues.StrawStatues;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class ServerModelRegistry {
    private static final Path SERVER_DIR = Paths.get("config", StrawStatues.MOD_ID, "server_models");
    private static final Set<String> AVAILABLE = ConcurrentHashMap.newKeySet();
    private static final Set<String> VALID_EXT = Set.of(".geo.json", ".png", ".animation.json");
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
                // Check for at least one .geo.json and one .png
                boolean hasGeo = false, hasPng = false;
                try (var files = Files.list(dir)) {
                    for (Path f : files.toList()) {
                        String n = f.getFileName().toString().toLowerCase();
                        if (n.endsWith(".geo.json")) hasGeo = true;
                        if (n.endsWith(".png")) hasPng = true;
                    }
                } catch (IOException ignored) {}
                if (hasGeo && hasPng) AVAILABLE.add(id);
            }
        } catch (IOException ignored) {}
    }

    public static Set<String> getAvailableModelIds() { return Collections.unmodifiableSet(AVAILABLE); }
    public static boolean isModelAvailable(String id) { return AVAILABLE.contains(id); }

    public static boolean saveUploadedModel(String modelId, Map<String, byte[]> files) {
        // Validate file names
        for (String name : files.keySet()) {
            String lower = name.toLowerCase();
            boolean valid = lower.endsWith(".geo.json") || lower.endsWith(".png") || lower.endsWith(".animation.json");
            if (!valid) {
                StrawStatues.LOGGER.warn("Rejected upload file '{}' for model '{}': invalid extension", name, modelId);
                return false;
            }
        }
        boolean hasGeo = files.keySet().stream().anyMatch(n -> n.toLowerCase().endsWith(".geo.json"));
        boolean hasPng = files.keySet().stream().anyMatch(n -> n.toLowerCase().endsWith(".png"));
        if (!hasGeo || !hasPng) {
            StrawStatues.LOGGER.warn("Rejected upload '{}': missing .geo.json or .png", modelId);
            return false;
        }
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
            return s.map(p -> p.getFileName().toString())
                     .filter(n -> !n.equals(".uploader"))
                     .toList();
        } catch (IOException e) { return List.of(); }
    }

    /** Read the uploader name from the metadata file, or empty string. */
    public static String getUploader(String modelId) {
        Path f = SERVER_DIR.resolve(modelId).resolve(".uploader");
        if (!Files.exists(f)) return "";
        try { return Files.readString(f).trim(); } catch (IOException e) { return ""; }
    }

    /** Return model IDs as "uploader:modelId" strings for display. */
    public static Set<String> getAvailableWithUploader() {
        Set<String> result = new LinkedHashSet<>();
        for (String id : AVAILABLE) {
            String uploader = getUploader(id);
            if (!uploader.isEmpty()) result.add(uploader + ":" + id);
            else result.add(id);
        }
        return result;
    }

    /** Compute SHA-256 hash for a model file, or null on failure. */
    public static String computeFileHash(String modelId, String fileName) {
        byte[] data = readModelFile(modelId, fileName);
        if (data == null) return null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data);
            StringBuilder sb = new StringBuilder(64);
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    /** Compute hashes for all files of a model, returning {fileName → hash}. */
    public static Map<String, String> computeAllHashes(String modelId) {
        Map<String, String> result = new LinkedHashMap<>();
        for (String f : listModelFiles(modelId)) {
            String hash = computeFileHash(modelId, f);
            if (hash != null) result.put(f, hash);
        }
        return result;
    }
}
