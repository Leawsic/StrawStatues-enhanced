package fuzs.strawstatues.world.entity.decoration;

import fuzs.puzzlesapi.api.statues.v1.helper.ArmorStandInteractHelper;
import fuzs.puzzlesapi.api.statues.v1.world.inventory.data.ArmorStandScreenType;
import fuzs.puzzleslib.api.event.v1.core.EventResultHolder;
import fuzs.strawstatues.importmodel.ImportedModelData;
import fuzs.strawstatues.init.ModRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class ImportedStrawStatue extends StrawStatue implements GeoEntity {

    public static final String IMPORTED_MODEL_KEY = "ImportedModel";

    public static final EntityDataAccessor<CompoundTag> DATA_IMPORTED_MODEL =
            SynchedEntityData.defineId(ImportedStrawStatue.class, EntityDataSerializers.COMPOUND_TAG);

    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);

    @Nullable
    private ImportedModelData importedModel;

    public ImportedStrawStatue(EntityType<? extends ImportedStrawStatue> entityType, Level level) {
        super(entityType, level);
    }

    public ImportedStrawStatue(Level level, double x, double y, double z) {
        this(ModRegistry.IMPORTED_STRAW_STATUE_ENTITY_TYPE.get(), level);
        this.setPos(x, y, z);
    }

    // ── Synced Data ─────────────────────────────────────────

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_IMPORTED_MODEL, new CompoundTag());
    }

    // ── NBT ─────────────────────────────────────────────────

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.importedModel != null) {
            tag.put(IMPORTED_MODEL_KEY, this.importedModel.toTag());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(IMPORTED_MODEL_KEY, Tag.TAG_COMPOUND)) {
            CompoundTag imTag = tag.getCompound(IMPORTED_MODEL_KEY);
            this.importedModel = ImportedModelData.fromTag(imTag);
            this.entityData.set(DATA_IMPORTED_MODEL, imTag);
        }
    }

    // ── Sync updates ────────────────────────────────────────

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_IMPORTED_MODEL.equals(key)) {
            CompoundTag tag = this.entityData.get(DATA_IMPORTED_MODEL);
            if (!tag.isEmpty()) {
                this.importedModel = ImportedModelData.fromTag(tag);
            } else {
                this.importedModel = null;
            }
        }
    }

    // ── Accessors ───────────────────────────────────────────

    @Nullable
    public ImportedModelData getImportedModel() {
        return this.importedModel;
    }

    public void setImportedModel(@Nullable ImportedModelData data) {
        this.importedModel = data;
        if (data != null) {
            this.entityData.set(DATA_IMPORTED_MODEL, data.toTag());
        } else {
            this.entityData.set(DATA_IMPORTED_MODEL, new CompoundTag());
        }
    }

    // ── Screen types ────────────────────────────────────────

    @Override
    public ArmorStandScreenType[] getScreenTypes() {
        return new ArmorStandScreenType[]{
                ArmorStandScreenType.ROTATIONS, ArmorStandScreenType.POSES,
                ArmorStandScreenType.STYLE,
                ModRegistry.MODEL_PARTS_SCREEN_TYPE,
                ModRegistry.STRAW_STATUE_POSITION_SCREEN_TYPE,
                ModRegistry.STRAW_STATUE_SCALE_SCREEN_TYPE,
                ArmorStandScreenType.EQUIPMENT
        };
    }

    // ── Interaction ─────────────────────────────────────────
    // Model management is done via commands (/strawstatues import ...).
    // No custom interact needed — the default armor stand menu is used.

    // ── Interaction ─────────────────────────────────────────

    public static EventResultHolder<InteractionResult> onUseEntityAt(Player player, Level level, InteractionHand interactionHand, Entity target, Vec3 hitVector) {
        if (!player.isSpectator() && target.getType() == ModRegistry.IMPORTED_STRAW_STATUE_ENTITY_TYPE.get()) {
            return ArmorStandInteractHelper.tryOpenArmorStatueMenu(player, level, interactionHand, (ArmorStand) target, ModRegistry.IMPORTED_STRAW_STATUE_MENU_TYPE.get(), null);
        }
        return EventResultHolder.pass();
    }

    // ── GeoEntity implementation ────────────────────────────

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 0, state -> {
            ImportedModelData modelData = this.importedModel;
            if (modelData == null || !modelData.hasModel()) return PlayState.STOP;

            // Play looping animation matching the model ID, or just keep alive
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animCache;
    }

    @Override
    public double getTick(Object object) {
        return this.tickCount;
    }
}
