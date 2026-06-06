package fuzs.strawstatues.importmodel;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/**
 * Per-entity data for an imported model statue.
 * Stored in NBT and synced via SynchedEntityData.
 */
public class ImportedModelData {

    private static final String BACKEND_KEY = "Backend";
    private static final String MODEL_ID_KEY = "ModelId";
    private static final String ANIMATION_KEY = "Animation";

    private ImportedModelBackend backend = ImportedModelBackend.GECKOLIB;
    private String modelId = "";
    private String animation = "idle";

    // ── Constructors ───────────────────────────────────────

    public ImportedModelData() {}

    public ImportedModelData(ImportedModelBackend backend, String modelId, String animation) {
        this.backend = backend;
        this.modelId = modelId != null ? modelId : "";
        this.animation = animation != null ? animation : "idle";
    }

    // ── Getters ────────────────────────────────────────────

    public ImportedModelBackend getBackend() {
        return this.backend;
    }

    public void setBackend(ImportedModelBackend backend) {
        this.backend = backend;
    }

    public String getModelId() {
        return this.modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId != null ? modelId : "";
    }

    public String getAnimation() {
        return this.animation;
    }

    public void setAnimation(String animation) {
        this.animation = animation != null ? animation : "idle";
    }

    // ── Validation ─────────────────────────────────────────
    // Model existence is checked on client side; on server just check non-empty ID

    public boolean hasModel() {
        return !this.modelId.isEmpty();
    }

    // ── NBT ────────────────────────────────────────────────

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString(BACKEND_KEY, this.backend.getSerializedName());
        tag.putString(MODEL_ID_KEY, this.modelId);
        tag.putString(ANIMATION_KEY, this.animation);
        return tag;
    }

    public static ImportedModelData fromTag(CompoundTag tag) {
        ImportedModelData data = new ImportedModelData();
        if (tag.contains(BACKEND_KEY, Tag.TAG_STRING)) {
            data.backend = ImportedModelBackend.fromName(tag.getString(BACKEND_KEY));
        }
        if (tag.contains(MODEL_ID_KEY, Tag.TAG_STRING)) {
            data.modelId = tag.getString(MODEL_ID_KEY);
        }
        if (tag.contains(ANIMATION_KEY, Tag.TAG_STRING)) {
            data.animation = tag.getString(ANIMATION_KEY);
        }
        return data;
    }

    public ImportedModelData copy() {
        return new ImportedModelData(this.backend, this.modelId, this.animation);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ImportedModelData that)) return false;
        return this.backend == that.backend
                && this.modelId.equals(that.modelId)
                && this.animation.equals(that.animation);
    }

    @Override
    public int hashCode() {
        int result = backend.hashCode();
        result = 31 * result + modelId.hashCode();
        result = 31 * result + animation.hashCode();
        return result;
    }
}
