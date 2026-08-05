package dev.z1kzak.potionhud.render;

import dev.z1kzak.potionhud.config.HudConfig;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

/** Everything a style needs for one frame. */
public final class HudContext {

    public final GuiGraphics g;
    public final Font font;
    public final HudConfig cfg;
    public final List<EffectEntry> entries;
    public final int overflow;
    public final int screenW;
    public final int screenH;
    /** Seconds since the game started, used to drive animations. */
    public final float time;
    /** Growing pixel offset for marquee text. */
    public final float marquee;

    public HudContext(GuiGraphics g, Font font, HudConfig cfg, List<EffectEntry> entries,
                      int overflow, int screenW, int screenH, float time, float marquee) {
        this.g = g;
        this.font = font;
        this.cfg = cfg;
        this.entries = entries;
        this.overflow = overflow;
        this.screenW = screenW;
        this.screenH = screenH;
        this.time = time;
        this.marquee = marquee;
    }

    public float scale() {
        return cfg.scale;
    }

    public float textScale() {
        return cfg.scale * cfg.textScale;
    }

    public int iconSize() {
        return Math.max(4, Math.round(18 * cfg.scale * cfg.iconScale));
    }

    public int lineHeight() {
        return Draw.lineHeight(font, textScale());
    }

    public int rowSpacing() {
        return Math.max(0, Math.round(cfg.rowSpacing * cfg.scale));
    }

    public int padX() {
        return Math.round(cfg.paddingX * cfg.scale);
    }

    public int padY() {
        return Math.round(cfg.paddingY * cfg.scale);
    }

    public int radius() {
        return Math.round(cfg.cornerRadius * Math.max(0.6f, cfg.scale));
    }

    public int textW(String s) {
        return Draw.textWidth(font, s, textScale());
    }

    public boolean anim() {
        return cfg.animations;
    }
}
