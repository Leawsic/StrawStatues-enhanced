package fuzs.strawstatues.network.server;

import fuzs.puzzleslib.api.network.v2.MessageV2;
import fuzs.strawstatues.StrawStatues;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

public class S2CDownloadCompleteMessage implements MessageV2<S2CDownloadCompleteMessage> {
    private String modelId;

    public S2CDownloadCompleteMessage() {}
    public S2CDownloadCompleteMessage(String modelId) { this.modelId = modelId; }

    @Override
    public void write(FriendlyByteBuf buf) { buf.writeUtf(this.modelId); }
    @Override
    public void read(FriendlyByteBuf buf) { this.modelId = buf.readUtf(); }

    @Override
    public MessageHandler<S2CDownloadCompleteMessage> makeHandler() {
        return new MessageHandler<>() {
            @Override
            public void handle(S2CDownloadCompleteMessage message, Player player, Object gameInstance) {
                fuzs.strawstatues.client.importmodel.ImportedModelRegistry.reload();
                StrawStatues.LOGGER.info("Downloaded and reloaded model '{}'", message.modelId);
            }
        };
    }
}
