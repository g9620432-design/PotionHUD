package dev.z1kzak.potionhud.screen.widget;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * One line in the settings list. Deliberately tiny and self-contained: the screen owns
 * the layout, the option owns its control and its value.
 */
public abstract class Option {

    public int x;
    public int y;
    public int w;
    public int h = 20;
    public Component label;
    public Component tooltip;
    /** Free-text used by the search box. */
    public String searchKey = "";

    protected Option(Component label) {
        this.label = label;
        if (label != null) {
            this.searchKey = label.getString().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public Option tooltip(Component t) {
        this.tooltip = t;
        if (t != null) {
            this.searchKey += " " + t.getString().toLowerCase(java.util.Locale.ROOT);
        }
        return this;
    }

    public void bounds(int x, int y, int w) {
        this.x = x;
        this.y = y;
        this.w = w;
    }

    public abstract void render(GuiGraphics g, int mx, int my, float pt, boolean hovered);

    public boolean click(double mx, double my, int button) {
        return false;
    }

    public boolean drag(double mx, double my) {
        return false;
    }

    public void release() {
    }

    public boolean scroll(double amount) {
        return false;
    }

    public boolean charTyped(char c) {
        return false;
    }

    public boolean keyPressed(int key, int modifiers) {
        return false;
    }

    public boolean isOver(double mx, double my) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    /** Right-hand control column. */
    protected int controlW() {
        return Math.min(132, Math.max(84, w / 2));
    }

    protected int controlX() {
        return x + w - controlW() - 4;
    }

    protected int labelMaxW() {
        return controlX() - x - 12;
    }

    protected void drawLabel(GuiGraphics g, int color) {
        if (label == null) {
            return;
        }
        Theme.text(g, Theme.clip(label.getString(), labelMaxW()), x + 6,
                y + (h - 8) / 2, color);
    }
}
