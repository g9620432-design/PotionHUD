package dev.z1kzak.potionhud.screen;

import dev.z1kzak.potionhud.config.HudConfig;
import dev.z1kzak.potionhud.render.Draw;
import dev.z1kzak.potionhud.render.HudRenderer;
import dev.z1kzak.potionhud.screen.widget.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Drag &amp; drop position editor: grab the HUD, snap it to the centre / edges, nudge it with
 * the arrow keys. Everything is applied live.
 */
public class PositionEditor {

    /** Implemented by the version-specific screen wrapper. */
    public interface Host {
        void close();
    }

    private static final int SNAP = 6;

    private final Host host;
    private int width;
    private int height;
    private boolean dragging;
    private double grabX;
    private double grabY;
    private int hudW = 80;
    private int hudH = 40;
    private boolean snappedX;
    private boolean snappedY;
    private boolean showGrid = true;

    public PositionEditor(Host host) {
        this.host = host;
    }

    public void layout(int width, int height) {
        this.width = width;
        this.height = height;
    }

    private int hudX() {
        return Math.round(HudConfig.get().posXFrac * width);
    }

    private int hudY() {
        return Math.round(HudConfig.get().posYFrac * height);
    }

    public void renderBackdrop(GuiGraphics g) {
        g.fill(0, 0, width, height, 0x8C0E1116);
    }

    public void render(GuiGraphics g, int mx, int my, float pt) {
        if (showGrid) {
            int cellW = Math.max(24, width / 12);
            int cellH = Math.max(24, height / 8);
            for (int x = cellW; x < width; x += cellW) {
                g.fill(x, 0, x + 1, height, 0x0DFFFFFF);
            }
            for (int y = cellH; y < height; y += cellH) {
                g.fill(0, y, width, y + 1, 0x0DFFFFFF);
            }
            g.fill(width / 2, 0, width / 2 + 1, height, snappedX ? 0xAAFFD166 : 0x33FFFFFF);
            g.fill(0, height / 2, width, height / 2 + 1, snappedY ? 0xAAFFD166 : 0x33FFFFFF);
        }

        HudConfig cfg = HudConfig.get();
        boolean wasFree = cfg.freePosition;
        cfg.freePosition = true;
        int[] size = HudRenderer.renderPreview(g, width, height, true);
        cfg.freePosition = wasFree;
        if (size[0] > 0) {
            hudW = size[0];
            hudH = size[1];
        }

        // selection frame around the HUD
        int bx = hudX();
        int by = hudY();
        Draw.roundedOutline(g, bx - 2, by - 2, hudW + 4, hudH + 4, 4,
                dragging ? 0xCC63B3FF : 0x8863B3FF, 1);
        int handle = 3;
        int corner = 0xFF63B3FF;
        g.fill(bx - 2, by - 2, bx - 2 + handle, by - 2 + handle, corner);
        g.fill(bx + hudW + 2 - handle, by - 2, bx + hudW + 2, by - 2 + handle, corner);
        g.fill(bx - 2, by + hudH + 2 - handle, bx - 2 + handle, by + hudH + 2, corner);
        g.fill(bx + hudW + 2 - handle, by + hudH + 2 - handle, bx + hudW + 2, by + hudH + 2, corner);

        // coordinate readout that follows the box
        String coords = Math.round(cfg.posXFrac * 100) + "% / " + Math.round(cfg.posYFrac * 100) + "%";
        int lx = Math.min(width - Theme.font().width(coords) - 6, bx);
        Draw.rounded(g, lx - 3, by - 15, Theme.font().width(coords) + 6, 12, 3, 0xAA000000);
        Theme.text(g, coords, lx, by - 13, Theme.TEXT);

        // hint card
        String hint = Component.translatable("potionhudx.position.hint").getString();
        int cardW = Math.min(width - 20, Theme.font().width(hint) + 24);
        int cardX = (width - cardW) / 2;
        int cardY = height - 34;
        Draw.rounded(g, cardX, cardY, cardW, 22, 6, 0xCC15181E);
        Draw.roundedOutline(g, cardX, cardY, cardW, 22, 6, Theme.BORDER, 1);
        Theme.textCenter(g, Theme.clip(hint, cardW - 12), width / 2, cardY + 7, Theme.TEXT_DIM);
    }

    public boolean click(double mx, double my, int button) {
        if (button == 0) {
            dragging = true;
            grabX = mx - hudX();
            grabY = my - hudY();
            // clicking outside the box re-centres the grab point on the box
            if (Math.abs(grabX) > hudW + 20 || Math.abs(grabY) > hudH + 20) {
                grabX = hudW / 2.0;
                grabY = hudH / 2.0;
                move(mx, my);
            }
            return true;
        }
        if (button == 1) {
            showGrid = !showGrid;
            return true;
        }
        return false;
    }

    public boolean drag(double mx, double my) {
        if (dragging) {
            move(mx, my);
            return true;
        }
        return false;
    }

    public void release() {
        if (dragging) {
            dragging = false;
            HudConfig.save();
        }
    }

    private void move(double mx, double my) {
        HudConfig cfg = HudConfig.get();
        float nx = (float) (mx - grabX);
        float ny = (float) (my - grabY);

        snappedX = false;
        snappedY = false;

        // snap: screen centre
        float centerX = width / 2f - hudW / 2f;
        float centerY = height / 2f - hudH / 2f;
        if (Math.abs(nx - centerX) <= SNAP) {
            nx = centerX;
            snappedX = true;
        }
        if (Math.abs(ny - centerY) <= SNAP) {
            ny = centerY;
            snappedY = true;
        }
        // snap: edges with a small margin
        int margin = 4;
        if (Math.abs(nx - margin) <= SNAP) {
            nx = margin;
            snappedX = true;
        }
        if (Math.abs(nx - (width - hudW - margin)) <= SNAP) {
            nx = width - hudW - margin;
            snappedX = true;
        }
        if (Math.abs(ny - margin) <= SNAP) {
            ny = margin;
            snappedY = true;
        }
        if (Math.abs(ny - (height - hudH - margin)) <= SNAP) {
            ny = height - hudH - margin;
            snappedY = true;
        }

        cfg.posXFrac = Math.max(-0.05f, Math.min(1.05f, nx / width));
        cfg.posYFrac = Math.max(-0.05f, Math.min(1.05f, ny / height));
    }

    public boolean key(int key, int modifiers) {
        HudConfig cfg = HudConfig.get();
        int step = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0 ? 10 : 1;
        switch (key) {
            case GLFW.GLFW_KEY_LEFT -> {
                cfg.posXFrac -= step / (float) width;
                return true;
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                cfg.posXFrac += step / (float) width;
                return true;
            }
            case GLFW.GLFW_KEY_UP -> {
                cfg.posYFrac -= step / (float) height;
                return true;
            }
            case GLFW.GLFW_KEY_DOWN -> {
                cfg.posYFrac += step / (float) height;
                return true;
            }
            case GLFW.GLFW_KEY_C -> {
                cfg.posXFrac = (width / 2f - hudW / 2f) / width;
                cfg.posYFrac = (height / 2f - hudH / 2f) / height;
                HudConfig.save();
                return true;
            }
            case GLFW.GLFW_KEY_G -> {
                showGrid = !showGrid;
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    /** Called by the wrapper when the screen goes away. */
    public void closed() {
        HudConfig.get().validate();
        HudConfig.save();
    }
}
