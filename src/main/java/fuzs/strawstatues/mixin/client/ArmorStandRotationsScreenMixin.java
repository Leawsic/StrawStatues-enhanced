package fuzs.strawstatues.mixin.client;

import fuzs.puzzlesapi.api.client.statues.v1.gui.components.BoxedSliderButton;
import fuzs.puzzlesapi.api.client.statues.v1.gui.screens.armorstand.ArmorStandRotationsScreen;
import fuzs.puzzlesapi.api.statues.v1.world.inventory.data.ArmorStandPose;
import fuzs.strawstatues.StrawStatues;
import fuzs.strawstatues.mixin.accessor.ArmorStandRotationsScreenAccessor;
import fuzs.strawstatues.mixin.accessor.ArmorStandScreenAccessor;
import fuzs.strawstatues.client.gui.BoxedSliderButtonForearmAccessor;
import fuzs.strawstatues.mixin.accessor.ScreenAccessor;
import fuzs.strawstatues.network.client.C2SStrawStatueSubBoneMessage;
import fuzs.strawstatues.network.client.C2SStrawStatueSubBoneModeMessage;
import fuzs.strawstatues.world.entity.decoration.StrawStatue;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.Rotations;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ArmorStandRotationsScreen.class)
public abstract class ArmorStandRotationsScreenMixin {

    @Unique
    private ArmorStandScreenAccessor strawstatues$as() {
        return (ArmorStandScreenAccessor) this;
    }

    @Unique
    private ScreenAccessor strawstatues$screen() {
        return (ScreenAccessor) this;
    }

    @Unique
    private ArmorStandRotationsScreenAccessor strawstatues$ac() {
        return (ArmorStandRotationsScreenAccessor) this;
    }

    @Unique
    private AbstractWidget strawstatues$subBoneToggleBtn;

    @Unique
    private AbstractWidget strawstatues$subBoneCheckbox;

    @Unique
    private float[] strawstatues$frozenArmLegXRot;

    @Inject(method = "init", at = @At("RETURN"))
    private void strawstatues$onInit(CallbackInfo ci) {
        if (!(this.strawstatues$as().getHolder().getArmorStand() instanceof StrawStatue statue)) return;

        boolean isManual = statue.isSubBoneMode();
        boolean isForearm = isManual && !statue.isSubBoneTargetUpper();

        int guiRight = this.strawstatues$as().getLeftPos() + this.strawstatues$as().getImageWidth();
        int topY = this.strawstatues$as().getTopPos();

        Button toggleBtn = Button.builder(
                isManual ? Component.literal("M") : Component.literal("A"),
                button -> {
                    boolean newMode = !statue.isSubBoneMode();
                    statue.setSubBoneMode(newMode);
                    StrawStatues.NETWORK.sendToServer(new C2SStrawStatueSubBoneModeMessage(newMode));
                    button.setMessage(newMode ? Component.literal("M") : Component.literal("A"));
                    if (this.strawstatues$subBoneCheckbox != null) {
                        this.strawstatues$subBoneCheckbox.visible = newMode;
                    }
                    this.strawstatues$onModeOrTargetChanged(statue);
                }
        ).bounds(guiRight + 4, topY + 6, 20, 20).build();
        toggleBtn.setTooltip(Tooltip.create(Component.translatable(
                isManual ? "strawstatues.screen.subBone.manual" : "strawstatues.screen.subBone.auto"
        )));
        this.strawstatues$addWidget(toggleBtn);
        this.strawstatues$subBoneToggleBtn = toggleBtn;

        Button checkBtn = Button.builder(
                Component.translatable("strawstatues.screen.subBone.upperArm"),
                button -> {
                    boolean newTarget = !statue.isSubBoneTargetUpper();
                    statue.setSubBoneTargetUpper(newTarget);
                    StrawStatues.NETWORK.sendToServer(new C2SStrawStatueSubBoneModeMessage(true, newTarget));
                    button.setMessage(Component.translatable(
                            newTarget ? "strawstatues.screen.subBone.upperArm" : "strawstatues.screen.subBone.forearm"
                    ));
                    this.strawstatues$onModeOrTargetChanged(statue);
                }
        ).bounds(guiRight + 4, topY + 30, 50, 20).build();
        checkBtn.visible = isManual;
        this.strawstatues$addWidget(checkBtn);
        this.strawstatues$subBoneCheckbox = checkBtn;

        this.strawstatues$tagForearmButtons(statue);

        this.strawstatues$initFrozenRotations();

        if (isForearm) {
            this.strawstatues$snapshotFrozenRotations();
            this.strawstatues$ac().callRefreshLiveButtons();
        }
    }

    @ModifyVariable(method = "setCurrentPose", at = @At("HEAD"), argsOnly = true)
    private ArmorStandPose strawstatues$modifySetCurrentPose(ArmorStandPose pose) {
        if (!this.strawstatues$isForearmMode()) return pose;
        if (this.strawstatues$frozenArmLegXRot == null) return pose;

        ArmorStandPose result = pose;
        for (int j = 0; j < 4; j++) {
            float frozenX = this.strawstatues$frozenArmLegXRot[j];
            if (Float.isNaN(frozenX)) continue;
            Rotations original = switch (j) {
                case 0 -> result.getRightArmPose();
                case 1 -> result.getLeftArmPose();
                case 2 -> result.getRightLegPose();
                case 3 -> result.getLeftLegPose();
                default -> null;
            };
            if (original == null) continue;
            Rotations frozenRot = new Rotations(frozenX, original.getY(), original.getZ());
            result = switch (j) {
                case 0 -> result.withRightArmPose(frozenRot);
                case 1 -> result.withLeftArmPose(frozenRot);
                case 2 -> result.withRightLegPose(frozenRot);
                case 3 -> result.withLeftLegPose(frozenRot);
                default -> result;
            };
        }
        return result;
    }

