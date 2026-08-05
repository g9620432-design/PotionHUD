package dev.z1kzak.potionhud.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;

/**
 * Draws the captured frame back inside the glass panel, row by row, with the UVs bent so the
 * scene behind the HUD is genuinely visible and genuinely distorted:
 *
 * <ul>
 *   <li>a lens curve pushes rows sideways, strongest near the top and bottom edges;</li>
 *   <li>a slow travelling sine wave adds the "liquid" wobble;</li>
 *   <li>the sampled region is slightly narrower than the panel, which magnifies the content
 *       the way real glass thickness does;</li>
 *   <li>extra passes with sub-pixel offsets approximate a frosted blur.</li>
 * </ul>
 *
 * The row loop also follows the rounded-corner arc, so the refraction has the same silhouette
 * as the panel itself.
 */
public final class GlassRefraction {

    private GlassRefraction() {
    }

    public static boolean draw(HudContext ctx, int x, int y, int w, int h, int radius) {
        if (!ctx.cfg.glassSeeThrough || !GlassCapture.available() || !GlassCapture.capture()) {
            return false;
        }
        int texW = GlassCapture.width();
        int texH = GlassCapture.height();
        if (texW <= 0 || texH <= 0 || w <= 2 || h <= 2) {
            return false;
        }

        GuiGraphics g = ctx.g;
        float sf = Math.max(1, Minecraft.getInstance().getWindow().getGuiScale());
        int r = Math.max(0, Math.min(radius, Math.min(w / 2, h / 2)));

        float edge = ctx.cfg.glassRefraction;
        float dist = ctx.cfg.glassDistortion;
        float blur = ctx.cfg.glassBlur;
        float time = ctx.anim() ? ctx.time * ctx.cfg.animSpeed : 0f;

        int passes = 1 + Math.round(blur * 2f);
        // vertical lens squeeze: sample a slightly shorter slab than we cover
        float squeeze = 1f - 0.14f * edge;

        for (int row = 0; row < h; row++) {
            float inset = Draw.cornerInset(row, h, r);
            int solid = (int) Math.ceil(inset);
            int left = x + solid;
            int right = x + w - solid;
            if (right - left < 1) {
                continue;
            }

            float t = h <= 1 ? 0f : (row - (h - 1) / 2f) / ((h - 1) / 2f); // -1 .. 1
            // lens: rows near the edges bend inwards; keeps the middle honest
            float lens = -t * Math.abs(t) * 4.5f * edge;
            float wave = (float) Math.sin(time * 1.6f + row * 0.15f) * 2.2f * dist
                    + (float) Math.sin(time * 0.7f + row * 0.05f) * 1.1f * dist;
            float shiftX = lens + wave;

            // source rectangle in framebuffer texels
            float srcYf = (y + (h - 1) / 2f + (row - (h - 1) / 2f) * squeeze) * sf;
            float srcH = Math.max(1f, sf);
            float magnify = 1f - 0.10f * edge;
            float coverW = right - left;
            float srcW = Math.max(1f, coverW * sf * magnify);
            float srcXf = (left + shiftX + coverW * (1f - magnify) * 0.5f) * sf;

            srcXf = Math.max(0f, Math.min(texW - srcW, srcXf));
            srcYf = Math.max(0f, Math.min(texH - srcH, srcYf));

            for (int pass = 0; pass < passes; pass++) {
                int tint;
                float offset;
                if (pass == 0) {
                    tint = 0xFFFFFFFF;
                    offset = 0f;
                } else {
                    float a = 0.45f * blur / pass;
                    tint = (Math.round(Math.min(1f, a) * 255f) << 24) | 0xFFFFFF;
                    offset = (pass % 2 == 0 ? 1f : -1f) * pass * (0.9f + 1.4f * blur) * sf;
                }
                float u = Math.max(0f, Math.min(texW - srcW, srcXf + offset));
                g.blit(RenderPipelines.GUI_TEXTURED, GlassCapture.TEXTURE_ID,
                        left, y + row, u, srcYf,
                        right - left, 1,
                        Math.round(srcW), Math.round(srcH),
                        texW, texH, tint);
            }
        }
        return true;
    }
}
