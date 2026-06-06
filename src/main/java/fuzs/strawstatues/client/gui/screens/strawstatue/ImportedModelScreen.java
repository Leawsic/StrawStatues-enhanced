package fuzs.strawstatues.client.gui.screens.strawstatue;

import fuzs.puzzlesapi.api.client.statues.v1.gui.screens.armorstand.ArmorStandPositionScreen;
import fuzs.puzzlesapi.api.statues.v1.network.client.data.DataSyncHandler;
import fuzs.puzzlesapi.api.statues.v1.world.inventory.ArmorStandHolder;
import fuzs.puzzlesapi.api.statues.v1.world.inventory.data.ArmorStandScreenType;
import fuzs.strawstatues.StrawStatues;
import fuzs.strawstatues.client.importmodel.ImportedModelRegistry;
import fuzs.strawstatues.importmodel.ImportedModelData;
import fuzs.strawstatues.init.ModRegistry;
import fuzs.strawstatues.network.client.C2SImportedStrawStatueMessage;
import fuzs.strawstatues.world.entity.decoration.ImportedStrawStatue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Screen for managing imported 3D models.
 * Features:
 * - Lists available imported models
 * - Accepts .zip file drag-and-drop for importing new models
 * - Allows selecting a model for the current statue
 * - Allows deleting imported models
 */
public class ImportedModelScreen extends ArmorStandPositionScreen {

    private static final Component TITLE = Component.translatable(StrawStatues.MOD_ID + ".screen.importedModel.title");

    private ModelList modelList;
    private Button selectButton;
    private Button deleteButton;
    private Button reloadButton;
    private String selectedModelId;

    public ImportedModelScreen(ArmorStandHolder holder, Inventory inventory, Component component, DataSyncHandler dataSyncHandler) {
        super(holder, inventory, component, dataSyncHandler);
    }

    @Override
    protected List<ArmorStandWidget> buildWidgets(net.minecraft.world.entity.decoration.ArmorStand armorStand) {
        return Collections.emptyList();
    }

    @Override
    protected void init() {
        super.init();

        // Model list widget
        this.modelList = new ModelList(this);
        this.addRenderableWidget(this.modelList);
        this.modelList.updateEntries();

        // Select button
        this.selectButton = this.addRenderableWidget(
                Button.builder(Component.translatable(StrawStatues.MOD_ID + ".screen.importedModel.select"),
                                btn -> selectCurrentModel())
                        .pos(this.leftPos + 8, this.topPos + this.imageHeight - 28)
                        .size(60, 20).build());
        this.selectButton.active = false;

        // Delete button
        this.deleteButton = this.addRenderableWidget(
                Button.builder(Component.translatable(StrawStatues.MOD_ID + ".screen.importedModel.delete"),
                                btn -> deleteSelectedModel())
                        .pos(this.leftPos + 74, this.topPos + this.imageHeight - 28)
                        .size(60, 20).build());
        this.deleteButton.active = false;

        // Reload button
        this.reloadButton = this.addRenderableWidget(
                Button.builder(Component.translatable(StrawStatues.MOD_ID + ".screen.importedModel.reload"),
                                btn -> reloadModels())
                        .pos(this.leftPos + 140, this.topPos + this.imageHeight - 28)
                        .size(60, 20).build());

        // Done button
        this.addRenderableWidget(
                Button.builder(CommonComponents.GUI_DONE, btn -> this.onClose())
                        .pos(this.leftPos + this.imageWidth - 68, this.topPos + this.imageHeight - 28)
                        .size(60, 20).build());

        this.selectedModelId = null;
    }

    // ── Actions ─────────────────────────────────────────────

    private void selectCurrentModel() {
        if (this.selectedModelId == null || this.selectedModelId.isEmpty()) return;

        ImportedModelData data = new ImportedModelData();
        data.setModelId(this.selectedModelId);

        if (this.holder.getArmorStand() instanceof ImportedStrawStatue statue) {
            statue.setImportedModel(data);
            C2SImportedStrawStatueMessage.sendToServer(data, this.selectedModelId);
        }

        this.selectButton.active = true;
    }

    private void deleteSelectedModel() {
        if (this.selectedModelId == null) return;

        Path modelDir = ImportedModelRegistry.getModelDir(this.selectedModelId);
        try {
            deleteDirectory(modelDir);
        } catch (IOException e) {
            StrawStatues.LOGGER.warn("Failed to delete model '{}'", this.selectedModelId, e);
        }

        // If the current statue uses this model, clear it
        if (this.holder.getArmorStand() instanceof ImportedStrawStatue statue) {
            ImportedModelData data = statue.getImportedModel();
            if (data != null && this.selectedModelId.equals(data.getModelId())) {
                statue.setImportedModel(null);
                C2SImportedStrawStatueMessage.sendToServer(null, "");
            }
        }

        this.selectedModelId = null;
        this.selectButton.active = false;
        this.deleteButton.active = false;
        reloadModels();
    }

