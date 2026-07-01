package fuzs.strawstatues.mixin.accessor;

import fuzs.puzzlesapi.api.client.statues.v1.gui.screens.armorstand.AbstractArmorStandScreen;
import fuzs.puzzlesapi.api.statues.v1.network.client.data.DataSyncHandler;
import fuzs.puzzlesapi.api.statues.v1.world.inventory.ArmorStandHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractArmorStandScreen.class)
public interface ArmorStandScreenAccessor {

    @Accessor(value = "holder", remap = false)
    ArmorStandHolder getHolder();

    @Accessor(value = "leftPos", remap = false)
    int getLeftPos();

    @Accessor(value = "topPos", remap = false)
    int getTopPos();

    @Accessor(value = "dataSyncHandler", remap = false)
    DataSyncHandler getDataSyncHandler();

    @Accessor(value = "imageWidth", remap = false)
    int getImageWidth();
}
