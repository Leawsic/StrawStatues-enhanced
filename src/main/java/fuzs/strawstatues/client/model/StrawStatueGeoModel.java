package fuzs.strawstatues.client.model;

import fuzs.strawstatues.StrawStatues;
import fuzs.strawstatues.client.renderer.entity.StrawStatueRenderer;
import fuzs.strawstatues.world.entity.decoration.StrawStatue;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

import org.jetbrains.annotations.Nullable;

public class StrawStatueGeoModel extends GeoModel<StrawStatue> {
    private static final ResourceLocation MODEL = StrawStatues.id("geo/straw_statue.geo.json");
    private static final ResourceLocation FALLBACK_TEXTURE = StrawStatues.id("textures/entity/straw_statue.png");

    @Override
    public ResourceLocation getModelResource(StrawStatue animatable, @Nullable software.bernie.geckolib.renderer.GeoRenderer<StrawStatue> renderer) {
        return MODEL;
    }

    @Override
    public ResourceLocation getModelResource(StrawStatue animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(StrawStatue animatable, @Nullable software.bernie.geckolib.renderer.GeoRenderer<StrawStatue> renderer) {
        return getTextureResource(animatable);
    }

    @Override
    public ResourceLocation getTextureResource(StrawStatue animatable) {
        return StrawStatueRenderer.getPlayerProfileTexture(animatable, com.mojang.authlib.minecraft.MinecraftProfileTexture.Type.SKIN)
                .orElse(FALLBACK_TEXTURE);
    }

    @Override
    public ResourceLocation getAnimationResource(StrawStatue animatable) {
        return MODEL;
    }

    // Override to disable re-render skip — ensures the pose updates every frame
    @Override
    public void handleAnimations(StrawStatue animatable, long instanceId, AnimationState<StrawStatue> animationState) {
        // Force-process bones every frame, bypassing the re-render check
        // that normally skips animation processing when the frame time hasn't changed
        getAnimationProcessor().preAnimationSetup(animatable, 0);
        if (!getAnimationProcessor().getRegisteredBones().isEmpty()) {
            getAnimationProcessor().tickAnimation(animatable, this,
                    animatable.getAnimatableInstanceCache().getManagerForId(instanceId), 0,
                    animationState, crashIfBoneMissing());
        }
        setCustomAnimations(animatable, instanceId, animationState);
    }

    @Override
    public void setCustomAnimations(StrawStatue animatable, long instanceId, AnimationState<StrawStatue> animationState) {
        applyPose(animatable);
        applySubBoneRotations(animatable);
    }

    private void applyPose(StrawStatue entity) {
        float DEG_TO_RAD = 0.017453292F;

        setBoneRotation("head", DEG_TO_RAD * entity.getHeadPose().getX(),
                DEG_TO_RAD * entity.getHeadPose().getY(),
                DEG_TO_RAD * entity.getHeadPose().getZ());

        setBoneRotation("body", DEG_TO_RAD * entity.getBodyPose().getX(),
                DEG_TO_RAD * entity.getBodyPose().getY(),
                DEG_TO_RAD * entity.getBodyPose().getZ());

        setBoneRotation("right_arm", DEG_TO_RAD * entity.getRightArmPose().getX(),
                DEG_TO_RAD * entity.getRightArmPose().getY(),
                DEG_TO_RAD * entity.getRightArmPose().getZ());

        setBoneRotation("left_arm", DEG_TO_RAD * entity.getLeftArmPose().getX(),
                DEG_TO_RAD * entity.getLeftArmPose().getY(),
                DEG_TO_RAD * entity.getLeftArmPose().getZ());

        setBoneRotation("right_leg", DEG_TO_RAD * entity.getRightLegPose().getX(),
                DEG_TO_RAD * entity.getRightLegPose().getY(),
                DEG_TO_RAD * entity.getRightLegPose().getZ());

        setBoneRotation("left_leg", DEG_TO_RAD * entity.getLeftLegPose().getX(),
                DEG_TO_RAD * entity.getLeftLegPose().getY(),
                DEG_TO_RAD * entity.getLeftLegPose().getZ());
    }

    private void applySubBoneRotations(StrawStatue entity) {
        float DEG_TO_RAD = 0.017453292F;

        // Elbow bend (forward flexion)
        float rightArmX = entity.getRightArmPose().getX();
        float rightElbowDeg = 5.0F + Math.max(0, (-rightArmX - 60.0F) * 0.08F);
        setBoneRotation("right_forearm", DEG_TO_RAD * rightElbowDeg, 0, 0);

        float leftArmX = entity.getLeftArmPose().getX();
        float leftElbowDeg = 5.0F + Math.max(0, (-leftArmX - 60.0F) * 0.08F);
        setBoneRotation("left_forearm", DEG_TO_RAD * leftElbowDeg, 0, 0);

        // Knee bend (always backward)
        float rightLegX = entity.getRightLegPose().getX();
        float rightKneeDeg = Math.abs(rightLegX) * 0.5F;
        setBoneRotation("right_lower_leg", DEG_TO_RAD * (-rightKneeDeg), 0, 0);

        float leftLegX = entity.getLeftLegPose().getX();
        float leftKneeDeg = Math.abs(leftLegX) * 0.5F;
        setBoneRotation("left_lower_leg", DEG_TO_RAD * (-leftKneeDeg), 0, 0);
    }

    private void setBoneRotation(String boneName, float xRot, float yRot, float zRot) {
        CoreGeoBone bone = getAnimationProcessor().getBone(boneName);
        if (bone != null) {
            bone.setRotX(xRot);
            bone.setRotY(yRot);
            bone.setRotZ(zRot);
        }
    }
}
