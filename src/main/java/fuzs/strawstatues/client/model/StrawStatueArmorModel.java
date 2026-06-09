package fuzs.strawstatues.client.model;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.decoration.ArmorStand;

public class StrawStatueArmorModel<T extends ArmorStand> extends HumanoidModel<T> {

    public final ModelPart leftForearm;
    public final ModelPart rightForearm;
    public final ModelPart leftLowerLeg;
    public final ModelPart rightLowerLeg;

    public StrawStatueArmorModel(ModelPart modelPart) {
        super(modelPart);
        this.leftForearm = modelPart.getChild("left_arm").getChild("left_forearm");
        this.rightForearm = modelPart.getChild("right_arm").getChild("right_forearm");
        this.leftLowerLeg = modelPart.getChild("left_leg").getChild("left_lower_leg");
        this.rightLowerLeg = modelPart.getChild("right_leg").getChild("right_lower_leg");
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        StrawStatueModel.setupPoseAnim(this, entity);

        // Apply procedural sub-bone bending to match the main model
        float DEG_TO_RAD = 0.017453292F;

        float rightArmX = entity.getRightArmPose().getX();
        float rightElbowDeg = Math.max(0, (-rightArmX - 15.0F) * 0.35F);
        this.rightForearm.xRot = -DEG_TO_RAD * rightElbowDeg;

        float leftArmX = entity.getLeftArmPose().getX();
        float leftElbowDeg = Math.max(0, (-leftArmX - 15.0F) * 0.35F);
        this.leftForearm.xRot = -DEG_TO_RAD * leftElbowDeg;

        float rightLegX = entity.getRightLegPose().getX();
        float rightKneeDeg = Math.max(0, rightLegX * 0.25F);
        this.rightLowerLeg.xRot = -DEG_TO_RAD * rightKneeDeg;

        float leftLegX = entity.getLeftLegPose().getX();
        float leftKneeDeg = Math.max(0, leftLegX * 0.25F);
        this.leftLowerLeg.xRot = -DEG_TO_RAD * leftKneeDeg;
    }
}
