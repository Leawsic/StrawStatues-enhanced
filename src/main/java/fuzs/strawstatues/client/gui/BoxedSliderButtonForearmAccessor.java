package fuzs.strawstatues.client.gui;

import fuzs.strawstatues.network.client.C2SStrawStatueSubBoneMessage;
import fuzs.strawstatues.world.entity.decoration.StrawStatue;

public interface BoxedSliderButtonForearmAccessor {

    void strawstatues$setForearmTarget(StrawStatue statue, C2SStrawStatueSubBoneMessage.SubBoneType type);

    StrawStatue strawstatues$getForearmStatue();

    C2SStrawStatueSubBoneMessage.SubBoneType strawstatues$getForearmType();

    void strawstatues$setFrozenXNormalized(double value);

    double strawstatues$getFrozenXNormalized();
}
