package fuzs.strawstatues.client.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import fuzs.strawstatues.client.importmodel.ImportedModelRegistry;
import fuzs.strawstatues.importmodel.ImportedModelData;
import fuzs.strawstatues.network.client.C2SImportedStrawStatueMessage;
import fuzs.strawstatues.world.entity.decoration.ImportedStrawStatue;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Client-side commands for imported model management.
 * Registered via ClientCommandRegistrationCallback.
 */
public final class ImportedModelCommands {

    private ImportedModelCommands() {}

    public static void register() {
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
                        c.getSource().sendFeedback(Component.literal("No imported models found. Place files in config/strawstatues/imported_models/<id>/"));
                    } else {
                        c.getSource().sendFeedback(Component.literal("Available: " + String.join(", ", ids)));
                    }
                    return ids.size();
                });

        var nearSelect = ClientCommandManager.literal("near")
                .then(ClientCommandManager.argument("modelId", StringArgumentType.greedyString())
                        .executes(c -> {
                            String modelId = StringArgumentType.getString(c, "modelId");
                            var player = c.getSource().getPlayer();
                            // Find closest ImportedStrawStatue within 10 blocks
                            var nearest = player.level().getEntitiesOfClass(
                                    fuzs.strawstatues.world.entity.decoration.ImportedStrawStatue.class,
                                    player.getBoundingBox().inflate(10));
                            if (nearest.isEmpty()) {
                                c.getSource().sendError(Component.literal("No Imported Straw Statue nearby"));
                                return 0;
                            }
                            ImportedStrawStatue statue = nearest.get(0);
                            if (!ImportedModelRegistry.isModelLoaded(modelId)) {
                                c.getSource().sendError(Component.literal("Model '" + modelId + "' not found"));
                                return 0;
                            }
                            ImportedModelData data = new ImportedModelData();
                            data.setModelId(modelId);
                            statue.setImportedModel(data);
                            C2SImportedStrawStatueMessage.sendToServer(data, modelId);
                            c.getSource().sendFeedback(Component.literal("Model set to: " + modelId + " (nearest statue)"));
                            return 1;
                        }));

        var select = ClientCommandManager.literal("select")
                .then(ClientCommandManager.argument("modelId", StringArgumentType.greedyString())
                        .executes(c -> {
                            String modelId = StringArgumentType.getString(c, "modelId");

                            // Method 1: client crosshair result (most accurate)
                            Minecraft mc = Minecraft.getInstance();
                            if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.ENTITY
                                    && mc.hitResult instanceof EntityHitResult entityHit
                                    && entityHit.getEntity() instanceof ImportedStrawStatue statue) {
                                if (!ImportedModelRegistry.isModelLoaded(modelId)) {
                                    c.getSource().sendError(Component.literal("Model '" + modelId + "' not found"));
                                    return 0;
                                }
                                ImportedModelData data = new ImportedModelData();
                                data.setModelId(modelId);
                                statue.setImportedModel(data);
                                C2SImportedStrawStatueMessage.sendToServer(data, modelId);
                                c.getSource().sendFeedback(Component.literal("Model set to: " + modelId));
                                return 1;
                            }

                            // Method 2: player pick trace
                            var player = c.getSource().getPlayer();
                            var hit = player.pick(20, 0, false);
                            if (hit.getType() == HitResult.Type.ENTITY
                                    && hit instanceof EntityHitResult entityHit2
                                    && entityHit2.getEntity() instanceof ImportedStrawStatue statue2) {
                                if (!ImportedModelRegistry.isModelLoaded(modelId)) {
                                    c.getSource().sendError(Component.literal("Model '" + modelId + "' not found"));
                                    return 0;
                                }
                                ImportedModelData data = new ImportedModelData();
                                data.setModelId(modelId);
                                statue2.setImportedModel(data);
                                C2SImportedStrawStatueMessage.sendToServer(data, modelId);
                                c.getSource().sendFeedback(Component.literal("Model set to: " + modelId));
                                return 1;
                            }

                            c.getSource().sendError(Component.literal("You must look at an Imported Straw Statue. Try /strawstatues import near " + modelId));
                            return 0;
                        }));

        var imp = ClientCommandManager.literal("import");
        imp.then(reload);
        imp.then(list);
        imp.then(select);
        imp.then(nearSelect);

        ClientCommandManager.getActiveDispatcher().register(
                ClientCommandManager.literal("strawstatues").then(imp));
    }
}
