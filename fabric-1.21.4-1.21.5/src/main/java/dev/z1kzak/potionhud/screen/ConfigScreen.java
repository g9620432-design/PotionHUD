package dev.z1kzak.potionhud.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Thin version wrapper: forwards the pre-1.21.9 input callbacks into the shared menu. */
public class ConfigScreen extends Screen implements HudScreen, ConfigMenu.Host {

    private final Screen parent;
    private final ConfigMenu menu = new ConfigMenu(this);

    public ConfigScreen(Screen parent) {
        super(Component.translatable("potionhudx.screen.config"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        menu.layout(this.width, this.height);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        menu.renderBackdrop(g);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        menu.render(g, mx, my, pt);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        return menu.click(mx, my, button) || super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        return menu.drag(mx, my) || super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        menu.release();
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        return menu.scroll(mx, my, sy) || super.mouseScrolled(mx, my, sx, sy);
    }

    @Override
    public boolean keyPressed(int key, int scancode, int modifiers) {
        return menu.key(key, modifiers) || super.keyPressed(key, scancode, modifiers);
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        return menu.chars(c) || super.charTyped(c, modifiers);
    }

    @Override
    public void onClose() {
        menu.closed();
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void close() {
        onClose();
    }

    @Override
    public void openPositionEditor() {
        if (minecraft != null) {
            minecraft.setScreen(new PositionScreen(this));
        }
    }
}
