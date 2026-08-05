package dev.z1kzak.potionhud.render;

import dev.z1kzak.potionhud.compat.Platform;

import java.util.ArrayList;
import java.util.List;

/**
 * Version independent maths of the see-through glass: it slices the panel into one-pixel rows and
 * works out, for every row, which part of the captured frame should be shown there.
 *
 * <ul>
 *   <li>a lens curve pushes rows sideways, strongest near the top and bottom edges;</li>
 *   <li>two travelling sine waves add the liquid wobble;</li>
 *   <li>the sampled strip is slightly narrower and shorter than the panel, which magnifies the
 *       content the way real glass thickness does;</li>
 *   <li>the row also follows the rounded-corner arc, so the refraction shares the panel silhouette.</li>
 * </ul>
 *
 * The version module only has to blit these slices — that part is a couple of lines.
 */
public final class GlassGeometry {

    /** One row of the refraction: draw the {@code src*} region of the capture into {@code dst*}. */
    public record Slice(int dstX, int dstY, int dstW, float srcX, float srcY, int srcW, int srcH) {
    }

    private GlassGeometry() {
    }

    /** How many extra offset passes approximate the frost blur. */
    public static int blurPasses(HudContext ctx) {
        return 1 + Math.round(ctx.cfg.glassBlur * 2f);
    }

    /** Alpha of blur pass {@code pass} (0 = the base pass, fully opaque). */
    public static float passAlpha(HudContext ctx, int pass) {
        return pass == 0 ? 1f : Math.min(1f, 0.45f * ctx.cfg.glassBlur / pass);
    }

    /** Horizontal offset of blur pass {@code pass}, in framebuffer texels. */
    public static float passOffset(HudContext ctx, int pass, float guiScale) {
        if (pass == 0) {
            return 0f;
        }
        float dir = pass % 2 == 0 ? 1f : -1f;
        return dir * pass * (0.9f + 1.4f * ctx.cfg.glassBlur) * guiScale;
    }

    public static List<Slice> slices(HudContext ctx, int x, int y, int w, int h, int radius,
                                     int texW, int texH) {
        List<Slice> out = new ArrayList<>(Math.max(0, h));
        if (texW <= 0 || texH <= 0 || w <= 2 || h <= 2) {
            return out;
        }
        float sf = Math.max(1f, Platform.get().guiScale());
        int r = Math.max(0, Math.min(radius, Math.min(w / 2, h / 2)));

        float edge = ctx.cfg.glassRefraction;
        float dist = ctx.cfg.glassDistortion;
        float time = ctx.anim() ? ctx.time * ctx.cfg.animSpeed : 0f;
        float squeeze = 1f - 0.14f * edge;
        float magnify = 1f - 0.10f * edge;

        for (int row = 0; row < h; row++) {
            float inset = Draw.cornerInset(row, h, r);
            int solid = (int) Math.ceil(inset);
            int left = x + solid;
            int right = x + w - solid;
            int coverW = right - left;
            if (coverW < 1) {
                continue;
            }

            float t = h <= 1 ? 0f : (row - (h - 1) / 2f) / ((h - 1) / 2f);
            float lens = -t * Math.abs(t) * 4.5f * edge;
            float wave = (float) Math.sin(time * 1.6f + row * 0.15f) * 2.2f * dist
                    + (float) Math.sin(time * 0.7f + row * 0.05f) * 1.1f * dist;

            float srcH = Math.max(1f, sf);
            float srcW = Math.max(1f, coverW * sf * magnify);
            float srcX = (left + lens + wave + coverW * (1f - magnify) * 0.5f) * sf;
            float srcY = (y + (h - 1) / 2f + (row - (h - 1) / 2f) * squeeze) * sf;

            srcX = Math.max(0f, Math.min(texW - srcW, srcX));
            srcY = Math.max(0f, Math.min(texH - srcH, srcY));
            out.add(new Slice(left, y + row, coverW, srcX, srcY, Math.round(srcW), Math.round(srcH)));
        }
        return out;
    }
}
