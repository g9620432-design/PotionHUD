package dev.z1kzak.potionhud.screen;

import dev.z1kzak.potionhud.config.Colors;
import dev.z1kzak.potionhud.render.Draw;
import dev.z1kzak.potionhud.screen.widget.ColorOption;
import dev.z1kzak.potionhud.screen.widget.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Floating HSV colour picker: saturation/value field, hue strip, hex input and a row of
 * handy presets. Drawn as an overlay on top of the settings window.
 */
public class ColorPicker {

    private static final int[] PRESETS = {
            0xFFFFFF, 0xB9BCC8, 0x7A7F8C, 0x000000,
            0x63B3FF, 0x38F2FF, 0x62D28E, 0xFFD166,
            0xFF8A5B, 0xE86A6A, 0xC792EA, 0x9FD8FF
    };

    private final ColorOption target;
    private final int startRgb;
    private final Runnable onChange;

    private int x;
    private int y;
    private final int w = 176;
    private final int h = 168;

    private float hue;
    private float sat;
    private float val;
    private String hexInput;
    private boolean hexFocused;
    private int dragMode; // 0 none, 1 sv, 2 hue

    public ColorPicker(ColorOption target, Runnable onChange) {
        this.target = target;
        this.onChange = onChange;
        this.startRgb = target.rgb();
        float[] hsv = rgbToHsv(startRgb);
        this.hue = hsv[0];
        this.sat = hsv[1];
        this.val = hsv[2];
        this.hexInput = Colors.toHex(startRgb);
    }

    public void position(int screenW, int screenH, int preferredX, int preferredY) {
        this.x = Math.max(6, Math.min(screenW - w - 6, preferredX));
        this.y = Math.max(6, Math.min(screenH - h - 6, preferredY));
    }

    public ColorOption target() {
        return target;
    }

    // ── geometry ─────────────────────────────────────────────────────────────
    private int svX() {
        return x + 10;
    }

    private int svY() {
        return y + 24;
    }

    private int svW() {
        return 122;
    }

    private int svH() {
        return 86;
    }

    private int hueX() {
        return x + w - 24;
    }

    private int hueY() {
        return svY();
    }

    private int hueW() {
        return 14;
    }

    private int hexY() {
        return svY() + svH() + 8;
    }

    private int presetY() {
        return hexY() + 20;
    }

    private int buttonY() {
        return y + h - 22;
    }

