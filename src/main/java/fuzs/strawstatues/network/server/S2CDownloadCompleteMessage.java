package fuzs.strawstatues.network.server;

import fuzs.puzzleslib.api.network.v2.MessageV2;
import fuzs.strawstatues.StrawStatues;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * S2C: Server signals that all required files for a remote model have been sent.
 * Includes file hashes so the client can verify and store them for resume.
 */
public class S2CDownloadCompleteMessage implements MessageV2<S2CDownloadCompleteMessage> {
    private String modelId;
    private Map<String, String> fileHashes;

    public S2CDownloadCompleteMessage() {}

    public S2CDownloadCompleteMessage(String modelId, Map<String, String> fileHashes) {
        this.modelId = modelId;
        this.fileHashes = fileHashes;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(this.modelId);
        buf.writeShort(this.fileHashes.size());
        for (var e : this.fileHashes.entrySet()) {
            buf.writeUtf(e.getKey());
            buf.writeUtf(e.getValue());
        }
    }

    @Override
    public void read(FriendlyByteBuf buf) {
        this.modelId = buf.readUtf();
        int n = buf.readShort();
        this.fileHashes = new HashMap<>(n);
        for (int i = 0; i < n; i++) this.fileHashes.put(buf.readUtf(), buf.readUtf());
    }

    @Override
    public MessageHandler<S2CDownloadCompleteMessage> makeHandler() {
        return new MessageHandler<>() {
            @Override
            public void handle(S2CDownloadCompleteMessage message, Player player, Object gameInstance) {
                fuzs.strawstatues.client.importmodel.RemoteModelCache.storeHashes(message.modelId, message.fileHashes);
                fuzs.strawstatues.client.importmodel.ImportedModelRegistry.reload();
                StrawStatues.LOGGER.info("Downloaded and reloaded model '{}'", message.modelId);
            }
        };
    }
}
