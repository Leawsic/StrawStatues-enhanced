package fuzs.strawstatues.client.gui.screens.strawstatue;

import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import fuzs.puzzlesapi.api.client.statues.v1.gui.screens.armorstand.ArmorStandPositionScreen;
import fuzs.puzzlesapi.api.statues.v1.network.client.data.DataSyncHandler;
import fuzs.puzzlesapi.api.statues.v1.world.inventory.ArmorStandHolder;
import fuzs.puzzlesapi.api.statues.v1.world.inventory.data.ArmorStandScreenType;
import fuzs.strawstatues.StrawStatues;
import fuzs.strawstatues.client.renderer.entity.StrawStatueRenderer;
import fuzs.strawstatues.init.ModRegistry;
import fuzs.strawstatues.network.client.C2SStrawStatueEyeMessage;
import fuzs.strawstatues.world.entity.decoration.StrawStatue;
import fuzs.strawstatues.world.entity.decoration.StrawStatueEyeData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Screen for configuring a straw statue's eye regions and pupil direction.
 * Displays the 8×8 face texture as a color grid and allows:
 * - Drawing rectangles to define eye and pupil regions
 * - Dragging to set pupil offset (gaze direction)
 */
public class StrawStatueEyeScreen extends ArmorStandPositionScreen {

    private static final int GRID_CELLS = 8;
    private static final int CELL_SIZE = 16;
    private static final int GRID_PIXELS = GRID_CELLS * CELL_SIZE;

    private enum Mode {
        LEFT_EYE("screen.eye.mode.leftEye", 0xFF55FF55),
        RIGHT_EYE("screen.eye.mode.rightEye", 0xFF55FF55),
        LEFT_PUPIL("screen.eye.mode.leftPupil", 0xFF5555FF),
        RIGHT_PUPIL("screen.eye.mode.rightPupil", 0xFF5555FF),
        ADJUST_LEFT("screen.eye.mode.adjustLeft", 0xFFFFFF55),
        ADJUST_RIGHT("screen.eye.mode.adjustRight", 0xFFFFFF55);

        final String key;
        final int color;

        Mode(String key, int color) { this.key = StrawStatues.MOD_ID + "." + key; this.color = color; }
        Mode next() { Mode[] v = values(); return v[(ordinal() + 1) % v.length]; }
        Mode prev() { Mode[] v = values(); return v[(ordinal() + v.length - 1) % v.length]; }
    }

    private Mode mode = Mode.LEFT_EYE;
    private StrawStatueEyeData eyeData;
    private int[] faceColors;

    private int gridLeft, gridTop;
    private boolean drawing;
    private int drawStartX, drawStartY, drawEndX, drawEndY;

    public StrawStatueEyeScreen(ArmorStandHolder holder, Inventory inventory, Component component, DataSyncHandler dataSyncHandler) {
        super(holder, inventory, component, dataSyncHandler);
        StrawStatue statue = (StrawStatue) holder.getArmorStand();
        this.eyeData = Optional.ofNullable(statue.getEyeData())
                .map(StrawStatueEyeData::copy)
                .orElseGet(StrawStatueEyeData::createDefault);
    }

    @Override
    protected List<ArmorStandWidget> buildWidgets(net.minecraft.world.entity.decoration.ArmorStand armorStand) {
        return Collections.emptyList();
    }

