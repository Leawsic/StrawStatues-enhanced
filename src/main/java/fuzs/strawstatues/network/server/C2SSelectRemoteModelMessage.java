package fuzs.strawstatues.network.server;

import fuzs.puzzleslib.api.network.v2.MessageV2;
import fuzs.strawstatues.StrawStatues;
import fuzs.strawstatues.importmodel.ImportedModelData;
import fuzs.strawstatues.server.importmodel.ServerModelRegistry;
import fuzs.strawstatues.world.entity.decoration.ImportedStrawStatue;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * C2S: Client selects a remote model for a statue.
 * Includes a set of fileName→hash of files the client already has (for resume).
 * Server only pushes files that are missing or have changed.
 * Tracks ongoing downloads per client to prevent duplicates.
 */
public class C2SSelectRemoteModelMessage implements MessageV2<C2SSelectRemoteModelMessage> {
    // Prevents duplicate downloads: key = "playerUUID:modelId"
    private static final Set<String> ONGOING_DOWNLOADS = ConcurrentHashMap.newKeySet();

    private int entityId;
    private String modelId;
    private Map<String, String> clientHashes; // fileName → sha256

    public C2SSelectRemoteModelMessage() {}

    public C2SSelectRemoteModelMessage(int entityId, String modelId, Map<String, String> clientHashes) {
        this.entityId = entityId;
        this.modelId = modelId;
        this.clientHashes = clientHashes;
    }

    public static void sendToServer(int entityId, String modelId, Map<String, String> clientHashes) {
        StrawStatues.NETWORK.sendToServer(new C2SSelectRemoteModelMessage(entityId, modelId, clientHashes));
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(this.entityId);
        buf.writeUtf(this.modelId);
        buf.writeShort(this.clientHashes.size());
        for (var e : this.clientHashes.entrySet()) {
            buf.writeUtf(e.getKey());
            buf.writeUtf(e.getValue());
        }
    }

    @Override
    public void read(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.modelId = buf.readUtf();
        int n = buf.readShort();
        this.clientHashes = new HashMap<>(n);
        for (int i = 0; i < n; i++) this.clientHashes.put(buf.readUtf(), buf.readUtf());
    }

    @Override
    public MessageHandler<C2SSelectRemoteModelMessage> makeHandler() {
        return new MessageHandler<>() {
            @Override
            public void handle(C2SSelectRemoteModelMessage message, Player player, Object gameInstance) {
                if (!(player instanceof ServerPlayer serverPlayer)) return;

                // Duplicate download prevention
                String downloadKey = player.getStringUUID() + ":" + message.modelId;
                if (!ONGOING_DOWNLOADS.add(downloadKey)) {
                    StrawStatues.LOGGER.debug("Download already in progress for '{}' -> '{}'", player.getScoreboardName(), message.modelId);
                    return;
                }

                try {
                    if (!ServerModelRegistry.isModelAvailable(message.modelId)) return;
                    if (!(player.level().getEntity(message.entityId) instanceof ImportedStrawStatue statue))
                        return;

                    // Set entity data
                    ImportedModelData data = new ImportedModelData();
                    data.setModelId(message.modelId);
                    statue.setImportedModel(data);

                    // Compute server-side file hashes
                    Map<String, String> serverHashes = ServerModelRegistry.computeAllHashes(message.modelId);

                    // Only send files the client doesn't have or has with wrong hash
                    Set<String> toSend = new HashSet<>();
                    for (var e : serverHashes.entrySet()) {
                        String clientHash = message.clientHashes.get(e.getKey());
                        if (clientHash == null || !clientHash.equals(e.getValue())) {
                            toSend.add(e.getKey());
                        }
                    }

                    for (String fileName : toSend) {
                        byte[] fileData = ServerModelRegistry.readModelFile(message.modelId, fileName);
                        if (fileData != null) {
                            StrawStatues.NETWORK.sendTo(
                                    new S2CDownloadModelMessage(message.modelId, fileName, fileData),
                                    serverPlayer);
                        }
                    }
                    // Signal completion with file hashes
                    StrawStatues.NETWORK.sendTo(
                            new S2CDownloadCompleteMessage(message.modelId, serverHashes),
                            serverPlayer);
                } finally {
                    ONGOING_DOWNLOADS.remove(downloadKey);
                }
            }
        };
    }
}
