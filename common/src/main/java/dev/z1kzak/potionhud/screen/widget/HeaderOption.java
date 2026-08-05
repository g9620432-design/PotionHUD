package dev.z1kzak.potionhud.screen.widget;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Section separator: small caps title plus a hairline. */
public class HeaderOption extends Option {

    public HeaderOption(Component label) {
        super(label);
        this.h = 16;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt, boolean hovered) {
        String s = label.getString().toUpperCase(java.util.Locale.ROOT);
        Theme.text(g, s, x + 2, y + 5, Theme.TEXT_FAINT);
        int lineX = x + 6 + Theme.font().width(s) + 6;
        int lineY = y + 8;
        if (lineX < x + w - 2) {
            g.fill(lineX, lineY, x + w - 2, lineY + 1, 0x14FFFFFF);
        }
    }
}
