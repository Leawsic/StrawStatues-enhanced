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
 * Render layer that overlays modified eye pixels onto the head front face.
 * Uses a single 8×8 modified-face DynamicTexture and renders two small
 * per-eye quads precisely positioned on the head model's front surface.
 *
 * <p>Head model: {@code head.addBox(-4, -8, -4, 8, 8, 8)} —
 * front face is the 8×8 quad at z=4 with y=[-8,0], x=[-4,4].
 * Each face-texture pixel (0-7, 0-7) maps to one head-model unit.
 */
public class StrawStatueEyeLayer extends RenderLayer<StrawStatue, StrawStatueModel> {

    private static final int FACE_SIZE = 8;
    private static final int SKIN_FACE_X = 8;
    private static final int SKIN_FACE_Y = 8;

    private final Map<UUID, CachedTexture> textureCache = new HashMap<>();
    private int textureGenerationCounter;

    public StrawStatueEyeLayer(RenderLayerParent<StrawStatue, StrawStatueModel> renderer) {
        super(renderer);
    }

    // ── Render ──────────────────────────────────────────────

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

        if (model.young) {
            matrixStack.translate(0.0, 0.75, 0.0);
            matrixStack.scale(0.5F, 0.5F, 0.5F);
        }

        // Navigate to head-local space (same as model.head rendering)
        model.head.translateAndRotate(matrixStack);

        // Render a small quad per eye, covering only the modified region
        renderEyeQuad(matrixStack, buffer, packedLight, overlayTexture,
                eyeData, true);
        renderEyeQuad(matrixStack, buffer, packedLight, overlayTexture,
                eyeData, false);

