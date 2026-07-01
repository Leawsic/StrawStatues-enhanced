package fuzs.strawstatues.network.client;

import fuzs.puzzlesapi.api.statues.v1.world.inventory.ArmorStandMenu;
import fuzs.puzzleslib.api.network.v2.MessageV2;
import fuzs.strawstatues.world.entity.decoration.StrawStatue;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

public class C2SStrawStatueSubBoneModeMessage implements MessageV2<C2SStrawStatueSubBoneModeMessage> {
    private boolean manualMode;
    private boolean targetUpper;

    public C2SStrawStatueSubBoneModeMessage() {

    }

    public C2SStrawStatueSubBoneModeMessage(boolean manualMode) {
        this.manualMode = manualMode;
        this.targetUpper = true;
    }

    public C2SStrawStatueSubBoneModeMessage(boolean manualMode, boolean targetUpper) {
        this.manualMode = manualMode;
        this.targetUpper = targetUpper;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(this.manualMode);
        buf.writeBoolean(this.targetUpper);
    }

    @Override
    public void read(FriendlyByteBuf buf) {
        this.manualMode = buf.readBoolean();
        this.targetUpper = buf.readBoolean();
    }

    @Override
    public MessageHandler<C2SStrawStatueSubBoneModeMessage> makeHandler() {
        return new MessageHandler<>() {

            @Override
            public void handle(C2SStrawStatueSubBoneModeMessage message, Player player, Object gameInstance) {
                if (player.containerMenu instanceof ArmorStandMenu menu && menu.stillValid(player)) {
                    StrawStatue statue = (StrawStatue) menu.getArmorStand();
                    statue.setSubBoneMode(message.manualMode);
                    statue.setSubBoneTargetUpper(message.targetUpper);
                }
            }
        };
    }
}
