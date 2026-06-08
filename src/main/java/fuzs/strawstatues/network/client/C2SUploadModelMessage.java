package fuzs.strawstatues.network.client;

import fuzs.puzzleslib.api.network.v2.MessageV2;
import fuzs.strawstatues.StrawStatues;
import fuzs.strawstatues.server.importmodel.ServerModelRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import java.util.HashMap;
import java.util.Map;

public class C2SUploadModelMessage implements MessageV2<C2SUploadModelMessage> {
    private String modelId;
    private Map<String, byte[]> files;

    public C2SUploadModelMessage() {}

    public C2SUploadModelMessage(String modelId, Map<String, byte[]> files) {
        this.modelId = modelId;
        this.files = files;
    }

    public static void sendToServer(String modelId, Map<String, byte[]> files) {
        StrawStatues.NETWORK.sendToServer(new C2SUploadModelMessage(modelId, files));
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(this.modelId);
        buf.writeShort(this.files.size());
        for (var e : this.files.entrySet()) {
            buf.writeUtf(e.getKey());
            buf.writeByteArray(e.getValue());
        }
    }

    @Override
    public void read(FriendlyByteBuf buf) {
        this.modelId = buf.readUtf();
        int n = buf.readShort();
        this.files = new HashMap<>(n);
        for (int i = 0; i < n; i++) this.files.put(buf.readUtf(), buf.readByteArray());
    }

    @Override
    public MessageHandler<C2SUploadModelMessage> makeHandler() {
        return new MessageHandler<>() {
            @Override
            public void handle(C2SUploadModelMessage message, Player player, Object gameInstance) {
                // Add uploader metadata file alongside the model files
                message.files.put(".uploader", player.getScoreboardName().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                ServerModelRegistry.saveUploadedModel(message.modelId, message.files);
            }
        };
    }
}
