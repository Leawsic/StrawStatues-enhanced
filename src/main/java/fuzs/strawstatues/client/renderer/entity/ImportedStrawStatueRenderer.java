package fuzs.strawstatues.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import fuzs.strawstatues.client.model.ImportedStrawStatueGeoModel;
import fuzs.strawstatues.world.entity.decoration.ImportedStrawStatue;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * GeckoLib renderer for imported model statues.
 * Delegates model/texture/animation resolution to the GeoModel.
 * Applies the same scale/rotation transforms as the regular StrawStatueRenderer.
 */
public class ImportedStrawStatueRenderer extends GeoEntityRenderer<ImportedStrawStatue> {

    public ImportedStrawStatueRenderer(EntityRendererProvider.Context context) {
        super(context, new ImportedStrawStatueGeoModel());
    }

    @Override
    public void render(ImportedStrawStatue entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        // Apply scale like the regular straw statue
        float modelScale = Mth.lerp(partialTick, entity.entityScaleO, entity.getEntityScale());
        modelScale /= 3.0F; // DEFAULT_ENTITY_SCALE
        modelScale *= 0.9375F;

        poseStack.pushPose();
        poseStack.scale(modelScale, modelScale, modelScale);

        // Apply entity Z rotation (this is on top of yaw which GeckoLib handles)
        float entityZRotation = Mth.lerp(partialTick, entity.entityRotationsO.getZ(), entity.getEntityZRotation());
        if (entityZRotation != 0) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(entityZRotation));
        }

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }

    @Override
    protected void applyRotations(ImportedStrawStatue animatable, PoseStack poseStack,
                                  float ageInTicks, float rotationYaw, float partialTicks) {
        // Apply entity X rotation in addition to GeckoLib's default rotation
        float entityXRotation = Mth.lerp(partialTicks, animatable.entityRotationsO.getX(), animatable.getEntityXRotation());
        if (entityXRotation != 0) {
            poseStack.mulPose(Axis.XP.rotationDegrees(entityXRotation));
        }
        super.applyRotations(animatable, poseStack, ageInTicks, rotationYaw, partialTicks);
    }

    @Override
    public ResourceLocation getTextureLocation(ImportedStrawStatue entity) {
        // Delegate to the GeoModel which handles dynamic textures
        return this.model.getTextureResource(entity, this);
    }
}