    @Inject(method = "renderBg", at = @At(value = "INVOKE", target = "Lfuzs/puzzlesapi/api/statues/v1/world/inventory/data/ArmorStandPose;applyToEntity(Lnet/minecraft/world/entity/decoration/ArmorStand;)V", shift = At.Shift.AFTER, ordinal = 0))
    private void strawstatues$afterApplyCurrentPoseToEntity(CallbackInfo ci) {
        if (!this.strawstatues$isForearmMode()) return;
        if (this.strawstatues$frozenArmLegXRot == null) return;

        ArmorStand armorStand = this.strawstatues$as().getHolder().getArmorStand();
        if (!Float.isNaN(this.strawstatues$frozenArmLegXRot[0])) {
            armorStand.setRightArmPose(new Rotations(this.strawstatues$frozenArmLegXRot[0],
                    armorStand.getRightArmPose().getY(), armorStand.getRightArmPose().getZ()));
        }
        if (!Float.isNaN(this.strawstatues$frozenArmLegXRot[1])) {
            armorStand.setLeftArmPose(new Rotations(this.strawstatues$frozenArmLegXRot[1],
                    armorStand.getLeftArmPose().getY(), armorStand.getLeftArmPose().getZ()));
        }
        if (!Float.isNaN(this.strawstatues$frozenArmLegXRot[2])) {
            armorStand.setRightLegPose(new Rotations(this.strawstatues$frozenArmLegXRot[2],
                    armorStand.getRightLegPose().getY(), armorStand.getRightLegPose().getZ()));
        }
        if (!Float.isNaN(this.strawstatues$frozenArmLegXRot[3])) {
            armorStand.setLeftLegPose(new Rotations(this.strawstatues$frozenArmLegXRot[3],
                    armorStand.getLeftLegPose().getY(), armorStand.getLeftLegPose().getZ()));
        }
    }

    @Unique
    private void strawstatues$tagForearmButtons(StrawStatue statue) {
        int leftPos = this.strawstatues$as().getLeftPos();
        int topPos = this.strawstatues$as().getTopPos();

        for (Renderable renderable : this.strawstatues$screen().getRenderables()) {
            if (!(renderable instanceof BoxedSliderButton btn)) continue;

            int x = btn.getX();
            int y = btn.getY();

            boolean rightSide = x == leftPos + 23;
            boolean leftSide = x == leftPos + 133;
            if (!rightSide && !leftSide) continue;

            C2SStrawStatueSubBoneMessage.SubBoneType type;
            if (y == topPos + 67) {
                type = rightSide
                        ? C2SStrawStatueSubBoneMessage.SubBoneType.RIGHT_ELBOW
                        : C2SStrawStatueSubBoneMessage.SubBoneType.LEFT_ELBOW;
            } else if (y == topPos + 127) {
                type = rightSide
                        ? C2SStrawStatueSubBoneMessage.SubBoneType.RIGHT_KNEE
                        : C2SStrawStatueSubBoneMessage.SubBoneType.LEFT_KNEE;
            } else {
                continue;
            }

            ((BoxedSliderButtonForearmAccessor) btn).strawstatues$setForearmTarget(statue, type);
        }
    }

    @Unique
    private void strawstatues$onModeOrTargetChanged(StrawStatue statue) {
        boolean isForearm = statue.isSubBoneMode() && !statue.isSubBoneTargetUpper();
        if (isForearm) {
            this.strawstatues$snapshotFrozenRotations();
        }
        this.strawstatues$ac().callRefreshLiveButtons();
    }

    @Unique
    private void strawstatues$snapshotFrozenRotations() {
        ArmorStandPose pose = this.strawstatues$ac().getCurrentPose();
        if (pose == null) return;
        if (this.strawstatues$frozenArmLegXRot == null) {
            this.strawstatues$frozenArmLegXRot = new float[4];
        }
        this.strawstatues$frozenArmLegXRot[0] = pose.getRightArmPose().getX();
        this.strawstatues$frozenArmLegXRot[1] = pose.getLeftArmPose().getX();
        this.strawstatues$frozenArmLegXRot[2] = pose.getRightLegPose().getX();
        this.strawstatues$frozenArmLegXRot[3] = pose.getLeftLegPose().getX();
    }

    @Unique
    private void strawstatues$initFrozenRotations() {
        if (this.strawstatues$frozenArmLegXRot == null) {
            this.strawstatues$frozenArmLegXRot = new float[4];
        }
        java.util.Arrays.fill(this.strawstatues$frozenArmLegXRot, Float.NaN);
    }

    @Unique
    private boolean strawstatues$isForearmMode() {
        if (!(this.strawstatues$as().getHolder().getArmorStand() instanceof StrawStatue statue)) return false;
        return statue.isSubBoneMode() && !statue.isSubBoneTargetUpper();
    }

    @Unique
    private void strawstatues$addWidget(AbstractWidget widget) {
        this.strawstatues$screen().getRenderables().add(widget);
        this.strawstatues$screen().callAddWidget(widget);
    }
}
