package fuzs.strawstatues.world.entity.decoration;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/**
 * Stores eye configuration data for a StrawStatue.
 * All coordinates are in 8×8 face texture space (0-7).
 */
public class StrawStatueEyeData {

    // Eye region bounding boxes (inclusive)
    public int leftEyeX1, leftEyeY1, leftEyeX2, leftEyeY2;
    public int rightEyeX1, rightEyeY1, rightEyeX2, rightEyeY2;

    // Pupil region bounding boxes within the eye region (inclusive)
    public int leftPupilX1, leftPupilY1, leftPupilX2, leftPupilY2;
    public int rightPupilX1, rightPupilY1, rightPupilX2, rightPupilY2;

    // Pupil offset in face texture pixels (how far the pupil has been moved)
    public int leftPupilDX, leftPupilDY;
    public int rightPupilDX, rightPupilDY;

    // ── Factories ──────────────────────────────────────────

    public static StrawStatueEyeData createDefault() {
        StrawStatueEyeData data = new StrawStatueEyeData();
        // Typical Steve-skin left eye (right side of face image)
        data.leftEyeX1 = 2; data.leftEyeY1 = 2; data.leftEyeX2 = 3; data.leftEyeY2 = 2;
        // Typical Steve-skin right eye (left side of face image)
        data.rightEyeX1 = 4; data.rightEyeY1 = 2; data.rightEyeX2 = 5; data.rightEyeY2 = 2;
        // Pupils same as eye regions by default
        data.leftPupilX1 = 2; data.leftPupilY1 = 2; data.leftPupilX2 = 3; data.leftPupilY2 = 2;
        data.rightPupilX1 = 4; data.rightPupilY1 = 2; data.rightPupilX2 = 5; data.rightPupilY2 = 2;
        data.leftPupilDX = 0; data.leftPupilDY = 0;
        data.rightPupilDX = 0; data.rightPupilDY = 0;
        return data;
    }

    // ── Validation ─────────────────────────────────────────

    public boolean hasLeftEye() {
        return leftEyeX1 <= leftEyeX2 && leftEyeY1 <= leftEyeY2
                && leftPupilX1 <= leftPupilX2 && leftPupilY1 <= leftPupilY2;
    }

    public boolean hasRightEye() {
        return rightEyeX1 <= rightEyeX2 && rightEyeY1 <= rightEyeY2
                && rightPupilX1 <= rightPupilX2 && rightPupilY1 <= rightPupilY2;
    }

    public boolean isValid() {
        return hasLeftEye() || hasRightEye();
    }

    public boolean hasOffset() {
        return leftPupilDX != 0 || leftPupilDY != 0 || rightPupilDX != 0 || rightPupilDY != 0;
    }

    // ── Clamp helpers ──────────────────────────────────────

