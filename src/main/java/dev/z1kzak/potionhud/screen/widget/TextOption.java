package dev.z1kzak.potionhud.screen.widget;

import dev.z1kzak.potionhud.render.Draw;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

/** Small self-contained text field (no vanilla EditBox plumbing needed). */
public class TextOption extends Option {

    private String value;
    private final Consumer<String> onChange;
    private final int maxLength;
    private boolean focused;
    private int cursor;

    public TextOption(Component label, String initial, int maxLength, Consumer<String> onChange) {
        super(label);
        this.value = initial == null ? "" : initial;
        this.cursor = this.value.length();
        this.maxLength = maxLength;
        this.onChange = onChange;
    }

    public String value() {
        return value;
    }

    public void setValue(String v) {
        this.value = v == null ? "" : v;
        this.cursor = Math.min(cursor, value.length());
    }

    public boolean focused() {
        return focused;
    }

    public void setFocused(boolean f) {
        this.focused = f;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt, boolean hovered) {
        Theme.row(g, x, y, w, h, hovered);
        drawLabel(g, Theme.TEXT);

        int cx = controlX();
        int cw = controlW();
        Draw.rounded(g, cx, y + 3, cw, h - 6, 3, 0x66000000);
        Draw.roundedOutline(g, cx, y + 3, cw, h - 6, 3, focused ? Theme.ACCENT : 0x22FFFFFF, 1);
        String shown = Theme.clip(value, cw - 10);
        Theme.text(g, shown, cx + 5, y + (h - 8) / 2, Theme.TEXT);
        if (focused && (System.currentTimeMillis() / 500) % 2 == 0) {
            int cursorX = cx + 5 + Theme.font().width(shown.substring(0, Math.min(cursor, shown.length())));
            g.fill(cursorX, y + 5, cursorX + 1, y + h - 5, Theme.TEXT);
        }
    }

    @Override
    public boolean click(double mx, double my, int button) {
        boolean inside = isOver(mx, my) && mx >= controlX();
        focused = inside;
        return inside;
    }

    @Override
    public boolean charTyped(char c) {
        if (!focused || value.length() >= maxLength) {
            return false;
        }
        if (c < 32 || c == 127) {
            return false;
        }
        value = value.substring(0, cursor) + c + value.substring(cursor);
        cursor++;
        fire();
        return true;
    }

    @Override
    public boolean keyPressed(int key, int modifiers) {
        if (!focused) {
            return false;
        }
        switch (key) {
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (cursor > 0) {
                    value = value.substring(0, cursor - 1) + value.substring(cursor);
                    cursor--;
                    fire();
                }
                return true;
            }
            case GLFW.GLFW_KEY_DELETE -> {
                if (cursor < value.length()) {
                    value = value.substring(0, cursor) + value.substring(cursor + 1);
                    fire();
                }
                return true;
            }
            case GLFW.GLFW_KEY_LEFT -> {
                cursor = Math.max(0, cursor - 1);
                return true;
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                cursor = Math.min(value.length(), cursor + 1);
                return true;
            }
            case GLFW.GLFW_KEY_HOME -> {
                cursor = 0;
                return true;
            }
            case GLFW.GLFW_KEY_END -> {
                cursor = value.length();
                return true;
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                focused = false;
                fire();
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    private void fire() {
        if (onChange != null) {
            onChange.accept(value);
        }
    }
}
