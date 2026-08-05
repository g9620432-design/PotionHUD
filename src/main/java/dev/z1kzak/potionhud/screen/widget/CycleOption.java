package dev.z1kzak.potionhud.screen.widget;

import dev.z1kzak.potionhud.render.Draw;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

/** &lt; value &gt; cycler for enums. Scroll wheel works too. */
public class CycleOption extends Option {

    private final Supplier<Component> value;
    private final Runnable next;
    private final Runnable prev;

    public CycleOption(Component label, Supplier<Component> value, Runnable prev, Runnable next) {
        super(label);
        this.value = value;
        this.prev = prev;
        this.next = next;
    }

    private int arrowW() {
        return 12;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt, boolean hovered) {
        Theme.row(g, x, y, w, h, hovered);
        drawLabel(g, Theme.TEXT);

        int cx = controlX();
        int cw = controlW();
        int cy = y + 2;
        int ch = h - 4;
        Draw.rounded(g, cx, cy, cw, ch, 4, Theme.ROW_ACTIVE);

        boolean overLeft = mx >= cx && mx < cx + arrowW() && my >= cy && my < cy + ch;
        boolean overRight = mx >= cx + cw - arrowW() && mx < cx + cw && my >= cy && my < cy + ch;
        Theme.textCenter(g, "‹", cx + arrowW() / 2, y + (h - 8) / 2, overLeft ? Theme.ACCENT : Theme.TEXT_DIM);
        Theme.textCenter(g, "›", cx + cw - arrowW() / 2, y + (h - 8) / 2, overRight ? Theme.ACCENT : Theme.TEXT_DIM);

        String v = Theme.clip(value.get().getString(), cw - arrowW() * 2 - 4);
        Theme.textCenter(g, v, cx + cw / 2, y + (h - 8) / 2, Theme.TEXT);
    }

    @Override
    public boolean click(double mx, double my, int button) {
        if (!isOver(mx, my)) {
            return false;
        }
        int cx = controlX();
        int cw = controlW();
        if (button == 1) {
            prev.run();
            return true;
        }
        if (button != 0) {
            return false;
        }
        if (mx >= cx && mx < cx + arrowW()) {
            prev.run();
            return true;
        }
        if (mx >= cx + cw - arrowW() && mx < cx + cw) {
            next.run();
            return true;
        }
        if (mx >= cx && mx < cx + cw) {
            next.run();
            return true;
        }
        return false;
    }

    @Override
    public boolean scroll(double amount) {
        if (amount > 0) {
            next.run();
        } else {
            prev.run();
        }
        return true;
    }
}
