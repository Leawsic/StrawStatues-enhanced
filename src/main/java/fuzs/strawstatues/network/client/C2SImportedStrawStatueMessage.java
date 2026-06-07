package fuzs.strawstatues.network.client;

import fuzs.puzzleslib.api.network.v2.MessageV2;
import fuzs.strawstatues.StrawStatues;
import fuzs.strawstatues.importmodel.ImportedModelData;
import fuzs.strawstatues.world.entity.decoration.ImportedStrawStatue;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

/**
 * C2S message that sets the imported model on a statue.
 * The entity is identified by its network ID (not requiring an open container menu).
 */
public class C2SImportedStrawStatueMessage implements MessageV2<C2SImportedStrawStatueMessage> {
    private int entityId;
    private CompoundTag modelTag;

    public C2SImportedStrawStatueMessage() {}

    public C2SImportedStrawStatueMessage(int entityId, ImportedModelData modelData) {
        this.entityId = entityId;
        this.modelTag = modelData != null ? modelData.toTag() : new CompoundTag();
    }

    /**
     * Send the message from client to server.
     * @param entityId The network ID of the target entity (can be obtained via entity.getId())
     * @param modelData The model data to set
     */
    public static void sendToServer(int entityId, ImportedModelData modelData) {
        StrawStatues.NETWORK.sendToServer(new C2SImportedStrawStatueMessage(entityId, modelData));
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(this.entityId);
        buf.writeNbt(this.modelTag);
    }

    @Override
    public void read(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.modelTag = buf.readNbt();
    }

    @Override
    public MessageHandler<C2SImportedStrawStatueMessage> makeHandler() {
        return new MessageHandler<>() {
            @Override
            public void handle(C2SImportedStrawStatueMessage message, Player player, Object gameInstance) {
                // Find the entity by network ID (no container menu required)
                if (player.level().getEntity(message.entityId) instanceof ImportedStrawStatue statue) {
                    if (message.modelTag != null && !message.modelTag.isEmpty()) {
                        statue.setImportedModel(ImportedModelData.fromTag(message.modelTag));
                    } else {
                        statue.setImportedModel(null);
                    }
                }
            }
        };
    }
}
