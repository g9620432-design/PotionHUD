package dev.z1kzak.potionhud.render.style;

import dev.z1kzak.potionhud.config.Colors;
import dev.z1kzak.potionhud.render.Draw;
import dev.z1kzak.potionhud.render.EffectEntry;
import dev.z1kzak.potionhud.render.HudContext;

/**
 * MODE 1 — the plain one. Icon on one side, effect name on the first line and the
 * remaining time right below it. This is the familiar look and stays the default.
 */
public class ClassicStyle extends AbstractStyle {

    @Override
    public int rowHeight(HudContext ctx) {
        int lines = (ctx.cfg.showName ? 1 : 0) + (ctx.cfg.showTimer ? 1 : 0);
        int textH = Math.max(1, lines) * ctx.lineHeight();
        return Math.max(ctx.cfg.showIcon ? ctx.iconSize() : 0, textH) + ctx.rowSpacing();
    }

    private int textWidth(HudContext ctx, EffectEntry e) {
        int w = 0;
        String name = nameOf(ctx, e);
        String time = timeOf(ctx, e);
        if (!name.isEmpty()) {
            w = Math.max(w, ctx.textW(name));
        }
        if (!time.isEmpty()) {
            w = Math.max(w, ctx.textW(time));
        }
        return w;
    }

    @Override
    public int[] measure(HudContext ctx) {
        int maxText = 0;
        for (EffectEntry e : ctx.entries) {
            maxText = Math.max(maxText, textWidth(ctx, e));
        }
        if (ctx.overflow > 0) {
            maxText = Math.max(maxText, ctx.textW(overflowLabel(ctx)));
        }
        int gap = ctx.cfg.showIcon && maxText > 0 ? Math.max(2, Math.round(4 * ctx.scale())) : 0;
        int iconW = ctx.cfg.showIcon ? ctx.iconSize() : 0;
        int contentW = iconW + gap + maxText;
        int rows = ctx.entries.size() + (ctx.overflow > 0 ? 1 : 0);
        int rowH = rowHeight(ctx);
        int contentH = Math.max(1, rows) * rowH - (rows > 0 ? ctx.rowSpacing() : 0);
        return new int[]{contentW + ctx.padX() * 2, contentH + ctx.padY() * 2};
    }

    @Override
    public void render(HudContext ctx, int x, int y) {
        int[] size = measure(ctx);
        panel(ctx, x, y, size[0], size[1]);

        int innerX = x + ctx.padX();
        int innerW = size[0] - ctx.padX() * 2;
        int rowH = rowHeight(ctx);
        int iconW = ctx.cfg.showIcon ? ctx.iconSize() : 0;
        int gap = iconW > 0 ? Math.max(2, Math.round(4 * ctx.scale())) : 0;
        int textW = innerW - iconW - gap;

        int count = ctx.entries.size();
        for (int i = 0; i < count; i++) {
            EffectEntry e = ctx.entries.get(i);
            int idx = ctx.cfg.growUpwards ? count - 1 - i : i;
            int rowY = y + ctx.padY() + idx * rowH;
            int slide = Math.round(e.slide() * (ctx.cfg.iconRight() ? 1 : -1));

            int iconX = ctx.cfg.iconRight() ? innerX + textW + gap : innerX;
            int textX = ctx.cfg.iconRight() ? innerX : innerX + iconW + gap;

            icon(ctx, e, iconX + slide, rowY + (rowH - ctx.rowSpacing() - ctx.iconSize()) / 2,
                    ctx.iconSize(), e.alpha());

            String name = nameOf(ctx, e);
            String time = timeOf(ctx, e);
            int lines = (name.isEmpty() ? 0 : 1) + (time.isEmpty() ? 0 : 1);
            int lineH = ctx.lineHeight();
            int block = Math.max(ctx.iconSize(), lines * lineH);
            int ty = rowY + (block - lines * lineH) / 2;

            if (!name.isEmpty()) {
                int tx = ctx.cfg.alignTextRight ? textX + textW - ctx.textW(name) : textX;
                Draw.scrollingText(ctx.g, ctx.font, name, tx + slide, ty, textW,
                        textColor(ctx, e), ctx.cfg.textShadow, ctx.textScale(),
                        ctx.marquee, ctx.cfg.marquee);
                ty += lineH;
            }
            if (!time.isEmpty()) {
                int tx = ctx.cfg.alignTextRight ? textX + textW - ctx.textW(time) : textX;
                Draw.text(ctx.g, ctx.font, time, tx + slide, ty, timeColor(ctx, e),
                        ctx.cfg.textShadow, ctx.textScale());
            }
        }

        if (ctx.overflow > 0) {
            int rowY = y + ctx.padY() + (ctx.cfg.growUpwards ? 0 : count) * rowH;
            if (ctx.cfg.growUpwards) {
                rowY = y + ctx.padY() + count * rowH;
            }
            String label = overflowLabel(ctx);
            int tx = ctx.cfg.iconRight() ? innerX : innerX + iconW + gap;
            if (ctx.cfg.alignTextRight) {
                tx = innerX + innerW - ctx.textW(label);
            }
            Draw.text(ctx.g, ctx.font, label, tx, rowY + (rowH - ctx.rowSpacing() - ctx.lineHeight()) / 2,
                    Colors.multAlpha(ctx.cfg.timeArgb(), 0.8f), ctx.cfg.textShadow, ctx.textScale());
        }
    }
}
