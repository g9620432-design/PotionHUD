package dev.z1kzak.potionhud.render;

import dev.z1kzak.potionhud.config.Colors;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Small drawing toolbox shared by every HUD style: anti-aliased rounded rectangles,
 * vertical gradients, outlines, soft shadows / glows and scaled text.
 *
 * Everything is built out of plain {@code fill} calls so it works on any pipeline
 * without custom shaders.
 */
public final class Draw {

    private Draw() {
    }

    // ── basics ───────────────────────────────────────────────────────────────
    public static void rect(GuiGraphics g, int x, int y, int w, int h, int argb) {
        if (w <= 0 || h <= 0 || (argb >>> 24) == 0) {
            return;
        }
        g.fill(x, y, x + w, y + h, argb);
    }

    /**
     * Rounded rectangle with a vertical gradient and anti-aliased corners.
     * Pass the same colour twice for a flat panel.
     */
    public static void roundedGradient(GuiGraphics g, int x, int y, int w, int h, int radius,
                                       int topArgb, int bottomArgb) {
        if (w <= 0 || h <= 0) {
            return;
        }
        int r = Math.max(0, Math.min(radius, Math.min(w / 2, h / 2)));
        boolean flat = topArgb == bottomArgb;
        if (r == 0 && flat) {
            rect(g, x, y, w, h, topArgb);
            return;
        }
        for (int row = 0; row < h; row++) {
            float t = h <= 1 ? 0f : (float) row / (h - 1);
            int color = flat ? topArgb : Colors.lerp(topArgb, bottomArgb, t);
            if ((color >>> 24) == 0) {
                continue;
            }
            float inset = cornerInset(row, h, r);
            int solid = (int) Math.ceil(inset);
            int left = x + solid;
            int right = x + w - solid;
            if (right > left) {
                g.fill(left, y + row, right, y + row + 1, color);
            }
            // fractional edge pixels give the corners their smooth silhouette
            float frac = solid - inset;
            if (solid > 0 && frac > 0.02f) {
                int edge = Colors.multAlpha(color, frac);
                g.fill(left - 1, y + row, left, y + row + 1, edge);
                g.fill(right, y + row, right + 1, y + row + 1, edge);
            }
        }
    }

    public static void rounded(GuiGraphics g, int x, int y, int w, int h, int radius, int argb) {
        roundedGradient(g, x, y, w, h, radius, argb, argb);
    }

    /** Horizontal distance the given row must be pushed in to follow the corner arc. */
    public static float cornerInset(int row, int h, int r) {
        if (r <= 0) {
            return 0f;
        }
        float dy;
        if (row < r) {
            dy = r - row - 0.5f;
        } else if (row >= h - r) {
            dy = row - (h - r) + 0.5f;
        } else {
            return 0f;
        }
        float inner = r * r - dy * dy;
        if (inner <= 0) {
            return r;
        }
        return r - (float) Math.sqrt(inner);
    }

    /** Rounded outline drawn just inside the given box. */
    public static void roundedOutline(GuiGraphics g, int x, int y, int w, int h, int radius,
                                      int argb, int thickness) {
        if (w <= 0 || h <= 0 || thickness <= 0 || (argb >>> 24) == 0) {
            return;
        }
        int r = Math.max(0, Math.min(radius, Math.min(w / 2, h / 2)));
        for (int row = 0; row < h; row++) {
            float inset = cornerInset(row, h, r);
            int solid = (int) Math.ceil(inset);
            int left = x + solid;
            int right = x + w - solid;
            boolean cap = row < thickness || row >= h - thickness || row < r || row >= h - r;
            if (row < thickness || row >= h - thickness) {
                if (right > left) {
                    g.fill(left, y + row, right, y + row + 1, argb);
                }
            } else {
                int t = Math.min(thickness, Math.max(1, (right - left) / 2));
                g.fill(left, y + row, left + t, y + row + 1, argb);
                g.fill(right - t, y + row, right, y + row + 1, argb);
            }
            if (cap && solid > 0) {
                float frac = solid - inset;
                if (frac > 0.02f) {
                    int edge = Colors.multAlpha(argb, frac);
                    g.fill(left - 1, y + row, left, y + row + 1, edge);
                    g.fill(right, y + row, right + 1, y + row + 1, edge);
                }
            }
        }
    }

    /** Soft shadow underneath a panel: a few expanding rounded rects with falling alpha. */
    public static void softShadow(GuiGraphics g, int x, int y, int w, int h, int radius,
                                  float strength, int offsetY) {
        if (strength <= 0.001f) {
            return;
        }
        int layers = 4;
        for (int i = layers; i >= 1; i--) {
            float a = strength * 0.16f * (1f - (i - 1) / (float) layers);
            int argb = Colors.withAlpha(0x000000, a);
            rounded(g, x - i, y - i + offsetY, w + i * 2, h + i * 2, radius + i, argb);
        }
    }

    /** Outer glow in a given colour, used by the Neon style. */
    public static void glow(GuiGraphics g, int x, int y, int w, int h, int radius,
                            int rgb, float strength, int layers) {
        if (strength <= 0.001f) {
            return;
        }
        for (int i = layers; i >= 1; i--) {
            float a = strength * 0.22f * (1f - (i - 1) / (float) (layers + 1));
            rounded(g, x - i, y - i, w + i * 2, h + i * 2, radius + i, Colors.withAlpha(rgb, a));
        }
    }

