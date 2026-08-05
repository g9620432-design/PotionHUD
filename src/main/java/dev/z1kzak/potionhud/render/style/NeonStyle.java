package dev.z1kzak.potionhud.render.style;

import dev.z1kzak.potionhud.config.Colors;
import dev.z1kzak.potionhud.render.Draw;
import dev.z1kzak.potionhud.render.EffectEntry;
import dev.z1kzak.potionhud.render.HudContext;
import dev.z1kzak.potionhud.render.HudRenderer;

/**
 * MODE 5 — Neon. Dark slab, glowing outline, uppercase glow text, a bright accent tick in
 * front of every row and optional scan lines. Pulses gently on the accent colour.
 */
public class NeonStyle extends AbstractStyle {

    @Override
    public int rowHeight(HudContext ctx) {
        return Math.max(ctx.cfg.showIcon ? ctx.iconSize() : 0, ctx.lineHeight() + 4) + ctx.rowSpacing();
    }

    private int contentW(HudContext ctx) {
        int min = Math.round(70 * ctx.scale());
        for (EffectEntry e : ctx.entries) {
            String name = nameOf(ctx, e).toUpperCase(java.util.Locale.ROOT);
            String time = timeOf(ctx, e);
            int w = ctx.textW(name) + (time.isEmpty() ? 0 : ctx.textW(time) + Math.round(10 * ctx.scale()));
            min = Math.max(min, w);
        }
        int tick = Math.max(2, Math.round(2 * ctx.scale())) + Math.round(4 * ctx.scale());
        int iconW = ctx.cfg.showIcon ? ctx.iconSize() + Math.round(4 * ctx.scale()) : 0;
        return tick + iconW + min;
    }

    @Override
    public int[] measure(HudContext ctx) {
        int rows = ctx.entries.size() + (ctx.overflow > 0 ? 1 : 0);
        int rowH = rowHeight(ctx);
        int h = Math.max(1, rows) * rowH - (rows > 0 ? ctx.rowSpacing() : 0);
        return new int[]{contentW(ctx) + ctx.padX() * 2, h + ctx.padY() * 2};
    }

    @Override
    public void render(HudContext ctx, int x, int y) {
        int[] size = measure(ctx);
        int w = size[0];
        int h = size[1];
        int radius = ctx.radius();
        int neonRgb = ctx.cfg.accentArgb() & 0xFFFFFF;
        float breathe = pulse(ctx, 2.0f, 0.35f);

        if (ctx.cfg.neonGlow > 0.01f) {
            Draw.glow(ctx.g, x, y, w, h, radius, neonRgb, ctx.cfg.neonGlow * breathe, 4);
        }
        if (ctx.cfg.dropShadow > 0.001f) {
            Draw.softShadow(ctx.g, x, y, w, h, radius, ctx.cfg.dropShadow, 1);
        }
        if (ctx.cfg.bgVisible()) {
            Draw.roundedGradient(ctx.g, x, y, w, h, radius, ctx.cfg.bgArgb(),
                    ctx.cfg.bgGradient ? ctx.cfg.bgArgb2() : ctx.cfg.bgArgb());
        }

        // scan lines
        if (ctx.cfg.neonScanline) {
            for (int row = 2; row < h - 1; row += 3) {
                ctx.g.fill(x + 1, y + row, x + w - 1, y + row + 1, Colors.withAlpha(0x000000, 0.10f));
            }
        }

        int borderAlphaSrc = ctx.cfg.borderAlpha > 0.004f ? ctx.cfg.borderArgb() : Colors.withAlpha(neonRgb, 0.55f);
        Draw.roundedOutline(ctx.g, x, y, w, h, radius,
                Colors.multAlpha(borderAlphaSrc, breathe), Math.max(1, ctx.cfg.borderWidth));

        int innerX = x + ctx.padX();
        int innerW = w - ctx.padX() * 2;
        int rowH = rowHeight(ctx);
        int tickW = Math.max(2, Math.round(2 * ctx.scale()));
        int tickGap = Math.round(4 * ctx.scale());

        int count = ctx.entries.size();
        for (int i = 0; i < count; i++) {
            EffectEntry e = ctx.entries.get(i);
            int idx = ctx.cfg.growUpwards ? count - 1 - i : i;
            int rowY = y + ctx.padY() + idx * rowH;
            int bodyH = rowH - ctx.rowSpacing();
            int slide = -Math.round(e.slide());

            int acc = accent(ctx, e);
            Draw.rounded(ctx.g, innerX + slide, rowY, tickW, bodyH, 1, acc);
            Draw.glow(ctx.g, innerX + slide, rowY, tickW, bodyH, 1, acc & 0xFFFFFF,
                    ctx.cfg.neonGlow * 0.6f * e.alpha(), 2);

            int cursor = innerX + tickW + tickGap + slide;
            if (ctx.cfg.showIcon) {
                icon(ctx, e, cursor, rowY + (bodyH - ctx.iconSize()) / 2, ctx.iconSize(), e.alpha());
                cursor += ctx.iconSize() + Math.round(4 * ctx.scale());
            }

            String name = nameOf(ctx, e).toUpperCase(java.util.Locale.ROOT);
            String time = timeOf(ctx, e);
            int timeW = time.isEmpty() ? 0 : ctx.textW(time);
            int right = innerX + innerW + slide;
            int nameMax = Math.max(8, right - cursor - timeW - (timeW > 0 ? Math.round(6 * ctx.scale()) : 0));
            int textY = rowY + (bodyH - ctx.lineHeight()) / 2;

            if (!name.isEmpty()) {
                int nw = ctx.textW(name);
                if (nw <= nameMax) {
                    Draw.glowText(ctx.g, ctx.font, name, cursor, textY, textColor(ctx, e),
                            acc & 0xFFFFFF, ctx.cfg.neonGlow * e.alpha(), ctx.textScale());
                } else {
                    Draw.scrollingText(ctx.g, ctx.font, name, cursor, textY, nameMax,
                            textColor(ctx, e), false, ctx.textScale(), ctx.marquee, ctx.cfg.marquee);
                }
            }
            if (!time.isEmpty()) {
                Draw.glowText(ctx.g, ctx.font, time, right - timeW, textY, timeColor(ctx, e),
                        acc & 0xFFFFFF, ctx.cfg.neonGlow * 0.6f * e.alpha(), ctx.textScale());
            }

            // hairline under the row showing remaining duration
            if (!e.infinite()) {
                float p = e.progress(HudRenderer.referenceTicks(e));
                int lineY = rowY + bodyH - 1;
                int lx = innerX + tickW + tickGap + slide;
                int lw = right - lx;
                ctx.g.fill(lx, lineY, lx + lw, lineY + 1, Colors.withAlpha(0xFFFFFF, 0.08f * e.alpha()));
                int fw = Math.max(1, Math.round(lw * p));
                ctx.g.fill(lx, lineY, lx + fw, lineY + 1, Colors.multAlpha(acc, 0.9f));
            }
        }

        if (ctx.overflow > 0) {
            int rowY = y + ctx.padY() + count * rowH;
            String label = overflowLabel(ctx).toUpperCase(java.util.Locale.ROOT);
            Draw.glowText(ctx.g, ctx.font, label, innerX + tickW + tickGap,
                    rowY + (rowH - ctx.rowSpacing() - ctx.lineHeight()) / 2,
                    Colors.multAlpha(ctx.cfg.textArgb(), 0.75f), neonRgb,
                    ctx.cfg.neonGlow * 0.5f, ctx.textScale());
        }
    }
}