    @Override
    protected void init() {
        super.init();
        this.gridLeft = this.leftPos + (this.imageWidth - GRID_PIXELS) / 2;
        this.gridTop = this.topPos + 18;

        this.addRenderableWidget(
                Button.builder(Component.literal("◀"), btn -> cycleMode(-1))
                        .pos(this.gridLeft - 22, this.gridTop + GRID_PIXELS / 2 - 10)
                        .size(20, 20).build());
        this.addRenderableWidget(
                Button.builder(Component.literal("▶"), btn -> cycleMode(1))
                        .pos(this.gridLeft + GRID_PIXELS + 2, this.gridTop + GRID_PIXELS / 2 - 10)
                        .size(20, 20).build());

        int btnY = this.gridTop + GRID_PIXELS + 6;
        this.addRenderableWidget(
                Button.builder(Component.translatable(StrawStatues.MOD_ID + ".screen.eye.reset"), btn -> resetEyeData())
                        .pos(this.gridLeft, btnY).size(60, 20).build());
        this.addRenderableWidget(
                Button.builder(CommonComponents.GUI_DONE, btn -> saveAndClose())
                        .pos(this.gridLeft + GRID_PIXELS - 60, btnY).size(60, 20).build());

        loadFaceColors();
    }

    private void cycleMode(int dir) {
        this.mode = dir > 0 ? this.mode.next() : this.mode.prev();
        this.drawing = false;
        this.drawStartX = this.drawStartY = this.drawEndX = this.drawEndY = -1;
    }

    private void resetEyeData() {
        this.eyeData = StrawStatueEyeData.createDefault();
        this.drawing = false;
        this.drawStartX = this.drawStartY = this.drawEndX = this.drawEndY = -1;
    }

    private void saveAndClose() {
        this.eyeData.clampToFace();
        ((StrawStatue) this.holder.getArmorStand()).setEyeData(this.eyeData.copy());
        C2SStrawStatueEyeMessage.sendToServer(this.eyeData);
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        this.onClose();
    }

    // ── Face texture loading ────────────────────────────────
    // Skins are dynamic textures (HttpTexture), NOT in ResourceManager.
    // We access the NativeImage via TextureManager + reflection.

    private void loadFaceColors() {
        StrawStatue statue = (StrawStatue) this.holder.getArmorStand();
        Optional<ResourceLocation> skinLoc = StrawStatueRenderer.getPlayerProfileTexture(statue, MinecraftProfileTexture.Type.SKIN);
        if (skinLoc.isPresent()) {
            try {
                com.mojang.blaze3d.platform.NativeImage img =
                        StrawStatueRenderer.readSkinNativeImage(skinLoc.get());
                if (img != null) {
                    this.faceColors = new int[GRID_CELLS * GRID_CELLS];
                    int i = 0;
                    for (int y = 0; y < GRID_CELLS; y++)
                        for (int x = 0; x < GRID_CELLS; x++)
                            this.faceColors[i++] = img.getPixelRGBA(8 + x, 8 + y);
                }
            } catch (Exception ignored) {}
        }
    }

    // ── Coordinate mapping ──────────────────────────────────

    private int cellX(double screenX) {
        double r = screenX - this.gridLeft;
        return r >= 0 && r < GRID_PIXELS ? (int) (r / CELL_SIZE) : -1;
    }

    private int cellY(double screenY) {
        double r = screenY - this.gridTop;
        return r >= 0 && r < GRID_PIXELS ? (int) (r / CELL_SIZE) : -1;
    }

    private void clampDraw() {
        drawStartX = Math.max(0, Math.min(GRID_CELLS - 1, drawStartX));
        drawStartY = Math.max(0, Math.min(GRID_CELLS - 1, drawStartY));
        drawEndX = Math.max(0, Math.min(GRID_CELLS - 1, drawEndX));
        drawEndY = Math.max(0, Math.min(GRID_CELLS - 1, drawEndY));
    }

    // ── Region get/set by mode ──────────────────────────────

    private interface RectAccessor {
        void set(StrawStatueEyeData d, int x1, int y1, int x2, int y2);
        int x1(StrawStatueEyeData d);
        int y1(StrawStatueEyeData d);
        int x2(StrawStatueEyeData d);
        int y2(StrawStatueEyeData d);
    }

