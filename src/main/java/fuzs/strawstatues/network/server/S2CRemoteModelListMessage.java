package fuzs.strawstatues.network.server;

import fuzs.puzzleslib.api.network.v2.MessageV2;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import java.util.HashSet;
import java.util.Set;

public class S2CRemoteModelListMessage implements MessageV2<S2CRemoteModelListMessage> {
    private Set<String> modelIds;

    public S2CRemoteModelListMessage() {}
    public S2CRemoteModelListMessage(Set<String> modelIds) { this.modelIds = modelIds; }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeShort(this.modelIds.size());
        for (String id : this.modelIds) buf.writeUtf(id);
    }

    @Override
    public void read(FriendlyByteBuf buf) {
        int n = buf.readShort();
        this.modelIds = new HashSet<>(n);
        for (int i = 0; i < n; i++) this.modelIds.add(buf.readUtf());
    }

    @Override
    public MessageHandler<S2CRemoteModelListMessage> makeHandler() {
        return new MessageHandler<>() {
            @Override
            public void handle(S2CRemoteModelListMessage message, Player player, Object gameInstance) {
                fuzs.strawstatues.client.importmodel.RemoteModelCache.setAvailable(message.modelIds);
            }
        };
    }
}