    // ── painting ─────────────────────────────────────────────────────────────
    public void render(GuiGraphics g, int mx, int my) {
        Draw.softShadow(g, x, y, w, h, 7, 1.0f, 3);
        Draw.roundedGradient(g, x, y, w, h, 7, 0xF71A1E26, 0xF7222834);
        Draw.roundedOutline(g, x, y, w, h, 7, 0x44FFFFFF, 1);

        Theme.text(g, Component.translatable("potionhudx.picker.title").getString(),
                x + 10, y + 9, Theme.TEXT);
        String name = target.label == null ? "" : target.label.getString();
        Theme.textRight(g, Theme.clip(name, 90), x + w - 10, y + 9, Theme.TEXT_FAINT);

        // saturation / value field, drawn in 2px blocks
        int step = 2;
        for (int py = 0; py < svH(); py += step) {
            float v = 1f - py / (float) svH();
            for (int px = 0; px < svW(); px += step) {
                float s = px / (float) svW();
                int rgb = hsvToRgb(hue, s, v);
                g.fill(svX() + px, svY() + py,
                        Math.min(svX() + svW(), svX() + px + step),
                        Math.min(svY() + svH(), svY() + py + step),
                        0xFF000000 | rgb);
            }
        }
        Draw.roundedOutline(g, svX() - 1, svY() - 1, svW() + 2, svH() + 2, 2, 0x44FFFFFF, 1);

        // cursor
        int cxp = svX() + Math.round(sat * svW());
        int cyp = svY() + Math.round((1f - val) * svH());
        int ring = val > 0.6f && sat < 0.5f ? 0xFF101318 : 0xFFFFFFFF;
        g.fill(cxp - 4, cyp, cxp - 1, cyp + 1, ring);
        g.fill(cxp + 2, cyp, cxp + 5, cyp + 1, ring);
        g.fill(cxp, cyp - 4, cxp + 1, cyp - 1, ring);
        g.fill(cxp, cyp + 2, cxp + 1, cyp + 5, ring);

        // hue strip
        for (int py = 0; py < svH(); py++) {
            int rgb = hsvToRgb(py / (float) svH(), 1f, 1f);
            g.fill(hueX(), hueY() + py, hueX() + hueW(), hueY() + py + 1, 0xFF000000 | rgb);
        }
        Draw.roundedOutline(g, hueX() - 1, hueY() - 1, hueW() + 2, svH() + 2, 2, 0x44FFFFFF, 1);
        int hy = hueY() + Math.round(hue * svH());
        g.fill(hueX() - 2, hy - 1, hueX() + hueW() + 2, hy + 1, 0xFFFFFFFF);

        // hex row: swatch + input
        int current = currentRgb();
        Draw.rounded(g, x + 10, hexY(), 22, 14, 3, 0xFF000000 | current);
        Draw.roundedOutline(g, x + 10, hexY(), 22, 14, 3, 0x55FFFFFF, 1);
        int fieldX = x + 36;
        int fieldW = w - 46;
        Draw.rounded(g, fieldX, hexY(), fieldW, 14, 3, 0x66000000);
        Draw.roundedOutline(g, fieldX, hexY(), fieldW, 14, 3, hexFocused ? Theme.ACCENT : 0x22FFFFFF, 1);
        Theme.text(g, hexInput, fieldX + 5, hexY() + 3, Theme.TEXT);
        if (hexFocused && (System.currentTimeMillis() / 500) % 2 == 0) {
            int cx2 = fieldX + 5 + Theme.font().width(hexInput);
            g.fill(cx2, hexY() + 2, cx2 + 1, hexY() + 12, Theme.TEXT);
        }

        // presets
        int sw = 12;
        int gap = 2;
        for (int i = 0; i < PRESETS.length; i++) {
            int px = x + 10 + i * (sw + gap);
            Draw.rounded(g, px, presetY(), sw, 12, 2, 0xFF000000 | PRESETS[i]);
            boolean over = mx >= px && mx < px + sw && my >= presetY() && my < presetY() + 12;
            Draw.roundedOutline(g, px, presetY(), sw, 12, 2, over ? 0xFFFFFFFF : 0x33FFFFFF, 1);
        }

        // buttons
        int bw = (w - 26) / 2;
        drawButton(g, x + 10, buttonY(), bw, mx, my,
                Component.translatable("potionhudx.picker.apply").getString(), Theme.ACCENT);
        drawButton(g, x + w - 10 - bw, buttonY(), bw, mx, my,
                Component.translatable("potionhudx.picker.cancel").getString(), 0x66000000);
    }

    private void drawButton(GuiGraphics g, int bx, int by, int bw, int mx, int my, String text, int tint) {
        boolean over = mx >= bx && mx < bx + bw && my >= by && my < by + 16;
        int fill = over ? tint : Colors.multAlpha(tint, 0.55f);
        Draw.rounded(g, bx, by, bw, 16, 4, fill);
        Draw.roundedOutline(g, bx, by, bw, 16, 4, over ? 0x66FFFFFF : 0x22FFFFFF, 1);
        Theme.textCenter(g, text, bx + bw / 2, by + 4, over ? 0xFF0E1116 : Theme.TEXT);
    }

    // ── interaction ──────────────────────────────────────────────────────────
    /** @return true when the picker consumed the click, "close" is reported via {@link #closed}. */
    public boolean click(double mx, double my, int button) {
        if (button != 0) {
            return contains(mx, my);
        }
        if (inside(mx, my, svX(), svY(), svW(), svH())) {
            dragMode = 1;
            applySv(mx, my);
            return true;
        }
        if (inside(mx, my, hueX(), hueY(), hueW(), svH())) {
            dragMode = 2;
            applyHue(my);
            return true;
        }
        int fieldX = x + 36;
        int fieldW = w - 46;
        hexFocused = inside(mx, my, fieldX, hexY(), fieldW, 14);
        if (hexFocused) {
            return true;
        }
        int sw = 12;
        int gap = 2;
        for (int i = 0; i < PRESETS.length; i++) {
            int px = x + 10 + i * (sw + gap);
            if (inside(mx, my, px, presetY(), sw, 12)) {
                setRgb(PRESETS[i]);
                return true;
            }
        }
        int bw = (w - 26) / 2;
        if (inside(mx, my, x + 10, buttonY(), bw, 16)) {
            apply();
            closed = true;
            return true;
        }
        if (inside(mx, my, x + w - 10 - bw, buttonY(), bw, 16)) {
            target.setRgb(startRgb);
            fireChange();
            closed = true;
            return true;
        }
        if (!contains(mx, my)) {
            apply();
            closed = true;
        }
        return true;
    }