    private void reloadModels() {
        ImportedModelRegistry.reload();
        this.modelList.updateEntries();
    }

    // ── Drag & Drop ─────────────────────────────────────────

    @Override
    public void onFilesDrop(List<Path> files) {
        for (Path file : files) {
            if (file.toString().endsWith(".zip")) {
                importZip(file);
            }
        }
        reloadModels();
    }

    private void importZip(Path zipPath) {
        String zipName = zipPath.getFileName().toString();
        if (zipName.endsWith(".zip")) {
            zipName = zipName.substring(0, zipName.length() - 4);
        }
        // Sanitize: only allow alphanumeric, dashes, underscores
        zipName = zipName.replaceAll("[^a-zA-Z0-9_-]", "_");

        Path targetDir = ImportedModelRegistry.getModelDir(zipName);
        try {
            Files.createDirectories(targetDir);
            extractZip(zipPath, targetDir);
            StrawStatues.LOGGER.info("Imported model '{}' from {}", zipName, zipPath);
        } catch (IOException e) {
            StrawStatues.LOGGER.warn("Failed to import model from {}: {}", zipPath, e.getMessage());
        }
    }

    private void extractZip(Path zipPath, Path targetDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                // Strip any directory prefix, only keep filename
                String fileName = entry.getName();
                if (fileName.contains("/")) {
                    fileName = fileName.substring(fileName.lastIndexOf('/') + 1);
                }
                if (fileName.contains("\\")) {
                    fileName = fileName.substring(fileName.lastIndexOf('\\') + 1);
                }
                if (fileName.isEmpty()) continue;
                Path targetFile = targetDir.resolve(fileName);
                try (InputStream entryStream = zis) {
                    Files.copy(entryStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private static void deleteDirectory(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) return;
        try (var walk = Files.walk(dir)) {
            walk.sorted(java.util.Comparator.reverseOrder())
                    .forEach(path -> {
                        try { Files.deleteIfExists(path); } catch (IOException ignored) {}
                    });
        }
    }

    // ── Render ──────────────────────────────────────────────

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawString(this.font, TITLE,
                this.leftPos + (this.imageWidth - this.font.width(TITLE)) / 2,
                this.topPos + 6, 0xFFFFFFFF, false);

        // Drop hint at bottom
        Component hint = Component.translatable(StrawStatues.MOD_ID + ".screen.importedModel.dropHint");
        int hintY = this.topPos + this.imageHeight - 50;
        guiGraphics.drawString(this.font, hint,
                this.leftPos + (this.imageWidth - this.font.width(hint)) / 2,
                hintY, 0xFF888888, false);
    }

    // ── Model list widget ───────────────────────────────────

    private class ModelList extends ObjectSelectionList<ModelList.Entry> {

        public ModelList(ImportedModelScreen screen) {
            super(Minecraft.getInstance(),
                    screen.imageWidth - 16, screen.imageHeight - 78,
                    screen.topPos + 18, screen.topPos + screen.imageHeight - 52, 18);
            this.setLeftPos(screen.leftPos + 8);
        }

        public void updateEntries() {
            this.clearEntries();
            for (String modelId : ImportedModelRegistry.getAvailableModelIds()) {
                this.addEntry(new Entry(modelId));
            }
        }

        @Override
        public int getRowWidth() {
            return this.width;
        }

        public class Entry extends ObjectSelectionList.Entry<Entry> {
            private final String modelId;

            public Entry(String modelId) {
                this.modelId = modelId;
            }

            @Override
            public Component getNarration() {
                return Component.literal(this.modelId);
            }

            @Override
            public void render(GuiGraphics guiGraphics, int index, int top, int left,
                               int width, int height, int mouseX, int mouseY,
                               boolean hovering, float partialTick) {
                guiGraphics.drawString(Minecraft.getInstance().font, this.modelId,
                        left + 4, top + (height - 8) / 2, 0xFFFFFFFF, false);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (button == 0) {
                    modelList.setSelected(this);
                    ImportedModelScreen.this.selectedModelId = this.modelId;
                    ImportedModelScreen.this.selectButton.active = true;
                    ImportedModelScreen.this.deleteButton.active = true;
                    return true;
                }
                return false;
            }
        }
    }

    @Override
    public ArmorStandScreenType getScreenType() {
        return ModRegistry.STRAW_STATUE_IMPORTED_MODEL_SCREEN_TYPE;
    }
}
