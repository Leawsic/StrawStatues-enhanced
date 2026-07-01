package fuzs.strawstatues.mixin.accessor;

import fuzs.puzzlesapi.api.client.statues.v1.gui.screens.armorstand.ArmorStandRotationsScreen;
import fuzs.puzzlesapi.api.statues.v1.world.inventory.data.ArmorStandPose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ArmorStandRotationsScreen.class)
public interface ArmorStandRotationsScreenAccessor {

    @Accessor("currentPose")
    ArmorStandPose getCurrentPose();

    @Accessor("currentPose")
    void strawstatues$setCurrentPose(ArmorStandPose pose);

    @Invoker("refreshLiveButtons")
    void callRefreshLiveButtons();
}
