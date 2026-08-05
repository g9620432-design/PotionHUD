package dev.z1kzak.potionhud.render.style;

import dev.z1kzak.potionhud.config.Colors;
import dev.z1kzak.potionhud.render.Draw;
import dev.z1kzak.potionhud.render.EffectEntry;
import dev.z1kzak.potionhud.render.HudContext;
import dev.z1kzak.potionhud.compat.Platform;
import dev.z1kzak.potionhud.render.HudRenderer;

/**
 * MODE 4 — Liquid Glass. A frosted panel with a soft drop shadow, vertical tint gradient,
 * specular highlight, rim light, a slowly travelling sheen and one translucent "glass pill"
 * per effect. Rows breathe slightly so the whole thing feels liquid rather than static.
 */
public class LiquidGlassStyle extends AbstractStyle {

    private int pillH(HudContext ctx) {
        int textH = ctx.lineHeight() + Math.max(2, Math.round(3 * ctx.scale()));
        return Math.max(ctx.cfg.showIcon ? ctx.iconSize() + Math.round(4 * ctx.scale()) : 0, textH + 4);
    }

    @Override
    public int rowHeight(HudContext ctx) {
        return pillH(ctx) + Math.max(2, ctx.rowSpacing());
    }

    private int contentW(HudContext ctx) {
        int min = Math.round(78 * ctx.scale());
        for (EffectEntry e : ctx.entries) {
            String name = nameOf(ctx, e);
            String time = timeOf(ctx, e);
            int w = ctx.textW(name) + (time.isEmpty() ? 0 : ctx.textW(time) + Math.round(10 * ctx.scale()));
            min = Math.max(min, w);
        }
        int iconW = ctx.cfg.showIcon ? ctx.iconSize() + Math.round(6 * ctx.scale()) : 0;
        return iconW + min + Math.round(10 * ctx.scale());
    }

    @Override
    public int[] measure(HudContext ctx) {
        int rows = ctx.entries.size() + (ctx.overflow > 0 ? 1 : 0);
        int spacing = Math.max(2, ctx.rowSpacing());
        int h = Math.max(1, rows) * pillH(ctx) + Math.max(0, rows - 1) * spacing;
        int padX = Math.max(ctx.padX(), Math.round(5 * ctx.scale()));
        int padY = Math.max(ctx.padY(), Math.round(5 * ctx.scale()));
        return new int[]{contentW(ctx) + padX * 2, h + padY * 2};
    }