    public boolean closed;

    public boolean drag(double mx, double my) {
        if (dragMode == 1) {
            applySv(mx, my);
            return true;
        }
        if (dragMode == 2) {
            applyHue(my);
            return true;
        }
        return false;
    }

    public void release() {
        dragMode = 0;
    }

    public boolean charTyped(char c) {
        if (!hexFocused) {
            return false;
        }
        if (hexInput.length() >= 7) {
            return true;
        }
        if (Character.isLetterOrDigit(c) || c == '#') {
            hexInput += Character.toUpperCase(c);
            tryParseHex();
        }
        return true;
    }

    public boolean keyPressed(int key) {
        if (hexFocused) {
            if (key == GLFW.GLFW_KEY_BACKSPACE) {
                if (!hexInput.isEmpty()) {
                    hexInput = hexInput.substring(0, hexInput.length() - 1);
                    tryParseHex();
                }
                return true;
            }
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                hexFocused = false;
                tryParseHex();
                return true;
            }
        }
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            target.setRgb(startRgb);
            fireChange();
            closed = true;
            return true;
        }
        return false;
    }

    public boolean contains(double mx, double my) {
        return inside(mx, my, x, y, w, h);
    }

    private boolean inside(double mx, double my, int bx, int by, int bw, int bh) {
        return mx >= bx && mx < bx + bw && my >= by && my < by + bh;
    }

    private void applySv(double mx, double my) {
        sat = (float) Math.max(0, Math.min(1, (mx - svX()) / svW()));
        val = 1f - (float) Math.max(0, Math.min(1, (my - svY()) / svH()));
        pushLive();
    }

    private void applyHue(double my) {
        hue = (float) Math.max(0, Math.min(1, (my - hueY()) / svH()));
        pushLive();
    }

    private void setRgb(int rgb) {
        float[] hsv = rgbToHsv(rgb);
        hue = hsv[0];
        sat = hsv[1];
        val = hsv[2];
        pushLive();
    }

    private void tryParseHex() {
        int parsed = Colors.parse(hexInput, -1);
        if (parsed >= 0) {
            float[] hsv = rgbToHsv(parsed);
            hue = hsv[0];
            sat = hsv[1];
            val = hsv[2];
            target.setRgb(parsed);
            fireChange();
        }
    }

    private int currentRgb() {
        return hsvToRgb(hue, sat, val);
    }

    /** Live preview: the HUD updates while the user drags. */
    private void pushLive() {
        int rgb = currentRgb();
        hexInput = Colors.toHex(rgb);
        target.setRgb(rgb);
        fireChange();
    }

    private void apply() {
        target.setRgb(currentRgb());
        fireChange();
    }

    private void fireChange() {
        if (onChange != null) {
            onChange.run();
        }
    }

    // ── colour maths ─────────────────────────────────────────────────────────
    public static int hsvToRgb(float h, float s, float v) {
        h = (h % 1f + 1f) % 1f;
        int i = (int) (h * 6);
        float f = h * 6 - i;
        float p = v * (1 - s);
        float q = v * (1 - f * s);
        float t = v * (1 - (1 - f) * s);
        float r;
        float g;
        float b;
        switch (i % 6) {
            case 0 -> {
                r = v;
                g = t;
                b = p;
            }
            case 1 -> {
                r = q;
                g = v;
                b = p;
            }
            case 2 -> {
                r = p;
                g = v;
                b = t;
            }
            case 3 -> {
                r = p;
                g = q;
                b = v;
            }
            case 4 -> {
                r = t;
                g = p;
                b = v;
            }
            default -> {
                r = v;
                g = p;
                b = q;
            }
        }
        int ri = Math.round(r * 255);
        int gi = Math.round(g * 255);
        int bi = Math.round(b * 255);
        return (ri << 16) | (gi << 8) | bi;
    }

    public static float[] rgbToHsv(int rgb) {
        float r = ((rgb >> 16) & 0xFF) / 255f;
        float g = ((rgb >> 8) & 0xFF) / 255f;
        float b = (rgb & 0xFF) / 255f;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float d = max - min;
        float h = 0f;
        if (d > 0.0001f) {
            if (max == r) {
                h = ((g - b) / d) % 6;
            } else if (max == g) {
                h = (b - r) / d + 2;
            } else {
                h = (r - g) / d + 4;
            }
            h /= 6f;
            if (h < 0) {
                h += 1f;
            }
        }
        float s = max <= 0.0001f ? 0f : d / max;
        return new float[]{h, s, max};
    }
}