        matrixStack.popPose();
    }

    /**
     * Renders one eye-overlay quad on the head front face.
     * The quad covers the eye region expanded by the pupil offset
     * so that the moved pupil is fully visible.
     *
     * <p>Face pixel (fx, fy) → head-local:  x = fx - 4,   y = fy - 8.
     */
    private void renderEyeQuad(PoseStack ms, MultiBufferSource buffer, int packedLight,
                               ResourceLocation overlayTexture, StrawStatueEyeData d, boolean left) {

        int ex1, ey1, ex2, ey2, dx, dy;
        if (left) {
            ex1 = d.leftEyeX1;  ey1 = d.leftEyeY1;
            ex2 = d.leftEyeX2;  ey2 = d.leftEyeY2;
            dx  = d.leftPupilDX; dy  = d.leftPupilDY;
        } else {
            ex1 = d.rightEyeX1;  ey1 = d.rightEyeY1;
            ex2 = d.rightEyeX2;  ey2 = d.rightEyeY2;
            dx  = d.rightPupilDX; dy  = d.rightPupilDY;
        }
        if (ex1 < 0 || ey1 < 0 || ex2 < 0 || ey2 < 0) return;
        if (dx == 0 && dy == 0) return;

        // Expand bounding box to cover both original eye region and offset pupil
        int bx1 = Math.min(ex1, ex1 + dx);
        int by1 = Math.min(ey1, ey1 + dy);
        int bx2 = Math.max(ex2, ex2 + dx);
        int by2 = Math.max(ey2, ey2 + dy);
        bx1 = Math.max(0, bx1);  bx2 = Math.min(FACE_SIZE - 1, bx2);
        by1 = Math.max(0, by1);  by2 = Math.min(FACE_SIZE - 1, by2);

        // Map face coordinates → head-local positions
        float headX1 = bx1 - 4.0F;          // left  edge of pixel bx1
        float headX2 = bx2 - 3.0F;          // right edge of pixel bx2
        float headY1 = by1 - 8.0F;          // top   edge of pixel by1
        float headY2 = by2 - 7.0F;          // bottom edge of pixel by2
        float z = 4.02F;

        // UV: the 8×8 texture covers [0,1]×[0,1]; each pixel is 1/8
        float u1 = bx1 / (float) FACE_SIZE;
        float v1 = by1 / (float) FACE_SIZE;
        float u2 = (bx2 + 1) / (float) FACE_SIZE;
        float v2 = (by2 + 1) / (float) FACE_SIZE;

        RenderType renderType = RenderType.entityCutoutNoCull(overlayTexture);
        VertexConsumer vc = buffer.getBuffer(renderType);
        Matrix4f pose = ms.last().pose();
        Matrix3f nml = ms.last().normal();

        vc.vertex(pose, headX1, headY2, z).color(255, 255, 255, 255)
                .uv(u1, v2).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight)
                .normal(nml, 0, 0, 1).endVertex();
        vc.vertex(pose, headX2, headY2, z).color(255, 255, 255, 255)
                .uv(u2, v2).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight)
                .normal(nml, 0, 0, 1).endVertex();
        vc.vertex(pose, headX2, headY1, z).color(255, 255, 255, 255)
                .uv(u2, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight)
                .normal(nml, 0, 0, 1).endVertex();
        vc.vertex(pose, headX1, headY1, z).color(255, 255, 255, 255)
                .uv(u1, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight)
                .normal(nml, 0, 0, 1).endVertex();
    }

    // ── Texture generation with caching ─────────────────────

    private ResourceLocation getOrCreateOverlayTexture(StrawStatue entity, StrawStatueEyeData eyeData) {
        UUID uuid = entity.getUUID();
        CachedTexture cached = textureCache.get(uuid);
        if (cached != null && cached.matches(eyeData)) {
            return cached.location;
        }
        ResourceLocation newTexture = generateOverlayTexture(entity, eyeData);
        if (newTexture != null) {
            textureCache.put(uuid, new CachedTexture(newTexture, eyeData));
            return newTexture;
        }
        return cached != null ? cached.location : null;
    }

    private ResourceLocation generateOverlayTexture(StrawStatue entity, StrawStatueEyeData eyeData) {
        Optional<ResourceLocation> skinLoc = StrawStatueRenderer.getPlayerProfileTexture(
                entity, MinecraftProfileTexture.Type.SKIN);
        if (skinLoc.isEmpty()) return null;

        NativeImage skinImg = null;
        boolean closeSkinImg = false;
        try {
            Minecraft mc = Minecraft.getInstance();
            skinImg = StrawStatueRenderer.readSkinNativeImage(skinLoc.get());
            if (skinImg == null) {
                StrawStatues.LOGGER.debug("EyeLayer: reflection failed, trying URL...");
                skinImg = StrawStatueRenderer.readSkinFromUrl(entity);
                closeSkinImg = true;
            }
            if (skinImg == null) return null;

            // Copy face (8×8) from skin
            NativeImage faceImg = new NativeImage(FACE_SIZE, FACE_SIZE, false);
            for (int y = 0; y < FACE_SIZE; y++)
                for (int x = 0; x < FACE_SIZE; x++)
                    faceImg.setPixelRGBA(x, y, skinImg.getPixelRGBA(SKIN_FACE_X + x, SKIN_FACE_Y + y));

            applyEye(faceImg, eyeData, true);
            applyEye(faceImg, eyeData, false);

            DynamicTexture dynamicTexture = new DynamicTexture(faceImg);
            ResourceLocation location = StrawStatues.id("eye_overlay/" +
                    entity.getUUID().toString().replace("-", "") + "_" + (++textureGenerationCounter));
            mc.getTextureManager().register(location, dynamicTexture);
            faceImg.close();
            return location;

        } catch (Exception e) {
            StrawStatues.LOGGER.warn("Failed to generate eye overlay texture", e);
            return null;
        } finally {
            if (closeSkinImg && skinImg != null) skinImg.close();
        }
    }

    // ── Pixel manipulation ──────────────────────────────────

    private void applyEye(NativeImage face, StrawStatueEyeData d, boolean left) {
        int ex1, ey1, ex2, ey2, px1, py1, px2, py2, dx, dy;
        if (left) {
            ex1 = d.leftEyeX1;  ey1 = d.leftEyeY1;
            ex2 = d.leftEyeX2;  ey2 = d.leftEyeY2;
            px1 = d.leftPupilX1; py1 = d.leftPupilY1;
            px2 = d.leftPupilX2; py2 = d.leftPupilY2;
            dx  = d.leftPupilDX; dy  = d.leftPupilDY;
        } else {
            ex1 = d.rightEyeX1;  ey1 = d.rightEyeY1;
            ex2 = d.rightEyeX2;  ey2 = d.rightEyeY2;
            px1 = d.rightPupilX1; py1 = d.rightPupilY1;
            px2 = d.rightPupilX2; py2 = d.rightPupilY2;
            dx  = d.rightPupilDX; dy  = d.rightPupilDY;
        }
        if (ex1 < 0 || ey1 < 0 || ex2 < 0 || ey2 < 0) return;
        if (px1 < 0 || py1 < 0 || px2 < 0 || py2 < 0) return;
        if (dx == 0 && dy == 0) return;

        // Average eye-white colour (non-pupil pixels inside the eye region)
        int r = 0, g = 0, b = 0, a = 0, cnt = 0;
        for (int y = ey1; y <= ey2; y++) {
            for (int x = ex1; x <= ex2; x++) {
                if (x >= px1 && x <= px2 && y >= py1 && y <= py2) continue;
                int c = face.getPixelRGBA(x, y);
                r += (c >> 16) & 0xFF;
                g += (c >> 8)  & 0xFF;
                b += c         & 0xFF;
                a += (c >> 24) & 0xFF;
                cnt++;
            }
        }
        int fill = cnt > 0
                ? ((a / cnt) << 24) | ((r / cnt) << 16) | ((g / cnt) << 8) | (b / cnt)
                : 0xFFFFFFFF;

        // Save pupil pixels, fill original pupil area with eye-white
        int pw = px2 - px1 + 1, ph = py2 - py1 + 1;
        int[] saved = new int[pw * ph];
        int idx = 0;
        for (int y = py1; y <= py2; y++)
            for (int x = px1; x <= px2; x++)
                saved[idx++] = face.getPixelRGBA(x, y);

        for (int y = py1; y <= py2; y++)
            for (int x = px1; x <= px2; x++)
                face.setPixelRGBA(x, y, fill);

        // Write pupil at offset position (clamped to face bounds)
        idx = 0;
        for (int y = py1; y <= py2; y++) {
            for (int x = px1; x <= px2; x++) {
                int tx = Math.max(0, Math.min(FACE_SIZE - 1, x + dx));
                int ty = Math.max(0, Math.min(FACE_SIZE - 1, y + dy));
                face.setPixelRGBA(tx, ty, saved[idx++]);
            }
        }
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