    @Override
    public void render(HudContext ctx, int x, int y) {
        int[] size = measure(ctx);
        int w = size[0];
        int h = size[1];
        int radius = Math.max(6, ctx.radius());

        // ── the glass slab ───────────────────────────────────────────────────
        Draw.softShadow(ctx.g, x, y, w, h, radius, Math.max(0.25f, ctx.cfg.dropShadow), 2);

        // real see-through: the frame behind the HUD, re-drawn with bent UVs
        boolean seeThrough = Platform.get().drawGlassBackdrop(ctx, x, y, w, h, radius);
        if (!seeThrough) {
            Draw.frostedBackdrop(ctx.g, x, y, w, h, radius, ctx.cfg.bgArgb(), ctx.cfg.glassBlur);
        }
        // tint on top of the refraction — kept lighter when the scene shows through
        float tintFactor = seeThrough ? 0.62f : 1f;
        Draw.roundedGradient(ctx.g, x, y, w, h, radius,
                Colors.multAlpha(ctx.cfg.bgArgb(), tintFactor),
                Colors.multAlpha(ctx.cfg.bgGradient ? ctx.cfg.bgArgb2() : ctx.cfg.bgArgb(), tintFactor));
        Draw.specularHighlight(ctx.g, x, y, w, h, radius, ctx.cfg.glassRefraction);

        // rim light: bright top edge, dim bottom edge
        float rim = ctx.cfg.glassRefraction;
        if (rim > 0.01f) {
            Draw.roundedOutline(ctx.g, x, y, w, h, radius,
                    Colors.withAlpha(0xFFFFFF, 0.16f * rim), Math.max(1, ctx.cfg.borderWidth));
            int inset = Math.max(1, radius / 2);
            ctx.g.fill(x + inset, y + 1, x + w - inset, y + 2, Colors.withAlpha(0xFFFFFF, 0.34f * rim));
            ctx.g.fill(x + inset, y + h - 2, x + w - inset, y + h - 1,
                    Colors.withAlpha(0x000000, 0.20f * rim));
        }
        if (ctx.cfg.borderAlpha > 0.004f && ctx.cfg.borderWidth > 0) {
            Draw.roundedOutline(ctx.g, x, y, w, h, radius, ctx.cfg.borderArgb(), ctx.cfg.borderWidth);
        }

        // travelling sheen
        if (ctx.anim() && ctx.cfg.glassSheen > 0.01f) {
            float period = 5.5f / Math.max(0.25f, ctx.cfg.animSpeed);
            float phase = (ctx.time % period) / period;
            Draw.sheen(ctx.g, x + 1, y + 1, w - 2, h - 2, phase, ctx.cfg.glassSheen);
        }

        // ── rows ─────────────────────────────────────────────────────────────
        int padX = Math.max(ctx.padX(), Math.round(5 * ctx.scale()));
        int padY = Math.max(ctx.padY(), Math.round(5 * ctx.scale()));
        int innerX = x + padX;
        int innerW = w - padX * 2;
        int ph = pillH(ctx);
        int spacing = Math.max(2, ctx.rowSpacing());
        int pillR = Math.max(3, radius - 3);

        int count = ctx.entries.size();
        for (int i = 0; i < count; i++) {
            EffectEntry e = ctx.entries.get(i);
            int idx = ctx.cfg.growUpwards ? count - 1 - i : i;
            int py = y + padY + idx * (ph + spacing);

            // liquid wobble — a fraction of a pixel per row, offset by index
            int wobble = 0;
            if (ctx.anim() && ctx.cfg.pulse) {
                wobble = Math.round((float) Math.sin(ctx.time * 1.6f * ctx.cfg.animSpeed + i * 0.7f) * 0.9f);
            }
            int px = innerX + Math.round(e.slide()) + wobble;

            int tint = ctx.cfg.useEffectColor ? (0xFF000000 | e.colorRgb()) : 0xFFFFFFFF;
            int pillTop = Colors.multAlpha(Colors.withAlpha(tint & 0xFFFFFF, 0.16f), e.alpha());
            int pillBottom = Colors.multAlpha(Colors.withAlpha(tint & 0xFFFFFF, 0.06f), e.alpha());
            Draw.roundedGradient(ctx.g, px, py, innerW, ph, pillR, pillTop, pillBottom);
            Draw.roundedOutline(ctx.g, px, py, innerW, ph, pillR,
                    Colors.multAlpha(Colors.withAlpha(0xFFFFFF, 0.13f), e.alpha()), 1);

            int iconSize = ctx.iconSize();
            int iconPad = Math.round(3 * ctx.scale());
            int iconX = ctx.cfg.iconRight() ? px + innerW - iconPad - iconSize : px + iconPad;
            int iconY = py + (ph - iconSize) / 2;
            if (ctx.cfg.showIcon) {
                // little glass tile behind the icon
                Draw.rounded(ctx.g, iconX - 1, iconY - 1, iconSize + 2, iconSize + 2,
                        Math.max(2, pillR - 2), Colors.multAlpha(Colors.withAlpha(0xFFFFFF, 0.10f), e.alpha()));
                icon(ctx, e, iconX, iconY, iconSize, e.alpha());
            }

            int textLeft = ctx.cfg.iconRight()
                    ? px + iconPad
                    : px + (ctx.cfg.showIcon ? iconPad + iconSize + Math.round(5 * ctx.scale()) : iconPad);
            int textRight = ctx.cfg.iconRight()
                    ? px + innerW - iconPad - iconSize - Math.round(5 * ctx.scale())
                    : px + innerW - iconPad;
            int textW = Math.max(8, textRight - textLeft);

            String name = nameOf(ctx, e);
            String time = timeOf(ctx, e);
            int timeW = time.isEmpty() ? 0 : ctx.textW(time);
            int nameMax = Math.max(8, textW - timeW - (timeW > 0 ? Math.round(6 * ctx.scale()) : 0));
            int textY = py + (ph - ctx.lineHeight()) / 2 - 1;

            if (!name.isEmpty()) {
                // soft dark halo keeps white text readable on bright backdrops
                Draw.text(ctx.g, ctx.font, name, textLeft + 1, textY + 1,
                        Colors.multAlpha(Colors.withAlpha(0x000000, 0.35f), e.alpha()), false, ctx.textScale());
                Draw.scrollingText(ctx.g, ctx.font, name, textLeft, textY, nameMax,
                        textColor(ctx, e), false, ctx.textScale(), ctx.marquee, ctx.cfg.marquee);
            }
            if (!time.isEmpty()) {
                int tx = textLeft + textW - timeW;
                Draw.text(ctx.g, ctx.font, time, tx + 1, textY + 1,
                        Colors.multAlpha(Colors.withAlpha(0x000000, 0.30f), e.alpha()), false, ctx.textScale());
                Draw.text(ctx.g, ctx.font, time, tx, textY, timeColor(ctx, e), false, ctx.textScale());
            }

            // capillary progress line hugging the bottom of the pill
            float p = e.infinite() ? 1f : e.progress(HudRenderer.referenceTicks(e));
            int lineY = py + ph - Math.max(2, Math.round(2 * ctx.scale()));
            int lineX = px + pillR / 2;
            int lineW = innerW - pillR;
            int glowRgb = (ctx.cfg.useEffectColor ? e.colorRgb() : (ctx.cfg.accentArgb() & 0xFFFFFF));
            Draw.rounded(ctx.g, lineX, lineY, lineW, Math.max(1, Math.round(ctx.scale())), 1,
                    Colors.multAlpha(Colors.withAlpha(0xFFFFFF, 0.10f), e.alpha()));
            int fw = Math.max(1, Math.round(lineW * p));
            Draw.rounded(ctx.g, lineX, lineY, fw, Math.max(1, Math.round(ctx.scale())), 1,
                    Colors.multAlpha(Colors.withAlpha(glowRgb, 0.85f), e.alpha()));
            if (warn(ctx, e)) {
                Draw.glow(ctx.g, lineX, lineY, fw, Math.max(1, Math.round(ctx.scale())), 1,
                        ctx.cfg.warnArgb() & 0xFFFFFF, 0.5f * e.alpha(), 2);
            }
        }

        if (ctx.overflow > 0) {
            int py = y + padY + count * (ph + spacing);
            String label = overflowLabel(ctx);
            int tx = innerX + (innerW - ctx.textW(label)) / 2;
            Draw.text(ctx.g, ctx.font, label, tx, py + (ph - ctx.lineHeight()) / 2,
                    Colors.multAlpha(0xFFFFFFFF, 0.65f), false, ctx.textScale());
        }
    }
}
