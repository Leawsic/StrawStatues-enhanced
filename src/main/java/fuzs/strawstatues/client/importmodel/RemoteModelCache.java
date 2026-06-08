package fuzs.strawstatues.client.importmodel;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class RemoteModelCache {
    private static Set<String> available = Collections.emptySet();
    private RemoteModelCache() {}

    public static void setAvailable(Set<String> ids) { available = new HashSet<>(ids); }
    public static Set<String> getAvailable() { return Collections.unmodifiableSet(available); }
    public static boolean isAvailable(String id) { return available.contains(id); }
    public static void clear() { available = Collections.emptySet(); }
}
