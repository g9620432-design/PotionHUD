package dev.z1kzak.potionhud.screen.widget;

import dev.z1kzak.potionhud.render.Draw;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

/** A push button living in the options list. Either right-aligned or full width. */
public class ActionOption extends Option {

    private final Supplier<Component> buttonText;
    private final Runnable action;
    private final boolean wide;
    private int tint = Theme.ACCENT;

    public ActionOption(Component label, Supplier<Component> buttonText, Runnable action, boolean wide) {
        super(label);
        this.buttonText = buttonText;
        this.action = action;
        this.wide = wide;
        if (label == null) {
            this.searchKey = buttonText.get().getString().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public ActionOption tint(int argb) {
        this.tint = argb;
        return this;
    }

    private int btnX() {
        return wide ? x : controlX();
    }

    private int btnW() {
        return wide ? w : controlW();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt, boolean hovered) {
        if (!wide) {
            Theme.row(g, x, y, w, h, hovered);
            drawLabel(g, Theme.TEXT);
        }
        int bx = btnX();
        int bw = btnW();
        boolean over = mx >= bx && mx < bx + bw && my >= y + 2 && my < y + h - 2;
        int base = over ? tint : (tint & 0x00FFFFFF) | 0x59000000;
        Draw.rounded(g, bx, y + 2, bw, h - 4, 4, base);
        Draw.roundedOutline(g, bx, y + 2, bw, h - 4, 4, over ? 0x55FFFFFF : 0x22FFFFFF, 1);
        String t = Theme.clip(buttonText.get().getString(), bw - 8);
        Theme.textCenter(g, t, bx + bw / 2, y + (h - 8) / 2, over ? 0xFF0E1116 : Theme.TEXT);
    }

    @Override
    public boolean click(double mx, double my, int button) {
        if (button != 0) {
            return false;
        }
        int bx = btnX();
        int bw = btnW();
        if (mx >= bx && mx < bx + bw && my >= y && my < y + h) {
            action.run();
            return true;
        }
        return false;
    }
}
