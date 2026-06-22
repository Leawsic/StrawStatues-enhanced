package fuzs.strawstatues.client.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.mojang.blaze3d.vertex.PoseStack;
import fuzs.strawstatues.world.entity.decoration.StrawStatue;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.decoration.ArmorStand;

import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class StrawStatueModel extends PlayerModel<StrawStatue> {
    public final ModelPart slimLeftArm;
    public final ModelPart slimRightArm;
    public final ModelPart slimLeftSleeve;
    public final ModelPart slimRightSleeve;
    public final ModelPart leftForearm;
    public final ModelPart rightForearm;
    public final ModelPart leftLowerLeg;
    public final ModelPart rightLowerLeg;
    public final ModelPart slimLeftForearm;
    public final ModelPart slimRightForearm;
    public final ModelPart rightSleeveForearm;
    public final ModelPart leftSleeveForearm;
    public final ModelPart rightPantsLower;
    public final ModelPart leftPantsLower;
    public final ModelPart slimLeftSleeveForearm;
    public final ModelPart slimRightSleeveForearm;
    private final ModelPart cloak;

    private boolean slim;

    public StrawStatueModel(ModelPart modelPart, boolean slim) {
        super(modelPart, slim);
        this.slimLeftArm = modelPart.getChild("slim_left_arm");
        this.slimRightArm = modelPart.getChild("slim_right_arm");
        this.slimLeftSleeve = modelPart.getChild("slim_left_sleeve");
        this.slimRightSleeve = modelPart.getChild("slim_right_sleeve");
        this.leftForearm = modelPart.getChild("left_arm").getChild("left_forearm");
        this.rightForearm = modelPart.getChild("right_arm").getChild("right_forearm");
        this.leftLowerLeg = modelPart.getChild("left_leg").getChild("left_lower_leg");
        this.rightLowerLeg = modelPart.getChild("right_leg").getChild("right_lower_leg");
        this.slimLeftForearm = modelPart.getChild("slim_left_arm").getChild("slim_left_forearm");
        this.slimRightForearm = modelPart.getChild("slim_right_arm").getChild("slim_right_forearm");
        this.rightSleeveForearm = modelPart.getChild("right_sleeve").getChild("right_sleeve_forearm");
        this.leftSleeveForearm = modelPart.getChild("left_sleeve").getChild("left_sleeve_forearm");
        this.rightPantsLower = modelPart.getChild("right_pants").getChild("right_pants_lower");
        this.leftPantsLower = modelPart.getChild("left_pants").getChild("left_pants_lower");
        this.slimLeftSleeveForearm = modelPart.getChild("slim_left_sleeve").getChild("slim_left_sleeve_forearm");
        this.slimRightSleeveForearm = modelPart.getChild("slim_right_sleeve").getChild("slim_right_sleeve_forearm");
        this.cloak = modelPart.getChild("cloak");
    }

    // ───────── Static layer factories ─────────

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshDefinition = PlayerModel.createMesh(CubeDeformation.NONE, false);
        PartDefinition partDefinition = meshDefinition.getRoot();
        splitArmLegCubes(partDefinition, CubeDeformation.NONE);
        splitSleeveCubes(partDefinition, CubeDeformation.NONE);
        splitPantsCubes(partDefinition, CubeDeformation.NONE);
        addSlimArmParts(partDefinition);
        return LayerDefinition.create(meshDefinition, 64, 64);
    }

    /**
     * Creates an armour model LayerDefinition with split limb cubes.
     * Use this instead of {@code ArmorStandArmorModel.createBodyLayer} for matching multi-bone alignment.
     */
    public static LayerDefinition createSplitArmorLayer(CubeDeformation deformation) {
        MeshDefinition mesh = HumanoidModel.createMesh(deformation, 0.0F);
        splitArmLegCubes(mesh.getRoot(), deformation);
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static void splitArmLegCubes(PartDefinition part, CubeDeformation deform) {
        // Overlap at joints: put child pivot closer to parent, make child cubes 8px
        // (instead of 6) so the two halves overlap ~1.5 px, hiding any rotation gap.
        // Each child uses two cubes:
        //   1) Main cube with texOffs(u, v+4) — side faces sample the lower portion of the
        //      original side strip, giving correct forearm/shank texture.
        //   2) Cap-fix cube with texOffs(u, v) and only Direction.DOWN visible — the
        //      DOWN face at the hand/foot end shows the original limb cap texture
        //      (rows v to v+d), avoiding side-strip bleed.
        PartDefinition rightArm = part.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(40, 16).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 6.0F, 4.0F, deform),
                PartPose.offset(-5.0F, 2.0F, 0.0F));
        rightArm.addOrReplaceChild("right_forearm",
                CubeListBuilder.create()
                    .texOffs(40, 20).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 8.0F, 4.0F, deform)
                    .texOffs(40, 16).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 8.0F, 4.0F, Set.of(Direction.DOWN, Direction.UP)),
                PartPose.offset(0.0F, 4.5F, 0.0F));

        PartDefinition leftArm = part.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(32, 48).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 6.0F, 4.0F, deform),
                PartPose.offset(5.0F, 2.0F, 0.0F));
        leftArm.addOrReplaceChild("left_forearm",
                CubeListBuilder.create()
                    .texOffs(32, 52).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 8.0F, 4.0F, deform)
                    .texOffs(32, 48).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 8.0F, 4.0F, Set.of(Direction.DOWN, Direction.UP)),
                PartPose.offset(0.0F, 4.5F, 0.0F));

        PartDefinition rightLeg = part.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F, deform),
                PartPose.offset(-1.9F, 12.0F, 0.0F));
        rightLeg.addOrReplaceChild("right_lower_leg",
                CubeListBuilder.create()
                    .texOffs(0, 20).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 8.0F, 4.0F, deform)
                    .texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 8.0F, 4.0F, Set.of(Direction.DOWN, Direction.UP)),
                PartPose.offset(0.0F, 4.5F, 0.0F));

        PartDefinition leftLeg = part.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(16, 48).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F, deform),
                PartPose.offset(1.9F, 12.0F, 0.0F));
        leftLeg.addOrReplaceChild("left_lower_leg",
                CubeListBuilder.create()
                    .texOffs(16, 52).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 8.0F, 4.0F, deform)
                    .texOffs(16, 48).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 8.0F, 4.0F, Set.of(Direction.DOWN, Direction.UP)),
                PartPose.offset(0.0F, 4.5F, 0.0F));
    }

    private static void splitSleeveCubes(PartDefinition part, CubeDeformation deform) {
        CubeDeformation sleeveDeform = deform.extend(0.25F);

        PartDefinition rightSleeve = part.addOrReplaceChild("right_sleeve",
                CubeListBuilder.create().texOffs(40, 32).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 6.0F, 4.0F, sleeveDeform),
                PartPose.offset(-5.0F, 2.0F, 0.0F));
        rightSleeve.addOrReplaceChild("right_sleeve_forearm",
                CubeListBuilder.create().texOffs(40, 36).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 8.0F, 4.0F, sleeveDeform),
                PartPose.offset(0.0F, 4.5F, 0.0F));

        PartDefinition leftSleeve = part.addOrReplaceChild("left_sleeve",
                CubeListBuilder.create().texOffs(48, 48).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 6.0F, 4.0F, sleeveDeform),
                PartPose.offset(5.0F, 2.0F, 0.0F));
        leftSleeve.addOrReplaceChild("left_sleeve_forearm",
                CubeListBuilder.create().texOffs(48, 52).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 8.0F, 4.0F, sleeveDeform),
                PartPose.offset(0.0F, 4.5F, 0.0F));
    }

    /** Split pants (outer leg layer) into upper + lower so they share the same knee pivot as the inner leg. */
    private static void splitPantsCubes(PartDefinition part, CubeDeformation deform) {
        CubeDeformation pantsDeform = deform.extend(0.25F);
        // Right pants
        PartDefinition rightPants = part.addOrReplaceChild("right_pants",
                CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F, pantsDeform),
                PartPose.offset(-1.9F, 12.0F, 0.0F));
        rightPants.addOrReplaceChild("right_pants_lower",
                CubeListBuilder.create().texOffs(0, 20).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 8.0F, 4.0F, pantsDeform),
                PartPose.offset(0.0F, 4.5F, 0.0F));
        // Left pants
        PartDefinition leftPants = part.addOrReplaceChild("left_pants",
                CubeListBuilder.create().texOffs(16, 48).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F, pantsDeform),
                PartPose.offset(1.9F, 12.0F, 0.0F));
        leftPants.addOrReplaceChild("left_pants_lower",
                CubeListBuilder.create().texOffs(16, 52).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 8.0F, 4.0F, pantsDeform),
                PartPose.offset(0.0F, 4.5F, 0.0F));
    }

    private static void addSlimArmParts(PartDefinition part) {
        PartDefinition slimLeftArm = part.addOrReplaceChild("slim_left_arm",
                CubeListBuilder.create().texOffs(32, 48).addBox(-1.0F, -2.0F, -2.0F, 3.0F, 6.0F, 4.0F, CubeDeformation.NONE),
                PartPose.offset(5.0F, 2.5F, 0.0F));
        slimLeftArm.addOrReplaceChild("slim_left_forearm",
                CubeListBuilder.create()
                    .texOffs(32, 52).addBox(-1.0F, -2.0F, -2.0F, 3.0F, 8.0F, 4.0F, CubeDeformation.NONE)
                    .texOffs(32, 48).addBox(-1.0F, -2.0F, -2.0F, 3.0F, 8.0F, 4.0F, Set.of(Direction.DOWN, Direction.UP)),
                PartPose.offset(0.0F, 4.5F, 0.0F));

        PartDefinition slimRightArm = part.addOrReplaceChild("slim_right_arm",
                CubeListBuilder.create().texOffs(40, 16).addBox(-2.0F, -2.0F, -2.0F, 3.0F, 6.0F, 4.0F, CubeDeformation.NONE),
                PartPose.offset(-5.0F, 2.5F, 0.0F));
        slimRightArm.addOrReplaceChild("slim_right_forearm",
                CubeListBuilder.create()
                    .texOffs(40, 20).addBox(-2.0F, -2.0F, -2.0F, 3.0F, 8.0F, 4.0F, CubeDeformation.NONE)
                    .texOffs(40, 16).addBox(-2.0F, -2.0F, -2.0F, 3.0F, 8.0F, 4.0F, Set.of(Direction.DOWN, Direction.UP)),
                PartPose.offset(0.0F, 4.5F, 0.0F));

        PartDefinition slimLeftSleeve = part.addOrReplaceChild("slim_left_sleeve",
                CubeListBuilder.create().texOffs(48, 48).addBox(-1.0F, -2.0F, -2.0F, 3.0F, 6.0F, 4.0F, CubeDeformation.NONE.extend(0.25F)),
                PartPose.offset(5.0F, 2.5F, 0.0F));
        slimLeftSleeve.addOrReplaceChild("slim_left_sleeve_forearm",
                CubeListBuilder.create().texOffs(48, 52).addBox(-1.0F, -2.0F, -2.0F, 3.0F, 8.0F, 4.0F, CubeDeformation.NONE.extend(0.25F)),
                PartPose.offset(0.0F, 4.5F, 0.0F));

        PartDefinition slimRightSleeve = part.addOrReplaceChild("slim_right_sleeve",
                CubeListBuilder.create().texOffs(40, 32).addBox(-2.0F, -2.0F, -2.0F, 3.0F, 6.0F, 4.0F, CubeDeformation.NONE.extend(0.25F)),
                PartPose.offset(-5.0F, 2.5F, 0.0F));
        slimRightSleeve.addOrReplaceChild("slim_right_sleeve_forearm",
                CubeListBuilder.create().texOffs(40, 36).addBox(-2.0F, -2.0F, -2.0F, 3.0F, 8.0F, 4.0F, CubeDeformation.NONE.extend(0.25F)),
                PartPose.offset(0.0F, 4.5F, 0.0F));
    }

    // ───────── Instance methods ─────────

    @Override
    protected Iterable<ModelPart> headParts() {
        return Iterables.concat(super.headParts(), ImmutableList.of(this.hat));
    }

    @Override
    protected Iterable<ModelPart> bodyParts() {
        return Stream.concat(StreamSupport.stream(super.bodyParts().spliterator(), false).filter(modelPart -> modelPart != this.hat), Stream.of(this.slimLeftArm, this.slimRightArm, this.slimLeftSleeve, this.slimRightSleeve)).collect(ImmutableList.toImmutableList());
    }

    @Override
    public void setupAnim(StrawStatue entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        setupPoseAnim(this, entity);
        this.setupSlimAnim(entity);
        this.setupCloakAnim(entity);
        this.hat.copyFrom(this.head);
        this.leftPants.copyFrom(this.leftLeg);
        this.rightPants.copyFrom(this.rightLeg);
        this.leftSleeve.copyFrom(this.leftArm);
        this.rightSleeve.copyFrom(this.rightArm);
        this.slimLeftSleeve.copyFrom(this.slimLeftArm);
        this.slimRightSleeve.copyFrom(this.slimRightArm);
        this.jacket.copyFrom(this.body);
        this.setupCrouchingAnimCape(entity);
        this.applySubBoneRotations(entity);
    }

    private void applySubBoneRotations(StrawStatue entity) {
        float DEG_TO_RAD = 0.017453292F;

        // Elbow bend: same rotation for inner (arm) + outer (sleeve) layers
        float rightArmX = entity.getRightArmPose().getX();
        float rightElbowDeg = 5.0F + Math.max(0, (-rightArmX - 60.0F) * 0.08F);
        this.rightForearm.xRot = DEG_TO_RAD * rightElbowDeg;
        this.rightSleeveForearm.xRot = DEG_TO_RAD * rightElbowDeg;

        float leftArmX = entity.getLeftArmPose().getX();
        float leftElbowDeg = 5.0F + Math.max(0, (-leftArmX - 60.0F) * 0.08F);
        this.leftForearm.xRot = DEG_TO_RAD * leftElbowDeg;
        this.leftSleeveForearm.xRot = DEG_TO_RAD * leftElbowDeg;

        if (this.slim) {
            this.slimRightForearm.xRot = this.rightForearm.xRot;
            this.slimLeftForearm.xRot = this.leftForearm.xRot;
            this.slimRightSleeveForearm.xRot = this.rightForearm.xRot;
            this.slimLeftSleeveForearm.xRot = this.leftForearm.xRot;
        }

        // Knee bend: same rotation for inner (leg) + outer (pants) layers
        float rightLegX = entity.getRightLegPose().getX();
        float rightKneeDeg = Math.abs(rightLegX) * 0.5F;
        this.rightLowerLeg.xRot = DEG_TO_RAD * rightKneeDeg;
        this.rightPantsLower.xRot = DEG_TO_RAD * rightKneeDeg;

        float leftLegX = entity.getLeftLegPose().getX();
        float leftKneeDeg = Math.abs(leftLegX) * 0.5F;
        this.leftLowerLeg.xRot = DEG_TO_RAD * leftKneeDeg;
        this.leftPantsLower.xRot = DEG_TO_RAD * leftKneeDeg;
    }

    private void setupSlimAnim(StrawStatue entity) {
        this.leftArm.visible = this.slimLeftArm.visible = true;
        this.rightArm.visible = this.slimRightArm.visible = true;
        this.slim = entity.slimArms();
        this.slimLeftArm.xRot = 0.017453292F * entity.getLeftArmPose().getX();
        this.slimLeftArm.yRot = 0.017453292F * entity.getLeftArmPose().getY();
        this.slimLeftArm.zRot = 0.017453292F * entity.getLeftArmPose().getZ();
        this.slimRightArm.xRot = 0.017453292F * entity.getRightArmPose().getX();
        this.slimRightArm.yRot = 0.017453292F * entity.getRightArmPose().getY();
        this.slimRightArm.zRot = 0.017453292F * entity.getRightArmPose().getZ();
        if (this.slim) {
            this.leftArm.visible = this.leftSleeve.visible = false;
            this.rightArm.visible = this.rightSleeve.visible = false;
        } else {
            this.slimLeftArm.visible = this.slimLeftSleeve.visible = false;
            this.slimRightArm.visible = this.slimRightSleeve.visible = false;
        }
    }

    private void setupCloakAnim(StrawStatue entity) {
        this.cloak.xRot = -0.017453292F * entity.getBodyPose().getX();
        this.cloak.yRot = 0.017453292F * entity.getBodyPose().getY();
        this.cloak.zRot = -0.017453292F * entity.getBodyPose().getZ();
    }

    private void setupCrouchingAnimCape(StrawStatue entity) {
        if (entity.getItemBySlot(EquipmentSlot.CHEST).isEmpty()) {
            if (this.crouching) {
                this.cloak.z = 1.4F;
                this.cloak.y = 1.85F;
            } else {
                this.cloak.z = 0.0F;
                this.cloak.y = 0.0F;
            }
        } else if (this.crouching) {
            this.cloak.z = 0.3F;
            this.cloak.y = 0.8F;
        } else {
            this.cloak.z = -1.1F;
            this.cloak.y = -0.85F;
        }
    }

    @Override
    public void translateToHand(HumanoidArm side, PoseStack poseStack) {
        ModelPart modelPart = this.getArm(side);
        ModelPart forearmPart = side == HumanoidArm.RIGHT ? this.rightForearm : this.leftForearm;
        if (this.slim) {
            float f = 0.5F * (float)(side == HumanoidArm.RIGHT ? 1 : -1);
            modelPart.x += f;
            modelPart.translateAndRotate(poseStack);
            modelPart.x -= f;
        } else {
            modelPart.translateAndRotate(poseStack);
        }
        forearmPart.translateAndRotate(poseStack);
        poseStack.translate(0.0F, 5.0F / 16.0F, 0.0F);
    }

    // ───────── Static pose helpers (used by armour model too) ─────────

    public static <T extends ArmorStand> void setupPoseAnim(HumanoidModel<T> model, T entity) {
        model.head.xRot = 0.017453292F * entity.getHeadPose().getX();
        model.head.yRot = 0.017453292F * entity.getHeadPose().getY();
        model.head.zRot = 0.017453292F * entity.getHeadPose().getZ();
        model.leftArm.xRot = 0.017453292F * entity.getLeftArmPose().getX();
        model.leftArm.yRot = 0.017453292F * entity.getLeftArmPose().getY();
        model.leftArm.zRot = 0.017453292F * entity.getLeftArmPose().getZ();
        model.rightArm.xRot = 0.017453292F * entity.getRightArmPose().getX();
        model.rightArm.yRot = 0.017453292F * entity.getRightArmPose().getY();
        model.rightArm.zRot = 0.017453292F * entity.getRightArmPose().getZ();
        model.leftLeg.xRot = 0.017453292F * entity.getLeftLegPose().getX();
        model.leftLeg.yRot = 0.017453292F * entity.getLeftLegPose().getY();
        model.leftLeg.zRot = 0.017453292F * entity.getLeftLegPose().getZ();
        model.rightLeg.xRot = 0.017453292F * entity.getRightLegPose().getX();
        model.rightLeg.yRot = 0.017453292F * entity.getRightLegPose().getY();
        model.rightLeg.zRot = 0.017453292F * entity.getRightLegPose().getZ();
        setupCrouchingAnim(model);
    }

    private static <T extends ArmorStand> void setupCrouchingAnim(HumanoidModel<T> model) {
        if (model.crouching) {
            model.body.xRot = 0.5F;
            model.rightArm.xRot += 0.4F;
            model.leftArm.xRot += 0.4F;
            model.rightLeg.z = 4.0F;
            model.leftLeg.z = 4.0F;
            model.rightLeg.y = 12.2F;
            model.leftLeg.y = 12.2F;
            model.head.y = 4.2F;
            model.body.y = 3.2F;
            model.leftArm.y = 5.2F;
            model.rightArm.y = 5.2F;
        } else {
            model.body.xRot = 0.0F;
            model.rightLeg.z = 0.1F;
            model.leftLeg.z = 0.1F;
            model.rightLeg.y = 12.0F;
            model.leftLeg.y = 12.0F;
            model.head.y = 0.0F;
            model.body.y = 0.0F;
            model.leftArm.y = 2.0F;
            model.rightArm.y = 2.0F;
        }
    }
}
