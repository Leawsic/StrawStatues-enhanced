package fuzs.strawstatues.client.importmodel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public final class RemoteModelCache {
    private static Set<String> available = Collections.emptySet();
    private static final Map<String, Map<String, String>> MODEL_HASHES = new HashMap<>(); // modelId → {fileName → sha256}

    private RemoteModelCache() {}

    public static void setAvailable(Set<String> ids) { available = new HashSet<>(ids); }
    public static Set<String> getAvailable() { return Collections.unmodifiableSet(available); }
    public static boolean isAvailable(String id) { return available.contains(id); }
    public static void clear() { available = Collections.emptySet(); MODEL_HASHES.clear(); }

    /** Store hashes received from server after download completes. */
    public static void storeHashes(String modelId, Map<String, String> hashes) {
        MODEL_HASHES.put(modelId, new HashMap<>(hashes));
    }

    /** Get hashes for a model, or null if unknown. */
    public static Map<String, String> getHashes(String modelId) {
        return MODEL_HASHES.get(modelId);
    }

    /**
     * Compute SHA-256 hashes for all files in a local model directory.
     * Returns {fileName → sha256} or empty map on failure.
     */
    public static Map<String, String> computeLocalHashes(String modelId) {
        Map<String, String> result = new LinkedHashMap<>();
        Path dir = ImportedModelRegistry.getModelDir(modelId);
        if (!Files.isDirectory(dir)) return result;
        try (var stream = Files.list(dir)) {
            for (Path f : stream.toList()) {
                if (!Files.isRegularFile(f)) continue;
                try {
                    byte[] data = Files.readAllBytes(f);
                    MessageDigest md = MessageDigest.getInstance("SHA-256");
                    byte[] hash = md.digest(data);
                    StringBuilder sb = new StringBuilder(64);
                    for (byte b : hash) sb.append(String.format("%02x", b));
                    result.put(f.getFileName().toString(), sb.toString());
                } catch (NoSuchAlgorithmException | IOException ignored) {}
            }
        } catch (IOException ignored) {}
        return result;
    }
}