    /**
     * Cheap frosted-glass backdrop: several translucent layers with slight colour drift,
     * which reads as a blur behind small panels without touching the render pipeline.
     */
    public static void frostedBackdrop(GuiGraphics g, int x, int y, int w, int h, int radius,
                                       int baseArgb, float blur) {
        if (blur <= 0.001f) {
            return;
        }
        int steps = 5;
        for (int i = 0; i < steps; i++) {
            float k = i / (float) (steps - 1);
            int shade = Colors.lerp(Colors.darken(baseArgb, 0.35f), Colors.brighten(baseArgb, 0.18f), k);
            int argb = Colors.multAlpha(shade, blur * 0.30f);
            int inset = i;
            rounded(g, x + inset, y + inset, w - inset * 2, h - inset * 2, Math.max(0, radius - inset), argb);
        }
    }

    /** Top-to-middle specular highlight that makes a panel look like polished glass. */
    public static void specularHighlight(GuiGraphics g, int x, int y, int w, int h, int radius,
                                         float strength) {
        if (strength <= 0.001f) {
            return;
        }
        int hh = Math.max(2, h / 2);
        int top = Colors.withAlpha(0xFFFFFF, 0.30f * strength);
        int bottom = Colors.withAlpha(0xFFFFFF, 0f);
        roundedGradient(g, x + 1, y + 1, w - 2, hh, Math.max(0, radius - 1), top, bottom);
    }

    /**
     * Moving diagonal sheen. {@code phase} is 0..1 and walks the highlight across the panel.
     */
    public static void sheen(GuiGraphics g, int x, int y, int w, int h, float phase, float strength) {
        if (strength <= 0.001f) {
            return;
        }
        int band = Math.max(6, w / 5);
        int cx = (int) (-band + phase * (w + band * 2));
        // the band is sheared in a handful of horizontal chunks — diagonal enough, cheap to draw
        int chunks = Math.max(1, Math.min(6, h / 3));
        int chunkH = Math.max(1, h / chunks);
        g.enableScissor(x, y, x + w, y + h);
        for (int i = 0; i < band; i++) {
            float t = i / (float) band;
            float a = (float) Math.sin(Math.PI * t) * 0.16f * strength;
            if (a <= 0.002f) {
                continue;
            }
            int argb = Colors.withAlpha(0xFFFFFF, a);
            int px = x + cx + i;
            for (int c = 0; c < chunks; c++) {
                int ry = y + c * chunkH;
                int rh = c == chunks - 1 ? h - c * chunkH : chunkH;
                int shear = Math.round((chunks - 1 - c) * chunkH * 0.35f);
                g.fill(px + shear, ry, px + shear + 1, ry + rh, argb);
            }
        }
        g.disableScissor();
    }

    /** Horizontal progress bar with rounded caps. */
    public static void progressBar(GuiGraphics g, int x, int y, int w, int h, float progress,
                                   int trackArgb, int fillArgb, boolean roundedCaps) {
        progress = Math.max(0f, Math.min(1f, progress));
        int r = roundedCaps ? Math.min(h / 2, 4) : 0;
        rounded(g, x, y, w, h, r, trackArgb);
        int fw = Math.round(w * progress);
        if (fw > 0) {
            rounded(g, x, y, Math.max(1, fw), h, r, fillArgb);
        }
    }

    // ── text ─────────────────────────────────────────────────────────────────
    public static int textWidth(Font font, String s, float scale) {
        return (int) Math.ceil(font.width(s) * scale);
    }

    public static int lineHeight(Font font, float scale) {
        return (int) Math.ceil(font.lineHeight * scale);
    }

    public static void text(GuiGraphics g, Font font, String s, int x, int y, int argb,
                            boolean shadow, float scale) {
        if (s == null || s.isEmpty() || (argb >>> 24) == 0) {
            return;
        }
        if (Math.abs(scale - 1.0f) < 0.001f) {
            g.drawString(font, s, x, y, argb, shadow);
            return;
        }
        g.pose().pushMatrix();
        g.pose().translate((float) x, (float) y);
        g.pose().scale(scale, scale);
        g.drawString(font, s, 0, 0, argb, shadow);
        g.pose().popMatrix();
    }

    /** Text with a coloured halo, used by Neon. */
    public static void glowText(GuiGraphics g, Font font, String s, int x, int y, int argb,
                                int glowRgb, float strength, float scale) {
        if (strength > 0.001f) {
            int halo = Colors.withAlpha(glowRgb, 0.30f * strength);
            text(g, font, s, x - 1, y, halo, false, scale);
            text(g, font, s, x + 1, y, halo, false, scale);
            text(g, font, s, x, y - 1, halo, false, scale);
            text(g, font, s, x, y + 1, halo, false, scale);
        }
        text(g, font, s, x, y, argb, false, scale);
    }

    /**
     * Draws text clipped to {@code maxW}; if it does not fit it scrolls (marquee).
     * {@code offset} is a monotonically growing pixel offset.
     */
    public static void scrollingText(GuiGraphics g, Font font, String s, int x, int y, int maxW,
                                     int argb, boolean shadow, float scale, float offset,
                                     boolean enabled) {
        int w = textWidth(font, s, scale);
        if (w <= maxW || !enabled) {
            if (w <= maxW) {
                text(g, font, s, x, y, argb, shadow, scale);
            } else {
                g.enableScissor(x, y - 1, x + maxW, y + lineHeight(font, scale) + 1);
                text(g, font, s, x, y, argb, shadow, scale);
                g.disableScissor();
            }
            return;
        }
        int gap = (int) (14 * scale);
        int cycle = w + gap;
        int off = (int) (offset % cycle);
        g.enableScissor(x, y - 1, x + maxW, y + lineHeight(font, scale) + 1);
        text(g, font, s, x - off, y, argb, shadow, scale);
        text(g, font, s, x - off + cycle, y, argb, shadow, scale);
        g.disableScissor();
    }
}