    private RectAccessor currentAccessor() {
        return switch (this.mode) {
            case LEFT_EYE -> new RectAccessor() {
                public void set(StrawStatueEyeData d, int x1, int y1, int x2, int y2) {
                    d.leftEyeX1 = x1;
                    d.leftEyeY1 = y1;
                    d.leftEyeX2 = x2;
                    d.leftEyeY2 = y2;
                }

                public int x1(StrawStatueEyeData d) {
                    return d.leftEyeX1;
                }

                public int y1(StrawStatueEyeData d) {
                    return d.leftEyeY1;
                }

                public int x2(StrawStatueEyeData d) {
                    return d.leftEyeX2;
                }

                public int y2(StrawStatueEyeData d) {
                    return d.leftEyeY2;
                }
            };
            case RIGHT_EYE -> new RectAccessor() {
                public void set(StrawStatueEyeData d, int x1, int y1, int x2, int y2) {
                    d.rightEyeX1 = x1;
                    d.rightEyeY1 = y1;
                    d.rightEyeX2 = x2;
                    d.rightEyeY2 = y2;
                }

                public int x1(StrawStatueEyeData d) {
                    return d.rightEyeX1;
                }

                public int y1(StrawStatueEyeData d) {
                    return d.rightEyeY1;
                }

                public int x2(StrawStatueEyeData d) {
                    return d.rightEyeX2;
                }

                public int y2(StrawStatueEyeData d) {
                    return d.rightEyeY2;
                }
            };
            case LEFT_PUPIL -> new RectAccessor() {
                public void set(StrawStatueEyeData d, int x1, int y1, int x2, int y2) {
                    d.leftPupilX1 = x1;
                    d.leftPupilY1 = y1;
                    d.leftPupilX2 = x2;
                    d.leftPupilY2 = y2;
                }

                public int x1(StrawStatueEyeData d) {
                    return d.leftPupilX1;
                }

                public int y1(StrawStatueEyeData d) {
                    return d.leftPupilY1;
                }

                public int x2(StrawStatueEyeData d) {
                    return d.leftPupilX2;
                }

                public int y2(StrawStatueEyeData d) {
                    return d.leftPupilY2;
                }
            };
            case RIGHT_PUPIL -> new RectAccessor() {
                public void set(StrawStatueEyeData d, int x1, int y1, int x2, int y2) {
                    d.rightPupilX1 = x1;
                    d.rightPupilY1 = y1;
                    d.rightPupilX2 = x2;
                    d.rightPupilY2 = y2;
                }

                public int x1(StrawStatueEyeData d) {
                    return d.rightPupilX1;
                }

                public int y1(StrawStatueEyeData d) {
                    return d.rightPupilY1;
                }

                public int x2(StrawStatueEyeData d) {
                    return d.rightPupilX2;
                }

                public int y2(StrawStatueEyeData d) {
                    return d.rightPupilY2;
                }
            };
            default -> throw new IllegalStateException("No region for mode " + mode);
        };
    }

