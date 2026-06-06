package fuzs.strawstatues.network.client;

import fuzs.puzzlesapi.api.statues.v1.world.inventory.ArmorStandMenu;
import fuzs.puzzleslib.api.network.v2.MessageV2;
import fuzs.strawstatues.StrawStatues;
import fuzs.strawstatues.importmodel.ImportedModelData;
import fuzs.strawstatues.world.entity.decoration.ImportedStrawStatue;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

public class C2SImportedStrawStatueMessage implements MessageV2<C2SImportedStrawStatueMessage> {
    private CompoundTag modelTag;
    private String modelId;

    public C2SImportedStrawStatueMessage() {}

    public C2SImportedStrawStatueMessage(ImportedModelData modelData, String modelId) {
        this.modelTag = modelData != null ? modelData.toTag() : new CompoundTag();
        this.modelId = modelId != null ? modelId : "";
    }

    public static void sendToServer(ImportedModelData modelData, String modelId) {
        StrawStatues.NETWORK.sendToServer(new C2SImportedStrawStatueMessage(modelData, modelId));
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeNbt(this.modelTag);
        buf.writeUtf(this.modelId);
    }

    @Override
    public void read(FriendlyByteBuf buf) {
        this.modelTag = buf.readNbt();
        this.modelId = buf.readUtf();
    }

    @Override
    public MessageHandler<C2SImportedStrawStatueMessage> makeHandler() {
        return new MessageHandler<>() {
            @Override
            public void handle(C2SImportedStrawStatueMessage message, Player player, Object gameInstance) {
                if (player.containerMenu instanceof ArmorStandMenu menu && menu.stillValid(player)) {
                    if (menu.getArmorStand() instanceof ImportedStrawStatue statue) {
                        if (message.modelTag != null && !message.modelTag.isEmpty()) {
                            statue.setImportedModel(ImportedModelData.fromTag(message.modelTag));
                        } else {
                            statue.setImportedModel(null);
                        }
                    }
                }
            }
        };
    }
}
