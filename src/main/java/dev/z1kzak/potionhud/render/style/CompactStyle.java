package dev.z1kzak.potionhud.render.style;

import dev.z1kzak.potionhud.config.Colors;
import dev.z1kzak.potionhud.render.Draw;
import dev.z1kzak.potionhud.render.EffectEntry;
import dev.z1kzak.potionhud.render.HudContext;
import dev.z1kzak.potionhud.render.HudRenderer;

/**
 * MODE 2 — compact icon strip / grid. No names, just icons with a micro timer and a thin
 * duration underline. Fits in a corner and stays out of the way.
 */
public class CompactStyle extends AbstractStyle {

    private float smallScale(HudContext ctx) {
        return Math.max(0.4f, ctx.textScale() * 0.72f);
    }

    private int cellW(HudContext ctx) {
        int w = ctx.iconSize();
        if (ctx.cfg.compactShowTimer) {
            for (EffectEntry e : ctx.entries) {
                w = Math.max(w, Draw.textWidth(ctx.font, timeOf(ctx, e), smallScale(ctx)));
            }
        }
        return w;
    }

    private int cellH(HudContext ctx) {
        int h = ctx.iconSize() + 2;
        if (ctx.cfg.compactShowTimer) {
            h += Draw.lineHeight(ctx.font, smallScale(ctx));
        }
        return h;
    }

    private int columns(HudContext ctx) {
        int n = Math.max(1, ctx.entries.size());
        if (ctx.cfg.compactHorizontal) {
            return ctx.cfg.compactColumns > 0 ? Math.min(ctx.cfg.compactColumns, n) : n;
        }
        return Math.max(1, ctx.cfg.compactColumns);
    }

    @Override
    public int rowHeight(HudContext ctx) {
        return cellH(ctx) + ctx.rowSpacing();
    }

    @Override
    public int[] measure(HudContext ctx) {
        int cols = columns(ctx);
        int n = ctx.entries.size() + (ctx.overflow > 0 ? 1 : 0);
        int rows = Math.max(1, (int) Math.ceil(n / (double) cols));
        int gap = Math.max(1, Math.round(3 * ctx.scale()));
        int w = cols * cellW(ctx) + (cols - 1) * gap;
        int h = rows * cellH(ctx) + (rows - 1) * gap;
        return new int[]{w + ctx.padX() * 2, h + ctx.padY() * 2};
    }

    @Override
    public void render(HudContext ctx, int x, int y) {
        int[] size = measure(ctx);
        panel(ctx, x, y, size[0], size[1]);

        int cols = columns(ctx);
        int gap = Math.max(1, Math.round(3 * ctx.scale()));
        int cw = cellW(ctx);
        int ch = cellH(ctx);
        int startX = x + ctx.padX();
        int startY = y + ctx.padY();

        int i = 0;
        for (EffectEntry e : ctx.entries) {
            int col = i % cols;
            int row = i / cols;
            int cx = startX + col * (cw + gap);
            int cy = startY + row * (ch + gap) + Math.round(e.slide());
            int iconX = cx + (cw - ctx.iconSize()) / 2;

            icon(ctx, e, iconX, cy, ctx.iconSize(), e.alpha());

            // thin duration underline
            if (!e.infinite()) {
                float p = e.progress(HudRenderer.referenceTicks(e));
                int barY = cy + ctx.iconSize();
                int track = Colors.withAlpha(0x000000, 0.35f * e.alpha());
                Draw.progressBar(ctx.g, iconX, barY, ctx.iconSize(), Math.max(1, Math.round(ctx.scale())),
                        p, track, accent(ctx, e), false);
            }

            if (ctx.cfg.compactShowTimer) {
                String time = timeOf(ctx, e);
                if (!time.isEmpty()) {
                    float s = smallScale(ctx);
                    int tw = Draw.textWidth(ctx.font, time, s);
                    Draw.text(ctx.g, ctx.font, time, cx + (cw - tw) / 2, cy + ctx.iconSize() + 2,
                            timeColor(ctx, e), ctx.cfg.textShadow, s);
                }
            }
            i++;
        }

        if (ctx.overflow > 0) {
            int col = i % cols;
            int row = i / cols;
            int cx = startX + col * (cw + gap);
            int cy = startY + row * (ch + gap);
            String label = overflowLabel(ctx);
            float s = smallScale(ctx);
            int tw = Draw.textWidth(ctx.font, label, s);
            Draw.text(ctx.g, ctx.font, label, cx + (cw - tw) / 2,
                    cy + (ctx.iconSize() - Draw.lineHeight(ctx.font, s)) / 2,
                    Colors.multAlpha(ctx.cfg.timeArgb(), 0.85f), ctx.cfg.textShadow, s);
        }
    }
}
