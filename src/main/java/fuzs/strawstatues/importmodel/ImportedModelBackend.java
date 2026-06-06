package fuzs.strawstatues.importmodel;

import net.minecraft.util.StringRepresentable;

public enum ImportedModelBackend implements StringRepresentable {
    GECKOLIB("geckolib"),
    BIL("bil");

    private final String name;

    ImportedModelBackend(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public static ImportedModelBackend fromName(String name) {
        for (ImportedModelBackend backend : values()) {
            if (backend.name.equals(name)) return backend;
        }
        return GECKOLIB;
    }
}
