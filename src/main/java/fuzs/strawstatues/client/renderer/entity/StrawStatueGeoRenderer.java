package fuzs.strawstatues.client.renderer.entity;

import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import fuzs.strawstatues.client.init.ModClientRegistry;
import fuzs.strawstatues.client.model.StrawStatueArmorModel;
import fuzs.strawstatues.client.model.StrawStatueGeoModel;
import fuzs.strawstatues.client.model.StrawStatueModel;
import fuzs.strawstatues.world.entity.decoration.StrawStatue;
import net.minecraft.client.model.ElytraModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.Optional;

public class StrawStatueGeoRenderer extends GeoEntityRenderer<StrawStatue> {
    private final StrawStatueArmorModel<StrawStatue> innerArmor;
    private final StrawStatueArmorModel<StrawStatue> outerArmor;
    private final ElytraModel<StrawStatue> elytraModel;
    private final ModelPart cloakModel;

    public StrawStatueGeoRenderer(EntityRendererProvider.Context context) {
        super(context, new StrawStatueGeoModel());
        this.innerArmor = new StrawStatueArmorModel<>(context.bakeLayer(ModClientRegistry.STRAW_STATUE_INNER_ARMOR));
        this.outerArmor = new StrawStatueArmorModel<>(context.bakeLayer(ModClientRegistry.STRAW_STATUE_OUTER_ARMOR));
        this.elytraModel = new ElytraModel<>(context.bakeLayer(ModelLayers.ELYTRA));
        this.cloakModel = context.bakeLayer(ModelLayers.PLAYER).getChild("cloak");

        this.addRenderLayer(new BlockAndItemGeoLayer<>(this) {
            @Override protected ItemStack getStackForBone(GeoBone bone, StrawStatue a) {
                String n = bone.getName();
                if ("right_arm".equals(n)) return a.getMainHandItem();
                if ("left_arm".equals(n)) return a.getOffhandItem();
                return null;
            }
            @Override protected net.minecraft.world.item.ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack, StrawStatue a) {
                return net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
            }
        });
        this.addRenderLayer(new ArmorGeoLayer(this));
        this.addRenderLayer(new ElytraGeoLayer(this));
        this.addRenderLayer(new CapeGeoLayer(this));
    }

    @Override
    public void render(StrawStatue entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        float modelScale = Mth.lerp(partialTick, entity.entityScaleO, entity.getEntityScale());
        modelScale /= 3.0F; modelScale *= 0.9375F;
        this.animatable = entity;
        poseStack.pushPose();
        poseStack.scale(modelScale, modelScale, modelScale);
        float zRot = Mth.lerp(partialTick, entity.entityRotationsO.getZ(), entity.getEntityZRotation());
        if (zRot != 0) poseStack.mulPose(Axis.ZP.rotationDegrees(zRot));
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }

    @Override
    protected void applyRotations(StrawStatue a, PoseStack poseStack, float age, float yaw, float pt) {
        float xRot = Mth.lerp(pt, a.entityRotationsO.getX(), a.getEntityXRotation());
        if (xRot != 0) poseStack.mulPose(Axis.XP.rotationDegrees(xRot));
        super.applyRotations(a, poseStack, age, yaw, pt);
    }

    @Override
    public ResourceLocation getTextureLocation(StrawStatue entity) {
        return this.model.getTextureResource(entity, this);
    }

    // ─────── Armor layer ───────

    private class ArmorGeoLayer extends GeoRenderLayer<StrawStatue> {
        ArmorGeoLayer(StrawStatueGeoRenderer r) { super(r); }

        @Override
        public void render(PoseStack poseStack, StrawStatue entity, BakedGeoModel model, RenderType renderType, MultiBufferSource buf, VertexConsumer buf2, float pt, int light, int overlay) {
            renderPiece(poseStack, entity, buf, light, EquipmentSlot.HEAD);
            renderPiece(poseStack, entity, buf, light, EquipmentSlot.CHEST);
            renderPiece(poseStack, entity, buf, light, EquipmentSlot.LEGS);
            renderPiece(poseStack, entity, buf, light, EquipmentSlot.FEET);
        }

        private void renderPiece(PoseStack poseStack, StrawStatue entity, MultiBufferSource buf, int light, EquipmentSlot slot) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (!(stack.getItem() instanceof ArmorItem item)) return;
            if (item.getEquipmentSlot() != slot) return;

            boolean inner = slot == EquipmentSlot.LEGS;
            StrawStatueArmorModel<StrawStatue> model = inner ? StrawStatueGeoRenderer.this.innerArmor : StrawStatueGeoRenderer.this.outerArmor;
            model.setupAnim(entity, 0, 0, entity.tickCount, 0, 0);
            StrawStatueModel.setupPoseAnim(model, entity);

            String matName = item.getMaterial().getName();
            int idx = inner ? 2 : 1;
            ResourceLocation tex = new ResourceLocation("textures/models/armor/" + matName + "_layer_" + idx + ".png");
            VertexConsumer vc = buf.getBuffer(RenderType.armorCutoutNoCull(tex));
            model.renderToBuffer(poseStack, vc, light, OverlayTexture.NO_OVERLAY, 1f, 1f, 1f, 1f);
        }
    }

    // ─────── Elytra layer ───────

    private class ElytraGeoLayer extends GeoRenderLayer<StrawStatue> {
        ElytraGeoLayer(StrawStatueGeoRenderer r) { super(r); }

        @Override
        public void render(PoseStack poseStack, StrawStatue entity, BakedGeoModel model, RenderType renderType, MultiBufferSource buf, VertexConsumer buf2, float pt, int light, int overlay) {
            if (!entity.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) return;
            StrawStatueGeoRenderer.this.elytraModel.setupAnim(entity, 0, 0, entity.tickCount, 0, 0);
            ResourceLocation tex = new ResourceLocation("textures/entity/elytra.png");
            VertexConsumer vc = buf.getBuffer(RenderType.entityCutout(tex));
            StrawStatueGeoRenderer.this.elytraModel.renderToBuffer(poseStack, vc, light, OverlayTexture.NO_OVERLAY, 1f, 1f, 1f, 1f);
        }
    }

    // ─────── Cape layer ───────

    private class CapeGeoLayer extends GeoRenderLayer<StrawStatue> {
        CapeGeoLayer(StrawStatueGeoRenderer r) { super(r); }

        @Override
        public void render(PoseStack poseStack, StrawStatue entity, BakedGeoModel model, RenderType renderType, MultiBufferSource buf, VertexConsumer buf2, float pt, int light, int overlay) {
            if (entity.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) return;
            if (!entity.isModelPartShown(PlayerModelPart.CAPE)) return;
            Optional<ResourceLocation> tex = StrawStatueRenderer.getPlayerProfileTexture(entity, MinecraftProfileTexture.Type.CAPE);
            if (tex.isEmpty()) return;
            VertexConsumer vc = buf.getBuffer(RenderType.entitySolid(tex.get()));
            StrawStatueGeoRenderer.this.cloakModel.render(poseStack, vc, light, OverlayTexture.NO_OVERLAY);
        }
    }
}
