package dev.z1kzak.potionhud.render.style;

import dev.z1kzak.potionhud.config.Colors;
import dev.z1kzak.potionhud.render.Draw;
import dev.z1kzak.potionhud.render.EffectEntry;
import dev.z1kzak.potionhud.render.HudContext;
import dev.z1kzak.potionhud.render.HudRenderer;

/**
 * MODE 3 — bars. Every effect gets a name line with the timer pushed to the right and a
 * shrinking duration bar underneath, tinted with the effect's own colour.
 */
public class BarStyle extends AbstractStyle {

    private int barH(HudContext ctx) {
        return Math.max(1, Math.round(ctx.cfg.barThickness * ctx.scale()));
    }

    @Override
    public int rowHeight(HudContext ctx) {
        int textPart = ctx.lineHeight() + 2 + barH(ctx);
        return Math.max(ctx.cfg.showIcon ? ctx.iconSize() : 0, textPart) + ctx.rowSpacing();
    }

    private int contentTextW(HudContext ctx) {
        int min = Math.round(64 * ctx.scale());
        for (EffectEntry e : ctx.entries) {
            String name = nameOf(ctx, e);
            String time = ctx.cfg.barShowRemainingText ? timeOf(ctx, e) : "";
            int w = ctx.textW(name) + (time.isEmpty() ? 0 : ctx.textW(time) + Math.round(8 * ctx.scale()));
            min = Math.max(min, w);
        }
        return min;
    }

    @Override
    public int[] measure(HudContext ctx) {
        int iconW = ctx.cfg.showIcon ? ctx.iconSize() : 0;
        int gap = iconW > 0 ? Math.max(2, Math.round(4 * ctx.scale())) : 0;
        int rows = ctx.entries.size() + (ctx.overflow > 0 ? 1 : 0);
        int rowH = rowHeight(ctx);
        int h = Math.max(1, rows) * rowH - (rows > 0 ? ctx.rowSpacing() : 0);
        return new int[]{iconW + gap + contentTextW(ctx) + ctx.padX() * 2, h + ctx.padY() * 2};
    }

    @Override
    public void render(HudContext ctx, int x, int y) {
        int[] size = measure(ctx);
        panel(ctx, x, y, size[0], size[1]);

        int innerX = x + ctx.padX();
        int innerW = size[0] - ctx.padX() * 2;
        int iconW = ctx.cfg.showIcon ? ctx.iconSize() : 0;
        int gap = iconW > 0 ? Math.max(2, Math.round(4 * ctx.scale())) : 0;
        int textW = innerW - iconW - gap;
        int rowH = rowHeight(ctx);
        int lineH = ctx.lineHeight();
        int bh = barH(ctx);

        int count = ctx.entries.size();
        for (int i = 0; i < count; i++) {
            EffectEntry e = ctx.entries.get(i);
            int idx = ctx.cfg.growUpwards ? count - 1 - i : i;
            int rowY = y + ctx.padY() + idx * rowH;
            int slide = -Math.round(e.slide());

            int iconX = ctx.cfg.iconRight() ? innerX + textW + gap : innerX;
            int textX = ctx.cfg.iconRight() ? innerX : innerX + iconW + gap;
            icon(ctx, e, iconX + slide, rowY + (rowH - ctx.rowSpacing() - ctx.iconSize()) / 2,
                    ctx.iconSize(), e.alpha());

            String name = nameOf(ctx, e);
            String time = ctx.cfg.barShowRemainingText ? timeOf(ctx, e) : "";
            int timeW = time.isEmpty() ? 0 : ctx.textW(time);
            int nameMax = Math.max(8, textW - timeW - (timeW > 0 ? Math.round(6 * ctx.scale()) : 0));

            int ty = rowY;
            if (!name.isEmpty()) {
                Draw.scrollingText(ctx.g, ctx.font, name, textX + slide, ty, nameMax,
                        textColor(ctx, e), ctx.cfg.textShadow, ctx.textScale(),
                        ctx.marquee, ctx.cfg.marquee);
            }
            if (!time.isEmpty()) {
                Draw.text(ctx.g, ctx.font, time, textX + textW - timeW + slide, ty,
                        timeColor(ctx, e), ctx.cfg.textShadow, ctx.textScale());
            }

            int barY = ty + lineH + 2;
            float p = e.infinite() ? 1f : e.progress(HudRenderer.referenceTicks(e));
            int track = Colors.withAlpha(0x000000, 0.42f * e.alpha());
            int fill = accent(ctx, e);
            if (e.infinite()) {
                // slow shimmer for infinite effects instead of a static full bar
                float k = 0.75f + 0.25f * (float) Math.sin(ctx.time * 2.2f * ctx.cfg.animSpeed);
                fill = Colors.multAlpha(fill, ctx.anim() ? k : 1f);
            }
            Draw.progressBar(ctx.g, textX + slide, barY, textW, bh, p, track, fill, bh >= 3);
        }

        if (ctx.overflow > 0) {
            int rowY = y + ctx.padY() + count * rowH;
            String label = overflowLabel(ctx);
            Draw.text(ctx.g, ctx.font, label, innerX + iconW + gap,
                    rowY + (rowH - ctx.rowSpacing() - lineH) / 2,
                    Colors.multAlpha(ctx.cfg.timeArgb(), 0.8f), ctx.cfg.textShadow, ctx.textScale());
        }
    }
}
