package dev.z1kzak.potionhud.render.style;

import dev.z1kzak.potionhud.config.Colors;
import dev.z1kzak.potionhud.config.HudConfig;
import dev.z1kzak.potionhud.render.Draw;
import dev.z1kzak.potionhud.render.EffectEntry;
import dev.z1kzak.potionhud.render.HudContext;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** Shared text building, icon blitting and colour resolution for all styles. */
public abstract class AbstractStyle implements HudStyle {

    protected static final String[] ROMAN = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};

    protected String nameOf(HudContext ctx, EffectEntry e) {
        StringBuilder sb = new StringBuilder();
        if (ctx.cfg.showName) {
            sb.append(e.name());
        }
        String lvl = levelOf(ctx, e);
        if (!lvl.isEmpty()) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(lvl);
        }
        return sb.toString();
    }

    protected String levelOf(HudContext ctx, EffectEntry e) {
        if (!ctx.cfg.showLevel) {
            return "";
        }
        int amp = e.amplifier();
        if (amp == 0 && ctx.cfg.hideLevelOne) {
            return "";
        }
        if (!ctx.cfg.romanNumerals) {
            return String.valueOf(amp + 1);
        }
        return amp >= 0 && amp < ROMAN.length ? ROMAN[amp] : String.valueOf(amp + 1);
    }

    protected String timeOf(HudContext ctx, EffectEntry e) {
        if (!ctx.cfg.showTimer) {
            return "";
        }
        return e.infinite() ? "∞" : ctx.cfg.time().format(e.ticks());
    }

    /** Warning state: the effect is about to run out. */
    protected boolean warn(HudContext ctx, EffectEntry e) {
        return !e.infinite() && e.ticks() <= ctx.cfg.warnTicks;
    }

    protected int textColor(HudContext ctx, EffectEntry e) {
        HudConfig cfg = ctx.cfg;
        int base = warn(ctx, e) ? cfg.warnArgb() : cfg.textArgb();
        if (cfg.useEffectColor && !warn(ctx, e)) {
            base = 0xFF000000 | e.colorRgb();
            if (Colors.luma(base) < 0.35f) {
                base = Colors.brighten(base, 0.45f);
            }
        }
        return Colors.multAlpha(base, e.alpha());
    }

    protected int timeColor(HudContext ctx, EffectEntry e) {
        int base = warn(ctx, e) ? ctx.cfg.warnArgb() : ctx.cfg.timeArgb();
        return Colors.multAlpha(base, e.alpha());
    }

    protected int accent(HudContext ctx, EffectEntry e) {
        int base = ctx.cfg.useEffectColor ? (0xFF000000 | e.colorRgb()) : ctx.cfg.accentArgb();
        if (warn(ctx, e)) {
            base = ctx.cfg.warnArgb();
        }
        return Colors.multAlpha(base, e.alpha());
    }

    /** Gentle breathing factor used by the fancier styles. */
    protected float pulse(HudContext ctx, float speed, float depth) {
        if (!ctx.cfg.animations || !ctx.cfg.pulse) {
            return 1f;
        }
        double v = Math.sin(ctx.time * speed * ctx.cfg.animSpeed);
        return (float) (1.0 - depth * 0.5 + depth * 0.5 * v);
    }

    protected void icon(HudContext ctx, EffectEntry e, int x, int y, int size, float alpha) {
        if (!ctx.cfg.showIcon || e.holder() == null) {
            return;
        }
        Identifier sprite;
        try {
            sprite = Gui.getMobEffectSprite(e.holder());
        } catch (Throwable t) {
            sprite = e.iconId();
        }
        if (sprite == null) {
            return;
        }
        ctx.g.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, size, size,
                Math.max(0f, Math.min(1f, alpha)));
    }

    /** Panel background shared by Classic / Bar / Neon (Glass overrides it). */
    protected void panel(HudContext ctx, int x, int y, int w, int h) {
        HudConfig cfg = ctx.cfg;
        int radius = ctx.radius();
        if (cfg.dropShadow > 0.001f) {
            Draw.softShadow(ctx.g, x, y, w, h, radius, cfg.dropShadow, 1);
        }
        if (cfg.bgVisible()) {
            if (cfg.bgGradient) {
                Draw.roundedGradient(ctx.g, x, y, w, h, radius, cfg.bgArgb(), cfg.bgArgb2());
            } else {
                Draw.rounded(ctx.g, x, y, w, h, radius, cfg.bgArgb());
            }
        }
        if (cfg.borderAlpha > 0.004f && cfg.borderWidth > 0) {
            Draw.roundedOutline(ctx.g, x, y, w, h, radius, cfg.borderArgb(), cfg.borderWidth);
        }
    }

    protected String overflowLabel(HudContext ctx) {
        return "+" + ctx.overflow;
    }

    @Override
    public int rowHeight(HudContext ctx) {
        return Math.max(ctx.iconSize(), ctx.lineHeight() * 2) + ctx.rowSpacing();
    }
}
