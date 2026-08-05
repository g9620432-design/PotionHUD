package dev.z1kzak.potionhud.screen.widget;

import dev.z1kzak.potionhud.render.Draw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Shared palette and small painting helpers so the whole menu looks like one piece. */
public final class Theme {

    public static final int PANEL = 0xF215181E;
    public static final int PANEL_2 = 0xF21B1F27;
    public static final int SIDEBAR = 0x18FFFFFF;
    public static final int BORDER = 0x2EFFFFFF;
    public static final int ROW = 0x12FFFFFF;
    public static final int ROW_HOVER = 0x24FFFFFF;
    public static final int ROW_ACTIVE = 0x33000000;
    public static final int TEXT = 0xFFE9ECF3;
    public static final int TEXT_DIM = 0xFF9AA2B4;
    public static final int TEXT_FAINT = 0xFF6E7686;
    public static final int ACCENT = 0xFF63B3FF;
    public static final int ACCENT_SOFT = 0x3363B3FF;
    public static final int GOOD = 0xFF62D28E;
    public static final int BAD = 0xFFE86A6A;
    public static final int TRACK = 0x59000000;

    private Theme() {
    }

    public static Font font() {
        return Minecraft.getInstance().font;
    }

    /** Main window: shadow, gradient body, hairline border. */
    public static void window(GuiGraphics g, int x, int y, int w, int h) {
        Draw.softShadow(g, x, y, w, h, 8, 0.9f, 3);
        Draw.roundedGradient(g, x, y, w, h, 8, PANEL, PANEL_2);
        Draw.roundedOutline(g, x, y, w, h, 8, BORDER, 1);
    }

    public static void card(GuiGraphics g, int x, int y, int w, int h, int argb, int radius) {
        Draw.rounded(g, x, y, w, h, radius, argb);
    }

    public static void row(GuiGraphics g, int x, int y, int w, int h, boolean hovered) {
        Draw.rounded(g, x, y, w, h, 4, hovered ? ROW_HOVER : ROW);
    }

    public static void text(GuiGraphics g, String s, int x, int y, int color) {
        g.drawString(font(), s, x, y, color, false);
    }

    public static void text(GuiGraphics g, Component s, int x, int y, int color) {
        g.drawString(font(), s, x, y, color, false);
    }

    public static void textRight(GuiGraphics g, String s, int right, int y, int color) {
        g.drawString(font(), s, right - font().width(s), y, color, false);
    }

    public static void textCenter(GuiGraphics g, String s, int cx, int y, int color) {
        g.drawString(font(), s, cx - font().width(s) / 2, y, color, false);
    }

    /** Truncates a string with an ellipsis so it never spills out of its slot. */
    public static String clip(String s, int maxW) {
        Font f = font();
        if (f.width(s) <= maxW) {
            return s;
        }
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (f.width(sb.toString() + c + "…") > maxW) {
                break;
            }
            sb.append(c);
        }
        return sb + "…";
    }
}
