package fuzs.strawstatues.client.renderer.entity.layers;

import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fuzs.strawstatues.StrawStatues;
import fuzs.strawstatues.client.model.StrawStatueModel;
import fuzs.strawstatues.client.renderer.entity.StrawStatueRenderer;
import fuzs.strawstatues.world.entity.decoration.StrawStatue;
import fuzs.strawstatues.world.entity.decoration.StrawStatueEyeData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Render layer that modifies the face texture based on eye region/pupil data
 * and renders an overlay quad on the head front surface.
 */
public class StrawStatueEyeLayer extends RenderLayer<StrawStatue, StrawStatueModel> {

    private static final int FACE_SIZE = 8;
    private static final int SKIN_FACE_X = 8; // front face position in 64×64 skin
    private static final int SKIN_FACE_Y = 8;

    // Cached modified face textures keyed by entity UUID
    private final Map<UUID, CachedTexture> textureCache = new HashMap<>();

    public StrawStatueEyeLayer(RenderLayerParent<StrawStatue, StrawStatueModel> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack matrixStack, MultiBufferSource buffer, int packedLight,
                       StrawStatue entity, float limbSwing, float limbSwingAmount,
                       float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {

        StrawStatueEyeData eyeData = entity.getEyeData();
        if (eyeData == null || !eyeData.isValid() || !eyeData.hasOffset()) return;

        ResourceLocation overlayTexture = getOrCreateOverlayTexture(entity, eyeData);
        if (overlayTexture == null) return;

        StrawStatueModel model = this.getParentModel();

        matrixStack.pushPose();

        // Match baby model transforms
        if (model.young) {
            matrixStack.translate(0.0, 0.75, 0.0);
            matrixStack.scale(0.5F, 0.5F, 0.5F);
        }

        // Position at the head
        model.head.translateAndRotate(matrixStack);

        // Render overlay quad on front face of head (z=4 in head-local 8×8 space)
        RenderType renderType = RenderType.entityCutoutNoCull(overlayTexture);
        VertexConsumer consumer = buffer.getBuffer(renderType);
        Matrix4f pose = matrixStack.last().pose();
        Matrix3f normal = matrixStack.last().normal();
        float z = 4.02F; // slightly in front to avoid z-fighting

        consumer.vertex(pose, -4.0F, -4.0F, z).color(255, 255, 255, 255).uv(0.0F, 0.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, 0, 0, 1).endVertex();
        consumer.vertex(pose, 4.0F, -4.0F, z).color(255, 255, 255, 255).uv(1.0F, 0.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, 0, 0, 1).endVertex();
        consumer.vertex(pose, 4.0F, 4.0F, z).color(255, 255, 255, 255).uv(1.0F, 1.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, 0, 0, 1).endVertex();
        consumer.vertex(pose, -4.0F, 4.0F, z).color(255, 255, 255, 255).uv(0.0F, 1.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, 0, 0, 1).endVertex();

        matrixStack.popPose();
    }

    // ── Texture generation with caching ─────────────────────

    private ResourceLocation getOrCreateOverlayTexture(StrawStatue entity, StrawStatueEyeData eyeData) {
        UUID uuid = entity.getUUID();
        CachedTexture cached = textureCache.get(uuid);

        // Check if cache is still valid (same eye data)
        if (cached != null && cached.matches(eyeData)) {
            return cached.location;
        }

        // Try to generate new texture
        ResourceLocation newTexture = generateOverlayTexture(entity, eyeData);
        if (newTexture != null) {
            // Clean up old texture
            if (cached != null) {
                Minecraft.getInstance().getTextureManager().release(cached.location);
            }
            textureCache.put(uuid, new CachedTexture(newTexture, eyeData));
            return newTexture;
        }

        return cached != null ? cached.location : null;
    }

    private ResourceLocation generateOverlayTexture(StrawStatue entity, StrawStatueEyeData eyeData) {
        Optional<ResourceLocation> skinLoc = StrawStatueRenderer.getPlayerProfileTexture(
                entity, MinecraftProfileTexture.Type.SKIN);
        if (skinLoc.isEmpty()) return null;

        try {
            Minecraft mc = Minecraft.getInstance();
            NativeImage skinImg = StrawStatueRenderer.readSkinNativeImage(skinLoc.get());
            if (skinImg == null) return null;

            NativeImage faceImg = new NativeImage(FACE_SIZE, FACE_SIZE, false);

            // Copy face from skin
            for (int y = 0; y < FACE_SIZE; y++)
                for (int x = 0; x < FACE_SIZE; x++)
                    faceImg.setPixelRGBA(x, y, skinImg.getPixelRGBA(SKIN_FACE_X + x, SKIN_FACE_Y + y));

            // Apply eye modifications
            applyEye(faceImg, eyeData, true);
            applyEye(faceImg, eyeData, false);

            // Register as dynamic texture
            DynamicTexture dynamicTexture = new DynamicTexture(faceImg);
            ResourceLocation location = StrawStatues.id("eye_overlay/" + entity.getUUID().toString().replace("-", ""));
            mc.getTextureManager().register(location, dynamicTexture);
            faceImg.close(); // DynamicTexture now owns a copy
            // skinImg is owned by TextureManager — do NOT close
            return location;

        } catch (Exception e) {
            StrawStatues.LOGGER.warn("Failed to generate eye overlay texture", e);
            return null;
        }
    }

    // ── Pixel manipulation ──────────────────────────────────

    private void applyEye(NativeImage faceImg, StrawStatueEyeData eyeData, boolean left) {
        int ex1, ey1, ex2, ey2, px1, py1, px2, py2, dx, dy;
        if (left) {
            ex1 = eyeData.leftEyeX1; ey1 = eyeData.leftEyeY1;
            ex2 = eyeData.leftEyeX2; ey2 = eyeData.leftEyeY2;
            px1 = eyeData.leftPupilX1; py1 = eyeData.leftPupilY1;
            px2 = eyeData.leftPupilX2; py2 = eyeData.leftPupilY2;
            dx = eyeData.leftPupilDX; dy = eyeData.leftPupilDY;
        } else {
            ex1 = eyeData.rightEyeX1; ey1 = eyeData.rightEyeY1;
            ex2 = eyeData.rightEyeX2; ey2 = eyeData.rightEyeY2;
            px1 = eyeData.rightPupilX1; py1 = eyeData.rightPupilY1;
            px2 = eyeData.rightPupilX2; py2 = eyeData.rightPupilY2;
            dx = eyeData.rightPupilDX; dy = eyeData.rightPupilDY;
        }

        if (ex1 < 0 || ey1 < 0 || ex2 < 0 || ey2 < 0) return;
        if (px1 < 0 || py1 < 0 || px2 < 0 || py2 < 0) return;
        if (dx == 0 && dy == 0) return;

        // 1. Find eye-white color: average of non-pupil pixels within eye region
        int[] whiteColor = new int[4]; // RGBA accumulator
        int whiteCount = 0;
        for (int y = ey1; y <= ey2; y++) {
            for (int x = ex1; x <= ex2; x++) {
                if (isInPupil(x, y, px1, py1, px2, py2)) continue;
                int c = faceImg.getPixelRGBA(x, y);
                whiteColor[0] += (c >> 16) & 0xFF;
                whiteColor[1] += (c >> 8) & 0xFF;
                whiteColor[2] += c & 0xFF;
                whiteColor[3] += (c >> 24) & 0xFF;
                whiteCount++;
            }
        }

        int fillColor;
        if (whiteCount > 0) {
            fillColor = ((whiteColor[3] / whiteCount) << 24)
                    | ((whiteColor[0] / whiteCount) << 16)
                    | ((whiteColor[1] / whiteCount) << 8)
                    | (whiteColor[2] / whiteCount);
        } else {
            fillColor = 0xFFFFFFFF; // fallback white
        }

        // 2. Save pupil pixels, then fill original pupil area with eye-white color
        int pupilW = px2 - px1 + 1;
        int pupilH = py2 - py1 + 1;
        int[] savedPupil = new int[pupilW * pupilH];
        int idx = 0;
        for (int y = py1; y <= py2; y++) {
            for (int x = px1; x <= px2; x++) {
                savedPupil[idx++] = faceImg.getPixelRGBA(x, y);
                faceImg.setPixelRGBA(x, y, fillColor);
            }
        }

        // 3. Write saved pupil pixels at offset position (clamped)
        idx = 0;
        for (int y = py1; y <= py2; y++) {
            for (int x = px1; x <= px2; x++) {
                int tx = x + dx;
                int ty = y + dy;
                tx = Math.max(0, Math.min(FACE_SIZE - 1, tx));
                ty = Math.max(0, Math.min(FACE_SIZE - 1, ty));
                faceImg.setPixelRGBA(tx, ty, savedPupil[idx++]);
            }
        }
    }

    private static boolean isInPupil(int x, int y, int px1, int py1, int px2, int py2) {
        return x >= px1 && x <= px2 && y >= py1 && y <= py2;
    }

    // ── Cache entry ─────────────────────────────────────────

    private static class CachedTexture {
        final ResourceLocation location;
        final StrawStatueEyeData eyeDataCopy;

        CachedTexture(ResourceLocation location, StrawStatueEyeData eyeData) {
            this.location = location;
            this.eyeDataCopy = eyeData.copy();
        }

        boolean matches(StrawStatueEyeData other) {
            return this.eyeDataCopy.equals(other);
        }
    }
}
