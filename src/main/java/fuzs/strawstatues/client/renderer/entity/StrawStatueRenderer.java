package fuzs.strawstatues.client.renderer.entity;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import fuzs.strawstatues.StrawStatues;
import fuzs.strawstatues.client.init.ModClientRegistry;
import fuzs.strawstatues.client.model.StrawStatueArmorModel;
import fuzs.strawstatues.client.model.StrawStatueModel;
import fuzs.strawstatues.client.renderer.entity.layers.StrawStatueCapeLayer;
import fuzs.strawstatues.client.renderer.entity.layers.StrawStatueDeadmau5EarsLayer;
import fuzs.strawstatues.world.entity.decoration.StrawStatue;
import fuzs.strawstatues.world.entity.decoration.StrawStatueEyeData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class StrawStatueRenderer extends LivingEntityRenderer<StrawStatue, StrawStatueModel> {
    public static final ResourceLocation STRAW_STATUE_LOCATION = StrawStatues.id("textures/entity/straw_statue.png");
    private static final int SKIN_FACE_X = 8, SKIN_FACE_Y = 8, FACE_SIZE = 8;

    /** Cache of modified skin textures per entity, regenerated when eye data changes. */
    private final Map<UUID, ModifiedSkinEntry> modifiedSkinCache = new HashMap<>();

    public StrawStatueRenderer(EntityRendererProvider.Context context) {
        super(context, new StrawStatueModel(context.bakeLayer(ModClientRegistry.STRAW_STATUE), false), 0.0F);
        this.addLayer(new HumanoidArmorLayer<>(this, new StrawStatueArmorModel<>(context.bakeLayer(ModClientRegistry.STRAW_STATUE_INNER_ARMOR)), new StrawStatueArmorModel<>(context.bakeLayer(ModClientRegistry.STRAW_STATUE_OUTER_ARMOR)), context.getModelManager()));
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
        this.addLayer(new ElytraLayer<>(this, context.getModelSet()));
        this.addLayer(new StrawStatueDeadmau5EarsLayer(this));
        this.addLayer(new StrawStatueCapeLayer(this));
        this.addLayer(new CustomHeadLayer<>(this, context.getModelSet(), context.getItemInHandRenderer()));
    }

    public static Optional<ResourceLocation> getPlayerProfileTexture(StrawStatue entity, MinecraftProfileTexture.Type type) {
        GameProfile gameProfile = entity.getOwner().orElse(null);
        if (gameProfile != null) {
            Minecraft minecraft = Minecraft.getInstance();
            Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> map = minecraft.getSkinManager().getInsecureSkinInformation(gameProfile);
            if (map.containsKey(type)) {
                return Optional.of(minecraft.getSkinManager().registerTexture(map.get(type), type));
            }
        }
        return Optional.empty();
    }

    /**
     * Reads the NativeImage from a dynamically-downloaded skin texture.
     * Skins are registered in the TextureManager, NOT in the ResourceManager.
     * Uses reflection to access the private {@code image} field of SimpleTexture/HttpTexture.
     */
    @Nullable
    public static com.mojang.blaze3d.platform.NativeImage readSkinNativeImage(ResourceLocation skinLocation) {
        Minecraft mc = Minecraft.getInstance();
        net.minecraft.client.renderer.texture.AbstractTexture tex = mc.getTextureManager().getTexture(skinLocation);

        // Approach 1: reflection into SimpleTexture/HttpTexture
        StrawStatues.LOGGER.debug("readSkinNativeImage: trying reflection for {}", skinLocation);
        try {
            Class<?> clazz = tex.getClass();
            while (clazz != null && clazz != Object.class) {
                try {
                    java.lang.reflect.Field field = clazz.getDeclaredField("image");
                    field.setAccessible(true);
                    Object value = field.get(tex);
                    StrawStatues.LOGGER.debug("readSkinNativeImage: class={} field.image={}", clazz.getSimpleName(),
                            value != null ? value.getClass().getSimpleName() : "null");
                    if (value instanceof com.mojang.blaze3d.platform.NativeImage img) {
                        return img;
                    }
                } catch (NoSuchFieldException e) {
                    StrawStatues.LOGGER.debug("readSkinNativeImage: no 'image' field in {}", clazz.getSimpleName());
                }
                clazz = clazz.getSuperclass();
            }
        } catch (Exception e) {
            StrawStatues.LOGGER.warn("readSkinNativeImage: reflection failed", e);
        }

        // Approach 2: ResourceManager fallback
        try {
            var resOpt = mc.getResourceManager().getResource(skinLocation);
            if (resOpt.isPresent()) {
                StrawStatues.LOGGER.debug("readSkinNativeImage: found in ResourceManager");
                return com.mojang.blaze3d.platform.NativeImage.read(resOpt.get().open());
            }
        } catch (Exception e) {
            StrawStatues.LOGGER.debug("readSkinNativeImage: ResourceManager fallback failed", e);
        }

        StrawStatues.LOGGER.warn("readSkinNativeImage: all approaches failed for {}", skinLocation);
        return null;
    }

    /**
     * Fallback: downloads the skin NativeImage directly from Mojang's skin server URL.
     */
    @Nullable
    public static com.mojang.blaze3d.platform.NativeImage readSkinFromUrl(StrawStatue entity) {
        GameProfile gameProfile = entity.getOwner().orElse(null);
        if (gameProfile == null) return null;

        Minecraft mc = Minecraft.getInstance();
        Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> map =
                mc.getSkinManager().getInsecureSkinInformation(gameProfile);
        MinecraftProfileTexture texture = map.get(MinecraftProfileTexture.Type.SKIN);
        if (texture == null || texture.getUrl() == null) {
            StrawStatues.LOGGER.warn("readSkinFromUrl: no skin URL for {}", gameProfile.getName());
            return null;
        }

        try {
            String url = texture.getUrl();
            StrawStatues.LOGGER.debug("readSkinFromUrl: downloading from {}", url);
            java.net.URL skinUrl = new java.net.URL(url);
            java.io.InputStream in = skinUrl.openStream();
            com.mojang.blaze3d.platform.NativeImage img =
                    com.mojang.blaze3d.platform.NativeImage.read(in);
            in.close();
            StrawStatues.LOGGER.debug("readSkinFromUrl: success, {}×{}", img.getWidth(), img.getHeight());
            return img;
        } catch (Exception e) {
            StrawStatues.LOGGER.warn("readSkinFromUrl: download failed", e);
            return null;
        }
    }

    @Override
    public void render(StrawStatue entity, float entityYaw, float partialTicks, PoseStack matrixStack, MultiBufferSource buffer, int packedLight) {
        this.setModelProperties(entity);
        super.render(entity, entityYaw, partialTicks, matrixStack, buffer, packedLight);
    }

    @Override
    public Vec3 getRenderOffset(StrawStatue entity, float partialTicks) {
        return entity.isCrouching() ? new Vec3(0.0, -0.125, 0.0) : super.getRenderOffset(entity, partialTicks);
    }

    private void setModelProperties(StrawStatue entity) {
        StrawStatueModel model = this.getModel();
        model.setAllVisible(true);
        model.hat.visible = entity.isModelPartShown(PlayerModelPart.HAT);
        model.jacket.visible = entity.isModelPartShown(PlayerModelPart.JACKET);
        model.leftPants.visible = entity.isModelPartShown(PlayerModelPart.LEFT_PANTS_LEG);
        model.rightPants.visible = entity.isModelPartShown(PlayerModelPart.RIGHT_PANTS_LEG);
        model.leftSleeve.visible = model.slimLeftSleeve.visible = entity.isModelPartShown(PlayerModelPart.LEFT_SLEEVE);
        model.rightSleeve.visible = model.slimRightSleeve.visible = entity.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE);
        model.crouching = entity.isCrouching();
    }

    @Override
    public ResourceLocation getTextureLocation(StrawStatue entity) {
        ResourceLocation original = getPlayerProfileTexture(entity, MinecraftProfileTexture.Type.SKIN)
                .orElse(STRAW_STATUE_LOCATION);

        StrawStatueEyeData eyeData = entity.getEyeData();
        if (eyeData == null || !eyeData.isValid() || !eyeData.hasOffset()) {
            return original;
        }

        // Return cached modified skin if still valid
        UUID uuid = entity.getUUID();
        ModifiedSkinEntry cached = modifiedSkinCache.get(uuid);
        if (cached != null && cached.matches(eyeData)) {
            return cached.location;
        }

        // Generate a new modified skin texture with eye changes baked in
        // (bakeEyeIntoTexture handles cache management internally)
        ResourceLocation modified = bakeEyeIntoTexture(entity, eyeData, original);
        if (modified != null) {
            return modified;
        }

        return original;
    }

    /**
     * Creates a full copy of the skin texture with face-pixel modifications
     * applied for the given eye data. Returns a registered DynamicTexture location.
     */
    @Nullable
    private ResourceLocation bakeEyeIntoTexture(StrawStatue entity, StrawStatueEyeData eyeData,
                                                 ResourceLocation originalSkin) {
        NativeImage skinImg = null;
        boolean closeSkin = false;
        try {
            skinImg = readSkinNativeImage(originalSkin);
            if (skinImg == null) {
                skinImg = readSkinFromUrl(entity);
                closeSkin = true;
            }
            if (skinImg == null) return null;

            // Create a full copy of the skin texture
            int w = skinImg.getWidth(), h = skinImg.getHeight();
            NativeImage copy = new NativeImage(w, h, false);
            for (int y = 0; y < h; y++)
                for (int x = 0; x < w; x++)
                    copy.setPixelRGBA(x, y, skinImg.getPixelRGBA(x, y));

            // Apply eye modifications to the face region of the copy
            applyEyeToSkin(copy, eyeData, true);
            applyEyeToSkin(copy, eyeData, false);

            DynamicTexture dynTex = new DynamicTexture(copy);
            // Stable per-entity location — no more infinite ResourceLocation growth
            UUID uuid = entity.getUUID();
            ResourceLocation loc = StrawStatues.id("skin_modified/" + uuid.toString().replace("-", ""));

            // Release old texture for this entity before registering new one
            ModifiedSkinEntry oldEntry = modifiedSkinCache.get(uuid);
            if (oldEntry != null) {
                oldEntry.release();
            }

            Minecraft.getInstance().getTextureManager().register(loc, dynTex);

            // Update cache with new entry (holds DynamicTexture ref for future release)
            modifiedSkinCache.put(uuid, new ModifiedSkinEntry(loc, eyeData.copy(), dynTex));

            copy.close();
            return loc;

        } catch (Exception e) {
            StrawStatues.LOGGER.warn("Failed to bake eye into skin texture", e);
            return null;
        } finally {
            if (closeSkin && skinImg != null) skinImg.close();
        }
    }

    private static void applyEyeToSkin(NativeImage skin, StrawStatueEyeData d, boolean left) {
        int ex1, ey1, ex2, ey2, px1, py1, px2, py2, dx, dy;
        if (left) {
            ex1 = d.leftEyeX1;  ey1 = d.leftEyeY1;  ex2 = d.leftEyeX2;  ey2 = d.leftEyeY2;
            px1 = d.leftPupilX1; py1 = d.leftPupilY1; px2 = d.leftPupilX2; py2 = d.leftPupilY2;
            dx  = d.leftPupilDX; dy  = d.leftPupilDY;
        } else {
            ex1 = d.rightEyeX1;  ey1 = d.rightEyeY1;  ex2 = d.rightEyeX2;  ey2 = d.rightEyeY2;
            px1 = d.rightPupilX1; py1 = d.rightPupilY1; px2 = d.rightPupilX2; py2 = d.rightPupilY2;
            dx  = d.rightPupilDX; dy  = d.rightPupilDY;
        }
        if (ex1 < 0 || ey1 < 0 || ex2 < 0 || ey2 < 0) return;
        if (px1 < 0 || py1 < 0 || px2 < 0 || py2 < 0) return;
        if (dx == 0 && dy == 0) return;

        // Map to skin texture coordinates (face is at offset SKIN_FACE_X, SKIN_FACE_Y)
        int sx1 = SKIN_FACE_X + ex1, sy1 = SKIN_FACE_Y + ey1;
        int sx2 = SKIN_FACE_X + ex2, sy2 = SKIN_FACE_Y + ey2;
        int spx1 = SKIN_FACE_X + px1, spy1 = SKIN_FACE_Y + py1;
        int spx2 = SKIN_FACE_X + px2, spy2 = SKIN_FACE_Y + py2;

        // Average eye-white colour from non-pupil pixels inside the eye region
        int r = 0, g = 0, b = 0, a = 0, cnt = 0;
        for (int y = sy1; y <= sy2; y++) {
            for (int x = sx1; x <= sx2; x++) {
                if (x >= spx1 && x <= spx2 && y >= spy1 && y <= spy2) continue;
                int c = skin.getPixelRGBA(x, y);
                r += (c >> 16) & 0xFF; g += (c >> 8) & 0xFF;
                b += c & 0xFF; a += (c >> 24) & 0xFF;
                cnt++;
            }
        }
        int fill = cnt > 0
                ? ((a / cnt) << 24) | ((r / cnt) << 16) | ((g / cnt) << 8) | (b / cnt)
                : 0xFFFFFFFF;

        // Save pupil pixels, fill original pupil area with eye-white
        int pw = spx2 - spx1 + 1, ph = spy2 - spy1 + 1;
        int[] saved = new int[pw * ph];
        int idx = 0;
        for (int y = spy1; y <= spy2; y++)
            for (int x = spx1; x <= spx2; x++)
                saved[idx++] = skin.getPixelRGBA(x, y);

        for (int y = spy1; y <= spy2; y++)
            for (int x = spx1; x <= spx2; x++)
                skin.setPixelRGBA(x, y, fill);

        // Write pupil at offset position (clamped to skin bounds)
        int maxW = skin.getWidth() - 1, maxH = skin.getHeight() - 1;
        idx = 0;
        for (int y = spy1; y <= spy2; y++) {
            for (int x = spx1; x <= spx2; x++) {
                int tx = Math.max(0, Math.min(maxW, x + dx));
                int ty = Math.max(0, Math.min(maxH, y + dy));
                skin.setPixelRGBA(tx, ty, saved[idx++]);
            }
        }
    }

    // ── Cache entry ─────────────────────────────────────────

    private static final class ModifiedSkinEntry {
        final ResourceLocation location;
        final StrawStatueEyeData eyeData;
        DynamicTexture texture;

        ModifiedSkinEntry(ResourceLocation location, StrawStatueEyeData eyeData, DynamicTexture texture) {
            this.location = location;
            this.eyeData = eyeData.copy();
            this.texture = texture;
        }

        boolean matches(StrawStatueEyeData other) {
            return this.eyeData.equals(other);
        }

        /** Release the underlying GPU texture and native image to prevent leaks. */
        void release() {
            if (this.texture != null) {
                this.texture.close();
                this.texture = null;
            }
        }
    }

    @Override
    protected void scale(StrawStatue livingEntity, PoseStack matrixStack, float partialTickTime) {
        float modelScale = Mth.lerp(partialTickTime, livingEntity.entityScaleO, livingEntity.getEntityScale());
        modelScale /= StrawStatue.DEFAULT_ENTITY_SCALE;
        modelScale *= 0.9375F;
        matrixStack.scale(modelScale, modelScale, modelScale);
    }

    @Override
    protected void setupRotations(StrawStatue entityLiving, PoseStack matrixStack, float ageInTicks, float rotationYaw, float partialTicks) {
        float entityZRotation = Mth.lerp(partialTicks, entityLiving.entityRotationsO.getZ(), entityLiving.getEntityZRotation());
        float entityXRotation = Mth.lerp(partialTicks, entityLiving.entityRotationsO.getX(), entityLiving.getEntityXRotation());
        matrixStack.mulPose(Axis.ZP.rotationDegrees(180.0F - entityZRotation));
        matrixStack.mulPose(Axis.YP.rotationDegrees(180.0F - rotationYaw));
        matrixStack.mulPose(Axis.XP.rotationDegrees(180.0F - entityXRotation));
        float hurtAmount = (float) (entityLiving.level().getGameTime() - entityLiving.lastHit) + partialTicks;
        if (hurtAmount < 5.0F) {
            matrixStack.mulPose(Axis.YP.rotationDegrees(Mth.sin(hurtAmount / 1.5F * 3.1415927F) * 3.0F));
        }
        if (isEntityUpsideDown(entityLiving)) {
            matrixStack.translate(0.0, entityLiving.getBbHeight() - 0.0625F, 0.0);
            matrixStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        }
    }

    @Override
    protected boolean shouldShowName(StrawStatue entity) {
        double d = this.entityRenderDispatcher.distanceToSqr(entity);
        float f = entity.isCrouching() ? 32.0F : 64.0F;
        return !(d >= f * f) && entity.isCustomNameVisible();
    }

    @Override
    @Nullable
    protected RenderType getRenderType(StrawStatue livingEntity, boolean bodyVisible, boolean translucent, boolean glowing) {
        if (!livingEntity.isMarker()) {
            return super.getRenderType(livingEntity, bodyVisible, translucent, glowing);
        } else {
            ResourceLocation resourceLocation = this.getTextureLocation(livingEntity);
            if (translucent) {
                return RenderType.entityTranslucent(resourceLocation, false);
            } else {
                return bodyVisible ? RenderType.entityCutoutNoCull(resourceLocation, false) : null;
            }
        }
    }
}
