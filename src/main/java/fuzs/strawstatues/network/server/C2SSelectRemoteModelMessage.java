package fuzs.strawstatues.network.server;

import fuzs.puzzleslib.api.network.v2.MessageV2;
import fuzs.strawstatues.StrawStatues;
import fuzs.strawstatues.importmodel.ImportedModelData;
import fuzs.strawstatues.server.importmodel.ServerModelRegistry;
import fuzs.strawstatues.world.entity.decoration.ImportedStrawStatue;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * C2S: Client selects a remote model for a statue.
 * Server sets the entity data and pushes all model files to the client via S2CDownloadModelMessage.
 */
public class C2SSelectRemoteModelMessage implements MessageV2<C2SSelectRemoteModelMessage> {
    private int entityId;
    private String modelId;

    public C2SSelectRemoteModelMessage() {}

    public C2SSelectRemoteModelMessage(int entityId, String modelId) {
        this.entityId = entityId;
        this.modelId = modelId;
    }

    public static void sendToServer(int entityId, String modelId) {
        StrawStatues.NETWORK.sendToServer(new C2SSelectRemoteModelMessage(entityId, modelId));
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(this.entityId);
        buf.writeUtf(this.modelId);
    }

    @Override
    public void read(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.modelId = buf.readUtf();
    }

    @Override
    public MessageHandler<C2SSelectRemoteModelMessage> makeHandler() {
        return new MessageHandler<>() {
            @Override
            public void handle(C2SSelectRemoteModelMessage message, Player player, Object gameInstance) {
                if (!(player instanceof ServerPlayer serverPlayer)) return;
                if (!ServerModelRegistry.isModelAvailable(message.modelId)) return;
                if (player.level().getEntity(message.entityId) instanceof ImportedStrawStatue statue) {
                    // Set entity data
                    ImportedModelData data = new ImportedModelData();
                    data.setModelId(message.modelId);
                    statue.setImportedModel(data);

                    // Push all model files to the requesting client
                    var files = ServerModelRegistry.listModelFiles(message.modelId);
                    for (String fileName : files) {
                        byte[] fileData = ServerModelRegistry.readModelFile(message.modelId, fileName);
                        if (fileData != null) {
                            StrawStatues.NETWORK.sendTo(
                                    new S2CDownloadModelMessage(message.modelId, fileName, fileData),
                                    serverPlayer);
                        }
                    }
                    // Signal completion so client triggers reload
                    StrawStatues.NETWORK.sendTo(
                            new S2CDownloadCompleteMessage(message.modelId),
                            serverPlayer);
                }
            }
        };
    }
}
