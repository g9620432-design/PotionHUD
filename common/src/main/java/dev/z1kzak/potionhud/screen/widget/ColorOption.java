package dev.z1kzak.potionhud.screen.widget;

import dev.z1kzak.potionhud.config.Colors;
import dev.z1kzak.potionhud.render.Draw;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Supplier;

/** Colour swatch + hex readout. Clicking hands control to the colour picker overlay. */
public class ColorOption extends Option {

    private final Supplier<String> getter;
    private final Consumer<String> setter;
    private final Consumer<ColorOption> onOpen;

    public ColorOption(Component label, Supplier<String> getter, Consumer<String> setter,
                       Consumer<ColorOption> onOpen) {
        super(label);
        this.getter = getter;
        this.setter = setter;
        this.onOpen = onOpen;
    }

    public int rgb() {
        return Colors.parse(getter.get(), 0xFFFFFF);
    }

    public void setRgb(int rgb) {
        setter.accept(Colors.toHex(rgb));
    }

    public String hex() {
        return Colors.toHex(rgb());
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt, boolean hovered) {
        Theme.row(g, x, y, w, h, hovered);
        drawLabel(g, Theme.TEXT);

        int cx = controlX();
        int cw = controlW();
        int sw = 26;
        int sy = y + 4;
        int sh = h - 8;
        // checkerboard-free simple swatch with border
        Draw.rounded(g, cx + cw - sw, sy, sw, sh, 3, 0xFF000000 | rgb());
        Draw.roundedOutline(g, cx + cw - sw, sy, sw, sh, 3, 0x55FFFFFF, 1);
        Theme.textRight(g, hex(), cx + cw - sw - 6, y + (h - 8) / 2, Theme.TEXT_DIM);
    }

    @Override
    public boolean click(double mx, double my, int button) {
        if (button != 0 || !isOver(mx, my)) {
            return false;
        }
        if (mx >= controlX() - 40) {
            onOpen.accept(this);
            return true;
        }
        return false;
    }
}