    public void clampToFace() {
        leftEyeX1 = clamp(leftEyeX1); leftEyeY1 = clamp(leftEyeY1);
        leftEyeX2 = clamp(leftEyeX2); leftEyeY2 = clamp(leftEyeY2);
        rightEyeX1 = clamp(rightEyeX1); rightEyeY1 = clamp(rightEyeY1);
        rightEyeX2 = clamp(rightEyeX2); rightEyeY2 = clamp(rightEyeY2);
        leftPupilX1 = clamp(leftPupilX1); leftPupilY1 = clamp(leftPupilY1);
        leftPupilX2 = clamp(leftPupilX2); leftPupilY2 = clamp(leftPupilY2);
        rightPupilX1 = clamp(rightPupilX1); rightPupilY1 = clamp(rightPupilY1);
        rightPupilX2 = clamp(rightPupilX2); rightPupilY2 = clamp(rightPupilY2);
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(7, v));
    }

    // ── NBT serialization ──────────────────────────────────

    private static final String LEFT_EYE = "LeftEye";
    private static final String RIGHT_EYE = "RightEye";
    private static final String LEFT_PUPIL = "LeftPupil";
    private static final String RIGHT_PUPIL = "RightPupil";
    private static final String LEFT_OFFSET = "LeftOffset";
    private static final String RIGHT_OFFSET = "RightOffset";

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putIntArray(LEFT_EYE, new int[]{leftEyeX1, leftEyeY1, leftEyeX2, leftEyeY2});
        tag.putIntArray(RIGHT_EYE, new int[]{rightEyeX1, rightEyeY1, rightEyeX2, rightEyeY2});
        tag.putIntArray(LEFT_PUPIL, new int[]{leftPupilX1, leftPupilY1, leftPupilX2, leftPupilY2});
        tag.putIntArray(RIGHT_PUPIL, new int[]{rightPupilX1, rightPupilY1, rightPupilX2, rightPupilY2});
        tag.putIntArray(LEFT_OFFSET, new int[]{leftPupilDX, leftPupilDY});
        tag.putIntArray(RIGHT_OFFSET, new int[]{rightPupilDX, rightPupilDY});
        return tag;
    }

    public static StrawStatueEyeData fromTag(CompoundTag tag) {
        StrawStatueEyeData data = new StrawStatueEyeData();
        readRect(tag, LEFT_EYE, arr -> { data.leftEyeX1 = arr[0]; data.leftEyeY1 = arr[1]; data.leftEyeX2 = arr[2]; data.leftEyeY2 = arr[3]; });
        readRect(tag, RIGHT_EYE, arr -> { data.rightEyeX1 = arr[0]; data.rightEyeY1 = arr[1]; data.rightEyeX2 = arr[2]; data.rightEyeY2 = arr[3]; });
        readRect(tag, LEFT_PUPIL, arr -> { data.leftPupilX1 = arr[0]; data.leftPupilY1 = arr[1]; data.leftPupilX2 = arr[2]; data.leftPupilY2 = arr[3]; });
        readRect(tag, RIGHT_PUPIL, arr -> { data.rightPupilX1 = arr[0]; data.rightPupilY1 = arr[1]; data.rightPupilX2 = arr[2]; data.rightPupilY2 = arr[3]; });
        if (tag.contains(LEFT_OFFSET, Tag.TAG_INT_ARRAY)) {
            int[] arr = tag.getIntArray(LEFT_OFFSET);
            if (arr.length == 2) { data.leftPupilDX = arr[0]; data.leftPupilDY = arr[1]; }
        }
        if (tag.contains(RIGHT_OFFSET, Tag.TAG_INT_ARRAY)) {
            int[] arr = tag.getIntArray(RIGHT_OFFSET);
            if (arr.length == 2) { data.rightPupilDX = arr[0]; data.rightPupilDY = arr[1]; }
        }
        return data;
    }

    private static void readRect(CompoundTag tag, String key, RectConsumer consumer) {
        if (tag.contains(key, Tag.TAG_INT_ARRAY)) {
            int[] arr = tag.getIntArray(key);
            if (arr.length == 4) consumer.accept(arr);
        }
    }

    @FunctionalInterface
    private interface RectConsumer {
        void accept(int[] arr);
    }

    // ── Copy ───────────────────────────────────────────────

    public StrawStatueEyeData copy() {
        StrawStatueEyeData data = new StrawStatueEyeData();
        data.leftEyeX1 = leftEyeX1; data.leftEyeY1 = leftEyeY1;
        data.leftEyeX2 = leftEyeX2; data.leftEyeY2 = leftEyeY2;
        data.rightEyeX1 = rightEyeX1; data.rightEyeY1 = rightEyeY1;
        data.rightEyeX2 = rightEyeX2; data.rightEyeY2 = rightEyeY2;
        data.leftPupilX1 = leftPupilX1; data.leftPupilY1 = leftPupilY1;
        data.leftPupilX2 = leftPupilX2; data.leftPupilY2 = leftPupilY2;
        data.rightPupilX1 = rightPupilX1; data.rightPupilY1 = rightPupilY1;
        data.rightPupilX2 = rightPupilX2; data.rightPupilY2 = rightPupilY2;
        data.leftPupilDX = leftPupilDX; data.leftPupilDY = leftPupilDY;
        data.rightPupilDX = rightPupilDX; data.rightPupilDY = rightPupilDY;
        return data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StrawStatueEyeData that)) return false;
        return leftEyeX1 == that.leftEyeX1 && leftEyeY1 == that.leftEyeY1
                && leftEyeX2 == that.leftEyeX2 && leftEyeY2 == that.leftEyeY2
                && rightEyeX1 == that.rightEyeX1 && rightEyeY1 == that.rightEyeY1
                && rightEyeX2 == that.rightEyeX2 && rightEyeY2 == that.rightEyeY2
                && leftPupilX1 == that.leftPupilX1 && leftPupilY1 == that.leftPupilY1
                && leftPupilX2 == that.leftPupilX2 && leftPupilY2 == that.leftPupilY2
                && rightPupilX1 == that.rightPupilX1 && rightPupilY1 == that.rightPupilY1
                && rightPupilX2 == that.rightPupilX2 && rightPupilY2 == that.rightPupilY2
                && leftPupilDX == that.leftPupilDX && leftPupilDY == that.leftPupilDY
                && rightPupilDX == that.rightPupilDX && rightPupilDY == that.rightPupilDY;
    }

    @Override
    public int hashCode() {
        int result = leftEyeX1;
        result = 31 * result + leftEyeY1;
        result = 31 * result + leftEyeX2;
        result = 31 * result + leftEyeY2;
        result = 31 * result + rightEyeX1;
        result = 31 * result + rightEyeY1;
        result = 31 * result + rightEyeX2;
        result = 31 * result + rightEyeY2;
        result = 31 * result + leftPupilX1;
        result = 31 * result + leftPupilY1;
        result = 31 * result + leftPupilX2;
        result = 31 * result + leftPupilY2;
        result = 31 * result + rightPupilX1;
        result = 31 * result + rightPupilY1;
        result = 31 * result + rightPupilX2;
        result = 31 * result + rightPupilY2;
        result = 31 * result + leftPupilDX;
        result = 31 * result + leftPupilDY;
        result = 31 * result + rightPupilDX;
        result = 31 * result + rightPupilDY;
        return result;
    }
}