    // ── Render ──────────────────────────────────────────────

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);

        // Grid background
        g.fill(this.gridLeft, this.gridTop, this.gridLeft + GRID_PIXELS, this.gridTop + GRID_PIXELS, 0xFF000000);

        // Face cells
        if (this.faceColors != null) {
            for (int y = 0; y < GRID_CELLS; y++)
                for (int x = 0; x < GRID_CELLS; x++) {
                    g.fill(this.gridLeft + x * CELL_SIZE + 1, this.gridTop + y * CELL_SIZE + 1,
                            this.gridLeft + (x + 1) * CELL_SIZE - 1, this.gridTop + (y + 1) * CELL_SIZE - 1,
                            0xFF000000 | this.faceColors[y * GRID_CELLS + x]);
                }
        }

        // Mouse cell highlight
        int hx = cellX(mouseX), hy = cellY(mouseY);
        if (hx >= 0 && hy >= 0)
            g.fill(this.gridLeft + hx * CELL_SIZE, this.gridTop + hy * CELL_SIZE,
                    this.gridLeft + (hx + 1) * CELL_SIZE, this.gridTop + (hy + 1) * CELL_SIZE, 0x33FFFFFF);

        // All region overlays
        drawRegionBorder(g, eyeData.leftEyeX1, eyeData.leftEyeY1, eyeData.leftEyeX2, eyeData.leftEyeY2, 0xAA55FF55);
        drawRegionBorder(g, eyeData.rightEyeX1, eyeData.rightEyeY1, eyeData.rightEyeX2, eyeData.rightEyeY2, 0xAA55FF55);
        drawRegionBorder(g, eyeData.leftPupilX1, eyeData.leftPupilY1, eyeData.leftPupilX2, eyeData.leftPupilY2, 0xAA5555FF);
        drawRegionBorder(g, eyeData.rightPupilX1, eyeData.rightPupilY1, eyeData.rightPupilX2, eyeData.rightPupilY2, 0xAA5555FF);

        // Pupil offset indicators
        drawOffsetLine(g, eyeData.leftPupilX1, eyeData.leftPupilY1, eyeData.leftPupilX2, eyeData.leftPupilY2,
                eyeData.leftPupilDX, eyeData.leftPupilDY);
        drawOffsetLine(g, eyeData.rightPupilX1, eyeData.rightPupilY1, eyeData.rightPupilX2, eyeData.rightPupilY2,
                eyeData.rightPupilDX, eyeData.rightPupilDY);

        // Current drawing rectangle
        if (this.drawing && this.mode.ordinal() < Mode.ADJUST_LEFT.ordinal()) {
            int x1 = Math.min(drawStartX, drawEndX), y1 = Math.min(drawStartY, drawEndY);
            int x2 = Math.max(drawStartX, drawEndX), y2 = Math.max(drawStartY, drawEndY);
            drawRegionBorder(g, x1, y1, x2, y2, this.mode.color | 0xFF000000);
        }

        // Grid lines
        for (int i = 1; i < GRID_CELLS; i++) {
            g.fill(this.gridLeft + i * CELL_SIZE, this.gridTop, this.gridLeft + i * CELL_SIZE + 1, this.gridTop + GRID_PIXELS, 0x33FFFFFF);
            g.fill(this.gridLeft, this.gridTop + i * CELL_SIZE, this.gridLeft + GRID_PIXELS, this.gridTop + i * CELL_SIZE + 1, 0x33FFFFFF);
        }

        // Mode label
        Component modeText = Component.translatable(this.mode.key);
        int tw = this.font.width(modeText);
        g.drawString(this.font, modeText, this.gridLeft + (GRID_PIXELS - tw) / 2, this.gridTop - 10, 0xFFFFFFFF, false);

        // Status line showing current selection info
        RectAccessor acc = this.mode.ordinal() < Mode.ADJUST_LEFT.ordinal() ? currentAccessor() : null;
        String status;
        if (this.mode == Mode.ADJUST_LEFT) {
            status = "Offset: (" + eyeData.leftPupilDX + ", " + eyeData.leftPupilDY + ")";
        } else if (this.mode == Mode.ADJUST_RIGHT) {
            status = "Offset: (" + eyeData.rightPupilDX + ", " + eyeData.rightPupilDY + ")";
        } else if (acc != null) {
            status = acc.x1(eyeData) + "," + acc.y1(eyeData) + " → " + acc.x2(eyeData) + "," + acc.y2(eyeData);
        } else {
            status = "";
        }
        g.drawString(this.font, status, this.gridLeft, this.gridTop + GRID_PIXELS + 28, 0xFFAAAAAA, false);
    }

    private void drawRegionBorder(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        if (x1 < 0 || y1 < 0 || x2 < 0 || y2 < 0 || x1 > x2 || y1 > y2) return;
        int l = this.gridLeft + x1 * CELL_SIZE, t = this.gridTop + y1 * CELL_SIZE;
        int r = this.gridLeft + (x2 + 1) * CELL_SIZE, b = this.gridTop + (y2 + 1) * CELL_SIZE;
        g.fill(l, t, r, t + 1, color);
        g.fill(l, b - 1, r, b, color);
        g.fill(l, t, l + 1, b, color);
        g.fill(r - 1, t, r, b, color);
    }

    private void drawOffsetLine(GuiGraphics g, int px1, int py1, int px2, int py2, int dx, int dy) {
        if (px1 < 0 || py1 < 0 || px2 < 0 || py2 < 0 || (dx == 0 && dy == 0)) return;
        int ocx = this.gridLeft + (px1 + px2 + 1) * CELL_SIZE / 2;
        int ocy = this.gridTop + (py1 + py2 + 1) * CELL_SIZE / 2;
        int ncx = ocx + dx * CELL_SIZE, ncy = ocy + dy * CELL_SIZE;
        g.fill(Math.min(ocx, ncx), Math.min(ocy, ncy), Math.max(ocx, ncx) + 1, Math.max(ocy, ncy) + 1, 0x88FFFF00);
        int r = 2;
        g.fill(ncx - r, ncy - r, ncx + r + 1, ncy + r + 1, 0xFFFFFF00);
    }

    // ── Mouse input ─────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            int cx = cellX(mx), cy = cellY(my);
            if (cx >= 0 && cy >= 0) {
                this.drawing = true;
                this.drawStartX = this.drawEndX = cx;
                this.drawStartY = this.drawEndY = cy;
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (button == 0 && this.drawing) {
            int cx = cellX(mx), cy = cellY(my);
            if (cx >= 0) this.drawEndX = cx; else this.drawEndX = this.drawStartX;
            if (cy >= 0) this.drawEndY = cy; else this.drawEndY = this.drawStartY;
            clampDraw();

            if (this.mode == Mode.ADJUST_LEFT || this.mode == Mode.ADJUST_RIGHT) {
                RectAccessor acc = this.mode == Mode.ADJUST_LEFT
                        ? new RectAccessor() {
                    public void set(StrawStatueEyeData d, int x1, int y1, int x2, int y2) {}
                    public int x1(StrawStatueEyeData d) { return d.leftPupilX1; } public int y1(StrawStatueEyeData d) { return d.leftPupilY1; }
                    public int x2(StrawStatueEyeData d) { return d.leftPupilX2; } public int y2(StrawStatueEyeData d) { return d.leftPupilY2; }
                }
                        : new RectAccessor() {
                    public void set(StrawStatueEyeData d, int x1, int y1, int x2, int y2) {}
                    public int x1(StrawStatueEyeData d) { return d.rightPupilX1; } public int y1(StrawStatueEyeData d) { return d.rightPupilY1; }
                    public int x2(StrawStatueEyeData d) { return d.rightPupilX2; } public int y2(StrawStatueEyeData d) { return d.rightPupilY2; }
                };
                if (acc.x1(eyeData) >= 0) {
                    int pcx = (acc.x1(eyeData) + acc.x2(eyeData)) / 2;
                    int pcy = (acc.y1(eyeData) + acc.y2(eyeData)) / 2;
                    int dx = drawEndX - pcx, dy = drawEndY - pcy;
                    if (this.mode == Mode.ADJUST_LEFT) { eyeData.leftPupilDX = dx; eyeData.leftPupilDY = dy; }
                    else { eyeData.rightPupilDX = dx; eyeData.rightPupilDY = dy; }
                }
            } else {
                int x1 = Math.min(drawStartX, drawEndX), y1 = Math.min(drawStartY, drawEndY);
                int x2 = Math.max(drawStartX, drawEndX), y2 = Math.max(drawStartY, drawEndY);
                currentAccessor().set(eyeData, x1, y1, x2, y2);
            }
            this.drawing = false;
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (this.drawing) {
            int cx = cellX(mx), cy = cellY(my);
            if (cx >= 0 && cy >= 0) { this.drawEndX = cx; this.drawEndY = cy; }
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    // ── Screen type ─────────────────────────────────────────

    @Override
    public ArmorStandScreenType getScreenType() {
        return ModRegistry.STRAW_STATUE_EYE_SCREEN_TYPE;
    }
}
