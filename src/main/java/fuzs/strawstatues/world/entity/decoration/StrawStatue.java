package fuzs.strawstatues.world.entity.decoration;

import com.google.common.collect.ImmutableSortedMap;
import com.mojang.authlib.GameProfile;
import fuzs.puzzlesapi.api.statues.v1.helper.ArmorStandInteractHelper;
import fuzs.puzzlesapi.api.statues.v1.world.entity.decoration.ArmorStandDataProvider;
import fuzs.puzzlesapi.api.statues.v1.world.inventory.data.*;
import fuzs.puzzleslib.api.event.v1.core.EventResultHolder;
import fuzs.strawstatues.init.ModRegistry;
import fuzs.strawstatues.mixin.accessor.ArmorStandAccessor;
import net.minecraft.core.Rotations;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.NavigableMap;
import java.util.Optional;

public class StrawStatue extends ArmorStand implements ArmorStandDataProvider {
    public static final Rotations DEFAULT_ENTITY_ROTATIONS = new Rotations(180.0F, 0.0F, 180.0F);
    public static final float DEFAULT_ENTITY_SCALE = 3.0F;
    public static final float MIN_MODEL_SCALE = 1.0F;
    public static final float MAX_MODEL_SCALE = 8.0F;
    public static final String OWNER_KEY = "Owner";
    public static final String SLIM_ARMS_KEY = "SlimArms";
    public static final String CROUCHING_KEY = "Crouching";
    public static final String MODEL_PARTS_KEY = "ModelParts";
    public static final String ENTITY_SCALE_KEY = "EntityScale";
    public static final String ENTITY_ROTATIONS_KEY = "EntityRotations";
    public static final String EYE_DATA_KEY = "EyeData";
    public static final String SUB_BONE_MODE_KEY = "SubBoneMode";
    public static final String SUB_BONE_TARGET_KEY = "SubBoneTargetUpper";
    public static final String RIGHT_ELBOW_KEY = "RightElbow";
    public static final String LEFT_ELBOW_KEY = "LeftElbow";
    public static final String RIGHT_KNEE_KEY = "RightKnee";
    public static final String LEFT_KNEE_KEY = "LeftKnee";
    public static final EntityDataAccessor<Optional<GameProfile>> DATA_OWNER = SynchedEntityData.defineId(StrawStatue.class, ModRegistry.GAME_PROFILE_ENTITY_DATA_SERIALIZER);
    public static final EntityDataAccessor<Boolean> DATA_SLIM_ARMS = SynchedEntityData.defineId(StrawStatue.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> DATA_CROUCHING = SynchedEntityData.defineId(StrawStatue.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Byte> DATA_PLAYER_MODE_CUSTOMISATION = SynchedEntityData.defineId(StrawStatue.class, EntityDataSerializers.BYTE);
    public static final EntityDataAccessor<Float> DATA_ENTITY_SCALE = SynchedEntityData.defineId(StrawStatue.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Rotations> DATA_ENTITY_ROTATIONS = SynchedEntityData.defineId(StrawStatue.class, EntityDataSerializers.ROTATIONS);
    public static final EntityDataAccessor<CompoundTag> DATA_EYE_DATA = SynchedEntityData.defineId(StrawStatue.class, EntityDataSerializers.COMPOUND_TAG);
    public static final EntityDataAccessor<Boolean> DATA_SUB_BONE_MODE = SynchedEntityData.defineId(StrawStatue.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> DATA_SUB_BONE_TARGET = SynchedEntityData.defineId(StrawStatue.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Float> DATA_RIGHT_ELBOW = SynchedEntityData.defineId(StrawStatue.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> DATA_LEFT_ELBOW = SynchedEntityData.defineId(StrawStatue.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> DATA_RIGHT_KNEE = SynchedEntityData.defineId(StrawStatue.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> DATA_LEFT_KNEE = SynchedEntityData.defineId(StrawStatue.class, EntityDataSerializers.FLOAT);

    private final NavigableMap<Float, EntityDimensions> defaultDimensions;
    private final NavigableMap<Float, EntityDimensions> babyDimensions;
    public float entityScaleO = DEFAULT_ENTITY_SCALE;
    public Rotations entityRotationsO = DEFAULT_ENTITY_ROTATIONS;
    @Nullable
    private StrawStatueEyeData eyeData;

    public StrawStatue(EntityType<? extends StrawStatue> entityType, Level level) {
        super(entityType, level);
        // important to enable arms beyond rendering in the model to allow for in world interactions (putting items into the hands by clicking on the statue)
        ArmorStandStyleOption.setArmorStandData(this, true, ArmorStand.CLIENT_FLAG_SHOW_ARMS);
        ArmorStandStyleOption.setArmorStandData(this, true, ArmorStand.CLIENT_FLAG_NO_BASEPLATE);
        this.defaultDimensions = buildStatueDimensions(entityType, false);
        this.babyDimensions = buildStatueDimensions(entityType, true);
    }

    public StrawStatue(Level level, double x, double y, double z) {
        this(ModRegistry.STRAW_STATUE_ENTITY_TYPE.get(), level);
        this.setPos(x, y, z);
    }

    private static NavigableMap<Float, EntityDimensions> buildStatueDimensions(EntityType<?> entityType, boolean forBaby) {
        final float defaultScale = DEFAULT_ENTITY_SCALE * (forBaby ? 2.0F : 1.0F);
        ImmutableSortedMap.Builder<Float, EntityDimensions> builder = ImmutableSortedMap.naturalOrder();
        for (float scale = MIN_MODEL_SCALE; scale <= MAX_MODEL_SCALE; scale += 0.5F) {
            builder.put(scale - 0.25F, entityType.getDimensions().scale(scale / defaultScale));
        }
        return builder.build();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_OWNER, Optional.empty());
        this.entityData.define(DATA_SLIM_ARMS, false);
        this.entityData.define(DATA_CROUCHING, false);
        this.entityData.define(DATA_PLAYER_MODE_CUSTOMISATION, getAllModelParts());
        this.entityData.define(DATA_ENTITY_SCALE, DEFAULT_ENTITY_SCALE);
        this.entityData.define(DATA_ENTITY_ROTATIONS, DEFAULT_ENTITY_ROTATIONS);
        this.entityData.define(DATA_EYE_DATA, new CompoundTag());
        this.entityData.define(DATA_SUB_BONE_MODE, false);
        this.entityData.define(DATA_SUB_BONE_TARGET, true);
        this.entityData.define(DATA_RIGHT_ELBOW, 0.0F);
        this.entityData.define(DATA_LEFT_ELBOW, 0.0F);
        this.entityData.define(DATA_RIGHT_KNEE, 0.0F);
        this.entityData.define(DATA_LEFT_KNEE, 0.0F);
    }

    private static byte getAllModelParts() {
        byte value = 0;
        for (PlayerModelPart modelPart : PlayerModelPart.values()) {
            value |= modelPart.getMask();
        }
        return value;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean(SLIM_ARMS_KEY, this.slimArms());
        tag.putBoolean(CROUCHING_KEY, this.isCrouching());
        tag.putByte(MODEL_PARTS_KEY, this.entityData.get(DATA_PLAYER_MODE_CUSTOMISATION));
        this.entityData.get(DATA_OWNER).ifPresent(owner -> {
            CompoundTag gameProfileTag = new CompoundTag();
            NbtUtils.writeGameProfile(gameProfileTag, owner);
            tag.put(OWNER_KEY, gameProfileTag);
        });
        tag.putFloat(ENTITY_SCALE_KEY, this.getEntityScale());
        Rotations entityRotations = this.getEntityRotations();
        if (!DEFAULT_ENTITY_ROTATIONS.equals(entityRotations)) {
            tag.put(ENTITY_ROTATIONS_KEY, entityRotations.save());
        }
        if (this.eyeData != null && this.eyeData.isValid()) {
            tag.put(EYE_DATA_KEY, this.eyeData.toTag());
        }
        tag.putBoolean(SUB_BONE_MODE_KEY, this.isSubBoneMode());
        tag.putBoolean(SUB_BONE_TARGET_KEY, this.isSubBoneTargetUpper());
        if (this.isSubBoneMode()) {
            tag.putFloat(RIGHT_ELBOW_KEY, this.getRightElbow());
            tag.putFloat(LEFT_ELBOW_KEY, this.getLeftElbow());
            tag.putFloat(RIGHT_KNEE_KEY, this.getRightKnee());
            tag.putFloat(LEFT_KNEE_KEY, this.getLeftKnee());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(SLIM_ARMS_KEY, Tag.TAG_BYTE)) {
            this.setSlimArms(tag.getBoolean(SLIM_ARMS_KEY));
        }
        if (tag.contains(CROUCHING_KEY, Tag.TAG_BYTE)) {
            this.setCrouching(tag.getBoolean(CROUCHING_KEY));
        }
        if (tag.contains(MODEL_PARTS_KEY, Tag.TAG_BYTE)) {
            this.entityData.set(DATA_PLAYER_MODE_CUSTOMISATION, tag.getByte(MODEL_PARTS_KEY));
        }
        if (tag.contains(OWNER_KEY, Tag.TAG_COMPOUND)) {
            this.verifyAndSetOwner(NbtUtils.readGameProfile(tag.getCompound(OWNER_KEY)));
        }
        if (tag.contains(ENTITY_SCALE_KEY, Tag.TAG_FLOAT)) {
            this.setEntityScale(tag.getFloat(ENTITY_SCALE_KEY));
            this.entityScaleO = this.getEntityScale();
        }
        if (tag.contains(ENTITY_ROTATIONS_KEY, Tag.TAG_LIST)) {
            Rotations entityRotations = new Rotations(tag.getList(ENTITY_ROTATIONS_KEY, Tag.TAG_FLOAT));
            this.setEntityRotations(entityRotations.getX(), entityRotations.getZ());
            this.entityRotationsO = this.getEntityRotations();
        }
        if (tag.contains(EYE_DATA_KEY, Tag.TAG_COMPOUND)) {
            CompoundTag eyeTag = tag.getCompound(EYE_DATA_KEY);
            this.eyeData = StrawStatueEyeData.fromTag(eyeTag);
            this.entityData.set(DATA_EYE_DATA, eyeTag);
        }
        if (tag.contains(SUB_BONE_MODE_KEY, Tag.TAG_BYTE)) {
            this.setSubBoneMode(tag.getBoolean(SUB_BONE_MODE_KEY));
        }
        if (tag.contains(SUB_BONE_TARGET_KEY, Tag.TAG_BYTE)) {
            this.setSubBoneTargetUpper(tag.getBoolean(SUB_BONE_TARGET_KEY));
        }
        if (tag.contains(RIGHT_ELBOW_KEY, Tag.TAG_FLOAT)) {
            this.setRightElbow(tag.getFloat(RIGHT_ELBOW_KEY));
            this.setLeftElbow(tag.getFloat(LEFT_ELBOW_KEY));
            this.setRightKnee(tag.getFloat(RIGHT_KNEE_KEY));
            this.setLeftKnee(tag.getFloat(LEFT_KNEE_KEY));
        }
    }

    @Override
    protected float getStandingEyeHeight(Pose pose, EntityDimensions dimensions) {
        return dimensions.height * 0.85F;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        if (this.isMarker() || this.babyDimensions == null || this.defaultDimensions == null) return super.getDimensions(pose);
        NavigableMap<Float, EntityDimensions> dimensions = this.isBaby() ? this.babyDimensions : this.defaultDimensions;
        return dimensions.floorEntry(this.getEntityScale()).getValue();
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        if (DATA_ENTITY_SCALE.equals(key)) {
            this.refreshDimensions();
        }
        if (DATA_EYE_DATA.equals(key)) {
            CompoundTag tag = this.entityData.get(DATA_EYE_DATA);
            if (!tag.isEmpty()) {
                this.eyeData = StrawStatueEyeData.fromTag(tag);
            } else {
                this.eyeData = null;
            }
        }
        super.onSyncedDataUpdated(key);
    }

    @Override
    public InteractionResult interactAt(Player player, Vec3 vec, InteractionHand hand) {
        ItemStack itemInHand = player.getItemInHand(hand);
        InteractionResult result = super.interactAt(player, vec, hand);
        if (!player.level().isClientSide && !player.isSpectator() && result == InteractionResult.SUCCESS && itemInHand.is(Items.PLAYER_HEAD) && itemInHand.hasTag()) {
            GameProfile gameProfile = null;
            CompoundTag compoundTag = itemInHand.getTag();
            if (compoundTag.contains("SkullOwner", Tag.TAG_COMPOUND)) {
                gameProfile = NbtUtils.readGameProfile(compoundTag.getCompound("SkullOwner"));
            } else if (compoundTag.contains("SkullOwner", Tag.TAG_STRING) && !StringUtils.isBlank(compoundTag.getString("SkullOwner"))) {
                gameProfile = new GameProfile(null, compoundTag.getString("SkullOwner"));
            }
            if (gameProfile != null) this.verifyAndSetOwner(gameProfile);
        }
        return result;
    }

    public static EventResultHolder<InteractionResult> onUseEntityAt(Player player, Level level, InteractionHand interactionHand, Entity target, Vec3 hitVector) {
        if (!player.isSpectator() && target.getType() == ModRegistry.STRAW_STATUE_ENTITY_TYPE.get()) {
            return ArmorStandInteractHelper.tryOpenArmorStatueMenu(player, level, interactionHand, (ArmorStand) target, ModRegistry.STRAW_STATUE_MENU_TYPE.get(), null);
        }
        return EventResultHolder.pass();
    }

    @Override
    public boolean isShowArms() {
        return true;
    }

    @Override
    public boolean isNoBasePlate() {
        return true;
    }

    public Optional<GameProfile> getOwner() {
        return this.entityData.get(DATA_OWNER);
    }

    public void verifyAndSetOwner(@Nullable GameProfile gameProfile) {
        // check for max name length here as client will crash when value is exceeded
        if (gameProfile != null && (!gameProfile.isComplete() || gameProfile.getName().length() > 16)) {
            if (gameProfile.getName().length() > 16) {
                if (gameProfile.getId() != null) {
                    // will throw exception if both uuid and name are empty
                    gameProfile = new GameProfile(gameProfile.getId(), "");
                } else {
                    this.setOwner(null);
                    return;
                }
            }
            SkullBlockEntity.updateGameprofile(gameProfile, this::setOwner);
        } else {
            this.setOwner(gameProfile);
        }
    }

    private void setOwner(@Nullable GameProfile gameProfile) {
        this.entityData.set(DATA_OWNER, Optional.ofNullable(gameProfile));
    }

    public boolean slimArms() {
        return this.entityData.get(DATA_SLIM_ARMS);
    }

    public void setSlimArms(boolean slimArms) {
        this.entityData.set(DATA_SLIM_ARMS, slimArms);
    }

    public boolean isModelPartShown(PlayerModelPart part) {
        return (this.getEntityData().get(DATA_PLAYER_MODE_CUSTOMISATION) & part.getMask()) == part.getMask();
    }

    public void setModelPart(PlayerModelPart modelPart, boolean enable) {
        this.entityData.set(DATA_PLAYER_MODE_CUSTOMISATION, ArmorStandStyleOption.setBit(this.entityData.get(DATA_PLAYER_MODE_CUSTOMISATION), modelPart.getMask(), enable));
    }

    public float getEntityScale() {
        return this.entityData.get(DATA_ENTITY_SCALE);
    }

    public float getEntityXRotation() {
        return this.getEntityRotations().getX();
    }

    public Rotations getEntityRotations() {
        return this.entityData.get(DATA_ENTITY_ROTATIONS);
    }

    public float getEntityZRotation() {
        return this.getEntityRotations().getZ();
    }

    public void setEntityXRotation(float rotationX) {
        this.setEntityRotations(rotationX, this.getEntityZRotation());
    }

    public void setEntityZRotation(float rotationZ) {
        this.setEntityRotations(this.getEntityXRotation(), rotationZ);
    }

    public void setEntityRotations(float rotationX, float rotationZ) {
        rotationX = Mth.clamp(rotationX, 0.0F, 360.0F);
        rotationZ = Mth.clamp(rotationZ, 0.0F, 360.0F);
        this.entityData.set(DATA_ENTITY_ROTATIONS, new Rotations(rotationX, 0.0F, rotationZ));
    }

    public void setEntityScale(float modelScale) {
        modelScale = clampModelScale(modelScale);
        this.entityData.set(DATA_ENTITY_SCALE, modelScale);
    }

    public static float clampModelScale(double modelScale) {
        modelScale = (int) (modelScale * 10.0) / 10.0;
        return Mth.clamp((float) modelScale, MIN_MODEL_SCALE, MAX_MODEL_SCALE);
    }

    @Nullable
    public StrawStatueEyeData getEyeData() {
        return this.eyeData;
    }

    public void setEyeData(@Nullable StrawStatueEyeData eyeData) {
        this.eyeData = eyeData;
        if (eyeData != null && eyeData.isValid()) {
            this.entityData.set(DATA_EYE_DATA, eyeData.toTag());
        } else {
            this.entityData.set(DATA_EYE_DATA, new CompoundTag());
        }
    }

    @Override
    public void baseTick() {
        super.baseTick();
        this.entityScaleO = this.getEntityScale();
        this.entityRotationsO = this.getEntityRotations();
    }

    public void setCrouching(boolean crouching) {
        this.entityData.set(DATA_CROUCHING, crouching);
    }

    @Override
    public boolean isCrouching() {
        return this.entityData.get(DATA_CROUCHING);
    }

    // ── Sub-bone mode ─────────────────────────────────────

    public boolean isSubBoneMode() {
        return this.entityData.get(DATA_SUB_BONE_MODE);
    }

    public void setSubBoneMode(boolean subBoneMode) {
        this.entityData.set(DATA_SUB_BONE_MODE, subBoneMode);
    }

    public boolean isSubBoneTargetUpper() {
        return this.entityData.get(DATA_SUB_BONE_TARGET);
    }

    public void setSubBoneTargetUpper(boolean upper) {
        this.entityData.set(DATA_SUB_BONE_TARGET, upper);
    }

    public float getRightElbow() {
        return this.entityData.get(DATA_RIGHT_ELBOW);
    }

    public void setRightElbow(float angle) {
        this.entityData.set(DATA_RIGHT_ELBOW, Mth.clamp(angle, -90.0F, 90.0F));
    }

    public float getLeftElbow() {
        return this.entityData.get(DATA_LEFT_ELBOW);
    }

    public void setLeftElbow(float angle) {
        this.entityData.set(DATA_LEFT_ELBOW, Mth.clamp(angle, -90.0F, 90.0F));
    }

    public float getRightKnee() {
        return this.entityData.get(DATA_RIGHT_KNEE);
    }

    public void setRightKnee(float angle) {
        this.entityData.set(DATA_RIGHT_KNEE, Mth.clamp(angle, -90.0F, 90.0F));
    }

    public float getLeftKnee() {
        return this.entityData.get(DATA_LEFT_KNEE);
    }

    public void setLeftKnee(float angle) {
        this.entityData.set(DATA_LEFT_KNEE, Mth.clamp(angle, -90.0F, 90.0F));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide && !this.isRemoved()) {
            if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                this.kill();
                return false;
            } else if (!this.isInvulnerableTo(source) && !this.isInvisible() && !this.isMarker()) {
                if (source.is(DamageTypeTags.IS_EXPLOSION)) {
                    ((ArmorStandAccessor) this).strawstatues$callBrokenByAnything(source);
                    this.kill();
                    return false;
                } else if (source.is(DamageTypeTags.IGNITES_ARMOR_STANDS)) {
                    if (this.isOnFire()) {
                        ((ArmorStandAccessor) this).strawstatues$callCauseDamage(source, 0.15F);
                    } else {
                        this.setSecondsOnFire(5);
                    }
                    return false;
                } else if (source.is(DamageTypeTags.BURNS_ARMOR_STANDS) && this.getHealth() > 0.5F) {
                    ((ArmorStandAccessor) this).strawstatues$callCauseDamage(source, 4.0F);
                    return false;
                } else {
                    boolean bl = source.getDirectEntity() instanceof AbstractArrow;
                    boolean bl2 = bl && ((AbstractArrow) source.getDirectEntity()).getPierceLevel() > 0;
                    boolean bl3 = "player".equals(source.getMsgId());
                    if (!bl3 && !bl) {
                        return false;
                    } else if (source.getEntity() instanceof Player && !((Player)source.getEntity()).getAbilities().mayBuild) {
                        return false;
                    } else if (source.isCreativePlayer()) {
                        this.playBrokenSound();
                        this.showBreakingParticles();
                        this.kill();
                        return bl2;
                    } else {
                        long gameTime = this.level().getGameTime();
                        if (gameTime - this.lastHit > 5L && !bl) {
                            this.level().broadcastEntityEvent(this, EntityEvent.ARMORSTAND_WOBBLE);
                            this.gameEvent(GameEvent.ENTITY_DAMAGE, source.getEntity());
                            this.lastHit = gameTime;
                            this.invulnerableTime = 20;
                            this.hurtDuration = 10;
                            this.hurtTime = this.hurtDuration;
                        } else {
                            this.brokenByPlayer(source);
                            this.showBreakingParticles();
                            this.kill();
                        }
                        return true;
                    }
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private void brokenByPlayer(DamageSource source) {
        Block.popResource(this.level(), this.blockPosition(), new ItemStack(ModRegistry.STRAW_STATUE_ITEM.get()));
        ((ArmorStandAccessor) this).strawstatues$callBrokenByAnything(source);
    }

    private void playBrokenSound() {
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GRASS_BREAK, this.getSoundSource(), 1.0F, 1.0F);
    }

    private void showBreakingParticles() {
        if (this.level() instanceof ServerLevel) {
            ((ServerLevel)this.level()).sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.HAY_BLOCK.defaultBlockState()), this.getX(), this.getY(0.6666666666666666), this.getZ(), 10, this.getBbWidth() / 4.0F, this.getBbHeight() / 4.0F, this.getBbWidth() / 4.0F, 0.05);
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EntityEvent.ARMORSTAND_WOBBLE) {
            this.lastHit = this.level().getGameTime();
        }
        super.handleEntityEvent(id);
    }

    @Override
    public LivingEntity.Fallsounds getFallSounds() {
        return new LivingEntity.Fallsounds(SoundEvents.GRASS_FALL, SoundEvents.GRASS_FALL);
    }

    @Override
    @Nullable
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.GRASS_HIT;
    }

    @Override
    @Nullable
    protected SoundEvent getDeathSound() {
        return SoundEvents.GRASS_BREAK;
    }

    @Override
    @Nullable
    public ItemStack getPickResult() {
        return new ItemStack(ModRegistry.STRAW_STATUE_ITEM.get());
    }

    @Override
    public ArmorStandScreenType[] getScreenTypes() {
        return new ArmorStandScreenType[]{ArmorStandScreenType.ROTATIONS, ArmorStandScreenType.POSES, ArmorStandScreenType.STYLE, ModRegistry.MODEL_PARTS_SCREEN_TYPE, ModRegistry.STRAW_STATUE_EYE_SCREEN_TYPE, ModRegistry.STRAW_STATUE_POSITION_SCREEN_TYPE, ModRegistry.STRAW_STATUE_SCALE_SCREEN_TYPE, ArmorStandScreenType.EQUIPMENT};
    }

    @Override
    public PosePartMutator[] getPosePartMutators() {
        return new PosePartMutator[]{PosePartMutator.HEAD, ModRegistry.CAPE_POSE_PART_MUTATOR, ModRegistry.RIGHT_ARM_POSE_MUTATOR, ModRegistry.LEFT_ARM_POSE_MUTATOR, PosePartMutator.RIGHT_LEG, PosePartMutator.LEFT_LEG};
    }

    @Override
    public ArmorStandPose getRandomPose(boolean clampRotations) {
        return ArmorStandPose.randomize(this.getPosePartMutators(), clampRotations);
    }

    @Override
    public ArmorStandStyleOption[] getStyleOptions() {
        return new ArmorStandStyleOption[]{ArmorStandStyleOptions.SHOW_NAME, ArmorStandStyleOptions.SMALL, ModRegistry.SLIM_ARMS_STYLE_OPTION, ModRegistry.CROUCHING_STYLE_OPTION, ArmorStandStyleOptions.NO_GRAVITY, ArmorStandStyleOptions.SEALED};
    }
}
