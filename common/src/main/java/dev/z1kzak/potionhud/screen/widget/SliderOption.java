package dev.z1kzak.potionhud.screen.widget;

import dev.z1kzak.potionhud.render.Draw;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoubleSupplier;

/** Draggable slider with a snapping step, a value pill and scroll-wheel support. */
public class SliderOption extends Option {

    private final DoubleSupplier getter;
    private final DoubleConsumer setter;
    private final double min;
    private final double max;
    private final double step;
    private final DoubleFunction<String> fmt;
    private boolean dragging;

    public SliderOption(Component label, double min, double max, double step,
                        DoubleSupplier getter, DoubleConsumer setter, DoubleFunction<String> fmt) {
        super(label);
        this.min = min;
        this.max = max;
        this.step = step;
        this.getter = getter;
        this.setter = setter;
        this.fmt = fmt;
    }

    private int trackX() {
        return controlX();
    }

    private int trackW() {
        return controlW();
    }

    private double snap(double v) {
        if (step > 0) {
            v = Math.round(v / step) * step;
        }
        return Math.max(min, Math.min(max, v));
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt, boolean hovered) {
        Theme.row(g, x, y, w, h, hovered || dragging);
        drawLabel(g, Theme.TEXT);

        double value = getter.getAsDouble();
        float frac = (float) ((value - min) / (max - min));
        frac = Math.max(0f, Math.min(1f, frac));

        int tx = trackX();
        int tw = trackW();
        int th = 4;
        int ty = y + (h - th) / 2;
        Draw.rounded(g, tx, ty, tw, th, 2, Theme.TRACK);
        Draw.rounded(g, tx, ty, Math.max(1, Math.round(tw * frac)), th, 2, Theme.ACCENT);

        int knobX = tx + Math.round((tw - 6) * frac);
        Draw.rounded(g, knobX, y + (h - 12) / 2, 6, 12, 3, hovered || dragging ? 0xFFFFFFFF : 0xFFD8DEE9);

        String text = fmt.apply(value);
        Theme.textRight(g, text, tx - 6, y + (h - 8) / 2, Theme.TEXT_DIM);
    }

    @Override
    public boolean click(double mx, double my, int button) {
        if (button != 0 || !isOver(mx, my)) {
            return false;
        }
        if (mx < trackX() - 40) {
            return false;
        }
        dragging = true;
        apply(mx);
        return true;
    }

    @Override
    public boolean drag(double mx, double my) {
        if (!dragging) {
            return false;
        }
        apply(mx);
        return true;
    }

    @Override
    public void release() {
        dragging = false;
    }

    @Override
    public boolean scroll(double amount) {
        double s = step > 0 ? step : (max - min) / 50.0;
        setter.accept(snap(getter.getAsDouble() + amount * s));
        return true;
    }

    private void apply(double mx) {
        double frac = (mx - trackX()) / (double) Math.max(1, trackW());
        frac = Math.max(0, Math.min(1, frac));
        setter.accept(snap(min + frac * (max - min)));
    }
}
