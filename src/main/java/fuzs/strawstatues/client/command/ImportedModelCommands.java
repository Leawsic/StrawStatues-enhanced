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
                        c.getSource().sendFeedback(Component.literal("No local models found"));
                    } else {
                        c.getSource().sendFeedback(Component.literal("Local models: " + String.join(", ", ids)));
                    }
                    return ids.size();
                });

        var select = ClientCommandManager.literal("select")
                .then(ClientCommandManager.argument("modelId", StringArgumentType.greedyString())
                        .executes(c -> {
                            if (!isSinglePlayer()) {
                                c.getSource().sendFeedback(Component.literal("Import model commands only work in singleplayer"));
                                return 0;
                            }
                            String modelId = StringArgumentType.getString(c, "modelId");
                            if (!ImportedModelRegistry.isModelLoaded(modelId)) {
                                c.getSource().sendError(Component.literal("Local model '" + modelId + "' not found"));
                                return 0;
                            }
                            return applyToLookedAt(c.getSource(), modelId);
                        }));

        var near = ClientCommandManager.literal("near")
                .then(ClientCommandManager.argument("modelId", StringArgumentType.greedyString())
                        .executes(c -> {
                            if (!isSinglePlayer()) {
                                c.getSource().sendFeedback(Component.literal("Import model commands only work in singleplayer"));
                                return 0;
                            }
                            String modelId = StringArgumentType.getString(c, "modelId");
                            if (!ImportedModelRegistry.isModelLoaded(modelId)) {
                                c.getSource().sendError(Component.literal("Local model '" + modelId + "' not found"));
                                return 0;
                            }
                            return applyToNearest(c.getSource(), modelId);
                        }));

        var local = ClientCommandManager.literal("local");
        local.then(reload);
        local.then(list);
        local.then(select);
        local.then(near);

        var imp = ClientCommandManager.literal("import");
        imp.then(local);

        ClientCommandManager.getActiveDispatcher().register(
                ClientCommandManager.literal("strawstatues").then(imp));
    }

    private static int applyToLookedAt(net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource source,
                                        String modelId) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.ENTITY
                && mc.hitResult instanceof EntityHitResult entityHit
                && entityHit.getEntity() instanceof ImportedStrawStatue statue) {
            return applyModel(source, statue, modelId);
        }

        var player = source.getPlayer();
        var hit = player.pick(20, 0, false);
        if (hit.getType() == HitResult.Type.ENTITY
                && hit instanceof EntityHitResult entityHit2
                && entityHit2.getEntity() instanceof ImportedStrawStatue statue2) {
            return applyModel(source, statue2, modelId);
        }

        source.sendError(Component.literal("You must look at an Imported Straw Statue. Try /strawstatues import local near " + modelId));
        return 0;
    }

    private static int applyToNearest(net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource source,
                                       String modelId) {
        var player = source.getPlayer();
        var nearest = player.level().getEntitiesOfClass(
                ImportedStrawStatue.class,
                player.getBoundingBox().inflate(10));
        if (nearest.isEmpty()) {
            source.sendError(Component.literal("No Imported Straw Statue within 10 blocks"));
            return 0;
        }
        return applyModel(source, nearest.get(0), modelId);
    }

    private static boolean isSinglePlayer() {
        return Minecraft.getInstance().hasSingleplayerServer();
    }

    private static int applyModel(net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource source,
                                   ImportedStrawStatue statue, String modelId) {
        ImportedModelData data = new ImportedModelData();
        data.setModelId(modelId);
        statue.setImportedModel(data);
        C2SImportedStrawStatueMessage.sendToServer(statue.getId(), data);
        source.sendFeedback(Component.literal("Model set to: " + modelId));
        return 1;
    }
}
