package fuzs.strawstatues.mixin.accessor;

import fuzs.puzzlesapi.api.client.statues.v1.gui.components.BoxedSliderButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BoxedSliderButton.class)
public interface BoxedSliderButtonAccessor {

    @Accessor("verticalValue")
    double getVerticalValue();

    @Accessor("verticalValue")
    void setVerticalValue(double value);
}
