package dev.z1kzak.potionhud.screen.widget;

import dev.z1kzak.potionhud.config.Colors;
import dev.z1kzak.potionhud.render.Draw;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** iOS-style switch. Animates between the two states. */
public class ToggleOption extends Option {

    private static final int PILL_W = 26;
    private static final int PILL_H = 13;

    private final BooleanSupplier getter;
    private final Consumer<Boolean> setter;
    private float anim;
    private long lastFrame;

    public ToggleOption(Component label, BooleanSupplier getter, Consumer<Boolean> setter) {
        super(label);
        this.getter = getter;
        this.setter = setter;
        this.anim = getter.getAsBoolean() ? 1f : 0f;
    }

    private int pillX() {
        return x + w - PILL_W - 8;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt, boolean hovered) {
        Theme.row(g, x, y, w, h, hovered);
        boolean on = getter.getAsBoolean();

        long now = System.currentTimeMillis();
        float dt = lastFrame == 0 ? 1f : Math.min(0.1f, (now - lastFrame) / 1000f);
        lastFrame = now;
        float target = on ? 1f : 0f;
        anim += (target - anim) * Math.min(1f, dt * 12f);
        if (Math.abs(target - anim) < 0.01f) {
            anim = target;
        }

        drawLabel(g, on ? Theme.TEXT : Theme.TEXT_DIM);

        int px = pillX();
        int py = y + (h - PILL_H) / 2;
        int track = Colors.lerp(0x66000000, Theme.ACCENT, anim);
        Draw.rounded(g, px, py, PILL_W, PILL_H, PILL_H / 2, track);
        Draw.roundedOutline(g, px, py, PILL_W, PILL_H, PILL_H / 2, 0x22FFFFFF, 1);
        int knobD = PILL_H - 4;
        int knobX = Math.round(px + 2 + anim * (PILL_W - 4 - knobD));
        Draw.rounded(g, knobX, py + 2, knobD, knobD, knobD / 2, 0xFFF2F5FA);
    }

    @Override
    public boolean click(double mx, double my, int button) {
        if (button != 0 || !isOver(mx, my)) {
            return false;
        }
        setter.accept(!getter.getAsBoolean());
        return true;
    }
}
