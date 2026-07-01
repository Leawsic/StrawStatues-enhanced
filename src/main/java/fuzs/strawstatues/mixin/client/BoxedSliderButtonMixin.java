package fuzs.strawstatues.mixin.client;

import fuzs.puzzlesapi.api.client.statues.v1.gui.components.BoxedSliderButton;
import fuzs.strawstatues.StrawStatues;
import fuzs.strawstatues.mixin.accessor.BoxedSliderButtonAccessor;
import fuzs.strawstatues.client.gui.BoxedSliderButtonForearmAccessor;
import fuzs.strawstatues.network.client.C2SStrawStatueSubBoneMessage;
import fuzs.strawstatues.world.entity.decoration.StrawStatue;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BoxedSliderButton.class)
public abstract class BoxedSliderButtonMixin implements BoxedSliderButtonForearmAccessor {

    @Unique
    private StrawStatue strawstatues$forearmStatue;

    @Unique
    private C2SStrawStatueSubBoneMessage.SubBoneType strawstatues$forearmType;

    @Override
    public void strawstatues$setForearmTarget(StrawStatue statue, C2SStrawStatueSubBoneMessage.SubBoneType type) {
        this.strawstatues$forearmStatue = statue;
        this.strawstatues$forearmType = type;
    }

    @Override
    public StrawStatue strawstatues$getForearmStatue() {
        return this.strawstatues$forearmStatue;
    }

    @Override
    public C2SStrawStatueSubBoneMessage.SubBoneType strawstatues$getForearmType() {
        return this.strawstatues$forearmType;
    }

    @Override
    public void strawstatues$setFrozenXNormalized(double value) {
    }

    @Override
    public double strawstatues$getFrozenXNormalized() {
        return Double.NaN;
    }

    @Unique
    private boolean strawstatues$isForearmActive() {
        return this.strawstatues$forearmStatue != null
                && this.strawstatues$forearmStatue.isSubBoneMode()
                && !this.strawstatues$forearmStatue.isSubBoneTargetUpper();
    }

    @Unique
    private BoxedSliderButtonAccessor strawstatues$btn() {
        return (BoxedSliderButtonAccessor) this;
    }

    @Inject(method = "refreshValues", at = @At("RETURN"))
    private void strawstatues$onRefreshValues(CallbackInfo ci) {
        if (!this.strawstatues$isForearmActive()) return;
        this.strawstatues$btn().setVerticalValue(this.strawstatues$subBoneNormalized());
    }

    @Inject(method = "onRelease", at = @At("HEAD"))
    private void strawstatues$onRelease(double mouseX, double mouseY, CallbackInfo ci) {
        if (!this.strawstatues$isForearmActive()) return;
        float angle = this.strawstatues$normalizedToAngle(this.strawstatues$btn().getVerticalValue());
        StrawStatues.NETWORK.sendToServer(new C2SStrawStatueSubBoneMessage(this.strawstatues$forearmType, angle));
        this.strawstatues$forearmType.consumer.accept(this.strawstatues$forearmStatue, angle);
    }

    @Unique
    private static final float STRAWHAT_STATUES_SUB_BONE_MIN = -90.0F;

    @Unique
    private static final float STRAWHAT_STATUES_SUB_BONE_MAX = 90.0F;

    @Unique
    private static final float STRAWHAT_STATUES_SUB_BONE_RANGE = STRAWHAT_STATUES_SUB_BONE_MAX - STRAWHAT_STATUES_SUB_BONE_MIN;

    @Unique
    private double strawstatues$subBoneNormalized() {
        float angle = this.strawstatues$getSubBoneAngle();
        return Mth.clamp((angle - STRAWHAT_STATUES_SUB_BONE_MIN) / STRAWHAT_STATUES_SUB_BONE_RANGE, 0.0, 1.0);
    }

    @Unique
    private float strawstatues$normalizedToAngle(double normalized) {
        return (float) (normalized * STRAWHAT_STATUES_SUB_BONE_RANGE + STRAWHAT_STATUES_SUB_BONE_MIN);
    }

    @Unique
    private float strawstatues$getSubBoneAngle() {
        if (this.strawstatues$forearmType == null) return 0.0F;
        return switch (this.strawstatues$forearmType) {
            case RIGHT_ELBOW -> this.strawstatues$forearmStatue.getRightElbow();
            case LEFT_ELBOW -> this.strawstatues$forearmStatue.getLeftElbow();
            case RIGHT_KNEE -> this.strawstatues$forearmStatue.getRightKnee();
            case LEFT_KNEE -> this.strawstatues$forearmStatue.getLeftKnee();
        };
    }
}
