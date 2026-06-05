package fuzs.strawstatues.network.client;

import fuzs.puzzlesapi.api.statues.v1.world.inventory.ArmorStandMenu;
import fuzs.puzzleslib.api.network.v2.MessageV2;
import fuzs.strawstatues.StrawStatues;
import fuzs.strawstatues.world.entity.decoration.StrawStatue;
import fuzs.strawstatues.world.entity.decoration.StrawStatueEyeData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

public class C2SStrawStatueEyeMessage implements MessageV2<C2SStrawStatueEyeMessage> {
    private CompoundTag eyeDataTag;

    public C2SStrawStatueEyeMessage() {
    }

    public C2SStrawStatueEyeMessage(StrawStatueEyeData eyeData) {
        this.eyeDataTag = eyeData != null ? eyeData.toTag() : new CompoundTag();
    }

    public static void sendToServer(StrawStatueEyeData eyeData) {
        StrawStatues.NETWORK.sendToServer(new C2SStrawStatueEyeMessage(eyeData));
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeNbt(this.eyeDataTag);
    }

    @Override
    public void read(FriendlyByteBuf buf) {
        this.eyeDataTag = buf.readNbt();
    }

    @Override
    public MessageHandler<C2SStrawStatueEyeMessage> makeHandler() {
        return new MessageHandler<>() {

            @Override
            public void handle(C2SStrawStatueEyeMessage message, Player player, Object gameInstance) {
                if (player.containerMenu instanceof ArmorStandMenu menu && menu.stillValid(player)) {
                    StrawStatue statue = (StrawStatue) menu.getArmorStand();
                    StrawStatueEyeData eyeData = StrawStatueEyeData.fromTag(message.eyeDataTag);
                    eyeData.clampToFace();
                    if (eyeData.isValid()) {
                        statue.setEyeData(eyeData);
                    } else {
                        statue.setEyeData(null);
                    }
                }
            }
        };
    }
}
