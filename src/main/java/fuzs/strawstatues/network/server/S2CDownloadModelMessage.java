package fuzs.strawstatues.network.server;

import fuzs.puzzleslib.api.network.v2.MessageV2;
import fuzs.strawstatues.StrawStatues;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import java.nio.file.Files;

public class S2CDownloadModelMessage implements MessageV2<S2CDownloadModelMessage> {
    private String modelId;
    private String fileName;
    private byte[] data;

    public S2CDownloadModelMessage() {}
    public S2CDownloadModelMessage(String modelId, String fileName, byte[] data) {
        this.modelId = modelId; this.fileName = fileName; this.data = data;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(this.modelId); buf.writeUtf(this.fileName); buf.writeByteArray(this.data);
    }

    @Override
    public void read(FriendlyByteBuf buf) {
        this.modelId = buf.readUtf(); this.fileName = buf.readUtf(); this.data = buf.readByteArray();
    }

    @Override
    public MessageHandler<S2CDownloadModelMessage> makeHandler() {
        return new MessageHandler<>() {
            @Override
            public void handle(S2CDownloadModelMessage message, Player player, Object gameInstance) {
                try {
                    var dir = fuzs.strawstatues.client.importmodel.ImportedModelRegistry.getModelDir(message.modelId);
                    Files.createDirectories(dir);
                    Files.write(dir.resolve(message.fileName), message.data);
                } catch (Exception e) {
                    StrawStatues.LOGGER.warn("Failed to save downloaded file '{}' for '{}'", message.fileName, message.modelId);
                }
            }
        };
    }
}
