package fuzs.strawstatues.client.gui.screens.strawstatue;

import com.mojang.brigadier.arguments.StringArgumentType;
import fuzs.strawstatues.client.importmodel.ImportedModelRegistry;
import fuzs.strawstatues.network.client.C2SImportedStrawStatueMessage;
import fuzs.strawstatues.world.entity.decoration.ImportedStrawStatue;
import fuzs.strawstatues.importmodel.ImportedModelData;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.minecraft.network.chat.Component;

/**
 * No GUI — all imported model management is done via commands.
 * Usage:
 *   /strawstatues import reload              — scan config dir, reload models
 *   /strawstatues import list                — list available model IDs
 *   /strawstatues import select <modelId>    — set model on looked-at statue
 */
public final class ImportedModelScreen {

    private ImportedModelScreen() {}

    public static void registerCommands() {
        var reloadCmd = ClientCommandManager.literal("reload")
                .executes(context -> {
                    ImportedModelRegistry.reload();
                    int count = ImportedModelRegistry.getAvailableModelIds().size();
                    context.getSource().sendFeedback(Component.literal("Reloaded " + count + " imported models"));
                    return count;
                });

        var listCmd = ClientCommandManager.literal("list")
                .executes(context -> {
                    var ids = ImportedModelRegistry.getAvailableModelIds();
                    if (ids.isEmpty()) {
                        context.getSource().sendFeedback(Component.literal("No imported models found. Place files in config/strawstatues/imported_models/<id>/"));
                    } else {
                        context.getSource().sendFeedback(Component.literal("Available models: " + String.join(", ", ids)));
                    }
                    return ids.size();
                });

        var selectCmd = ClientCommandManager.literal("select")
                .then(ClientCommandManager.argument("modelId", StringArgumentType.greedyString())
                        .executes(context -> {
                            String modelId = StringArgumentType.getString(context, "modelId");
                            var player = context.getSource().getPlayer();
                            var hit = player.pick(20, 0, false);
                            if (hit.getType() == net.minecraft.world.phys.HitResult.Type.ENTITY
                                    && hit instanceof net.minecraft.world.phys.EntityHitResult entityHit
                                    && entityHit.getEntity() instanceof ImportedStrawStatue statue) {
                                if (!ImportedModelRegistry.isModelLoaded(modelId)) {
                                    context.getSource().sendError(Component.literal("Model '" + modelId + "' not found"));
                                    return 0;
                                }
                                ImportedModelData data = new ImportedModelData();
                                data.setModelId(modelId);
                                statue.setImportedModel(data);
                                C2SImportedStrawStatueMessage.sendToServer(data, modelId);
                                context.getSource().sendFeedback(Component.literal("Model set to: " + modelId));
                                return 1;
                            }
                            context.getSource().sendError(Component.literal("You must be looking at an Imported Straw Statue"));
                            return 0;
                        }));

        var importCmd = ClientCommandManager.literal("import");
        importCmd.then(reloadCmd);
        importCmd.then(listCmd);
        importCmd.then(selectCmd);

        ClientCommandManager.getActiveDispatcher().register(
                ClientCommandManager.literal("strawstatues").then(importCmd)
        );
    }
}
