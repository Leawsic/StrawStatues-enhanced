package fuzs.strawstatues.client.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import fuzs.strawstatues.client.importmodel.ImportedModelRegistry;
import fuzs.strawstatues.client.importmodel.RemoteModelCache;
import fuzs.strawstatues.importmodel.ImportedModelData;
import fuzs.strawstatues.network.client.C2SImportedStrawStatueMessage;
import fuzs.strawstatues.network.client.C2SUploadModelMessage;
import fuzs.strawstatues.network.server.C2SRequestRemoteModelsMessage;
import fuzs.strawstatues.network.server.C2SSelectRemoteModelMessage;
import fuzs.strawstatues.world.entity.decoration.ImportedStrawStatue;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class ImportedModelCommands {

    private ImportedModelCommands() {}

    public static void register() {
        // --- local subcommands ---

        var reload = ClientCommandManager.literal("reload")
                .executes(c -> {
                    ImportedModelRegistry.reload();
                    int count = ImportedModelRegistry.getAvailableModelIds().size();
                    c.getSource().sendFeedback(Component.literal("Reloaded " + count + " imported models"));
                    return count;
                });

        var list = ClientCommandManager.literal("list")
                .executes(c -> {
                    var ids = ImportedModelRegistry.getAvailableModelIds();
                    if (ids.isEmpty()) {
                        c.getSource().sendFeedback(Component.literal("No local models found"));
                    } else {
                        c.getSource().sendFeedback(Component.literal("Local models: " + String.join(", ", ids)));
                    }
                    return ids.size();
                });

        var select = ClientCommandManager.literal("select")
                .then(ClientCommandManager.argument("modelId", StringArgumentType.greedyString())
                        .executes(c -> {
                            String modelId = StringArgumentType.getString(c, "modelId");
                            if (!ImportedModelRegistry.isModelLoaded(modelId)) {
                                c.getSource().sendError(Component.literal("Local model '" + modelId + "' not found"));
                                return 0;
                            }
                            return applyToLookedAt(c.getSource(), modelId, false);
                        }));

        var near = ClientCommandManager.literal("near")
                .then(ClientCommandManager.argument("modelId", StringArgumentType.greedyString())
                        .executes(c -> {
                            String modelId = StringArgumentType.getString(c, "modelId");
                            if (!ImportedModelRegistry.isModelLoaded(modelId)) {
                                c.getSource().sendError(Component.literal("Local model '" + modelId + "' not found"));
                                return 0;
                            }
                            return applyToNearest(c.getSource(), modelId, false);
                        }));

        var upload = ClientCommandManager.literal("upload")
                .then(ClientCommandManager.argument("modelId", StringArgumentType.greedyString())
                        .executes(c -> {
                            String modelId = StringArgumentType.getString(c, "modelId");
                            Path dir = ImportedModelRegistry.getModelDir(modelId);
                            if (!Files.isDirectory(dir)) {
                                c.getSource().sendError(Component.literal("Local model '" + modelId + "' not found on disk"));
                                return 0;
                            }
                            Map<String, byte[]> files = new HashMap<>();
                            try (var stream = Files.list(dir)) {
                                for (Path f : stream.toList()) {
                                    if (!Files.isRegularFile(f)) continue;
                                    String name = f.getFileName().toString().toLowerCase();
                                    if (!name.endsWith(".geo.json") && !name.endsWith(".png") && !name.endsWith(".animation.json")) {
                                        c.getSource().sendFeedback(Component.literal("Skipping unsupported file: " + f.getFileName()));
                                        continue;
                                    }
                                    files.put(f.getFileName().toString(), Files.readAllBytes(f));
                                }
                            } catch (IOException e) {
                                c.getSource().sendError(Component.literal("Failed to read model files: " + e.getMessage()));
                                return 0;
                            }
                            if (files.isEmpty()) {
                                c.getSource().sendError(Component.literal("No valid model files found (need .geo.json + .png)"));
                                return 0;
                            }
                            if (files.keySet().stream().noneMatch(n -> n.endsWith(".geo.json"))) {
                                c.getSource().sendError(Component.literal("Upload must include at least one .geo.json file"));
                                return 0;
                            }
                            if (files.keySet().stream().noneMatch(n -> n.endsWith(".png"))) {
                                c.getSource().sendError(Component.literal("Upload must include a .png texture"));
                                return 0;
                            }
                            C2SUploadModelMessage.sendToServer(modelId, files);
                            c.getSource().sendFeedback(Component.literal("Uploaded model '" + modelId + "' (" + files.size() + " files) to server"));
                            return 1;
                        }));

        var local = ClientCommandManager.literal("local");
        local.then(reload);
        local.then(list);
        local.then(select);
        local.then(near);
        local.then(upload);

        // --- remote subcommands ---

        var remoteList = ClientCommandManager.literal("list")
                .executes(c -> {
                    fuzs.strawstatues.StrawStatues.NETWORK.sendToServer(new C2SRequestRemoteModelsMessage());
                    // Also show what we have cached from previous responses
                    var cached = RemoteModelCache.getAvailable();
                    if (cached.isEmpty()) {
                        c.getSource().sendFeedback(Component.literal("Requesting remote model list... (none cached yet)"));
                    } else {
                        c.getSource().sendFeedback(Component.literal("Cached remote models: " + String.join(", ", cached)));
                    }
                    return 1;
                });

        var remoteSelect = ClientCommandManager.literal("select")
                .then(ClientCommandManager.argument("modelId", StringArgumentType.greedyString())
                        .executes(c -> {
                            String modelId = StringArgumentType.getString(c, "modelId");
                            if (!RemoteModelCache.isAvailable(modelId)) {
                                c.getSource().sendError(Component.literal("Remote model '" + modelId + "' not available. Use /strawstatues import remote list"));
                                return 0;
                            }
                            return applyToLookedAt(c.getSource(), modelId, true);
                        }));

        var remoteNear = ClientCommandManager.literal("near")
                .then(ClientCommandManager.argument("modelId", StringArgumentType.greedyString())
                        .executes(c -> {
                            String modelId = StringArgumentType.getString(c, "modelId");
                            if (!RemoteModelCache.isAvailable(modelId)) {
                                c.getSource().sendError(Component.literal("Remote model '" + modelId + "' not available"));
                                return 0;
                            }
                            return applyToNearest(c.getSource(), modelId, true);
                        }));

        var remote = ClientCommandManager.literal("remote");
        remote.then(remoteList);
        remote.then(remoteSelect);
        remote.then(remoteNear);

        // --- root ---

        var imp = ClientCommandManager.literal("import");
        imp.then(local);
        imp.then(remote);

        ClientCommandManager.getActiveDispatcher().register(
                ClientCommandManager.literal("strawstatues").then(imp));
    }

    /** Apply model to statue the player is looking at. */
    private static int applyToLookedAt(net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource source,
                                        String modelId, boolean remote) {
        Minecraft mc = Minecraft.getInstance();

        // Crosshair result
        if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.ENTITY
                && mc.hitResult instanceof EntityHitResult entityHit
                && entityHit.getEntity() instanceof ImportedStrawStatue statue) {
            return applyModel(source, statue, modelId, remote);
        }

        // Ray trace fallback
        var player = source.getPlayer();
        var hit = player.pick(20, 0, false);
        if (hit.getType() == HitResult.Type.ENTITY
                && hit instanceof EntityHitResult entityHit2
                && entityHit2.getEntity() instanceof ImportedStrawStatue statue2) {
            return applyModel(source, statue2, modelId, remote);
        }

        source.sendError(Component.literal("You must look at an Imported Straw Statue. Try /strawstatues import <local|remote> near " + modelId));
        return 0;
    }

    /** Apply model to nearest statue within 10 blocks. */
    private static int applyToNearest(net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource source,
                                       String modelId, boolean remote) {
        var player = source.getPlayer();
        var nearest = player.level().getEntitiesOfClass(
                ImportedStrawStatue.class,
                player.getBoundingBox().inflate(10));
        if (nearest.isEmpty()) {
            source.sendError(Component.literal("No Imported Straw Statue within 10 blocks"));
            return 0;
        }
        return applyModel(source, nearest.get(0), modelId, remote);
    }

    /** Set model on entity and send the appropriate network message. */
    private static int applyModel(net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource source,
                                   ImportedStrawStatue statue, String modelId, boolean remote) {
        ImportedModelData data = new ImportedModelData();
        data.setModelId(modelId);
        statue.setImportedModel(data);

        if (remote) {
            // Send client's existing file hashes for resume support
            Map<String, String> localHashes = RemoteModelCache.computeLocalHashes(modelId);
            C2SSelectRemoteModelMessage.sendToServer(statue.getId(), modelId, localHashes);
            int knownFiles = localHashes.size();
            source.sendFeedback(Component.literal("Requested remote model '" + modelId + "' from server" +
                    (knownFiles > 0 ? " (" + knownFiles + " known files, resuming)" : "")));
        } else {
            C2SImportedStrawStatueMessage.sendToServer(statue.getId(), data);
            source.sendFeedback(Component.literal("Model set to: " + modelId));
        }
        return 1;
    }
}
