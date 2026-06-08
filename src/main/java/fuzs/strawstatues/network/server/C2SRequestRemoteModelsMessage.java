package fuzs.strawstatues.network.server;

import fuzs.puzzleslib.api.network.v2.MessageV2;
import fuzs.strawstatues.StrawStatues;
import fuzs.strawstatues.server.importmodel.ServerModelRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class C2SRequestRemoteModelsMessage implements MessageV2<C2SRequestRemoteModelsMessage> {
    public C2SRequestRemoteModelsMessage() {}

    @Override
    public void write(FriendlyByteBuf buf) {}
    @Override
    public void read(FriendlyByteBuf buf) {}

    @Override
    public MessageHandler<C2SRequestRemoteModelsMessage> makeHandler() {
        return new MessageHandler<>() {
            @Override
            public void handle(C2SRequestRemoteModelsMessage message, Player player, Object gameInstance) {
                if (player instanceof ServerPlayer sp) {
                    // Send uploader-prefixed IDs for display: "uploader:modelId"
                    StrawStatues.NETWORK.sendTo(
                        new S2CRemoteModelListMessage(ServerModelRegistry.getAvailableWithUploader()), sp);
                }
            }
        };
    }
}
